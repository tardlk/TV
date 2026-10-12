package com.github.tvbox.osc.modern.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tvbox.osc.modern.data.*
import com.github.tvbox.osc.modern.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class LibraryMode { History, Collection }
enum class LibraryLoad { Loading, Ready, Failed }
data class LibraryRow(val item: ContentItem, val recordId: Int = 0)
data class LibraryUiState(val mode: LibraryMode, val rows: List<LibraryRow> = emptyList(),
                          val deleteMode: Boolean = false, val load: LibraryLoad = LibraryLoad.Loading)
sealed interface LibraryNavigation {
    data object OperationFailed : LibraryNavigation
    data class Details(val item: ContentItem, val fromCollection: Boolean) : LibraryNavigation
    data class Search(val title: String, val fast: Boolean, val clearTop: Boolean = false) : LibraryNavigation
}

class LibraryViewModel(private val mode: LibraryMode, private val store: LibraryStore,
                       private val sources: SourceAccess, private val settings: SettingsStore) : ViewModel() {
    private val mutableState = MutableStateFlow(LibraryUiState(mode))
    val state = mutableState.asStateFlow()
    private val navigationChannel = Channel<LibraryNavigation>(Channel.BUFFERED)
    val navigation = navigationChannel.receiveAsFlow()
    private val writes = Mutex()
    private var refreshJob: Job? = null
    private var generation = 0

    fun refresh() {
        val current = ++generation
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                val rows = if (mode == LibraryMode.History) store.recent(100).map { entry ->
                    LibraryRow(entry.item.copy(note = if (entry.playedNote.isEmpty()) entry.item.note else "上次看到${entry.playedNote}"))
                } else store.collections().map { LibraryRow(it.item, it.recordId) }
                if (current == generation) mutableState.value = mutableState.value.copy(rows = rows, load = LibraryLoad.Ready)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { if (current == generation) mutableState.value = mutableState.value.copy(load = LibraryLoad.Failed) }
        }
    }

    fun toggleDelete() { mutableState.value = mutableState.value.copy(deleteMode = !mutableState.value.deleteMode) }
    fun open(row: LibraryRow) {
        if (mutableState.value.deleteMode) {
            mutate { if (mode == LibraryMode.History) store.removeRecent(row.item.ref) else store.setCollected(row.item, false) }
            return
        }
        val available = sources.supports(row.item.ref.sourceKey)
        val action = if (available) LibraryNavigation.Details(row.item, mode == LibraryMode.Collection)
            else LibraryNavigation.Search(row.item.title, mode == LibraryMode.History && settings.homePreferences().fastSearch,
                clearTop = mode == LibraryMode.Collection)
        viewModelScope.launch { navigationChannel.send(action) }
    }
    fun search(row: LibraryRow) { viewModelScope.launch {
        navigationChannel.send(LibraryNavigation.Search(row.item.title, settings.homePreferences().fastSearch))
    } }
    fun clear() { mutate { if (mode == LibraryMode.History) store.clearRecent() else store.clearCollections() } }
    private fun mutate(operation: suspend () -> Unit) { viewModelScope.launch {
        try { writes.withLock { operation() }; refresh() }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) {
            mutableState.value = mutableState.value.copy(load = LibraryLoad.Failed)
            navigationChannel.send(LibraryNavigation.OperationFailed)
        }
    } }
}
