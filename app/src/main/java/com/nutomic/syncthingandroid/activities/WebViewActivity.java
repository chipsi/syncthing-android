package com.nutomic.syncthingandroid.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.http.SslError;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.nutomic.syncthingandroid.R;
import com.nutomic.syncthingandroid.util.Util;

/**
 * Holds a WebView that shows the web ui of the local syncthing instance.
 */
public class WebViewActivity extends SyncthingActivity {

    private static final String TAG = "WebViewActivity";

    private AlertDialog mSecurityNoticeDialog;
    private Boolean sslNoticeUserDecision = false;

    private WebView mWebView;
    private View mLoadingView;

    private String webPageUrl = "";

    /**
     * Hides the loading screen and shows the WebView once it is fully loaded.
     */
    private final WebViewClient mWebViewClient = new WebViewClient() {

        @Override
        public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
            if (sslNoticeUserDecision) {
                handler.proceed();
                return;
            }
            final DialogInterface.OnClickListener listener = (dialog, which) -> {
                try {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            handler.proceed();
                            sslNoticeUserDecision = true;
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            handler.cancel();
                            finish();
                            break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onReceivedSslError:OnClickListener", e);
                }
            };
            mSecurityNoticeDialog = new AlertDialog.Builder(WebViewActivity.this)
                .setTitle(R.string.security_notice)
                .setMessage(getString(R.string.ssl_cert_invalid_text, webPageUrl))
                .setPositiveButton(R.string.cont, listener)
                .setNegativeButton(R.string.cancel_title, listener)
                .show();
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
    @SuppressLint("SetJavaScriptEnabled")
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_gui);

        webPageUrl = getString(R.string.issue_tracker_url);

        mLoadingView = findViewById(R.id.loading);
        mWebView = findViewById(R.id.webview);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.setWebViewClient(mWebViewClient);
        mWebView.clearCache(true);

        if (mWebView.getUrl() == null) {
            mWebView.stopLoading();
            mWebView.loadUrl(webPageUrl);
        }
    }

    /**
     * Saves current tab index and fragment states.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Util.dismissDialogSafe(mSecurityNoticeDialog, this);
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
