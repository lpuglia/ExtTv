package com.android.exttv.util;

import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SSLWebViewClient extends WebViewClient {
    public Runnable pageFinished;
    @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {handler.proceed();}
    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        pageFinished.run();
    }
}


