package com.android.exttv;

import android.annotation.SuppressLint;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.PersistableBundle;
import android.os.StrictMode;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;

import androidx.tvprovider.media.tv.PreviewProgram;
import androidx.tvprovider.media.tv.TvContractCompat;

import com.android.exttv.model.Episode;
import com.android.exttv.model.Program;
import com.android.exttv.scrapers.ScriptEngine;
import com.android.exttv.util.AppLinkHelper;
import com.google.gson.Gson;

import org.conscrypt.Conscrypt;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.security.Security;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class SyncProgramsJobService extends JobService {

    Map<String, Set<String>> idMap = new HashMap<>();
    private final Map<Integer, Program> programMap = new LinkedHashMap<>();
    private final Map<Integer, Program> onDemand = new HashMap<>();

    private String host = "172.24.192.1";

    ArrayList<String> plugins = new ArrayList<String>() {{
        add("http://"+host+"/plugins/la7plugin.js");
        add("http://"+host+"/plugins/raiplugin.js");
        add("http://"+host+"/plugins/mediasetplugin.js");
        add("http://"+host+"/plugins/discoveryplugin.js");
    }};

    public class SyncProgramManager extends ScriptEngine{

        private final PersistableBundle bundle;
        private final String pluginURl;

        public SyncProgramManager(JobService jobService, String pluginUrl, PersistableBundle bundle) {
            super(jobService, pluginUrl);
            this.bundle = bundle;
            this.pluginURl = pluginUrl;
            init();
        }

        @Override
        public void postFinished() {
            webView.evaluateJavascript("(function() { if (typeof pluginRequiresProxy == 'undefined') {pluginRequiresProxy=false}; return {pluginRequiresProxy}; })();", new ValueCallback<String>() {
                @Override public void onReceiveValue(String name) {
                    try { buildClient(new JSONObject(name).getBoolean("pluginRequiresProxy"));
                    } catch (JSONException e) { e.printStackTrace(); }
                }});
            webView.evaluateJavascript("(function() { if (typeof name == 'undefined') {name='undefined'}; return {name}; })();", new ValueCallback<String>() {
                @Override public void onReceiveValue(String name) {
                    try { plugin.setName(new JSONObject(name).getString("name"));
                    } catch (JSONException e) { e.printStackTrace(); }
                }});
            webView.evaluateJavascript("(function() { if (typeof programs == 'undefined') {programs=[]}; return programs; })();", new ValueCallback<String>() {
                @Override public void onReceiveValue(String jsonPrograms) { getPrograms(jsonPrograms); }
            });
        }

        public void getPrograms(String jsonPrograms) {
            ArrayList<Program> listPrograms = new ArrayList<>();
            try {
                JSONArray jObject = new JSONArray(jsonPrograms);
                for (int k = 0; k < jObject.length(); k++) {
                    JSONObject jsonProgram = jObject.getJSONObject(k);
                    Iterator<?> keys = jsonProgram.keys();
                    Program program = new Program();

                    while( keys.hasNext() ){
                        String key = (String)keys.next();
                        switch (key){
                            case "Title": program.setTitle(jsonProgram.getString(key)); break;
                            case "Description": program.setDescription(jsonProgram.getString(key)); break;
                            case "Type": program.setType(jsonProgram.getString(key)); break;
                            case "VideoUrl": program.setVideoUrl(jsonProgram.getString(key)); break;
                            case "Logo" : program.setLogo(jsonProgram.getString(key)); break;
                            case "CardImageUrl": program.setCardImageUrl(jsonProgram.getString(key)); break;
                            case "RequireProxy": program.setRequireProxy(Boolean.parseBoolean(jsonProgram.getString(key))); break;
                        }
                        program.setChannelId(bundle.getLong(program.getType()))
                               .setScraperURL(plugin.getName()+".js");
                    }
                    listPrograms.add(program);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            // Search Live programs and remove
            Iterator<Program> i = listPrograms.iterator();
            while (i.hasNext()) {
                Program p = i.next();
                if(p.isLive()){
                    programMap.put(p.hashCode(), p);
                    i.remove();
                }
            }
            for(Program p : listPrograms){
                onDemand.put(p.hashCode(), p);
                idMap.get(pluginURl).add(p.getTitle());
            }
            for(Program p : listPrograms)
                runOnMainLoop(() -> webView.evaluateJavascript(
                        "scrapeLastEpisode('" + p.getVideoUrl() + "','" + p.getTitle() + "')"+
                               ".then(response => {"+
                                    "addEpisode(response, true, '"+p.getTitle()+"')" +
                                               ".catch(err => console.log(err))" +
                                               ".then(() => android.finalize('"+p.getTitle()+"'))" +
                                "})",
                        null));

            if(idMap.get(pluginURl).isEmpty()){
                idMap.remove(pluginURl);
                if(idMap.isEmpty()) setPrograms();
            }
        }

        @JavascriptInterface
        public void finalize(String title){
            idMap.get(pluginURl).remove(title);
            if(idMap.get(pluginURl).isEmpty()) idMap.remove(pluginURl);
            if(idMap.isEmpty()) setPrograms();
        }

        @SuppressLint("RestrictedApi")
        @Override public void _handleEpisode(Episode episode, boolean play, String title) {
            Program program = onDemand.get(Objects.hash(title));
            program.setEpisode(episode);
        }

        @SuppressLint("RestrictedApi")
        private void setPrograms() {
            List<Program> onDemandPrograms = new ArrayList<>(onDemand.values());
            Collections.sort(onDemandPrograms);
            for(Program p : onDemandPrograms) programMap.put(p.hashCode(), p);

            SharedPreferences mPrefs = getSharedPreferences("test", MODE_PRIVATE);
            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            prefsEditor.remove("programs");
            prefsEditor.apply();
            Gson gson = new Gson();
            String json = gson.toJson(programMap);
            prefsEditor.putString("programs", json);
            prefsEditor.apply();

            boolean found = false;
//            Cursor cursor = getContentResolver().query(TvContractCompat.PreviewPrograms.CONTENT_URI, null, null, null, null);
//            if (cursor != null && cursor.moveToFirst()) {
//                do {
//                    PreviewProgram previewProgram = PreviewProgram.fromCursor(cursor);
//                    if(previewProgram.getTitle().equals(program.getTitle())){
//                        found = true;
//                        getContentResolver().update(
//                                TvContractCompat.buildPreviewProgramUri(previewProgram.getId()),
//                                createPreviewProgram(program).toContentValues(),
//                                null,
//                                null);
//                        break;
//                    }
//                } while (cursor.moveToNext());
//            }

            Cursor cursor = getContentResolver().query(TvContractCompat.PreviewPrograms.CONTENT_URI, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    PreviewProgram previewProgram = PreviewProgram.fromCursor(cursor);
                    getContentResolver().delete(TvContractCompat.buildPreviewProgramUri(previewProgram.getId()), null, null);
                } while (cursor.moveToNext());
            }
//
            for(Map.Entry<Integer, Program> p : programMap.entrySet())
                if(!found){
                    getContentResolver().insert(
                            TvContractCompat.PreviewPrograms.CONTENT_URI,
                            createPreviewProgram(p.getValue()).toContentValues());
                }
        }

        private PreviewProgram createPreviewProgram(Program program){
            Uri intentUri = AppLinkHelper.buildPlaybackUri(program.getChannelId(), program.hashCode());
            PreviewProgram.Builder builder = new PreviewProgram.Builder().setChannelId(program.getChannelId())
                    .setType(TvContractCompat.PreviewProgramColumns.TYPE_MOVIE)
                    .setLive(program.isLive())
                    .setPosterArtUri(Uri.parse(program.getCardImageUrl()))
                    .setPosterArtAspectRatio(program.getPosterArtAspectRatio())
                    .setLogoUri(Uri.parse(program.getLogo()))
                    .setIntentUri(intentUri);
            String airDate = "";
            if(!program.isLive()) {
                airDate += " - " + program.getEpisode().getAirDate().toZonedDateTime().format(DateTimeFormatter.ofPattern("d MMM uuuu"));

                builder.setDurationMillis((int) program.getEpisode().getDurationLong())
                        .setDescription(program.getEpisode().getDescription())
                        .setThumbnailUri(Uri.parse(program.getEpisode().getThumbURL()))
                        .setEpisodeTitle(program.getEpisode().getTitle());
            }
            builder.setTitle(program.getTitle() + airDate);
            return builder.build();
        }

    }

    List<SyncProgramManager> syncProgramManager = new ArrayList<>(); //keep a reference of syncProgramManager if you don't want your job to be terminated
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Security.insertProviderAt(Conscrypt.newProvider(), 1); //without this I get handshake error
        PersistableBundle bundle = jobParameters.getExtras();

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); //avoid NetworkOnMainThreadException when the job is called from system
        StrictMode.setThreadPolicy(policy);

        for(String p : plugins) idMap.put(p, new HashSet<String>());

        for(String p : plugins) {
            syncProgramManager.add(new SyncProgramManager(this, p, bundle));
        }
//        jobFinished(jobParameters, true);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }
}
