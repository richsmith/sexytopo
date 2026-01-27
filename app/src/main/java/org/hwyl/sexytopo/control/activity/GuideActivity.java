package org.hwyl.sexytopo.control.activity;

import android.os.Bundle;
import android.webkit.WebView;

import org.hwyl.sexytopo.R;

public class GuideActivity extends SexyTopoActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        applyEdgeToEdgeInsets(R.id.rootLayout, true, true);

        WebView webView = findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/guide/index.html");
    }


}
