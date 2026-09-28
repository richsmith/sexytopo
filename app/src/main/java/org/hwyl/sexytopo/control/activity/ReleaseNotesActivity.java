package org.hwyl.sexytopo.control.activity;

import android.os.Bundle;
import android.webkit.WebView;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.Log;
import org.hwyl.sexytopo.control.util.ReleaseNotesFormatter;

/** Shows docs/releases.md, which the build copies into the app's assets. */
public class ReleaseNotesActivity extends SexyTopoActivity {

    private static final String RELEASE_NOTES_ASSET = "releases.md";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);
        setupMaterialToolbar();

        applyEdgeToEdgeInsets(R.id.rootLayout, true, true);

        String markdown;
        try (InputStream stream = getAssets().open(RELEASE_NOTES_ASSET)) {
            markdown = IOUtils.toString(stream, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            Log.e(exception);
            markdown = getString(R.string.release_notes_unavailable);
        }

        WebView webView = findViewById(R.id.webview);
        webView.loadDataWithBaseURL(
                null, ReleaseNotesFormatter.toHtml(markdown), "text/html", "utf-8", null);
    }
}
