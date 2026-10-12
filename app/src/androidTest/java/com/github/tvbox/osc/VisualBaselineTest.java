package com.github.tvbox.osc;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.ui.activity.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class VisualBaselineTest {
    @Test public void captureEmptyScreens() throws Exception {
        String label = InstrumentationRegistry.getArguments().getString("label", "candidate");
        File output = new File(App.getInstance().getExternalFilesDir(null), "visual/" + label);
        assertTrue(output.isDirectory() || output.mkdirs());
        Class<?>[] screens = {HomeActivity.class, SearchActivity.class, HistoryActivity.class,
                CollectActivity.class, SettingActivity.class, LivePlayActivity.class};
        for (Class<?> screen : screens) {
            Activity activity = InstrumentationRegistry.getInstrumentation().startActivitySync(
                    new Intent(App.getInstance(), screen).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            SystemClock.sleep(600);
            Bitmap bitmap = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
            assertNotNull(screen.getSimpleName(), bitmap);
            try (FileOutputStream out = new FileOutputStream(new File(output, screen.getSimpleName() + ".png"))) {
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, out));
            }
            bitmap.recycle();
            InstrumentationRegistry.getInstrumentation().runOnMainSync(activity::finish);
        }
    }
}
