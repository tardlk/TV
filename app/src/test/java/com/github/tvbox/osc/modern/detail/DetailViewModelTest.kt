package com.github.tvbox.osc.modern.detail

import com.github.tvbox.osc.modern.data.LibraryStore
import com.github.tvbox.osc.modern.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = Store()
    private val first = ContentItem(ContentRef("first", "same-id"), "First", poster = "poster")
    private val second = first.copy(ref = ContentRef("second", "same-id"), title = "Second")
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private fun present(model: DetailViewModel, item: ContentItem = first) {
        model.present(item, DetailInfo(title = item.title))
    }

    @Test fun collectionCannotBeWrittenUntilBothContentAndSavedStateAreKnown() = runTest(dispatcher) {
        val model = DetailViewModel(store)
        model.begin(first.ref); model.toggleCollected(); runCurrent()
        assertEquals(0, store.writes.size)
        present(model); model.toggleCollected(); advanceUntilIdle()
        assertEquals(listOf(first to true), store.writes)
    }
    @Test fun rapidConfirmationWritesOnceAndReportsCommittedState() = runTest(dispatcher) {
        val model = DetailViewModel(store); present(model); advanceUntilIdle()
        model.toggleCollected(); model.toggleCollected(); advanceUntilIdle()
        assertEquals(listOf(first to true), store.writes)
        assertTrue(model.state.value.collected)
        assertEquals(DetailNotice.Added, model.notices.first())
        model.toggleCollected(); advanceUntilIdle()
        assertEquals(first to false, store.writes.last())
        assertEquals(DetailNotice.Removed, model.notices.first())
    }
    @Test fun staleCollectionReadDoesNotOverwriteNewSourceWithSameId() = runTest(dispatcher) {
        val old = CompletableDeferred<Boolean>()
        store.read = { ref -> if (ref == first.ref) withContext(NonCancellable) { old.await() } else false }
        val model = DetailViewModel(store); present(model); runCurrent()
        present(model, second); runCurrent(); old.complete(true); advanceUntilIdle()
        assertEquals(second, model.state.value.item)
        assertFalse(model.state.value.collected)
    }
    @Test fun pendingWriteCompletesForOriginalItemWithoutRelabelingNewItem() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>(); store.writeGate = gate
        val model = DetailViewModel(store); present(model); advanceUntilIdle()
        model.toggleCollected(); runCurrent(); present(model, second); runCurrent()
        gate.complete(Unit); advanceUntilIdle()
        assertEquals(listOf(first to true), store.writes)
        assertEquals(second, model.state.value.item)
        assertFalse(model.state.value.collected)
        assertFalse(model.state.value.collectionBusy)
    }
    @Test fun returningToSameItemWaitsForPendingWriteBeforeReadingStorage() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>(); store.writeGate = gate
        val model = DetailViewModel(store); present(model); advanceUntilIdle()
        model.toggleCollected(); runCurrent(); model.begin(first.ref); present(model); runCurrent()
        gate.complete(Unit); advanceUntilIdle()
        assertTrue(model.state.value.collected)
    }
    @Test fun failedWriteDoesNotDisplayAnUnpersistedCollection() = runTest(dispatcher) {
        store.failWrite = true
        val model = DetailViewModel(store); present(model); advanceUntilIdle()
        model.toggleCollected(); advanceUntilIdle()
        assertFalse(model.state.value.collected)
        assertFalse(model.state.value.collectionBusy)
        assertEquals(DetailNotice.Failed, model.notices.first())
    }
    private class Store : LibraryStore {
        val saved = mutableSetOf<ContentRef>()
        val writes = mutableListOf<Pair<ContentItem, Boolean>>()
        var read: (suspend (ContentRef) -> Boolean)? = null
        var writeGate: CompletableDeferred<Unit>? = null
        var failWrite = false
        override suspend fun isCollected(ref: ContentRef) = read?.invoke(ref) ?: (ref in saved)
        override suspend fun setCollected(item: ContentItem, collected: Boolean) {
            writeGate?.await()
            if (failWrite) error("Expected storage failure")
            writes += item to collected
            if (collected) saved.add(item.ref) else saved.remove(item.ref)
        }
        override suspend fun recent(limit: Int): List<HistoryEntry> = error("Not used")
        override suspend fun collections(): List<CollectionEntry> = error("Not used")
        override suspend fun removeRecent(ref: ContentRef) = error("Not used")
        override suspend fun clearRecent() = error("Not used")
        override suspend fun clearCollections() = error("Not used")
    }
}
