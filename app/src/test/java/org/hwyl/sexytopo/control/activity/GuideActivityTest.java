package org.hwyl.sexytopo.control.activity;

import android.webkit.WebView;

import org.hwyl.sexytopo.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class GuideActivityTest {

    @Test
    public void webviewVisible() {
        GuideActivity activity = Robolectric.buildActivity(GuideActivity.class)
                .create()
                .start()
                .resume()
                .get();

        WebView webView = activity.findViewById(R.id.webview);
        assertNotNull("WebView should be found by ID", webView);

        String url = webView.getUrl();
        assertNotNull("WebView should have loaded a URL", url);
        assertTrue("WebView should load guide HTML", url.contains("guide/index.html"));
    }
}
