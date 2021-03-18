package com.android.exttv.scrapers;

import android.annotation.SuppressLint;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;

import com.android.exttv.PlayerActivity;
import com.android.exttv.model.Episode;
import com.android.exttv.model.Program;
import com.android.exttv.util.SSLWebViewClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Consumer;

public class WebAppInterface {
    ScraperManager scraperManager;
    WebView webView;
    Program program;

    WebAppInterface(ScraperManager scraperManager, WebView webView, Program program, PlayerActivity playerActivity) {
        this.scraperManager = scraperManager;
        this.webView = webView;
        this.program = program;
        webView.addJavascriptInterface(this, "android");
        ((SSLWebViewClient)webView.getWebViewClient()).pageFinished = pageFinished;

        String response = "";
        try {
            InputStream inputStream = null;
            inputStream = playerActivity.getBaseContext().openFileInput(program.getScraperURL());

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }

                inputStream.close();
                response = stringBuilder.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        webView.loadData(response, "text/html", "utf-8");
    }

    Runnable pageFinished = ()->{
        final String promise =
                "function getResponse(url) {\n" +
                        "    return new Promise(resolve => {\n" +
                        "        randomId = 'id'+(Math.random()*100000000000000000).toString(36)\n" +
                        "        window[randomId.toString()] = resolve\n" +
                        "        android.getResponseAsync(url, randomId)\n" +
                        "    }); \n" +
                        "};";

        if(!webView.getUrl().equals("about:blank")){
            webView.evaluateJavascript(promise, null);
            webView.loadUrl("javascript:preBackground()");
            if(program.isLive()){
                webView.loadUrl("javascript:getLiveStream('"+program.getVideoUrl()+"')");
            }else{
                webView.loadUrl("javascript:scrapeEpisodes('"+program.getVideoUrl()+"')");
                Log.d("asd","Finished Calling scrapeEpisodes");
            }
        }
    };

    /** Show a toast from the web page */
    @JavascriptInterface
    public void showToast(String toast) {
        scraperManager.toastOnUi(toast);
    }

    @JavascriptInterface
    public String getResponse(String url) {
        return scraperManager.getResponse(url);
    }

    @JavascriptInterface
    public void getResponseAsync(String url, String id) {
        Consumer<String> consumer = (response) -> {
            String escapedResponse = response.replace("\\","\\\\").replace("'","\\'");
            scraperManager.runOnUiThread(() -> webView.loadUrl("javascript:window['"+ id +"']('"+escapedResponse+"')"));
        };
        scraperManager.getResponseAsync(url, consumer);
    }

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
            scraperManager.headers.put(e.getKey(), e.getValue());
        }
    }

    @JavascriptInterface
    public void handleEpisode(String episode, boolean play){
        scraperManager.handleEpisode(new Episode(episode), play);
    }

    void scrapeVideoURL(String url){
        webView.loadUrl("javascript:android.playStream(scrapeVideo('"+url+"'))");
    }

    @JavascriptInterface
    public void playStream(String jsonMediaSource){
        scraperManager.playStream(parseJson(jsonMediaSource));
    }

    @SuppressLint("SetJavaScriptEnabled")
    public void cancel(){
        webView.stopLoading();
        webView.getSettings().setJavaScriptEnabled(false);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("about:blank");
    }

}
