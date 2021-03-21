package com.android.exttv.scrapers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.android.exttv.DelegatingSocketFactory;
import com.android.exttv.model.Episode;
import com.android.exttv.model.Plugin;
import com.android.exttv.util.ProxyProvider;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.net.ssl.SSLSocketFactory;

import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public abstract class ScriptEngine {
    public WebView webView;
    public Plugin plugin;

    protected final Context context;
//    protected final Program currentProgram;
    protected OkHttpClient client;
    protected Map<String,String> headers = new HashMap<>();


    public ScriptEngine(Context context, WebView webView) {
        this.webView = webView;
        this.context = context;
    }

    protected Runnable pageFinished = ()->{
        final String promise =
                "function getResponse(url) {\n" +
                        "    return new Promise(resolve => {\n" +
                        "        randomId = 'id'+(Math.random()*100000000000000000).toString(36)\n" +
                        "        window[randomId.toString()] = resolve\n" +
                        "        android.getResponseAsync(url, randomId)\n" +
                        "    }); \n" +
                        "};";
        webView.evaluateJavascript(promise, null);
        webView.loadUrl("javascript:preBackground()");
        postFinished();
    };

    private Map<String, String> parseJson(String jsonMediaSource){
        try {
            Map<String, String> map = new HashMap<>();
            JSONObject jObject = new JSONObject(jsonMediaSource);
            Iterator<?> keys = jObject.keys();

            while( keys.hasNext() ){
                String key = (String)keys.next();
                map.put(key, jObject.getString(key));
            }
            return map;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    @JavascriptInterface
    public void addHeaders(String jsonHeaders){
        Map<String, String> hdrs = parseJson(jsonHeaders);
        for(Map.Entry<String, String> e : hdrs.entrySet()){
            headers.put(e.getKey(), e.getValue());
        }
    }

    @JavascriptInterface
    public void handleEpisode(String episode, boolean play, String title){
        _handleEpisode(new Episode(episode), play, title);
    }

    void scrapeVideoURL(String url){
        webView.loadUrl("javascript:android.playStream(scrapeVideo('"+url+"'))");
    }

    @JavascriptInterface
    public void playStream(String jsonMediaSource){
        _playStream(parseJson(jsonMediaSource));
    }


    protected OkHttpClient.Builder initClientBuilder(){
        return new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS);
    }

    protected String initClientProxy(OkHttpClient.Builder clientb) {
        String best_proxy = "";
        SharedPreferences prefs = context.getSharedPreferences("com.android.exttv", 0);
        if(prefs.contains("username") && prefs.contains("username")) {
            final String username = (prefs.contains("username")) ? prefs.getString("username", null) : "" ;
            final String password = (prefs.contains("password")) ? prefs.getString("password", null) : "" ;

            Authenticator proxyAuthenticator = (route, response) -> {
                String credential = Credentials.basic(username, password);
                return response.request().newBuilder()
                        .header("Proxy-Authorization", credential)
                        .build();
            };

            client = clientb.build();
            best_proxy = ProxyProvider.getBest(this);

//            toastOnUi("Using " + best_proxy + " as proxy");

            clientb.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(best_proxy, 89)))
                    .proxyAuthenticator(proxyAuthenticator)
                    .socketFactory(new DelegatingSocketFactory(SSLSocketFactory.getDefault())); // important for HTTPS proxy
        }
        return best_proxy;
    }

    private Request getRequest(String url){
        Request.Builder requestb = new Request.Builder().url(url).get();
        for(Map.Entry<String, String> a : headers.entrySet())
            requestb.addHeader(a.getKey(), a.getValue());

        return requestb.tag("REQUEST").build();
    }

    @JavascriptInterface
    public String getResponse(String url) {
//        Log.d("asd","ask response");
        Request request = getRequest(url);
        String stringResponse = "failed";
        try {
            Response response = client.newCall(request).execute();
            stringResponse = response.body().string();
            response.close();
        } catch (IOException e) {
//            toastOnUi("Failed to scrape: " + url);
            e.printStackTrace();
        }
//        Log.d("asd","return response");
        return stringResponse;
    }

    @JavascriptInterface
    public void getResponseAsync(String url, String id) {
        Consumer<String> consumer = (response) -> {
            String escapedResponse = response.replace("\\","\\\\").replace("'","\\'");
            runOnMainLoop(() -> webView.loadUrl("javascript:window['"+ id +"']('"+escapedResponse+"')"));
        };
//        Log.d("asd","ask response");
        Request request = getRequest(url);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String string = response.body().string();
//                Log.d("asd","return response");
                consumer.accept(string);
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
//                toastOnUi("Failed to scrape: " + url);
                e.printStackTrace();
            }
        });
    }


    @SuppressLint("SetJavaScriptEnabled")
    public void cancel(){
//        if(client != null){
//            for (Call call : client.dispatcher().queuedCalls()) {
//                if (call.request().tag().equals("REQUEST"))
//                    call.cancel();
//            }
//
//            for (Call call : client.dispatcher().runningCalls()) {
//                if (call.request().tag().equals("REQUEST"))
//                    call.cancel();
//            }
//        }
        webView.stopLoading();
        webView.getSettings().setJavaScriptEnabled(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("about:blank");
    }

    public boolean isLive(){
        return true;
    }

    protected void runOnMainLoop(Runnable runnable){
        new Handler(context.getMainLooper()).post(runnable);
    };

    public abstract void _playStream(Map<String, String> mediaSource);
    public abstract void _handleEpisode(Episode episode, boolean play, String title);
    public abstract void postFinished();
}
