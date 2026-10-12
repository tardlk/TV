package com.github.tvbox.osc.modern;

import android.app.Activity;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.ui.activity.HomeActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HomeRemoteTest {
    private void key(int value) {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(value);
        SystemClock.sleep(120);
    }
    private AccessibilityNodeInfo root() {
        AccessibilityNodeInfo result = null;
        for (int i = 0; i < 40 && result == null; i++) {
            result = InstrumentationRegistry.getInstrumentation().getUiAutomation().getRootInActiveWindow();
            if (result == null) SystemClock.sleep(50);
        }
        assertNotNull("Accessibility window must become available", result);
        return result;
    }
    private void assertFocus(String name) {
        for (int i = 0; i < 30; i++) {
            AccessibilityNodeInfo candidate = findNode(root(), "com.coke.tv:id/" + name);
            if (candidate != null && candidate.isFocused()) return;
            SystemClock.sleep(50);
        }
        AccessibilityNodeInfo current = root();
        java.util.ArrayList<String> focused = new java.util.ArrayList<>();
        collectFocus(current, focused);
        android.graphics.Bitmap shot = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        try (java.io.FileOutputStream output = new java.io.FileOutputStream(new java.io.File(App.getInstance().getExternalFilesDir(null), "remote-failure.png"))) {
            if (shot != null) shot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output);
        } catch (java.io.IOException failure) { throw new AssertionError(failure); }
        fail("Focus must reach " + name + "; package=" + current.getPackageName() + "; focused=" + focused);
    }
    private AccessibilityNodeInfo findNode(AccessibilityNodeInfo node, String id) {
        if (id.equals(node.getViewIdResourceName())) return node;
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) { AccessibilityNodeInfo found = findNode(child, id); if (found != null) return found; }
        }
        return null;
    }
    private void collectFocus(AccessibilityNodeInfo node, java.util.List<String> result) {
        if (node.isFocused()) result.add(node.getViewIdResourceName() + ":" + node.getText());
        for (int i = 0; i < node.getChildCount(); i++) { AccessibilityNodeInfo child = node.getChild(i); if (child != null) collectFocus(child, result); }
    }
    @Test public void headerAndHeroRemainReachableByRemote() {
        android.app.UiAutomation automation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
        AccessibilityServiceInfo info = automation.getServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        automation.setServiceInfo(info);
        Activity activity = InstrumentationRegistry.getInstrumentation().startActivitySync(
            new Intent(App.getInstance(), HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        SystemClock.sleep(700);
        key(KeyEvent.KEYCODE_DPAD_UP); assertFocus("tvNavHome");
        for (String expected : new String[]{"tvNavLive", "tvNavHistory", "tvNavCollection", "tvNavSettings", "tvNavSource", "tvNavSearch"}) {
            key(KeyEvent.KEYCODE_DPAD_RIGHT); assertFocus(expected);
        }
        key(KeyEvent.KEYCODE_DPAD_CENTER); SystemClock.sleep(400);
        assertFalse("Original search keyboard must open", root().findAccessibilityNodeInfosByViewId("com.coke.tv:id/keyBoardRoot").isEmpty());
        key(KeyEvent.KEYCODE_BACK); SystemClock.sleep(250);
        key(KeyEvent.KEYCODE_DPAD_LEFT); key(KeyEvent.KEYCODE_DPAD_LEFT); assertFocus("tvNavSettings");
        key(KeyEvent.KEYCODE_DPAD_CENTER); SystemClock.sleep(300);
        assertFalse("Settings remain available", root().findAccessibilityNodeInfosByText("配置地址").isEmpty());
        key(KeyEvent.KEYCODE_BACK); SystemClock.sleep(200);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(activity::finish);
    }
}
