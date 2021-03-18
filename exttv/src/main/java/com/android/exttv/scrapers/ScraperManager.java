package com.android.exttv.scrapers;

import android.content.SharedPreferences;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;

import com.android.exttv.DelegatingSocketFactory;
import com.android.exttv.PlayerActivity;
import com.android.exttv.model.Episode;
import com.android.exttv.model.Program;
import com.android.exttv.util.ProxyProvider;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.DataSource;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.CookieManager;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.net.ssl.SSLSocketFactory;

import okhttp3.Authenticator;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ScraperManager {

    private final PlayerActivity playerActivity;
    private final Program currentProgram;
    private final Handler setupHandler;
    private final Runnable runnable;
    private final Episode overrideEpisode;

    public DataSource.Factory dataSourceFactory;
    public DisplayerManager displayerManager;
    private OkHttpClient client;
    protected Map<String,String> headers = new HashMap<>();
    private WebAppInterface webAppInterface;

    public ScraperManager(PlayerActivity playerActivity, WebView webView, Program currentProgram, Episode overrideEpisode) {
        this.playerActivity = playerActivity;
        this.currentProgram = currentProgram;
        this.displayerManager = new DisplayerManager(playerActivity, this, currentProgram.isLive());
        this.overrideEpisode = overrideEpisode;

        ScraperManager scraperManager = this;
        setupHandler = new Handler();
        runnable = () -> {
            OkHttpClient.Builder clientb = initClientBuilder();
            if(currentProgram.requiresProxy()){
                initClientProxy(clientb);
            }
            clientb.cookieJar(new JavaNetCookieJar(new CookieManager()));
            client = clientb.build();
            dataSourceFactory = new OkHttpDataSourceFactory(client);
            webAppInterface = new WebAppInterface(scraperManager, webView, currentProgram, playerActivity);
            toastOnUi("Playing " + currentProgram.getVideoUrl());
        };
        setupHandler.post(runnable);
    }

    protected void handleEpisode(Episode episode, boolean play){
        runOnUiThread(() -> {
            if(play) {
                Episode toPlay = overrideEpisode == null ? episode : overrideEpisode;
                displayerManager.setTopContainer(toPlay);
                webAppInterface.scrapeVideoURL(toPlay.getPageURL());
                playerActivity.setCurrentEpisode(toPlay);
            }
            displayerManager.addData(episode);
        });
        playerActivity.cardsReady = true;
    }

    protected void playStream(Map<String, String> mediaSource){
        runOnUiThread(() -> {
            if(mediaSource.containsKey("Source")) toastOnUi("Media source: " + mediaSource.get("Source"));
            else toastOnUi("Null media source");
            playerActivity.preparePlayer(mediaSource);
        });
    }

    protected OkHttpClient.Builder initClientBuilder(){
        return new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS);
    }

    protected void initClientProxy(OkHttpClient.Builder clientb) {
        SharedPreferences prefs = playerActivity.getApplicationContext().getSharedPreferences("com.android.exttv", 0);
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
            String best_proxy = ProxyProvider.getBest(this);

            toastOnUi("Using " + best_proxy + " as proxy");

            clientb.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(best_proxy, 89)))
                    .proxyAuthenticator(proxyAuthenticator)
                    .socketFactory(new DelegatingSocketFactory(SSLSocketFactory.getDefault())); // important for HTTPS proxy
        }
    }

    private Request getRequest(String url){
        Request.Builder requestb = new Request.Builder().url(url).get();
        for(Map.Entry<String, String> a : headers.entrySet())
            requestb.addHeader(a.getKey(), a.getValue());

        return requestb.tag("REQUEST").build();
    }

    public String getResponse(String url){
        Log.d("asd","ask response");
        Request request = getRequest(url);
        String stringResponse = "failed";
        try {
            Response response = client.newCall(request).execute();
            stringResponse = response.body().string();
            response.close();
        } catch (IOException e) {
            toastOnUi("Failed to scrape: " + url);
            e.printStackTrace();
        }
        Log.d("asd","return response");
        return stringResponse;
    }

    public void getResponseAsync(String url, Consumer<String> consumer) {
        Log.d("asd","ask response");
        Request request = getRequest(url);

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                String string = response.body().string();
                Log.d("asd","return response");
                consumer.accept(string);
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
//                toastOnUi("Failed to scrape: " + url);
                e.printStackTrace();
            }
        });

    }

    public Program getCurrentProgram() {
        return currentProgram;
    }

    public void toastOnUi(String message){
        displayerManager.toastOnUi(message);
    }

    protected void runOnUiThread(Runnable runnable){
        displayerManager.runOnUiThread(runnable);
    }

    public void cancel() {
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
        setupHandler.removeCallbacks(runnable);
        webAppInterface.cancel();
    }
}
