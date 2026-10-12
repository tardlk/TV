package com.github.tvbox.osc.modern.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.os.Build
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.modern.data.LegacySourceAccess
import com.github.tvbox.osc.modern.data.SourceAccess
import com.github.tvbox.osc.modern.model.ContentItem
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

data class SearchSite(val key: String, val searchable: Boolean = true, val blocking: Boolean = true)
enum class SearchPhase { Idle, Loading, Content, Empty, Error }
data class SearchUiState(
    val query: String = "", val phase: SearchPhase = SearchPhase.Idle,
    val results: List<ContentItem> = emptyList(), val pending: Int = 0,
    val total: Int = 0, val timedOut: Int = 0, val failed: Int = 0,
    val paused: Boolean = false, val folderDepth: Int = 0, val selection: Int = 0,
)
enum class SearchNotice { NoSources }

object LegacySearchSites {
    @JvmStatic fun snapshot(): List<SearchSite> = ApiConfig.get().sourceBeanList.map {
        SearchSite(it.key.orEmpty(), it.isSearchable, it.type == 3)
    }
    @JvmStatic fun homeKey(): String = ApiConfig.get().homeSourceBean.key.orEmpty()
}

/** Ordinary search ownership. Native keyboard, TV adapters and detail/player remain the UI hosts. */
class SearchViewModel @JvmOverloads constructor(
    private val source: SourceAccess = LegacySourceAccess(),
    private val maxBlocking: Int = if (Build.VERSION.SDK_INT >= 35) 24 else if (Build.VERSION.SDK_INT >= 30) 18 else 12,
    private val siteTimeoutMs: Long = 15_000,
    private val advanceMs: Long = 3_000,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SearchUiState())
    val state = mutableState.asStateFlow()
    private val noticeChannel = Channel<SearchNotice>(Channel.BUFFERED)
    val notices = noticeChannel.receiveAsFlow()
    private var generation = 0
    private var requests: Job? = null
    private var folderRequest: Job? = null
    private var folderGeneration = 0
    private var sites = emptyList<SearchSite>()
    private val pending = linkedSetOf<String>()
    private val waiting = ArrayDeque<SearchSite>()
    private val active = mutableSetOf<String>()
    private val released = mutableSetOf<String>()
    private val prefixResults = mutableListOf<ContentItem>()
    private val fallbackResults = mutableListOf<ContentItem>()
    private var showPrefixes = false
    private data class FolderFrame(val results: List<ContentItem>, val selection: Int)
    private val folders = mutableListOf<FolderFrame>()

    fun search(query: String, available: List<SearchSite>, homeKey: String, checked: Set<String>?) {
        stopRequests()
        closeFolderRequest()
        folders.clear(); prefixResults.clear(); fallbackResults.clear(); showPrefixes = false
        sites = available.filter { it.searchable && (checked == null || it.key in checked) }
            .distinctBy { it.key }.sortedBy { if (it.key == homeKey) 0 else 1 }
        pending.clear(); pending.addAll(sites.map { it.key })
        mutableState.value = SearchUiState(query = query,
            phase = if (sites.isEmpty()) SearchPhase.Empty else SearchPhase.Loading,
            pending = sites.size, total = sites.size)
        if (sites.isEmpty()) noticeChannel.trySend(SearchNotice.NoSources) else launchPending(initial = true)
    }

    fun pause() {
        if (mutableState.value.query.isEmpty()) return
        stopRequests()
        mutableState.value = mutableState.value.copy(paused = pending.isNotEmpty())
    }

    fun resume() {
        if (!mutableState.value.paused || folders.isNotEmpty()) return
        mutableState.value = mutableState.value.copy(paused = false)
        launchPending(initial = false)
    }

    private fun stopRequests() {
        ++generation
        requests?.cancel(); requests = null
        waiting.clear(); active.clear(); released.clear()
    }

    private fun launchPending(initial: Boolean) {
        val request = ++generation
        val parent = SupervisorJob(viewModelScope.coroutineContext[Job])
        requests = parent
        val scope = CoroutineScope(viewModelScope.coroutineContext + parent)
        val remaining = sites.filter { it.key in pending }
        // The ordinary legacy page starts all HTTP sites immediately, then six spider calls.
        // On resume it restarts only unfinished sites through the bounded batch path.
        waiting.addAll(if (initial) remaining.filter { it.blocking } else remaining)
        if (initial) remaining.filterNot { it.blocking }.forEach { startSite(it, request, scope) }
        repeat(minOf(6, maxBlocking)) { startNext(request, scope) }
    }

    private fun startNext(request: Int, scope: CoroutineScope) {
        if (request != generation || active.size >= maxBlocking || waiting.isEmpty()) return
        val site = waiting.removeFirst()
        startSite(site, request, scope)
    }

    private fun startSite(site: SearchSite, request: Int, scope: CoroutineScope) {
        if (site.key !in pending) return
        if (site.blocking) active.add(site.key)
        scope.launch {
            if (site.blocking) {
                launch {
                    delay(advanceMs)
                    if (request == generation && site.key in pending && released.add(site.key)) startNext(request, scope)
                }
            }
            try {
                val data = withTimeout(siteTimeoutMs) { source.search(site.key, mutableState.value.query) }
                if (request == generation) finishSite(site, data.filter { it.ref.sourceKey == site.key }, false, false)
            } catch (timeout: TimeoutCancellationException) {
                if (request == generation) finishSite(site, emptyList(), true, false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) {
                if (request == generation) finishSite(site, emptyList(), false, true)
            } finally {
                if (request == generation) {
                    active.remove(site.key)
                    // A completed worker frees capacity even if its 3s advance was already consumed.
                    startNext(request, scope)
                }
            }
        }
    }

    private fun finishSite(site: SearchSite, data: List<ContentItem>, timeout: Boolean, failed: Boolean) {
        if (!pending.remove(site.key)) return
        val before = mutableState.value
        val prefix = data.filter { highMatch(it.title, before.query) }
        prefixResults.addAll(prefix); fallbackResults.addAll(prefix)
        var results = before.results + if (showPrefixes) prefix else data.filter { it.title.trim() == before.query.trim() }
        // Keep the ordinary page's threshold: only an accepted response switches to prefixes.
        if (!timeout && !failed && !showPrefixes && results.isEmpty() &&
            before.total - pending.size >= minOf(6, before.total)) {
            showPrefixes = true
            results = prefixResults.toList()
        }
        val failures = before.failed + if (failed) 1 else 0
        val timeouts = before.timedOut + if (timeout) 1 else 0
        mutableState.value = before.copy(results = results.toList(), pending = pending.size,
            failed = failures, timedOut = timeouts,
            phase = when {
                results.isNotEmpty() -> SearchPhase.Content
                pending.isNotEmpty() -> SearchPhase.Loading
                failures + timeouts == before.total -> SearchPhase.Error
                else -> SearchPhase.Empty
            })
    }

    fun select(position: Int) { mutableState.value = mutableState.value.copy(selection = position.coerceAtLeast(0)) }

    fun openFolder(item: ContentItem, position: Int) {
        pause(); closeFolderRequest()
        val before = mutableState.value
        folders.add(FolderFrame(before.results, position))
        val request = folderGeneration
        mutableState.value = before.copy(phase = SearchPhase.Loading, results = emptyList(),
            folderDepth = folders.size, selection = 0)
        folderRequest = viewModelScope.launch {
            try {
                val data = withTimeout(siteTimeoutMs) { source.folder(item.ref) }
                if (request == folderGeneration) mutableState.value = mutableState.value.copy(
                    results = data.toList(), phase = if (data.isEmpty()) SearchPhase.Empty else SearchPhase.Content)
            } catch (cancelled: CancellationException) {
                if (cancelled is TimeoutCancellationException && request == folderGeneration)
                    mutableState.value = mutableState.value.copy(phase = SearchPhase.Error)
                else throw cancelled
            } catch (failure: Exception) {
                if (request == folderGeneration) mutableState.value = mutableState.value.copy(phase = SearchPhase.Error)
            }
        }
    }

    fun backFolder(): Boolean {
        if (folders.isEmpty()) return false
        closeFolderRequest()
        val frame = folders.removeAt(folders.lastIndex)
        mutableState.value = mutableState.value.copy(results = frame.results, selection = frame.selection,
            folderDepth = folders.size, phase = if (frame.results.isEmpty()) SearchPhase.Empty else SearchPhase.Content)
        return true
    }

    fun fallbackCandidates(selected: ContentItem): List<ContentItem> = fallbackResults
        .filter { it.ref.id.isNotEmpty() && it.title.trim() == selected.title.trim() }
        .distinctBy { it.ref }.take(20)

    private fun closeFolderRequest() { ++folderGeneration; folderRequest?.cancel(); folderRequest = null }
    override fun onCleared() { stopRequests(); closeFolderRequest() }

    private fun highMatch(title: String, query: String): Boolean = title.isNotEmpty() && query.isNotEmpty() &&
        title.replace(Regex("\\s+"), "").startsWith(query.replace(Regex("\\s+"), ""))
}
