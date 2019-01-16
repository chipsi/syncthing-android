package com.nutomic.syncthingandroid.activities;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
// import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.nutomic.syncthingandroid.R;

/**
 * Holds a WebView that shows the web ui of the local syncthing instance.
 */
public class WebViewActivity extends SyncthingActivity {

    private static final String TAG = "WebViewActivity";

    private WebView mWebView;

    private View mLoadingView;

    /**
     * Hides the loading screen and shows the WebView once it is fully loaded.
     */
    private final WebViewClient mWebViewClient = new WebViewClient() {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);
            if (uri.getHost().equals("www.google.de")) {
                return false;
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            }
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            mWebView.setVisibility(View.VISIBLE);
            mLoadingView.setVisibility(View.GONE);
        }
    };

    /**
     * Initialize WebView.
     */
    @Override
    // @SuppressLint("SetJavaScriptEnabled")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_gui);
        mLoadingView = findViewById(R.id.loading);
        mWebView = findViewById(R.id.webview);
        // mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.setWebViewClient(mWebViewClient);
        mWebView.clearCache(true);

        if (mWebView.getUrl() == null) {
            mWebView.stopLoading();
            mWebView.loadUrl("google.de");
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            finish();
            super.onBackPressed();
        }
    }

    @Override
    public void onPause() {
        mWebView.onPause();
        mWebView.pauseTimers();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mWebView.resumeTimers();
        mWebView.onResume();
    }

    @Override
    protected void onDestroy() {
        mWebView.destroy();
        mWebView = null;
        super.onDestroy();
    }
}
