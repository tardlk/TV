package com.github.tvbox.osc.modern.search

import com.github.tvbox.osc.modern.data.SourceAccess
import com.github.tvbox.osc.modern.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val source = Source()
    private fun item(key: String, title: String = "TV", id: String = key) = ContentItem(ContentRef(key, id), title)
    private fun sites(count: Int) = (1..count).map { SearchSite("s$it") }
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun onlySearchableCheckedSourcesRunAndHomeHasFirstPriority() = runTest(dispatcher) {
        val model = SearchViewModel(source)
        model.search("TV", listOf(SearchSite("other"), SearchSite("home"), SearchSite("disabled", false)),
            "home", setOf("home", "disabled"))
        advanceUntilIdle()
        assertEquals(listOf("home" to "TV"), source.calls)
        assertEquals(SearchPhase.Empty, model.state.value.phase)
        model.search("TV", listOf(SearchSite("other"), SearchSite("home")), "home", null)
        advanceUntilIdle()
        assertEquals(listOf("home", "other"), source.calls.takeLast(2).map { it.first })
    }

    @Test fun emptyExplicitSelectionDoesNotFallBackToAllSources() = runTest(dispatcher) {
        val model = SearchViewModel(source)
        model.search("TV", sites(2), "s1", emptySet()); advanceUntilIdle()
        assertTrue(source.calls.isEmpty())
        assertEquals(SearchPhase.Empty, model.state.value.phase)
        assertEquals(0, model.state.value.pending)
    }

    @Test fun httpStartsImmediatelySpidersAdvanceAfterThreeSecondsAndStayBounded() = runTest(dispatcher) {
        source.search = { _, _ -> awaitCancellation() }
        val model = SearchViewModel(source, maxBlocking = 12)
        model.search("TV", sites(20) + SearchSite("http", blocking = false), "s1", null)
        runCurrent()
        assertEquals(7, source.calls.size)
        assertEquals("http", source.calls.first().first)
        advanceTimeBy(3_000); runCurrent()
        assertEquals(13, source.calls.size)
        advanceTimeBy(3_000); runCurrent()
        assertEquals(13, source.calls.size)
        model.pause(); advanceUntilIdle()
    }

    @Test fun prefixesAppearOnlyAtOrdinarySearchCompletionThreshold() = runTest(dispatcher) {
        val gates = (1..7).associate { "s$it" to CompletableDeferred<List<ContentItem>>() }
        source.search = { key, _ -> gates.getValue(key).await() }
        val model = SearchViewModel(source)
        model.search("T V", sites(7), "s1", null); runCurrent()
        gates.getValue("s1").complete(listOf(item("s1", " TV 第2季"))); runCurrent()
        assertTrue(model.state.value.results.isEmpty())
        (2..5).forEach { gates.getValue("s$it").complete(emptyList()); runCurrent() }
        assertTrue(model.state.value.results.isEmpty())
        gates.getValue("s6").complete(emptyList()); runCurrent()
        assertEquals(listOf(item("s1", " TV 第2季")), model.state.value.results)
        gates.getValue("s7").complete(listOf(item("s7", "TV 第3季"))); advanceUntilIdle()
        assertEquals(2, model.state.value.results.size)
    }

    @Test fun exactResultKeepsPrefixesHiddenAndFallbackKeepsSourceIdentity() = runTest(dispatcher) {
        source.search = { key, _ -> listOf(item(key, " TV ", "shared"), item(key, "TV 2")) }
        val model = SearchViewModel(source)
        model.search("TV", sites(2), "s1", null); advanceUntilIdle()
        assertEquals(listOf("s1", "s2"), model.state.value.results.map { it.ref.sourceKey })
        assertEquals(2, model.fallbackCandidates(item("s1")).size)
        assertTrue(model.state.value.results.all { it.title == " TV " })
    }

    @Test fun sourceReturningAnotherSourcesIdentityIsRejected() = runTest(dispatcher) {
        source.search = { _, _ -> listOf(item("wrong")) }
        val model = SearchViewModel(source)
        model.search("TV", sites(1), "s1", null); advanceUntilIdle()
        assertEquals(SearchPhase.Empty, model.state.value.phase)
    }

    @Test fun timeoutAndFailureFinishWithoutWaitingForever() = runTest(dispatcher) {
        source.search = { key, _ -> if (key == "s1") awaitCancellation() else error("fixture") }
        val model = SearchViewModel(source)
        model.search("TV", sites(2), "s1", null); advanceUntilIdle()
        assertEquals(SearchPhase.Error, model.state.value.phase)
        assertEquals(0, model.state.value.pending)
        assertEquals(1, model.state.value.timedOut)
        assertEquals(1, model.state.value.failed)
    }

    @Test fun pauseRetainsResultsAndResumeOnlyRestartsUnfinishedSites() = runTest(dispatcher) {
        val gate = CompletableDeferred<List<ContentItem>>()
        source.search = { key, _ -> if (key == "s1") listOf(item(key)) else gate.await() }
        val model = SearchViewModel(source)
        model.search("TV", sites(2), "s1", null); runCurrent()
        model.pause(); runCurrent()
        assertTrue(model.state.value.paused)
        assertEquals(listOf(item("s1")), model.state.value.results)
        model.resume(); runCurrent()
        assertEquals(listOf("s1", "s2", "s2"), source.calls.map { it.first })
        gate.complete(listOf(item("s2"))); advanceUntilIdle()
        assertEquals(2, model.state.value.results.size)
    }

    @Test fun cancelledBlockingResultCannotReplaceANewQuery() = runTest(dispatcher) {
        val late = CompletableDeferred<List<ContentItem>>()
        source.search = { key, query -> if (query == "Old") withContext(NonCancellable) { late.await() }
            else listOf(item(key, query)) }
        val model = SearchViewModel(source)
        model.search("Old", sites(1), "s1", null); runCurrent()
        model.search("New", sites(1), "s1", null); runCurrent()
        late.complete(listOf(item("s1", "Old"))); advanceUntilIdle()
        assertEquals(listOf(item("s1", "New")), model.state.value.results)
    }

    @Test fun folderReturnRestoresSnapshotAndRejectsLateFolderResponse() = runTest(dispatcher) {
        source.search = { key, _ -> listOf(item(key), item(key, id = "folder").copy(tag = "folder", action = "original-action")) }
        val late = CompletableDeferred<List<ContentItem>>()
        source.folder = { withContext(NonCancellable) { late.await() } }
        val model = SearchViewModel(source)
        model.search("TV", sites(1), "s1", null); advanceUntilIdle()
        val original = model.state.value.results
        model.openFolder(original[1], 1); runCurrent()
        assertEquals(1, model.state.value.folderDepth)
        assertTrue(model.backFolder())
        late.complete(listOf(item("s1", "late"))); advanceUntilIdle()
        assertEquals(original, model.state.value.results)
        assertEquals(1, model.state.value.selection)
        assertEquals("original-action", model.state.value.results[1].action)
        assertFalse(model.backFolder())
    }

    @Test fun returningFromFolderWhileSearchIsPausedDoesNotStartItEarly() = runTest(dispatcher) {
        source.search = { key, _ -> if (key == "s1") listOf(item(key).copy(tag = "folder")) else awaitCancellation() }
        source.folder = { listOf(item("s1", "child")) }
        val model = SearchViewModel(source)
        model.search("TV", sites(2), "s1", null); runCurrent()
        model.openFolder(model.state.value.results[0], 0); runCurrent()
        model.resume(); runCurrent()
        assertEquals(2, source.calls.size)
        assertEquals("child", model.state.value.results.single().title)
        assertTrue(model.backFolder()); advanceUntilIdle()
        assertTrue(model.state.value.paused)
    }

    private class Source : SourceAccess {
        val calls = mutableListOf<Pair<String, String>>()
        var search: suspend (String, String) -> List<ContentItem> = { _, _ -> emptyList() }
        var folder: suspend (ContentRef) -> List<ContentItem> = { emptyList() }
        override suspend fun search(sourceKey: String, query: String): List<ContentItem> {
            calls += sourceKey to query
            return search.invoke(sourceKey, query)
        }
        override suspend fun folder(ref: ContentRef) = folder.invoke(ref)
        override fun selection() = SourceSelection()
        override suspend fun catalog(sourceKey: String): SourceCatalog = error("unused")
        override suspend fun details(ref: ContentRef): ContentDetails = error("unused")
        override suspend fun resolve(ref: ContentRef, flag: String, url: String, progressKey: String, subtitleKey: String): ResolvedPlayback = error("unused")
    }
}
