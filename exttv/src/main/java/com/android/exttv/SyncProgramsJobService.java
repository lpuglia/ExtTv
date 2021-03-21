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
import android.webkit.WebView;

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

import java.net.CookieManager;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;

public class SyncProgramsJobService extends JobService {

    private int pluginCounter = 0;

    ArrayList<String> plugins = new ArrayList<String>() {{
        add("http://172.26.96.1/plugins/la7plugin.js");
        add("http://172.26.96.1/plugins/raiplugin.js");
        add("http://172.26.96.1/plugins/mediasetplugin.js");
        add("http://172.26.96.1/plugins/discoveryplugin.js");
    }};

    public class SyncProgramManager extends ScriptEngine{

        private PersistableBundle bundle;

        public SyncProgramManager(JobService jobService, WebView webView, String pluginUrl, PersistableBundle bundle) {
            super(jobService, webView);
            this.bundle = bundle;
            plugin = new Plugin(pluginUrl, context);
            OkHttpClient.Builder clientb = initClientBuilder();

            clientb.cookieJar(new JavaNetCookieJar(new CookieManager()));
            client = clientb.build();

            webView.addJavascriptInterface(this, "android");
//            ((SSLWebViewClient)webView.getWebViewClient()).pageFinished = pageFinished;
//            webView.loadData(plugin.getScript(), "text/html", "utf-8");
            webView.evaluateJavascript(plugin.getScript(), new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String s) {
                    Log.d("asd", "asd");
                    pageFinished.run();
                }
            });
        }

        @Override
        public void postFinished() {
//            if(!webView.getUrl().equals("about:blank")){
                Log.d("asd", String.valueOf(pluginCounter));
                webView.loadUrl("javascript:android.getName(name)");
                webView.loadUrl("javascript:if (typeof programs == 'undefined') {programs=[]}"); //define programs if it is undefined
                webView.loadUrl("javascript:android.getPrograms(JSON.stringify(programs))");
//            }
        }

        @JavascriptInterface
        public void getName(String name) {
            plugin.setName(name);
        }

        @Override public void _playStream(Map<String, String> mediaSource) {}

        /** Show a toast from the web page */
        @JavascriptInterface
        public void getPrograms(String jsonPrograms) {
//            Log.d("asd", String.valueOf(jsonPrograms));
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
                    runOnMainLoop(() -> webView.loadUrl("javascript:scrapeLastEpisode('"+p.getVideoUrl()+"','"+p.getTitle()+"')"));
            }
//            runOnMainLoop(() -> webView.loadUrl("javascript:android.getNextPlugin()"));
        }

//        @JavascriptInterface
//        public void getNextPlugin() {
//            if(pluginCounter < plugins.size()-1){
//                plugin = new Plugin(plugins.get(++pluginCounter), context);
//                runOnMainLoop(() ->
//                        webView.evaluateJavascript(plugin.getScript(), new ValueCallback<String>() {
//                                    @Override
//                                    public void onReceiveValue(String s) {
//                                        pageFinished.run();
//                                    }
//                                }));
//            }
//        }

        @Override public void _handleEpisode(Episode episode, boolean play, String title) {
            Program program = ProgramDatabase.programs.get(Objects.hash(title));
            program.setEpisode(episode);
            addProgram(program);
        }

        @SuppressLint("RestrictedApi")
        protected void addProgram(Program program) {

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
    }

    private PreviewProgram createPreviewProgram(Program program){
        Uri intentUri = AppLinkHelper.buildPlaybackUri(program.getChannelId(), program.hashCode());
        PreviewProgram.Builder builder = new PreviewProgram.Builder().setChannelId(program.getChannelId())
                .setType(TvContractCompat.PreviewProgramColumns.TYPE_MOVIE)
                .setTitle(program.getTitle())
                .setLive(program.isLive())
                .setPosterArtUri(Uri.parse(program.getCardImageUrl()))
                .setPosterArtAspectRatio(program.getPosterArtAspectRatio())
                .setLogoUri(Uri.parse(program.getLogo()));
        if(!program.isLive())
            builder.setDurationMillis((int) program.getEpisode().durationLong)
                    .setDescription(program.getEpisode().getAirDate().toZonedDateTime().format(DateTimeFormatter.ofPattern("d MMM uuuu")) + " - " + program.getEpisode().getDescription())
                    .setThumbnailUri(Uri.parse(program.getEpisode().getThumbURL()))
                    .setEpisodeTitle(program.getEpisode().getTitle())
                    .setIntentUri(intentUri);
        return builder.build();
    }

    List<SyncProgramManager> syncProgramManager = new ArrayList<>(); //keep a reference of syncProgramManager if you don't want your job to be terminated

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        PersistableBundle bundle = jobParameters.getExtras();
        Log.d("asd","onStartJob" + bundle);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build(); //avoid NetworkOnMainThreadException when the job is called from system
        StrictMode.setThreadPolicy(policy);

        for(String p : plugins) {
            WebView webView = new WebView(this);
            webView.getSettings().setJavaScriptEnabled(true);
            syncProgramManager.add(new SyncProgramManager(this, webView, p, bundle));
        }
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        Log.d("asd","onStopJob" + jobParameters);
        return true;
    }
}
