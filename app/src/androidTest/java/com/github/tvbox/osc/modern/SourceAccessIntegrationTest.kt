package com.github.tvbox.osc.modern

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.modern.data.*
import com.github.tvbox.osc.modern.model.*
import com.github.tvbox.osc.util.HawkConfig
import com.orhanobut.hawk.Hawk
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class SourceAccessIntegrationTest {
    @Test fun adapterUsesOriginalConfigAndSourceRequestPipeline() = runBlocking {
        val apiWasPresent = Hawk.contains(HawkConfig.API_URL)
        val oldApi = Hawk.get(HawkConfig.API_URL, "")
        val homeWasPresent = Hawk.contains(HawkConfig.HOME_API)
        val oldHome = Hawk.get(HawkConfig.HOME_API, "")
        var base = ""
        var stage = "config"
        val requests = java.util.concurrent.CopyOnWriteArrayList<String>()
        val server = object : NanoHTTPD("127.0.0.1", 0) {
            override fun serve(session: IHTTPSession): Response {
                requests.add(session.uri + "?" + session.queryParameterString.orEmpty())
                val title = if (session.parameters["wd"]?.firstOrNull() == "Other") "Other fixture" else "Adapter fixture"
                val body = if (session.uri == "/config.json") JSONObject()
                    .put("spider", "").put("sites", JSONArray().put(JSONObject()
                        .put("key", "modern-fixture").put("name", "Modern fixture").put("type", 1)
                        .put("api", base + "api").put("searchable", 1)))
                    .put("lives", JSONArray()).put("parses", JSONArray())
                else JSONObject().put("class", JSONArray().put(JSONObject().put("type_id", "1").put("type_name", "Fixture")))
                    .put("list", JSONArray().put(JSONObject().put("vod_id", "fixture-id").put("vod_name", title)
                        .put("vod_play_from", "direct").put("vod_play_url", "Episode\$${base}clip.mp4")))
                return newFixedLengthResponse(Response.Status.OK, "application/json", body.toString())
            }
        }
        server.start(); base = "http://127.0.0.1:${server.listeningPort}/"
        try {
            Hawk.put(HawkConfig.API_URL, base + "config.json")
            val loaded = CompletableDeferred<Unit>()
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                ApiConfig.get().loadConfig(false, object : ApiConfig.LoadConfigCallback {
                    override fun success() { loaded.complete(Unit) }
                    override fun notice(message: String) = Unit
                    override fun error(message: String) { loaded.completeExceptionally(AssertionError(message)) }
                }, null)
            }
            withTimeout(10_000) { loaded.await() }
            val source = LegacySourceAccess()
            assertEquals("modern-fixture", source.selection().selected!!.key)
            assertTrue(source.supports("push_agent"))
            assertFalse(source.selection().sources.any { it.key == "push_agent" })
            stage = "catalog"
            val home = withTimeout(3_000) { source.catalog("modern-fixture") }
            assertEquals("Adapter fixture", home.recommendations.single().title)
            assertEquals("Fixture", home.categories.single().name)
            stage = "search"
            val results = withTimeout(3_000) { source.search("modern-fixture", "Adapter") }
            assertEquals("fixture-id", results.single().ref.id)
            stage = "concurrent-search"
            val first = async { source.search("modern-fixture", "Adapter") }
            val second = async { source.search("modern-fixture", "Other") }
            withTimeout(3_000) {
                assertEquals("Adapter fixture", first.await().single().title)
                assertEquals("Other fixture", second.await().single().title)
            }
            stage = "details"
            val details = withTimeout(3_000) { source.details(results.single().ref) }
            assertEquals(base + "clip.mp4", details.lines.single().episodes.single().url)
            stage = "resolve"
            val resolved = withTimeout(3_000) { source.resolve(details.item.ref, "direct", base + "clip.mp4", "fixture-progress", "fixture-subtitle") }
            assertEquals(base + "clip.mp4", JSONObject(resolved.responseJson).getString("url"))
        } catch (failure: Throwable) {
            throw AssertionError("Source stage=$stage requests=$requests", failure)
        } finally {
            server.stop()
            if (apiWasPresent) Hawk.put(HawkConfig.API_URL, oldApi) else Hawk.delete(HawkConfig.API_URL)
            if (homeWasPresent) Hawk.put(HawkConfig.HOME_API, oldHome) else Hawk.delete(HawkConfig.HOME_API)
        }
    }

    @Test fun collectionAdapterKeepsSourceAndContentIdentity() = runBlocking {
        val store = LegacyLibraryStore()
        val item = ContentItem(ContentRef("modern-fixture", "collection-fixture"), "Collection fixture")
        try {
            store.setCollected(item, true)
            assertEquals(item, store.collections().single { it.item.ref == item.ref }.item)
            store.setCollected(item, false)
            assertFalse(store.collections().any { it.item.ref == item.ref })
        } finally { store.setCollected(item, false) }
    }
}
