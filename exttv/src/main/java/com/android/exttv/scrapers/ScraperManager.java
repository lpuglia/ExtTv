package com.android.exttv.scrapers;

import android.util.Log;
import android.webkit.JavascriptInterface;

import com.android.exttv.PlayerActivity;
import com.android.exttv.model.Episode;
import com.android.exttv.model.Program;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.android.exoplayer2.upstream.DataSource;

import java.util.Map;

public class ScraperManager extends ScriptEngine{

    private final Episode overrideEpisode;
    public final DisplayerManager displayerManager;
    private final PlayerActivity playerActivity;
    public DataSource.Factory dataSourceFactory;
    private final Program currentProgram;

    public ScraperManager(PlayerActivity playerActivity, Program currentProgram, Episode overrideEpisode) {
        super(playerActivity, currentProgram.getScraperURL());

        this.displayerManager = new DisplayerManager(playerActivity, this, currentProgram.isLive());
        this.overrideEpisode = overrideEpisode;
        this.playerActivity = playerActivity;
        this.currentProgram = currentProgram;

        toastOnUi("Playing " + currentProgram.getVideoUrl());
        init();
    }

    @Override
    public void postFinished() {
        buildClient(currentProgram.requiresProxy());
        this.dataSourceFactory = new OkHttpDataSourceFactory(client);
        if(currentProgram.isLive()){
            webView.evaluateJavascript("getLiveStream('"+currentProgram.getVideoUrl()+"')", null);
        }else{
            webView.evaluateJavascript("scrapeEpisodes('"+currentProgram.getVideoUrl()+"')", null);
            Log.d("asd","Finished Calling scrapeEpisodes");
        }
        if(currentProgram.requiresProxy())
            toastOnUi("Using " + client.proxy().address() + " as proxy");
    }

    @Override
    public void _handleEpisode(Episode episode, boolean play, String title){
        runOnMainLoop(() -> {
            if(play) {
                Episode toPlay = overrideEpisode == null ? episode : overrideEpisode;
                displayerManager.setTopContainer(toPlay);
                scrapeVideoURL(toPlay.getPageURL());
                playerActivity.setCurrentEpisodeCursor();
                playerActivity.setCurrentEpisode(toPlay);
            }
            displayerManager.addData(episode);
        });
        playerActivity.cardsReady = true;
    }
    
    @JavascriptInterface
    public void playStream(String jsonMediaSource){
        Map<String, String> mediaSource = parseJson(jsonMediaSource);
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
    public void cancel() {
        super.cancel();
    }
}
