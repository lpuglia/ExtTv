/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.exttv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.os.StrictMode;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;

import com.android.exttv.model.Episode;
import com.android.exttv.model.Program;
import com.android.exttv.model.ProgramDatabase;
import com.android.exttv.scrapers.ScraperManager;
import com.android.exttv.util.AppLinkHelper;
import com.android.exttv.util.RemoteKeyEvent;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.drm.DefaultDrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.ui.CaptionStyleCompat;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.ui.SubtitleView;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.squareup.picasso.Picasso;

import org.conscrypt.Conscrypt;

import java.security.Security;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import okhttp3.OkHttpClient;

import static com.android.exttv.util.AppLinkHelper.getEpisodeCursor;
import static com.android.exttv.util.AppLinkHelper.setEpisodeCursor;

public class PlayerActivity extends Activity {

    private PlayerView playerView;
    private DefaultTrackSelector trackSelector;
    private boolean subtitlesEnabled = false;

    private RemoteKeyEvent remoteKeyEvent;
    private Episode currentEpisode;
    private boolean paused = false;

    public SimpleExoPlayer player;
    public ScraperManager scraper;
    public boolean cardsReady = false;

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        return remoteKeyEvent.dispatchKeyEvent(event);
    }


    private Long getCurrentEpisodeCursor(){
        Long position = getEpisodeCursor(currentEpisode, getBaseContext());
        Long duration = currentEpisode.getDurationLong();
        if(duration==0)
            return position;
        if(position < duration-(duration/99)) return position; // if cursor is before 99% of the duration
        return Long.valueOf(0);
    }
    public void setCurrentEpisodeCursor(){
        setEpisodeCursor(player.getCurrentPosition(), currentEpisode, getBaseContext());
    }

    public void preparePlayer(Map<String, String> mediaSource){
        for (Map.Entry<String, String> entry : mediaSource.entrySet()) {
            Log.d("mediasource", entry.getKey() + ": " + entry.getValue());
        }
        if(player != null){
            player.stop(true);
            if(!paused)
                player.setPlayWhenReady(true);
            else
                paused = false;

            if(mediaSource == null) return;

            DefaultDrmSessionManager drmManager = null;
            if(mediaSource.containsKey("DRM") && mediaSource.containsKey("License")){
                HttpMediaDrmCallback playreadyCallback = new HttpMediaDrmCallback( mediaSource.get("License"), (HttpDataSource.Factory) scraper.dataSourceFactory);
                if(mediaSource.containsKey("Preauthorization"))
                    playreadyCallback.setKeyRequestProperty("preauthorization", mediaSource.get("Preauthorization"));

                drmManager = new DefaultDrmSessionManager.Builder()
                        .setUuidAndExoMediaDrmProvider(mediaSource.get("DRM").equals("widevine") ? C.WIDEVINE_UUID : C.CLEARKEY_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                        .build(playreadyCallback);
            }

            MediaSource ms = null;
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(mediaSource.get("Source")));

            if(mediaSource.containsKey("StreamType")){
                switch (mediaSource.get("StreamType")) {
                    case "Dash":
                        DashMediaSource.Factory dashMediaSource = new DashMediaSource.Factory(scraper.dataSourceFactory);
                        if (drmManager != null)
                            dashMediaSource.setDrmSessionManager(drmManager);
                        if (mediaSource.containsKey("Source")) {
                            ms = dashMediaSource.createMediaSource(mediaItem);
                        }
                        break;
                    case "Hls":
                        ms = new HlsMediaSource.Factory(scraper.dataSourceFactory).createMediaSource(mediaItem);
                        break;
                    case "Extractor":
                        ms = new ProgressiveMediaSource.Factory(scraper.dataSourceFactory).createMediaSource(mediaItem);
                        break;
                    case "Default":
                        ms = new DefaultMediaSourceFactory(scraper.dataSourceFactory).createMediaSource(mediaItem);
                        break;
                }
                player.setMediaSource(ms);
                player.prepare();
            }
            if(currentEpisode!=null) {
                Long position = getCurrentEpisodeCursor();
                if (position != 0) player.seekTo(position);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Gson gson = new Gson();
        SharedPreferences  mPrefs = getSharedPreferences("test", MODE_PRIVATE);
        String json = mPrefs.getString("programs", "");
        java.lang.reflect.Type type = new TypeToken<LinkedHashMap<Integer, Program>>(){}.getType();
        ProgramDatabase.programs = gson.fromJson(json, type);

        Security.insertProviderAt(Conscrypt.newProvider(), 1); //without this I get handshake error

        // disable strict mode because ScraperManager.postfinished may need to scrape a proxy when onDemand is called
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_player);
        playerView = findViewById(R.id.video_view);

        Intent intent = getIntent();
        Uri data = intent.getData();
//        Bundle extras = intent.getExtras();

//        if (extras != null) {
//            for (String key : extras.keySet()) {
//                Object value = extras.get(key);
//                Log.d("IntentExtras", String.format("%s %s (%s)", key,
//                        value.toString(), value.getClass().getName()));
//            }
//        }

        // Check if the intent and data are not null
        if (intent != null && data != null) {
            String uriString = data.toString();
            Log.d("PlayerActivity", "Full Intent: " + intent.toString());
            Log.d("PlayerActivity", "Video URI: " + uriString);
            if(uriString.startsWith("tvrecommendation")){
                Program currentProgram = setCurrentProgramFromIntent(data);
                if(currentProgram==null){ //for sync program
                    finish();
                    return;
                }
                initializePlayer(currentProgram.isLive());

                startScraper(currentProgram);

                if(currentProgram.isLive()){
                    for(Program p : ProgramDatabase.programs.values()){
                        Picasso.with(getBaseContext()).load(p.getLogo()).fetch(); // pre-fetch all the logo images
                    }
                }
            }else{
                Log.d("PlayerActivity", "No intent or data available");
                initializePlayer(false);
//                if(mediaSource.containsKey("Source")) toastOnUi("Media source: " + mediaSource.get("Source"));
//                else toastOnUi("Null media source");
                Map<String, String> mediaSource = new HashMap<>();
                String extension = uriString.substring(uriString.lastIndexOf(".") + 1);
                switch (extension.toLowerCase()) {
                    case "mp4":
                    case "mkv":
                        // Handle .mp4 files
                        mediaSource.put("StreamType", "Default");
                        mediaSource.put("Source", uriString);
                        break;
                    case "mpd":
                        // Handle .mpd files
                        mediaSource.put("StreamType", "Dash");
                        mediaSource.put("Source", uriString);
                        break;
                    case "m3u8":
                        // TODO Handle .m3u8 files (notice there is no break)
                    default: // not idea, need to improve
                        mediaSource.put("StreamType", "Hls");
                        mediaSource.put("Source", uriString);
                        break;
                }
                currentEpisode = new Episode().setPageURL(uriString).setTitle("External Video Stream").setDescription(uriString).setAirDate(new GregorianCalendar());
                Program program = new Program().setType("OnDemand").setVideoUrl(uriString).setEpisode(currentEpisode);
                remoteKeyEvent = new RemoteKeyEvent(this, program.isLive(), program.hashCode());
                scraper = new ScraperManager(this, program, currentEpisode);
                scraper.postFinished();
                scraper.displayerManager.setTopContainer(currentEpisode);
                preparePlayer(mediaSource);
            }
        }

    }

    @Override
    protected void onRestart() { //only called at standby thanks to onUserLeaveHint
        super.onRestart();

        setContentView(R.layout.activity_player);
        playerView = findViewById(R.id.video_view);

        Program currentProgram = setCurrentProgramFromIntent(getIntent().getData());
        paused = false;
        initializePlayer(currentProgram.isLive());

        startScraper(currentProgram, currentEpisode);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private Program setCurrentProgramFromIntent(Uri intentUri){
        AppLinkHelper.AppLinkAction action = AppLinkHelper.extractAction(intentUri);
        Program program = null;
        if (AppLinkHelper.PLAYBACK.equals(action.getAction())) {
            AppLinkHelper.PlaybackAction paction = (AppLinkHelper.PlaybackAction) action;
            program = ProgramDatabase.programs.get((int) paction.getMovieId());
            remoteKeyEvent = new RemoteKeyEvent(this, program.isLive(), program.hashCode());

            if (program.isLive()) {
                ImageView watermark = findViewById(R.id.watermark);
                Picasso.with(getBaseContext()).load(program.getLogo()).into(watermark);
            }
//        } else if (AppLinkHelper.BROWSE.equals(action.getAction())) {
        } else {
            throw new IllegalArgumentException("Invalid Action " + action);
        }
        Set<String> plugin_files = new LinkedHashSet<>();
        PersistableBundle bundle = new PersistableBundle ();
        for(Program p : ProgramDatabase.programs.values()){
            plugin_files.add(p.getScraperURL());
            bundle.putLong(p.getType(), p.getChannelId());
        }
        SyncProgramsJobService syncProgramsJobService = new SyncProgramsJobService();

        for(String p : plugin_files)
            syncProgramsJobService.idMap.put(p, new HashSet<String>());
        for(String p : plugin_files) {
            syncProgramsJobService.syncProgramManager.add(syncProgramsJobService.new SyncProgramManager(this, p, bundle));
        }
        return program;
    }

    public void showLogIn(Program currentProgram){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View dialogView = inflater.inflate(R.layout.popup_login, null);
        builder.setView(dialogView)
                // Add action buttons
                .setPositiveButton("SignIn", (dialog, id) -> {
                    SharedPreferences prefs = getApplicationContext().getSharedPreferences("com.android.exttv", 0);
                    SharedPreferences.Editor editor = prefs.edit();
                    EditText editText = dialogView.findViewById(R.id.username);
                    editor.putString("username", editText.getText().toString());
                    editText = dialogView.findViewById(R.id.password);
                    editor.putString("password", editText.getText().toString());
                    editor.apply();
                    scrape(currentProgram);
                })
                .setNegativeButton("cancel", (dialog, id) -> scrape(currentProgram));
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void startScraper(Program currentProgram){
        startScraper(currentProgram, null);
    }

    public void startScraper(Program currentProgram, Episode episode){
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("com.android.exttv", 0);
        if(currentProgram.requiresProxy() && !(prefs.contains("username") && prefs.contains("password"))) {
                showLogIn(currentProgram);
        }else
            scrape(currentProgram, episode);
    }

    private void scrape(Program currentProgram){
        scrape(currentProgram, null);
    }

    private void scrape(Program currentProgram, Episode episode){
        player.stop(true);
        if(scraper!=null){
            scraper.cancel();
            scraper = null;
        }

        scraper = new ScraperManager(this, currentProgram, episode);
    }

    public void setCurrentEpisode(Episode episode) {
        currentEpisode = episode;
    }

    @Override
    public void onPause() {
        super.onPause();
        player.setPlayWhenReady(false);
        if(currentEpisode!=null){ //if on-demand program
            setCurrentEpisodeCursor();
        }

    }


    @Override
    protected void onUserLeaveHint() { // this function is only called on home button press, not called on standby
        super.onUserLeaveHint();
        returnHomeScreen();
    }

    private boolean leaving = false;
    public void returnHomeScreen() {
        leaving = true;
        scraper.cancel(); // This ensure the release of all network resources;
        finish();
    }

    @Override
    public void onStop() {
        super.onStop();
        if(scraper.isLive()){
            player.release();
            finish();
        }else{
            player.release();
            paused=true;
            cardsReady = false;
            scraper.cancel();
            scraper = null;
        }

    }

    int previousPlaybackState  = ExoPlayer.STATE_IDLE;
    boolean previousState = false;

    private int findTextRendererIndex() {
        for (int i = 0; i < player.getRendererCount(); i++) {
            if (player.getRendererType(i) == C.TRACK_TYPE_TEXT) {
                return i;
            }
        }
        return -1; // Text renderer not found
    }

    private void enableSubtitlesByDefault() {
        MappingTrackSelector.MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
        if (mappedTrackInfo == null) {
            return; // No tracks available
        }

        // Assuming the text renderer is at a conventional index (often 2, but this can vary)
        int rendererIndex = findTextRendererIndex();
        if (rendererIndex == -1) {
            return; // No text renderer found
        }
        DefaultTrackSelector.Parameters params = trackSelector.getParameters();
        params.buildUpon().setRendererDisabled(rendererIndex, true);
    }

    public void toggleSubtitles() {
        // Obtain the current track selection parameters from the track selector
        DefaultTrackSelector.Parameters params = trackSelector.getParameters();

        // Identify the text (subtitle) renderer index
        int textRendererIndex = -1;
        for (int i = 0; i < player.getRendererCount(); i++) {
            if (player.getRendererType(i) == C.TRACK_TYPE_TEXT) {
                textRendererIndex = i;
                break;
            }
        }

        if (textRendererIndex == -1) {
            // No text renderer found, can't toggle subtitles
            return;
        }

        // Toggle the enabling state of the text renderer
        boolean isDisabled = params.getRendererDisabled(textRendererIndex);
        DefaultTrackSelector.ParametersBuilder parametersBuilder = params.buildUpon()
                .setRendererDisabled(textRendererIndex, !isDisabled);
        parametersBuilder.clearSelectionOverrides();

        // Apply the changes to the track selector
        trackSelector.setParameters(parametersBuilder);
    }

    private void customizeSubtitlesAppearance() {
        // Example customization: White text, semi-transparent black background
        CaptionStyleCompat style = new CaptionStyleCompat(Color.YELLOW, Color.TRANSPARENT, Color.TRANSPARENT,
                CaptionStyleCompat.EDGE_TYPE_OUTLINE, Color.BLACK, null);

        SubtitleView subtitleView = playerView.getSubtitleView();
        if (subtitleView != null) {
            subtitleView.setStyle(style);
        }
    }

    private void initializePlayer(boolean isLive) {

        ProgressBar progressBar = findViewById(R.id.progress_bar);

        ShapeDrawable circle = new ShapeDrawable(new OvalShape());
        circle.getPaint().setColor(Color.parseColor("#DDDDDD"));

        ImageButton play = findViewById(R.id.exo_play);
        play.setBackground(circle);
        Drawable drawable = play.getDrawable();
        drawable.setTintList(ColorStateList.valueOf(Color.parseColor("#222222")));

        ImageButton pause = findViewById(R.id.exo_pause);
        pause.setBackground(circle);
        drawable = pause.getDrawable();
        drawable.setTintList(ColorStateList.valueOf(Color.parseColor("#222222")));

        trackSelector = new DefaultTrackSelector(this);
        player = new SimpleExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build();

        playerView.setControllerShowTimeoutMs(0);

        player.addListener(new ExoPlayer.EventListener() {
            private void showUI(){
                if(handler != null)
                    handler.removeCallbacksAndMessages(null);
                ViewGroup hiddenPanel = (ViewGroup) findViewById(R.id.top_container);
                if(hiddenPanel.getVisibility() != View.VISIBLE) {
                    findViewById(R.id.playpause).setVisibility(View.VISIBLE);
                    Animation bottomUp = AnimationUtils.loadAnimation(getBaseContext(),
                            R.anim.controls_pop_in_top);
                    hiddenPanel.startAnimation(bottomUp);
                    hiddenPanel.setVisibility(View.VISIBLE);

                    bottomUp = AnimationUtils.loadAnimation(getBaseContext(),
                            R.anim.controls_pop_in);
                    hiddenPanel = (ViewGroup) findViewById(R.id.control_container);
                    hiddenPanel.startAnimation(bottomUp);
                    hiddenPanel.setVisibility(View.VISIBLE);
                }
            }
            Handler handler;
            private void hideUI(){
                handler = new Handler(Looper.getMainLooper());
                handler.postDelayed((Runnable) () -> {
                    ViewGroup hiddenPanel = (ViewGroup) findViewById(R.id.top_container);
                    if(hiddenPanel.getVisibility() == View.VISIBLE) {
                        findViewById(R.id.playpause).setVisibility(View.INVISIBLE);
                        Animation bottomUp = AnimationUtils.loadAnimation(getBaseContext(),
                                R.anim.controls_pop_out_top);
                        if (hiddenPanel.getVisibility() == View.VISIBLE) {
                            hiddenPanel.startAnimation(bottomUp);
                            hiddenPanel.setVisibility(View.INVISIBLE);
                        }
                        bottomUp = AnimationUtils.loadAnimation(getBaseContext(),
                                R.anim.controls_pop_out);
                        hiddenPanel = (ViewGroup) findViewById(R.id.control_container);
                        if (hiddenPanel.getVisibility() == View.VISIBLE) {
                            hiddenPanel.startAnimation(bottomUp);
                            hiddenPanel.setVisibility(View.INVISIBLE);
                        }
                    }
                }, 3000);
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                ImageView watermark = findViewById(R.id.watermark);

                if(isLive) {
                    if (!(playWhenReady && playbackState == player.STATE_READY)){
                        findViewById(R.id.playpause).setVisibility(View.VISIBLE);
                    } else {
                        findViewById(R.id.playpause).setVisibility(View.INVISIBLE);
                    }
                }else{
                    if(playbackState==ExoPlayer.STATE_ENDED){
                        scraper.displayerManager.playNextEpisode(currentEpisode);
                    }else if(!leaving) {
                        if ((previousState && previousPlaybackState == player.STATE_READY) && playbackState == ExoPlayer.STATE_BUFFERING) {
                            showUI();
                        } else if (previousPlaybackState == ExoPlayer.STATE_BUFFERING && (playWhenReady && playbackState == player.STATE_READY)) {
                            hideUI();
                        } else if ((previousPlaybackState == player.STATE_READY && playbackState == ExoPlayer.STATE_BUFFERING) ||
                                (playbackState == player.STATE_READY && previousPlaybackState == ExoPlayer.STATE_BUFFERING)) {
                            //DO NOTHING
                        } else if (!(playWhenReady && playbackState == player.STATE_READY) && playbackState != ExoPlayer.STATE_IDLE && playbackState != ExoPlayer.STATE_BUFFERING) {
                            showUI();
                        } else if (playWhenReady && playbackState == player.STATE_READY) {
                            hideUI();
                        }
                    }
                }

                switch(playbackState){
                    case ExoPlayer.STATE_IDLE:
                        Log.d("STATE", "STATE_IDLE");
                        break;
                    case ExoPlayer.STATE_BUFFERING:
                        Log.d("STATE", "STATE_BUFFERING");
                        break;
                    case ExoPlayer.STATE_READY:
                        if(playWhenReady){
                            Log.d("STATE", "STATE_READY_PLAY");
                        }else{
                            Log.d("STATE", "STATE_READY_PAUSE");
                        }
                        break;
                    case ExoPlayer.STATE_ENDED:
                        Log.d("STATE", "STATE_ENDED");
                        break;
                }

                if (playbackState == ExoPlayer.STATE_BUFFERING ) {
                    progressBar.setVisibility(View.VISIBLE);
                    watermark.setVisibility(View.VISIBLE);
                } else if (playbackState == ExoPlayer.STATE_READY ){
                    progressBar.setVisibility(View.INVISIBLE);
                    watermark.setVisibility(View.INVISIBLE);
                }
                previousState = playWhenReady;
                previousPlaybackState = playbackState;
            }
        });
        enableSubtitlesByDefault();
        findViewById(R.id.btnToggleSubtitles).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSubtitles();
            }
        });

        customizeSubtitlesAppearance();

        playerView.setPlayer(player);

    }

}
