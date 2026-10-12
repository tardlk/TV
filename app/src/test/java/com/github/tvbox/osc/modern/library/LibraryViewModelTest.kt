package com.github.tvbox.osc.modern.library

import com.github.tvbox.osc.modern.data.*
import com.github.tvbox.osc.modern.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = Store()
    private val sources = Sources()
    private val settings = Settings()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }
    private fun model(mode: LibraryMode) = LibraryViewModel(mode, store, sources, settings)
    private val first = ContentItem(ContentRef("source", "same-id"), "First", note = "Source remarks")
    private val second = ContentItem(ContentRef("other", "same-id"), "Second")

    @Test fun historyKeepsOriginalCaptionAndFullPageLimit() = runTest(dispatcher) {
        store.history = listOf(HistoryEntry(first, "第3集"), HistoryEntry(second, ""))
        val model = model(LibraryMode.History); model.refresh(); advanceUntilIdle()
        assertEquals(100, store.limit)
        assertEquals("上次看到第3集", model.state.value.rows.first().item.note)
        assertEquals(second, model.state.value.rows.last().item)
    }
    @Test fun knownSourceOpensDetailWithCollectionFlag() = runTest(dispatcher) {
        val model = model(LibraryMode.Collection)
        model.open(LibraryRow(first, 12)); runCurrent()
        assertEquals(LibraryNavigation.Details(first, true), model.navigation.first())
    }
    @Test fun missingSourcesKeepDifferentHistoryAndCollectionSearchRoutes() = runTest(dispatcher) {
        sources.available = emptyList()
        val history = model(LibraryMode.History); history.open(LibraryRow(first)); runCurrent()
        assertEquals(LibraryNavigation.Search("First", true), history.navigation.first())
        val collection = model(LibraryMode.Collection); collection.open(LibraryRow(first)); runCurrent()
        assertEquals(LibraryNavigation.Search("First", false, true), collection.navigation.first())
        collection.search(LibraryRow(first)); runCurrent()
        assertEquals(LibraryNavigation.Search("First", true), collection.navigation.first())
    }
    @Test fun virtualSourceCanOpenDetailsWithoutAppearingInConfiguredSourceList() = runTest(dispatcher) {
        sources.available = emptyList(); sources.virtual = setOf("push_agent")
        val item = first.copy(ref = ContentRef("push_agent", "local-id"))
        val model = model(LibraryMode.History); model.open(LibraryRow(item)); runCurrent()
        assertEquals(LibraryNavigation.Details(item, false), model.navigation.first())
    }
    @Test fun deletionUsesSourceAndIdTogether() = runTest(dispatcher) {
        store.history = listOf(HistoryEntry(first, ""), HistoryEntry(second, ""))
        val model = model(LibraryMode.History); model.refresh(); advanceUntilIdle()
        model.toggleDelete(); model.open(model.state.value.rows.first()); advanceUntilIdle()
        assertEquals(listOf(second), model.state.value.rows.map { it.item })
        assertEquals(first.ref, store.removed)
    }
    @Test fun clearCollectionDoesNotClearHistory() = runTest(dispatcher) {
        store.history = listOf(HistoryEntry(first, "")); store.collected = listOf(CollectionEntry(1, first))
        val model = model(LibraryMode.Collection); model.refresh(); advanceUntilIdle(); model.clear(); advanceUntilIdle()
        assertTrue(model.state.value.rows.isEmpty())
        assertEquals(1, store.history.size)
    }
    @Test fun failedDeleteKeepsVisibleDataAndReportsFailure() = runTest(dispatcher) {
        store.history = listOf(HistoryEntry(first, "")); store.failDelete = true
        val model = model(LibraryMode.History); model.refresh(); advanceUntilIdle()
        model.toggleDelete(); model.open(model.state.value.rows.single()); advanceUntilIdle()
        assertEquals(LibraryLoad.Failed, model.state.value.load)
        assertEquals(1, model.state.value.rows.size)
        assertEquals(LibraryNavigation.OperationFailed, model.navigation.first())
    }
    @Test fun staleRefreshCannotResurrectDeletedRecord() = runTest(dispatcher) {
        val old = CompletableDeferred<List<HistoryEntry>>()
        var calls = 0
        store.history = listOf(HistoryEntry(first, ""))
        store.read = { if (calls++ == 0) withContext(NonCancellable) { old.await() } else store.history }
        val model = model(LibraryMode.History); model.refresh(); runCurrent()
        model.toggleDelete(); model.open(LibraryRow(first)); runCurrent()
        old.complete(listOf(HistoryEntry(first, ""))); advanceUntilIdle()
        assertTrue(model.state.value.rows.isEmpty())
    }

    private class Store : LibraryStore {
        var history = emptyList<HistoryEntry>()
        var collected = emptyList<CollectionEntry>()
        var removed: ContentRef? = null
        var failDelete = false
        var limit = 0
        var read: (suspend () -> List<HistoryEntry>)? = null
        override suspend fun recent(limit: Int): List<HistoryEntry> { this.limit = limit; return read?.invoke() ?: history }
        override suspend fun collections() = collected
        override suspend fun removeRecent(ref: ContentRef) {
            if (failDelete) error("Expected storage failure")
            removed = ref; history = history.filterNot { it.item.ref == ref }
        }
        override suspend fun clearRecent() { history = emptyList() }
        override suspend fun setCollected(item: ContentItem, collected: Boolean) {
            this.collected = this.collected.filterNot { it.item.ref == item.ref }
        }
        override suspend fun clearCollections() { collected = emptyList() }
    }
    private class Sources : SourceAccess {
        var available = listOf(SourceOption("source", "Source"))
        var virtual = emptySet<String>()
        override fun supports(sourceKey: String) = sourceKey in virtual || available.any { it.key == sourceKey }
        override fun selection() = SourceSelection(available, available.firstOrNull())
        override suspend fun catalog(sourceKey: String): SourceCatalog = error("Not used")
        override suspend fun search(sourceKey: String, query: String): List<ContentItem> = error("Not used")
        override suspend fun details(ref: ContentRef): ContentDetails = error("Not used")
        override suspend fun resolve(ref: ContentRef, flag: String, url: String, progressKey: String, subtitleKey: String): ResolvedPlayback = error("Not used")
    }
    private class Settings : SettingsStore {
        override fun homePreferences() = HomePreferences(fastSearch = true)
        override fun saveHomePreferences(value: HomePreferences) = Unit
    }
}
