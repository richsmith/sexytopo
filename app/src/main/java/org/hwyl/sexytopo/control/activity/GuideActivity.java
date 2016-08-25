package org.hwyl.sexytopo.control.activity;

import android.os.Bundle;
import android.webkit.WebView;

import org.hwyl.sexytopo.R;

public class GuideActivity extends SexyTopoActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guide);

        WebView webView = (WebView)(findViewById(R.id.guideWebView));
        webView.loadUrl("file:///android_asset/guide/index.html");
    }


}
