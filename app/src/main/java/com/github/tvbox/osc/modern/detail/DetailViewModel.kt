package com.github.tvbox.osc.modern.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.tvbox.osc.modern.data.LegacyLibraryStore
import com.github.tvbox.osc.modern.data.LibraryStore
import com.github.tvbox.osc.modern.model.ContentItem
import com.github.tvbox.osc.modern.model.ContentRef
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class DetailInfo(val title: String = "", val source: String = "", val year: String = "",
    val area: String = "", val language: String = "", val type: String = "", val actor: String = "",
    val director: String = "", val description: String = "")
data class DetailUiState(val ref: ContentRef? = null, val item: ContentItem? = null,
    val info: DetailInfo = DetailInfo(), val collected: Boolean = false,
    val collectionKnown: Boolean = false, val collectionBusy: Boolean = false)
enum class DetailNotice { Added, Removed, Failed }

/** Presentation and collection ownership. The existing detail/fallback and playback chain supplies one accepted snapshot. */
class DetailViewModel @JvmOverloads constructor(private val library: LibraryStore = LegacyLibraryStore()) : ViewModel() {
    private val mutableState = MutableStateFlow(DetailUiState())
    val state = mutableState.asStateFlow()
    private val noticesChannel = Channel<DetailNotice>(Channel.BUFFERED)
    val notices = noticesChannel.receiveAsFlow()
    private val collectionOperations = Mutex()
    private var generation = 0
    private var collectionRead: Job? = null

    fun begin(ref: ContentRef) {
        val request = ++generation
        collectionRead?.cancel()
        mutableState.value = DetailUiState(ref = ref, collectionBusy = true)
        collectionRead = viewModelScope.launch {
            try {
                val collected = collectionOperations.withLock { library.isCollected(ref) }
                if (request == generation) mutableState.value = mutableState.value.copy(
                    collected = collected, collectionKnown = true, collectionBusy = false)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { if (request == generation) {
                mutableState.value = mutableState.value.copy(collectionBusy = false)
                noticesChannel.send(DetailNotice.Failed)
            } }
        }
    }

    fun present(item: ContentItem, info: DetailInfo) {
        if (item.ref != mutableState.value.ref) begin(item.ref)
        mutableState.value = mutableState.value.copy(item = item, info = info)
    }

    fun toggleCollected() {
        val before = mutableState.value
        val item = before.item ?: return
        if (!before.collectionKnown || before.collectionBusy) return
        val request = generation
        val desired = !before.collected
        mutableState.value = before.copy(collectionBusy = true)
        viewModelScope.launch {
            try {
                collectionOperations.withLock { library.setCollected(item, desired) }
                if (request == generation) {
                    mutableState.value = mutableState.value.copy(collected = desired, collectionBusy = false)
                    noticesChannel.send(if (desired) DetailNotice.Added else DetailNotice.Removed)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { if (request == generation) {
                mutableState.value = mutableState.value.copy(collectionBusy = false)
                noticesChannel.send(DetailNotice.Failed)
            } }
        }
    }
}
