/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.android.exttv;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

import androidx.tvprovider.media.tv.PreviewProgram;
import androidx.tvprovider.media.tv.TvContractCompat;

import com.android.exttv.model.Program;
import com.android.exttv.model.ProgramDatabase;
import com.android.exttv.model.Subscription;
import com.android.exttv.scrapers.WebAppInterface;
import com.android.exttv.util.AppLinkHelper;
import com.android.exttv.util.SSLWebViewClient;
import com.android.exttv.util.SharedPreferencesHelper;
import com.android.exttv.util.TvUtil;
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.CookieManager;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import okhttp3.JavaNetCookieJar;
import okhttp3.OkHttpClient;

/*
 * Displays subscriptions that can be added to the main launcher's channels.
 */
public class MainActivity extends Activity {

    private int pluginCounter = 0;
    ArrayList<String> plugins = new ArrayList<String>() {{
        add("La7");
        add("Rai");
    }};

    Map<String, String> plugins_map = new LinkedHashMap<String, String>() {{
        put("La7","https://raw.githubusercontent.com/lpuglia/ExtTv/master/plugins/la7plugin.html");
        put("Rai","https://raw.githubusercontent.com/lpuglia/ExtTv/master/plugins/raiplugin.html");
    }};

    public class mainAppInterface {
        MainActivity mainActivity;
        WebView webView;


        public mainAppInterface(MainActivity mainActivity, WebView webView) {
            this.mainActivity = mainActivity;
            this.webView = webView;
        }

        /** Show a toast from the web page */
        @JavascriptInterface
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
                        program.setScraperURL(plugins.get(pluginCounter)+".html");
                    }
                    listPrograms.add(program);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            for(Program p : listPrograms)
                ProgramDatabase.programs.put(p.hashCode(), p);
        }

        @JavascriptInterface
        public void getNextPlugin() {
            if(pluginCounter < plugins.size()-1){
                mainActivity.runOnUiThread(() -> webView.loadData(returnPlugin(plugins.get(++pluginCounter)), "text/html", "utf-8"));
            }else {
                new AddChannelTask(getApplicationContext()).execute();
                finish();
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        WebView webView = findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new mainAppInterface(this, webView), "android");
        SSLWebViewClient sslWebViewClient = new SSLWebViewClient();

        sslWebViewClient.pageFinished = ()->{
            webView.loadUrl("javascript:android.getPrograms(getPrograms())");
            webView.loadUrl("javascript:android.getNextPlugin()");
        };
        webView.setWebViewClient( sslWebViewClient );

        webView.loadData(returnPlugin(plugins.get(pluginCounter)), "text/html", "utf-8");
        finish();
    }

    private String returnPlugin(String name){
        try {
            String out = new Scanner(new URL(plugins_map.get(name)).openStream(), "UTF-8").useDelimiter("\\A").next();

            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(getBaseContext().openFileOutput(name+".html", Context.MODE_PRIVATE));
            outputStreamWriter.write(out);
            outputStreamWriter.close();

            return out;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private class AddChannelTask extends AsyncTask<Subscription, Void, Long> {

        private final Context mContext;

        AddChannelTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected Long doInBackground(Subscription... varArgs) {
            setChannels(mContext);
            return null;
        }

        protected void setChannels(Context mContext) {

            SharedPreferences mPrefs = getSharedPreferences("test", MODE_PRIVATE);

            SharedPreferences.Editor prefsEditor = mPrefs.edit();
            prefsEditor.remove("programs");
            prefsEditor.apply();
            Gson gson = new Gson();
            String json = gson.toJson(ProgramDatabase.programs);
            prefsEditor.putString("programs", json);
            prefsEditor.apply();

            List<Subscription> subscriptions = SharedPreferencesHelper.readSubscriptions(mContext);
            int numOfChannelsInTVProvider = TvUtil.getNumberOfChannels(mContext);

            Subscription flagshipSubscription =
                    Subscription.createSubscription(
                            "OnDemand",
                            "",
                            AppLinkHelper.buildBrowseUri("OnDemand").toString(),
                            R.drawable.icon_ch);

            Subscription videoSubscription =
                    Subscription.createSubscription(
                            "Live",
                            "",
                            AppLinkHelper.buildBrowseUri("Live").toString(),
                            R.drawable.icon_ch);

            subscriptions = Arrays.asList(videoSubscription, flagshipSubscription);


            for (Subscription subscription : subscriptions) {
                long channelId = TvUtil.createChannel(mContext, subscription);
                subscription.setChannelId(channelId);
                setPrograms(subscription.getName(), channelId);
                TvContractCompat.requestChannelBrowsable(mContext, channelId);
            }

            SharedPreferencesHelper.storeSubscriptions(mContext, subscriptions);
        }


        @SuppressLint("RestrictedApi")
        protected void setPrograms(String channel, long channelId) {

            Cursor cursor = getContentResolver().query(TvContractCompat.PreviewPrograms.CONTENT_URI, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    PreviewProgram previewProgram = PreviewProgram.fromCursor(cursor);
                    if (previewProgram.getChannelId() == channelId) {
                        getContentResolver().delete(TvContractCompat.buildPreviewProgramUri(previewProgram.getId()), null, null);
                    }
                } while (cursor.moveToNext());
            }

            for (Program p : ProgramDatabase.programs.values()) {
                if (!(p.getType().equals(channel))) {
                    continue;
                }

                Uri intentUri = AppLinkHelper.buildPlaybackUri(channelId, p.hashCode());

                PreviewProgram.Builder builder = new PreviewProgram.Builder();

                builder.setChannelId(channelId)
                        .setType(TvContractCompat.PreviewProgramColumns.TYPE_MOVIE)
                        .setTitle(p.getTitle())
                        .setDescription("")
                        .setPosterArtUri(Uri.parse(p.getCardImageUrl()))
                        .setPosterArtAspectRatio(p.getPosterArtAspectRatio())
                        .setLogoUri(Uri.parse(p.getLogo()))
                        .setIntentUri(intentUri);
                PreviewProgram previewProgram = builder.build();

                getContentResolver().insert(
                    TvContractCompat.PreviewPrograms.CONTENT_URI,
                    previewProgram.toContentValues());
            }
        }

    }

}