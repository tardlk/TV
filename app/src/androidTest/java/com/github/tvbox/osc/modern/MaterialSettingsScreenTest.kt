package com.github.tvbox.osc.modern

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.ui.activity.SettingActivity
import com.github.tvbox.osc.ui.compose.TvSettingsCatalog
import com.github.tvbox.osc.util.HawkConfig
import com.github.tvbox.osc.util.HistoryHelper
import com.orhanobut.hawk.Hawk
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class MaterialSettingsScreenTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val automation get() = instrumentation.uiAutomation
    private val activities = mutableListOf<Activity>()
    private val label = InstrumentationRegistry.getArguments().getString("label", "material-settings")
    private fun node(id: String): AccessibilityNodeInfo? {
        // API 33 has no UiAutomation.clearCache; reapplying its service info clears the cache.
        automation.serviceInfo = automation.serviceInfo
        fun find(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
            if (node == null) return null
            if (node.viewIdResourceName == "com.coke.tv:id/$id") return node
            repeat(node.childCount) { find(node.getChild(it))?.let { result -> return result } }
            return null
        }
        return find(automation.rootInActiveWindow)
    }
    private fun key(code: Int, pause: Long = 130) { instrumentation.sendKeyDownUpSync(code); SystemClock.sleep(pause) }
    private fun waitFor(message: String, condition: () -> Boolean) {
        repeat(60) { if (condition()) return; SystemClock.sleep(100) }
        capture("failure.png")
        val tree = StringBuilder()
        fun walk(node: AccessibilityNodeInfo?) {
            if (node == null) return
            tree.append("${node.viewIdResourceName}: ${node.text} focused=${node.isFocused}\n")
            repeat(node.childCount) { walk(node.getChild(it)) }
        }
        walk(automation.rootInActiveWindow)
        File(App.getInstance().getExternalFilesDir(null), "$label/failure-tree.txt").writeText(tree.toString())
        throw AssertionError(message)
    }
    private fun focus(id: String) = waitFor("Remote focus must reach $id") { node(id)?.isFocused == true }
    private fun launch(): Activity {
        automation.serviceInfo = automation.serviceInfo.apply { flags = flags or AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS }
        return instrumentation.startActivitySync(Intent(App.getInstance(), SettingActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)).also { activities.add(it); focus("tvSettingsGroup0") }
    }
    private fun capture(name: String) {
        val dir = File(App.getInstance().getExternalFilesDir(null), label).apply { mkdirs() }
        automation.takeScreenshot()!!.let { image ->
            File(dir, name).outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }; image.recycle()
        }
    }
    private fun cleanup() = instrumentation.runOnMainSync { activities.reversed().forEach { if (!it.isDestroyed) it.finish() } }

    @Test fun everyOriginalSettingsEntryRemainsReachableWithTheRemote() {
        val keys = listOf(HawkConfig.API_URL, HawkConfig.API_LINE_LIST)
        val saved = keys.associateWith { if (Hawk.contains(it)) Hawk.get<Any>(it) else null }
        try {
            // Activate the original conditional line row without performing a config change.
            Hawk.put(HawkConfig.API_URL, "http://127.0.0.1/material-fixture.json")
            Hawk.put(HawkConfig.API_LINE_LIST, arrayListOf(HistoryHelper.buildApiLine("固定线路", "http://127.0.0.1/material-fixture.json")))
            launch()
            assertEquals("All original actions are represented", 29, TvSettingsCatalog.rows.size)
            for ((group, section) in TvSettingsCatalog.groups.withIndex()) {
                focus("tvSettingsGroup$group")
                if (group == 3) repeat(4) { key(KeyEvent.KEYCODE_0, 20) }
                key(KeyEvent.KEYCODE_DPAD_RIGHT)
                section.rows.forEachIndexed { index, row ->
                    val name = App.getInstance().resources.getResourceEntryName(row.actionId)
                    focus(name)
                    val headingBounds = android.graphics.Rect()
                    node("tvSettingsHeading")!!.getBoundsInScreen(headingBounds)
                    assertTrue("Settings heading must remain fixed while the pane scrolls: $headingBounds", headingBounds.top >= 0)
                    if (index == 0) capture("group-$group-top.png")
                    if (index == section.rows.lastIndex) capture("group-$group-bottom.png")
                    if (index < section.rows.lastIndex) key(KeyEvent.KEYCODE_DPAD_DOWN)
                }
                key(KeyEvent.KEYCODE_DPAD_LEFT); focus("tvSettingsGroup$group")
                if (group < 3) key(KeyEvent.KEYCODE_DPAD_DOWN)
            }
            key(KeyEvent.KEYCODE_BACK)
        } finally {
            cleanup()
            saved.forEach { (key, value) -> if (value == null) Hawk.delete(key) else Hawk.put(key, value) }
        }
    }

    @Test fun originalDialogsAndStoredSettingsSurviveComposeUpdatesAndRecreation() {
        val keys = listOf(HawkConfig.SHOW_PREVIEW, HawkConfig.FAST_SEARCH_MODE, HawkConfig.SEARCH_VIEW, HawkConfig.API_LINE_LIST)
        val saved = keys.associateWith { if (Hawk.contains(it)) Hawk.get<Any>(it) else null }
        try {
            Hawk.put(HawkConfig.SHOW_PREVIEW, true); Hawk.put(HawkConfig.FAST_SEARCH_MODE, true)
            Hawk.put(HawkConfig.SEARCH_VIEW, 0); Hawk.put(HawkConfig.API_LINE_LIST, arrayListOf<String>())
            val settings = launch()
            key(KeyEvent.KEYCODE_DPAD_RIGHT); focus("llApi")
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            waitFor("Original configuration dialog opens") { node("input") != null }
            key(KeyEvent.KEYCODE_BACK)
            // The original EditText may close IME first; close the dialog if it remains.
            if (node("input") != null) key(KeyEvent.KEYCODE_BACK)
            focus("llApi")
            key(KeyEvent.KEYCODE_DPAD_LEFT); key(KeyEvent.KEYCODE_DPAD_DOWN); focus("tvSettingsGroup1")
            key(KeyEvent.KEYCODE_DPAD_RIGHT); focus("llPlay")
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            waitFor("Original player selection dialog opens") { node("list") != null }
            key(KeyEvent.KEYCODE_BACK); focus("llPlay")
            repeat(4) { key(KeyEvent.KEYCODE_DPAD_DOWN) }; focus("showPreview")
            key(KeyEvent.KEYCODE_DPAD_CENTER, 20); key(KeyEvent.KEYCODE_DPAD_CENTER, 20)
            waitFor("Rapid confirmations write the preview setting once") { !Hawk.get(HawkConfig.SHOW_PREVIEW, true) }
            SystemClock.sleep(600)
            assertFalse(Hawk.get(HawkConfig.SHOW_PREVIEW, true))
            focus("showPreview")
            waitFor("Compose preview row displays the committed value") { node("showPreviewText")?.text?.toString() == "关闭" }
            key(KeyEvent.KEYCODE_DPAD_LEFT); key(KeyEvent.KEYCODE_DPAD_DOWN); focus("tvSettingsGroup2")
            key(KeyEvent.KEYCODE_DPAD_RIGHT); focus("llSearchView")
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            waitFor("Original search layout selector opens") { node("list") != null }
            key(KeyEvent.KEYCODE_DPAD_DOWN); key(KeyEvent.KEYCODE_DPAD_CENTER)
            waitFor("Original selector writes the same Hawk key") { Hawk.get(HawkConfig.SEARCH_VIEW, 0) == 1 }
            if (node("list") != null) key(KeyEvent.KEYCODE_BACK)
            focus("llSearchView")
            key(KeyEvent.KEYCODE_DPAD_DOWN); focus("showFastSearch")
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            waitFor("Aggregate search setting persists") { !Hawk.get(HawkConfig.FAST_SEARCH_MODE, true) }
            focus("showFastSearch")
            capture("changed-values.png")
            instrumentation.runOnMainSync { settings.recreate() }
            var replacement: Activity? = null
            waitFor("Settings Activity rebuilds") {
                instrumentation.runOnMainSync { replacement = ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(Stage.RESUMED).firstOrNull { it is SettingActivity && it !== settings } }
                replacement != null
            }
            activities.add(replacement!!)
            waitFor("Recreated settings restore the active group") { node("tvSettingsGroup2") != null }
            assertFalse(Hawk.get(HawkConfig.SHOW_PREVIEW, true))
            assertEquals(1, Hawk.get(HawkConfig.SEARCH_VIEW, 0))
            assertFalse(Hawk.get(HawkConfig.FAST_SEARCH_MODE, true))
            waitFor("Recreated Compose row displays the stored value") { node("showFastSearchText")?.text?.toString() == "关闭" }
        } finally {
            cleanup()
            saved.forEach { (key, value) -> if (value == null) Hawk.delete(key) else Hawk.put(key, value) }
        }
    }
}
