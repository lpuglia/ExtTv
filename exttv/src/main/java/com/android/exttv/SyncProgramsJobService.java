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
import com.android.exttv.model.Plugin;
import com.android.exttv.model.Program;
import com.android.exttv.model.ProgramDatabase;
import com.android.exttv.scrapers.ScriptEngine;
import com.android.exttv.util.AppLinkHelper;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SyncProgramsJobService extends JobService {

    private int pluginCounter = 0;

    ArrayList<String> plugins = new ArrayList<String>() {{
        add("http://172.23.160.1/plugins/la7plugin.js");
        add("http://172.23.160.1/plugins/raiplugin.js");
        add("http://172.23.160.1/plugins/mediasetplugin.js");
        add("http://172.23.160.1/plugins/discoveryplugin.js");
    }};

    public class SyncProgramManager extends ScriptEngine{

        private PersistableBundle bundle;

        public SyncProgramManager(JobService jobService, String pluginUrl, PersistableBundle bundle) {
            super(jobService, pluginUrl, false);
            this.bundle = bundle;
            init();
        }

        @Override
        public void postFinished() {
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
//            Log.d("asd", String.valueOf(listPrograms));
            for(Program p : listPrograms){
                ProgramDatabase.programs.put(p.hashCode(), p);
                if(p.isLive())
                    _handleEpisode(new Episode(""), false, p.getTitle());
                else
                    runOnMainLoop(() -> webView.evaluateJavascript("scrapeLastEpisode('"+p.getVideoUrl()+"','"+p.getTitle()+"')", null));
            }
        }

        @SuppressLint("RestrictedApi")
        @Override public void _handleEpisode(Episode episode, boolean play, String title) {
            Program program = ProgramDatabase.programs.get(Objects.hash(title));
            program.setEpisode(episode);

            SharedPreferences mPrefs = getSharedPreferences("test", MODE_PRIVATE);
            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            prefsEditor.remove("programs");
            prefsEditor.apply();
            Gson gson = new Gson();
            String json = gson.toJson(ProgramDatabase.programs);
            prefsEditor.putString("programs", json);
            prefsEditor.apply();

            boolean found = false;
            Cursor cursor = getContentResolver().query(TvContractCompat.PreviewPrograms.CONTENT_URI, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    PreviewProgram previewProgram = PreviewProgram.fromCursor(cursor);
                    if(previewProgram.getTitle().equals(program.getTitle())){
                        found = true;
                        getContentResolver().update(
                                        TvContractCompat.buildPreviewProgramUri(previewProgram.getId()),
                                        createPreviewProgram(program).toContentValues(),
                                        null,
                                        null);
                        break;
                    }
//                    getContentResolver().delete(TvContractCompat.buildPreviewProgramUri(previewProgram.getId()), null, null);
                } while (cursor.moveToNext());
            }

            if(!found){
                getContentResolver().insert(
                        TvContractCompat.PreviewPrograms.CONTENT_URI,
                        createPreviewProgram(program).toContentValues());
            }
        }

        private PreviewProgram createPreviewProgram(Program program){
            Uri intentUri = AppLinkHelper.buildPlaybackUri(program.getChannelId(), program.hashCode());
            PreviewProgram.Builder builder = new PreviewProgram.Builder().setChannelId(program.getChannelId())
                    .setType(TvContractCompat.PreviewProgramColumns.TYPE_MOVIE)
                    .setTitle(program.getTitle())
                    .setLive(program.isLive())
                    .setPosterArtUri(Uri.parse(program.getCardImageUrl()))
                    .setPosterArtAspectRatio(program.getPosterArtAspectRatio())
                    .setLogoUri(Uri.parse(program.getLogo()))
                    .setIntentUri(intentUri);
            if(!program.isLive())
                builder.setDurationMillis((int) program.getEpisode().getDurationLong())
                        .setDescription(program.getEpisode().getAirDate().toZonedDateTime().format(DateTimeFormatter.ofPattern("d MMM uuuu")) + " - " + program.getEpisode().getDescription())
                        .setThumbnailUri(Uri.parse(program.getEpisode().getThumbURL()))
                        .setEpisodeTitle(program.getEpisode().getTitle());
            return builder.build();
        }

    }

    List<SyncProgramManager> syncProgramManager = new ArrayList<>(); //keep a reference of syncProgramManager if you don't want your job to be terminated
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        PersistableBundle bundle = jobParameters.getExtras();
        Log.d("asd","onStartJob" + bundle);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); //avoid NetworkOnMainThreadException when the job is called from system
        StrictMode.setThreadPolicy(policy);

        for(String p : plugins) {
            syncProgramManager.add(new SyncProgramManager(this, p, bundle));
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d("asd","onStopJob" + jobParameters);
        return true;
    }
}
