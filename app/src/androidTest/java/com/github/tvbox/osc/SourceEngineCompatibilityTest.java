package com.github.tvbox.osc;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.github.catvod.crawler.JarLoader;
import com.github.catvod.crawler.Spider;
import com.github.catvod.crawler.js.JsSpider;
import com.github.tvbox.osc.base.App;
import com.undcover.freedom.pyramid.PythonLoader;
import fi.iki.elonen.NanoHTTPD;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collections;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SourceEngineCompatibilityTest {
    private Context assets;
    private NanoHTTPD server;
    private String baseUrl;

    @Before public void startFixtureServer() throws Exception {
        assets = InstrumentationRegistry.getInstrumentation().getContext();
        server = new NanoHTTPD("127.0.0.1", 0) {
            @Override public Response serve(IHTTPSession session) {
                try {
                    return newChunkedResponse(Response.Status.OK, "text/plain", assets.getAssets().open(session.getUri().substring(1)));
                } catch (Exception e) {
                    return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "No fixture");
                }
            }
        };
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getListeningPort() + "/";
    }

    @After public void stopFixtureServer() { server.stop(); }

    @Test public void dynamicJarRetainsHostContract() throws Exception {
        File jar = new File(App.getInstance().getCacheDir(), "engine-smoke.jar");
        jar.delete();
        try (InputStream in = assets.getAssets().open("engine-smoke.jar"); FileOutputStream out = new FileOutputStream(jar)) {
            byte[] buffer = new byte[8192]; int size;
            while ((size = in.read(buffer)) != -1) out.write(buffer, 0, size);
        }
        JarLoader loader = new JarLoader();
        try {
            assertTrue("Host JarLoader must load the dynamic DEX", loader.load(jar.getAbsolutePath()));
            Spider spider = loader.getSpider("fixture-site", "csp_Smoke", "", "");
            assertHome(spider, "JAR smoke");
            JSONObject play = new JSONObject(spider.playerContent("local", "jar", Collections.emptyList()));
            assertEquals("jar-fixture", play.getString("url"));
            assertEquals("fixture-site", play.getString("site"));
        } finally { loader.clear(); jar.delete(); }
    }

    @Test public void quickJsExecutesSpiderModule() throws Exception {
        Spider spider = new JsSpider("fixture-js", baseUrl + "engine-smoke.js", null);
        try {
            spider.init(App.getInstance(), "");
            assertHome(spider, "JS smoke");
            assertEquals("js-fixture", new JSONObject(spider.playerContent("local", "js", Collections.emptyList())).getString("url"));
        } finally { spider.destroy(); }
    }

    @Test public void pythonExecutesSpiderAndNativeDependencies() throws Exception {
        PythonLoader loader = PythonLoader.getInstance().setApplication(App.getInstance());
        try {
            Spider spider = loader.getSpider("fixture-python", baseUrl + "engine_smoke.py", "");
            assertHome(spider, "Python smoke");
            assertEquals("python-fixture", new JSONObject(spider.playerContent("local", "python", Collections.emptyList())).getString("url"));
        } finally { loader.clear(); }
    }

    private static void assertHome(Spider spider, String expected) throws Exception {
        JSONObject home = new JSONObject(spider.homeContent(false));
        assertEquals(expected, home.getJSONArray("class").getJSONObject(0).getString("type_name"));
        assertEquals(expected, home.getJSONArray("list").getJSONObject(0).getString("vod_name"));
    }
}
