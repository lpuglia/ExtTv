package com.android.exttv.scrapers;

import android.os.Handler;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.android.exttv.PlayerActivity;
import com.android.exttv.model.Episode;
import com.android.exttv.model.Plugin;
import com.android.exttv.model.Program;
import com.android.exttv.util.SSLWebViewClient;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.DataSource;

import java.net.CookieManager;
import java.util.Map;

import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;

public class ScraperManager extends ScriptEngine{

    private final Episode overrideEpisode;
    public DisplayerManager displayerManager;
    private PlayerActivity playerActivity;
    public DataSource.Factory dataSourceFactory;
    private Program currentProgram;
    public Handler setupHandler;
    public Runnable runnable;


    public ScraperManager(PlayerActivity playerActivity, WebView webView, Program currentProgram, Episode overrideEpisode) {
        super(playerActivity, webView);
        this.displayerManager = new DisplayerManager(playerActivity, this, currentProgram.isLive());
        this.overrideEpisode = overrideEpisode;
        this.playerActivity = playerActivity;
        this.currentProgram = currentProgram;

        setupHandler = new Handler();
        runnable = () -> {
            plugin = new Plugin(currentProgram.getScraperURL(), context);
            OkHttpClient.Builder clientb = initClientBuilder();
            if(currentProgram.requiresProxy()){
                toastOnUi("Using " + initClientProxy(clientb) + " as proxy");
            }
            clientb.cookieJar(new JavaNetCookieJar(new CookieManager()));
            client = clientb.build();

            dataSourceFactory = new OkHttpDataSourceFactory(client);

            webView.addJavascriptInterface(this, "android");
            ((SSLWebViewClient)webView.getWebViewClient()).pageFinished = pageFinished;
            webView.loadData("<script>"+plugin.getScript()+"</script>", "text/html", "utf-8");

//            webView.evaluateJavascript(plugin.getScript(), new ValueCallback<String>() {
//                @Override
//                public void onReceiveValue(String s) {
//                    pageFinished.run();
//                }
//            });

        };
        setupHandler.post(runnable);

        toastOnUi("Playing " + currentProgram.getVideoUrl());
    }

    @Override
    public void _handleEpisode(Episode episode, boolean play, String title){
        runOnMainLoop(() -> {
            if(play) {
                Episode toPlay = overrideEpisode == null ? episode : overrideEpisode;
                displayerManager.setTopContainer(toPlay);
                scrapeVideoURL(toPlay.getPageURL());
                playerActivity.setCurrentEpisode(toPlay);
            }
            displayerManager.addData(episode);
        });
        playerActivity.cardsReady = true;
    }

    @Override
    public void _playStream(Map<String, String> mediaSource){
        runOnMainLoop(() -> {
            if(mediaSource.containsKey("Source")) toastOnUi("Media source: " + mediaSource.get("Source"));
            else toastOnUi("Null media source");
            playerActivity.preparePlayer(mediaSource);
        });
    }

    public void toastOnUi(String message){
        displayerManager.toastOnUi(message);
    }

    @Override
    public void postFinished() {
        if(!webView.getUrl().equals("about:blank")){
            if(currentProgram.isLive()){
                webView.loadUrl("javascript:getLiveStream('"+currentProgram.getVideoUrl()+"')");
            }else{
                webView.loadUrl("javascript:scrapeEpisodes('"+currentProgram.getVideoUrl()+"')");
                Log.d("asd","Finished Calling scrapeEpisodes");
            }
        }
    }

    @Override
    public void cancel() {
        super.cancel();
        setupHandler.removeCallbacks(runnable);
    }
}
