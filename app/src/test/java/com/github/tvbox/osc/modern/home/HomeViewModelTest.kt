package com.github.tvbox.osc.modern.home

import com.github.tvbox.osc.modern.data.*
import com.github.tvbox.osc.modern.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val sources = FakeSources()
    private val library = FakeLibrary()
    private val settings = FakeSettings()
    private lateinit var model: HomeViewModel

    @Before fun prepare() {
        Dispatchers.setMain(dispatcher)
        model = HomeViewModel(sources, library, settings)
    }
    @After fun cleanup() { model.cancelRefresh(); Dispatchers.resetMain() }

    @Test fun emptyShellDoesNotRequestAnySourceOrShowStaleHero() = runTest(dispatcher) {
        sources.current = SourceSelection()
        library.history = listOf(item("old", "Old history"))
        model.refresh(); advanceUntilIdle()
        assertEquals(0, sources.calls)
        assertEquals(HomeLoad.Empty, model.state.value.load)
        assertNull(model.state.value.hero)
        assertEquals(1, model.state.value.recent.size)
    }

    @Test fun readyCatalogAndHistoryArePublishedAsOnePageState() = runTest(dispatcher) {
        sources.load = { catalog(it, "Recommended") }
        library.history = listOf(item("a", "Recent"))
        model.refresh(); advanceUntilIdle()
        assertEquals(HomeLoad.Ready, model.state.value.load)
        assertEquals("Recommended", model.state.value.hero!!.title)
        assertEquals("Recent", model.state.value.recent.single().title)
    }

    @Test fun emptyCatalogRemainsEmptyAndCanUseHistoryAsHero() = runTest(dispatcher) {
        sources.load = { SourceCatalog(it, emptyList(), emptyList()) }
        library.history = listOf(item("a", "Recent"))
        model.refresh(); advanceUntilIdle()
        assertEquals(HomeLoad.Empty, model.state.value.load)
        assertEquals("Recent", model.state.value.hero!!.title)
    }

    @Test fun failedSourceKeepsHistoryAndExposesFailureState() = runTest(dispatcher) {
        sources.load = { throw SourceUnavailable() }
        library.history = listOf(item("a", "Recent"))
        model.refresh(); advanceUntilIdle()
        assertEquals(HomeLoad.Failed, model.state.value.load)
        assertTrue(model.state.value.recommendations.isEmpty())
        assertEquals("Recent", model.state.value.recent.single().title)
    }

    @Test fun staleBlockingSourceCannotOverwriteNewSelection() = runTest(dispatcher) {
        val first = CompletableDeferred<SourceCatalog>()
        val second = CompletableDeferred<SourceCatalog>()
        sources.load = { key -> withContext(NonCancellable) { if (key == "a") first.await() else second.await() } }
        model.refresh(); runCurrent()
        sources.select("b"); model.refresh(); runCurrent()
        second.complete(catalog("b", "New source")); runCurrent()
        first.complete(catalog("a", "Stale source")); advanceUntilIdle()
        assertEquals("b", model.state.value.selection.selected!!.key)
        assertEquals("New source", model.state.value.hero!!.title)
    }

    @Test fun cancelRefreshPreventsLateResultPublication() = runTest(dispatcher) {
        val response = CompletableDeferred<SourceCatalog>()
        sources.load = { withContext(NonCancellable) { response.await() } }
        model.refresh(); runCurrent(); model.cancelRefresh()
        response.complete(catalog("a", "Cancelled result")); advanceUntilIdle()
        assertEquals(HomeLoad.Empty, model.state.value.load)
        assertTrue(model.state.value.recommendations.isEmpty())
    }

    @Test fun suppliedCatalogReusesExistingRequestInsteadOfFetchingTwice() = runTest(dispatcher) {
        model.refresh(catalog("a", "Existing request")); advanceUntilIdle()
        assertEquals(0, sources.calls)
        assertEquals("Existing request", model.state.value.hero!!.title)
    }

    @Test fun mismatchedCatalogIsRejected() = runTest(dispatcher) {
        sources.load = { catalog("other", "Wrong source") }
        model.refresh(); advanceUntilIdle()
        assertEquals(HomeLoad.Failed, model.state.value.load)
        assertTrue(model.state.value.recommendations.isEmpty())
    }

    private fun item(key: String, title: String) = ContentItem(ContentRef(key, title), title)
    private fun catalog(key: String, title: String) = SourceCatalog(key, emptyList(), listOf(item(key, title)))

    private class FakeSources : SourceAccess {
        var current = SourceSelection(listOf(SourceOption("a", "A")), SourceOption("a", "A"))
        var calls = 0
        var load: suspend (String) -> SourceCatalog = { SourceCatalog(it, emptyList(), emptyList()) }
        fun select(key: String) { current = SourceSelection(listOf(SourceOption(key, key)), SourceOption(key, key)) }
        override fun selection() = current
        override suspend fun catalog(sourceKey: String): SourceCatalog { calls++; return load(sourceKey) }
        override suspend fun search(sourceKey: String, query: String): List<ContentItem> = error("Not used")
        override suspend fun details(ref: ContentRef): ContentDetails = error("Not used")
        override suspend fun resolve(ref: ContentRef, flag: String, url: String, progressKey: String, subtitleKey: String): ResolvedPlayback = error("Not used")
    }
    private class FakeLibrary : LibraryStore {
        var history = emptyList<ContentItem>()
        override suspend fun recent() = history
        override suspend fun collections(): List<CollectionEntry> = emptyList()
        override suspend fun removeRecent(ref: ContentRef) { history = history.filterNot { it.ref == ref } }
        override suspend fun clearRecent() { history = emptyList() }
        override suspend fun setCollected(item: ContentItem, collected: Boolean) = Unit
        override suspend fun clearCollections() = Unit
    }
    private class FakeSettings : SettingsStore {
        private var value = HomePreferences()
        override fun homePreferences() = value
        override fun saveHomePreferences(value: HomePreferences) { this.value = value }
    }
}
