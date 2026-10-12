package com.github.tvbox.osc.modern

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.github.tvbox.osc.api.ApiConfig
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.event.ServerEvent
import com.github.tvbox.osc.ui.activity.DetailActivity
import com.github.tvbox.osc.ui.activity.FastSearchActivity
import com.github.tvbox.osc.ui.activity.SearchActivity
import com.github.tvbox.osc.ui.adapter.PinyinAdapter
import com.github.tvbox.osc.ui.adapter.SearchAdapter
import com.github.tvbox.osc.util.HawkConfig
import com.owen.tvrecyclerview.widget.TvRecyclerView
import com.orhanobut.hawk.Hawk
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.greenrobot.eventbus.EventBus
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(AndroidJUnit4::class)
class SearchScreenTest {
    @Test fun ordinarySearchKeepsNativeKeyboardFolderAndDetailNavigation() = fixture(false) { context ->
        val search = context.launch(SearchActivity::class.java)
        val input = search.findViewById<EditText>(context.id("etSearch"))
        val keyboard = search.findViewById<ViewGroup>(context.id("keyBoardRoot"))
        assertTrue(keyboard.javaClass.name.endsWith("SearchKeyboard"))
        val a = context.textView(keyboard, "A")!!
        context.instrumentation.waitForIdleSync(); SystemClock.sleep(400)
        context.main { context.focusableParent(a).requestFocus() }; context.key(KeyEvent.KEYCODE_DPAD_CENTER)
        assertEquals("A", input.text.toString())
        val delete = context.textView(keyboard, "删除")!!
        context.main { context.focusableParent(delete).requestFocus() }; context.key(KeyEvent.KEYCODE_DPAD_CENTER)
        assertEquals("", input.text.toString())

        // Exercise remote input through the original ServerEvent entry point.
        context.main { EventBus.getDefault().post(ServerEvent(ServerEvent.SERVER_SEARCH, TITLE)) }
        val grid = search.findViewById<TvRecyclerView>(context.id("mGridView"))
        val adapter = grid.adapter as SearchAdapter
        context.waitFor("Ordinary search receives both exact results") { adapter.data.size == 2 }
        assertTrue(adapter.data.all { it.name == TITLE })
        assertEquals("fixture-action", adapter.data.last().action)
        context.main {
            (search.findViewById<TvRecyclerView>(context.id("mGridViewWord")).adapter as PinyinAdapter)
                .setNewData(listOf("固定热词"))
            search.findViewById<TextView>(context.id("wordSwitch")).text = "热词 | 历史"
            search.findViewById<View>(context.id("tvSearch")).requestFocus()
        }
        context.capture("results.png")

        context.focusResult(grid, 1); context.key(KeyEvent.KEYCODE_DPAD_CENTER)
        context.waitFor("Original folder request displays its child") { adapter.data.singleOrNull()?.id == "child" }
        context.capture("folder.png")
        context.key(KeyEvent.KEYCODE_BACK)
        context.waitFor("Back restores the original search list") { adapter.data.size == 2 }
        if (context.candidate) context.waitFor("Folder return restores the selected result") {
            grid.findViewHolderForAdapterPosition(1)?.itemView?.hasFocus() == true
        }

        context.focusResult(grid, 0)
        val monitor = context.instrumentation.addMonitor(DetailActivity::class.java.name, null, false)
        context.key(KeyEvent.KEYCODE_DPAD_CENTER)
        val detail = context.instrumentation.waitForMonitorWithTimeout(monitor, 5_000)
        context.instrumentation.removeMonitor(monitor)
        assertNotNull("Confirm opens original DetailActivity", detail)
        context.activities.add(detail!!)
        context.waitFor("Original detail chain requests the selected video") {
            context.requests.any { it.contains("ids=video") }
        }
        context.main { detail.finish() }
        context.waitFor("Search resumes") { search.hasWindowFocus() }
        if (context.candidate) context.waitFor("Detail return restores result focus") { grid.hasFocus() }
        assertEquals(2, adapter.data.size)

        if (context.candidate) {
            val searchesBefore = context.requests.count { it.contains("wd=") }
            context.main { search.recreate() }
            var recreated: Activity? = null
            context.waitFor("Search Activity rebuilds") {
                context.main {
                    recreated = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                        .firstOrNull { it is SearchActivity && it !== search }
                }
                recreated != null
            }
            context.activities.add(recreated!!)
            val newGrid = recreated!!.findViewById<TvRecyclerView>(context.id("mGridView"))
            context.waitFor("Retained ViewModel repopulates the native result host") {
                (newGrid.adapter as SearchAdapter).data.size == 2
            }
            assertEquals(TITLE, recreated!!.findViewById<EditText>(context.id("etSearch")).text.toString())
            assertEquals("Rebuild does not issue a second search", searchesBefore,
                context.requests.count { it.contains("wd=") })
        }
    }

    @Test fun aggregateModeStillRoutesToOriginalFastSearchPage() = fixture(true) { context ->
        val search = context.launch(SearchActivity::class.java)
        context.main { search.findViewById<EditText>(context.id("etSearch")).setText(TITLE) }
        val monitor = context.instrumentation.addMonitor(FastSearchActivity::class.java.name, null, false)
        context.main { search.findViewById<View>(context.id("tvSearch")).requestFocus() }
        context.key(KeyEvent.KEYCODE_DPAD_CENTER)
        val fast = context.instrumentation.waitForMonitorWithTimeout(monitor, 5_000)
        context.instrumentation.removeMonitor(monitor)
        assertNotNull("Aggregate mode opens original FastSearchActivity", fast)
        context.activities.add(fast!!)
        val grid = fast.findViewById<TvRecyclerView>(context.id("mGridView"))
        context.waitFor("Original aggregate page receives search results") {
            (grid.adapter as com.github.tvbox.osc.ui.adapter.FastSearchAdapter).data.size == 2
        }
        context.capture("fast-results.png")
    }

    private fun fixture(aggregate: Boolean, action: (Context) -> Unit) = runBlocking {
        val context = Context()
        val keys = listOf(HawkConfig.API_URL, HawkConfig.HOME_API, HawkConfig.FAST_SEARCH_MODE,
            HawkConfig.SEARCH_VIEW, HawkConfig.SEARCH_HISTORY, HawkConfig.SHOW_PREVIEW)
        val previous = keys.associateWith { if (Hawk.contains(it)) Hawk.get<Any>(it) else null }
        var base = ""
        val server = object : NanoHTTPD("127.0.0.1", 0) {
            override fun serve(session: IHTTPSession): Response {
                context.requests.add(session.uri + "?" + session.queryParameterString.orEmpty())
                if (session.uri == "/poster.png") return newChunkedResponse(Response.Status.OK, "image/png", context.instrumentation.context.assets.open("poster.png"))
                val body = if (session.uri == "/config.json") JSONObject().put("spider", "")
                    .put("sites", JSONArray().put(JSONObject().put("key", "search-fixture").put("name", "搜索固定源")
                        .put("type", 1).put("api", base + "api").put("searchable", 1)))
                    .put("lives", JSONArray()).put("parses", JSONArray())
                else {
                    val list = JSONArray()
                    val folder = session.parameters["t"]?.isNotEmpty() == true
                    list.put(JSONObject().put("vod_id", if (folder) "child" else "video")
                        .put("vod_name", if (folder) "目录子项" else TITLE).put("vod_pic", base + "poster.png")
                        .put("vod_play_from", "local").put("vod_play_url", "片段\$${base}clip.mp4"))
                    if (session.parameters["wd"]?.isNotEmpty() == true) list.put(JSONObject()
                        .put("vod_id", "folder").put("vod_name", TITLE).put("vod_tag", "folder")
                        .put("vod_pic", base + "poster.png").put("action", "fixture-action"))
                    JSONObject().put("list", list)
                }
                return newFixedLengthResponse(Response.Status.OK, "application/json", body.toString())
            }
        }
        server.start(); base = "http://127.0.0.1:${server.listeningPort}/"
        try {
            Hawk.put(HawkConfig.API_URL, base + "config.json")
            Hawk.put(HawkConfig.FAST_SEARCH_MODE, aggregate)
            Hawk.put(HawkConfig.SEARCH_VIEW, InstrumentationRegistry.getArguments().getString("searchView", "0").toInt())
            Hawk.put(HawkConfig.SHOW_PREVIEW, false)
            val loaded = CompletableDeferred<Unit>()
            context.main { ApiConfig.get().loadConfig(false, object : ApiConfig.LoadConfigCallback {
                override fun success() { loaded.complete(Unit) }
                override fun notice(message: String) = Unit
                override fun error(message: String) { loaded.completeExceptionally(AssertionError(message)) }
            }, null) }
            withTimeout(10_000) { loaded.await() }
            action(context)
        } finally {
            context.main { context.activities.reversed().forEach { if (!it.isDestroyed) it.finish() } }
            server.stop()
            previous.forEach { (key, value) -> if (value == null) Hawk.delete(key) else Hawk.put(key, value) }
        }
    }

    private class Context {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val requests = CopyOnWriteArrayList<String>()
        val activities = mutableListOf<Activity>()
        val candidate = InstrumentationRegistry.getArguments().getString("baseline", "false") != "true"
        val label = InstrumentationRegistry.getArguments().getString("label", "search-screen")
        fun id(name: String) = App.getInstance().resources.getIdentifier(name, "id", App.getInstance().packageName)
        fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)
        fun key(code: Int) { instrumentation.sendKeyDownUpSync(code); SystemClock.sleep(160) }
        fun launch(type: Class<out Activity>): Activity = instrumentation.startActivitySync(
            Intent(App.getInstance(), type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)).also { activities.add(it) }
        fun waitFor(message: String, condition: () -> Boolean) {
            repeat(70) { if (condition()) return; SystemClock.sleep(100) }
            throw AssertionError(message)
        }
        fun textView(root: View, text: String): TextView? {
            if (root is TextView && root.text.toString() == text) return root
            if (root is ViewGroup) for (i in 0 until root.childCount) textView(root.getChildAt(i), text)?.let { return it }
            return null
        }
        fun focusableParent(view: View): View = if (view.isFocusable) view else focusableParent(view.parent as View)
        fun focusResult(grid: TvRecyclerView, position: Int) {
            main { grid.setSelectedPosition(position); grid.setSelection(position) }
            waitFor("Result child is available") { grid.findViewHolderForAdapterPosition(position) != null }
            main { grid.findViewHolderForAdapterPosition(position)!!.itemView.requestFocus() }
        }
        fun capture(name: String) {
            SystemClock.sleep(700)
            val dir = File(App.getInstance().getExternalFilesDir(null), label).apply { mkdirs() }
            instrumentation.uiAutomation.takeScreenshot()!!.let { bitmap ->
                File(dir, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
        }
    }

    companion object { private const val TITLE = "TV 搜索验证" }
}
