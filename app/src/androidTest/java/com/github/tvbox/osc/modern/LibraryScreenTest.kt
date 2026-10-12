package com.github.tvbox.osc.modern

import android.app.Activity
import android.content.Intent
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.tvbox.osc.base.App
import com.github.tvbox.osc.bean.VodInfo
import com.github.tvbox.osc.cache.RoomDataManger
import com.github.tvbox.osc.ui.activity.CollectActivity
import com.github.tvbox.osc.ui.activity.HistoryActivity
import com.github.tvbox.osc.ui.adapter.HistoryAdapter
import com.owen.tvrecyclerview.widget.TvRecyclerView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LibraryScreenTest {
    @Test fun nativeListDeleteAndClearOperateThroughModernState() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.uiAutomation
        automation.serviceInfo = automation.serviceInfo.apply { flags = flags or android.accessibilityservice.AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS }
        assertTrue("Clear test requires empty test collections", RoomDataManger.getAllVodCollect().isEmpty())
        val suffix = UUID.randomUUID().toString()
        val alpha = VodInfo().apply { id = "shared-$suffix"; sourceKey = "alpha-$suffix"; name = "Library alpha"; pic = ""; playNote = "第3集" }
        val beta = VodInfo().apply { id = alpha.id; sourceKey = "beta-$suffix"; name = "Library beta"; pic = "" }
        RoomDataManger.insertVodRecord(alpha.sourceKey, alpha); RoomDataManger.insertVodRecord(beta.sourceKey, beta)
        RoomDataManger.insertVodCollect(alpha.sourceKey, alpha); RoomDataManger.insertVodCollect(beta.sourceKey, beta)
        var history: Activity? = null
        var collection: Activity? = null
        fun id(name: String) = App.getInstance().resources.getIdentifier(name, "id", App.getInstance().packageName)
        fun waitFor(message: String, condition: () -> Boolean) {
            repeat(60) { if (condition()) return; SystemClock.sleep(50) }
            throw AssertionError("$message; focus=${history?.currentFocus}; grid=${history?.findViewById<TvRecyclerView>(id("mGridView"))?.childCount}")
        }
        fun key(code: Int) { instrumentation.sendKeyDownUpSync(code); SystemClock.sleep(150) }
        fun node(id: String): AccessibilityNodeInfo? = automation.rootInActiveWindow
            ?.findAccessibilityNodeInfosByViewId("com.coke.tv:id/$id")?.firstOrNull()
        try {
            history = instrumentation.startActivitySync(Intent(App.getInstance(), HistoryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            lateinit var grid: TvRecyclerView
            instrumentation.runOnMainSync { grid = history!!.findViewById(id("mGridView")) }
            waitFor("History renders saved rows") { (grid.adapter as HistoryAdapter).data.any { it.sourceKey == alpha.sourceKey } }
            instrumentation.runOnMainSync {
                val adapter = grid.adapter as HistoryAdapter
                assertEquals("上次看到第3集", adapter.data.first { it.sourceKey == alpha.sourceKey }.note)
            }
            waitFor("First history card is laid out") { grid.findViewHolderForAdapterPosition(0) != null }
            instrumentation.runOnMainSync { grid.findViewHolderForAdapterPosition(0)!!.itemView.requestFocus() }
            key(KeyEvent.KEYCODE_DPAD_UP)
            waitFor("Original delete toolbar remains reachable") { history!!.findViewById<View>(id("tvDelete")).hasFocus() }
            key(KeyEvent.KEYCODE_DPAD_CENTER)
            waitFor("Delete mode is visible") { history!!.findViewById<View>(id("tvDelTip")).visibility == View.VISIBLE }
            instrumentation.runOnMainSync {
                val index = (grid.adapter as HistoryAdapter).data.indexOfFirst { it.sourceKey == alpha.sourceKey }
                grid.findViewHolderForAdapterPosition(index)!!.itemView.requestFocus()
            }
            SystemClock.sleep(350); key(KeyEvent.KEYCODE_DPAD_CENTER)
            waitFor("Only alpha identity is deleted") { RoomDataManger.getVodInfo(alpha.sourceKey, alpha.id) == null }
            assertNotNull(RoomDataManger.getVodInfo(beta.sourceKey, beta.id))
            key(KeyEvent.KEYCODE_BACK)
            waitFor("Back leaves delete mode before leaving screen") { history!!.findViewById<View>(id("tvDelTip")).visibility == View.GONE }
            instrumentation.runOnMainSync { history!!.finish() }
            collection = instrumentation.startActivitySync(Intent(App.getInstance(), CollectActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            SystemClock.sleep(350)
            instrumentation.runOnMainSync { collection!!.findViewById<View>(id("tvClear")).performClick() }
            waitFor("Cancel button appears") { node("btnCancel") != null }
            assertTrue(node("btnCancel")!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            assertEquals(2, RoomDataManger.getAllVodCollect().size)
            instrumentation.runOnMainSync { collection!!.findViewById<View>(id("tvClear")).performClick() }
            waitFor("Confirm button appears") { node("btnConfirm") != null }
            assertTrue(node("btnConfirm")!!.performAction(AccessibilityNodeInfo.ACTION_CLICK))
            waitFor("Confirmed clear is persisted") { RoomDataManger.getAllVodCollect().isEmpty() }
            assertNotNull(RoomDataManger.getVodInfo(beta.sourceKey, beta.id))
        } finally {
            instrumentation.runOnMainSync { history?.finish(); collection?.finish() }
            RoomDataManger.deleteVodRecord(alpha.sourceKey, alpha); RoomDataManger.deleteVodRecord(beta.sourceKey, beta)
            RoomDataManger.deleteVodCollect(alpha.sourceKey, alpha); RoomDataManger.deleteVodCollect(beta.sourceKey, beta)
        }
    }
}
