package com.github.tvbox.osc.modern

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.ui.activity.DetailActivity
import com.github.tvbox.osc.ui.activity.HomeActivity
import com.github.tvbox.osc.util.HawkConfig
import com.orhanobut.hawk.Hawk
import fi.iki.elonen.NanoHTTPD
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import androidx.fragment.app.FragmentActivity
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.bean.VodInfo

@RunWith(AndroidJUnit4::class)
class HomePlaybackSmokeTest {
    @Test fun composeHeroOpensOriginalDetailAndPlaysFixedVideo() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply { flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS }
        automation.grantRuntimePermission("com.coke.tv", Manifest.permission.READ_EXTERNAL_STORAGE)
        automation.grantRuntimePermission("com.coke.tv", Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val oldUrl = Hawk.get(HawkConfig.API_URL, "")
        val hadUrl = Hawk.contains(HawkConfig.API_URL)
        val oldHome = Hawk.get(HawkConfig.HOME_API, "")
        val hadHome = Hawk.contains(HawkConfig.HOME_API)
        val hadPreview = Hawk.contains(HawkConfig.SHOW_PREVIEW)
        val oldPreview = Hawk.get(HawkConfig.SHOW_PREVIEW, true)
        val originallyCollected = RoomDataManger.isVodCollect("home-smoke", "local-smoke")
        val detailRequests = AtomicInteger()
        val label = InstrumentationRegistry.getArguments().getString("label", "home-smoke")
        val output = File(App.getInstance().getExternalFilesDir(null), label).apply { mkdirs() }
        fun id(name: String) = App.getInstance().resources.getIdentifier(name, "id", App.getInstance().packageName)
        var base = ""
        val server = object : NanoHTTPD("127.0.0.1", 0) {
            override fun serve(session: IHTTPSession): Response {
                if (session.parameters["ids"]?.isNotEmpty() == true) detailRequests.incrementAndGet()
                if (session.uri == "/poster.png") return newChunkedResponse(Response.Status.OK, "image/png", instrumentation.context.assets.open("poster.png"))
                if (session.uri == "/clip.mp4") return newChunkedResponse(Response.Status.OK, "video/mp4", instrumentation.context.assets.open("clip.mp4"))
                val body = if (session.uri == "/config.json") JSONObject().put("spider", "")
                    .put("sites", JSONArray().put(JSONObject().put("key", "home-smoke").put("name", "本地验证")
                        .put("type", 1).put("api", base + "api").put("searchable", 1)))
                    .put("parses", JSONArray()).put("lives", JSONArray())
                else JSONObject().put("class", JSONArray().put(JSONObject().put("type_id", "1").put("type_name", "验证")))
                    .put("list", JSONArray().put(JSONObject().put("vod_id", "local-smoke").put("vod_name", "TV 播放验证")
                        .put("vod_year", "2026").put("type_name", "本地验证").put("vod_pic", base + "poster.png")
                        .put("vod_area", "中国").put("vod_lang", "国语").put("vod_actor", "演员甲 / 演员乙").put("vod_director", "导演甲")
                        .put("vod_content", "固定视频片段，用于验证播放与界面接线。这里是较长的简介，检查电视详情的信息层级、文字折行以及原生按钮位置。重复这段文字，用于确认三行省略仍与原来相同。固定视频片段，用于验证播放与界面接线。这里是较长的简介，检查电视详情的信息层级、文字折行以及原生按钮位置。")
                        .put("vod_play_from", "local").put("vod_play_url", "片段一\$${base}clip.mp4#片段二\$${base}clip.mp4")))
                return newFixedLengthResponse(Response.Status.OK, "application/json", body.toString())
            }
        }
        server.start(); base = "http://127.0.0.1:${server.listeningPort}/"
        var home: Activity? = null
        var detail: Activity? = null
        fun key(code: Int) { instrumentation.sendKeyDownUpSync(code); SystemClock.sleep(130) }
        fun hasText(node: AccessibilityNodeInfo?, text: String): Boolean {
            if (node == null) return false
            if (node.text?.toString()?.contains(text) == true) return true
            return (0 until node.childCount).any { hasText(node.getChild(it), text) }
        }
        fun frameHash(): Int {
            val shot = automation.takeScreenshot() ?: throw AssertionError("No video screenshot")
            val pixels = IntArray(shot.width * 20)
            shot.getPixels(pixels, 0, shot.width, 0, (shot.height * .40).toInt(), shot.width, 20)
            shot.recycle()
            return pixels.contentHashCode()
        }
        fun waitFor(message: String, condition: () -> Boolean) {
            repeat(70) { if (condition()) return; SystemClock.sleep(100) }
            throw AssertionError(message)
        }
        fun screenshot(name: String) {
            automation.takeScreenshot()!!.let { bitmap -> File(output, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle() }
        }
        try {
            Hawk.put(HawkConfig.SHOW_PREVIEW, true)
            Hawk.put(HawkConfig.API_URL, base + "config.json")
            home = instrumentation.startActivitySync(Intent(App.getInstance(), HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            var ready = false
            for (i in 0..80) {
                if (hasText(automation.rootInActiveWindow, "TV 播放验证")) { ready = true; break }
                SystemClock.sleep(100)
            }
            assertTrue("Real recommendation must reach Compose", ready)
            SystemClock.sleep(700)
            screenshot("home.png")
            val monitor = instrumentation.addMonitor(DetailActivity::class.java.name, null, false)
            instrumentation.runOnMainSync { home!!.findViewById<android.view.View>(id("mGridView")).requestFocus() }
            key(KeyEvent.KEYCODE_DPAD_DOWN)
            key(KeyEvent.KEYCODE_DPAD_RIGHT)
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            detail = instrumentation.waitForMonitorWithTimeout(monitor, 5_000)
            assertNotNull("Original detail opens", detail)
            val collect = detail!!.findViewById<android.widget.TextView>(id("tvCollect"))
            waitFor("Detail collection state and original buttons are ready") { collect.isEnabled }
            SystemClock.sleep(1_000)
            val preview = (detail as FragmentActivity).supportFragmentManager.findFragmentById(id("previewPlayer"))
            assertNotNull("Original preview fragment exists", preview)
            val video = preview!!.requireView().findViewById<android.view.View>(id("mVideoView"))
            assertNotNull(video)
            val requestsBeforeCollection = detailRequests.get()
            assertTrue("Existing detail chain supplied content", requestsBeforeCollection > 0)
            screenshot("detail.png")
            instrumentation.runOnMainSync { collect.requestFocus() }
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            waitFor("Collection write is persisted") { RoomDataManger.isVodCollect("home-smoke", "local-smoke") != originallyCollected && collect.isEnabled }
            assertEquals(if (originallyCollected) "加入收藏" else "取消收藏", collect.text.toString())
            assertTrue("Collection keeps the original remote focus: ${detail?.currentFocus}", collect.hasFocus())
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            waitFor("Collection toggle restores original state") { RoomDataManger.isVodCollect("home-smoke", "local-smoke") == originallyCollected && collect.isEnabled }
            instrumentation.runOnMainSync { detail!!.findViewById<android.view.View>(id("tvPlay")).performClick() }
            SystemClock.sleep(800)
            assertSame("Preview/fullscreen keeps the fragment", preview, (detail as FragmentActivity).supportFragmentManager.findFragmentById(id("previewPlayer")))
            assertSame("Collection state/fullscreen does not replace the video view", video, preview.requireView().findViewById(id("mVideoView")))
            assertEquals("Collection and fullscreen must not request detail again", requestsBeforeCollection, detailRequests.get())
            screenshot("fullscreen.png")
            val motion = mutableListOf<Boolean>()
            repeat(4) {
                val first = frameHash(); SystemClock.sleep(500); motion += first != frameHash()
                key(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE); SystemClock.sleep(200)
            }
            assertTrue("Pause / resume must alternate motion: $motion", motion == listOf(true, false, true, false) || motion == listOf(false, true, false, true))
            File(output, "motion.txt").writeText(motion.toString())
        } finally {
            instrumentation.runOnMainSync { detail?.finish(); home?.finish() }
            server.stop()
            if (hadUrl) Hawk.put(HawkConfig.API_URL, oldUrl) else Hawk.delete(HawkConfig.API_URL)
            if (hadHome) Hawk.put(HawkConfig.HOME_API, oldHome) else Hawk.delete(HawkConfig.HOME_API)
            if (hadPreview) Hawk.put(HawkConfig.SHOW_PREVIEW, oldPreview) else Hawk.delete(HawkConfig.SHOW_PREVIEW)
            if (!originallyCollected) RoomDataManger.deleteVodCollect("home-smoke", VodInfo().apply { id = "local-smoke" })
        }
    }
}
