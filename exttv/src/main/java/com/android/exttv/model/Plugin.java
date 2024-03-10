package com.android.exttv.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.webkit.URLUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import static android.content.Context.MODE_PRIVATE;

public class Plugin {
    private String name;
    private String script;
    private final Context context;
    private final String uri;
    private ArrayList<Program> programs;

    public Plugin(String uri, Context context) {
        this.uri = uri;
        this.context = context;
        try {
            if(uri!=null) {
                // check if uri is an URL
                if(URLUtil.isValidUrl(uri)){
                    try {
                        HttpURLConnection conn = (HttpURLConnection) new URL(uri).openConnection();
                        conn.setConnectTimeout(2000);
                        conn.setReadTimeout(5000);
                        InputStreamReader inputStreamReader = new InputStreamReader(conn.getInputStream());
                        BufferedReader in = new BufferedReader(inputStreamReader);
                        String str;
                        script = in.readLine();
                        while ((str = in.readLine()) != null) script += "\n" + str;

                        conn.disconnect();
                        inputStreamReader.close();
                        in.close();
                        return;
                    }catch(IOException e){ // if plugin file is not reachable
                        SharedPreferences mPrefs = context.getSharedPreferences("test", MODE_PRIVATE);
                        uri = mPrefs.getString(uri, ""); //overwrite uri variable with file name
                        Log.d("Plugin", "URI not accessible, attempting to read " +uri+"...");
                    }
                } else {
                    Log.d("Plugin", "URI is not an URL, attempting to read file...");
                }
                //otherwise uri is a PATH
                InputStream inputStream;
                inputStream = context.openFileInput(uri);

                if (inputStream != null) {
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String receiveString;
                    StringBuilder stringBuilder = new StringBuilder();

                    while ((receiveString = bufferedReader.readLine()) != null) {
                        stringBuilder.append("\n").append(receiveString);
                    }
                    inputStreamReader.close();
                    bufferedReader.close();
                    inputStream.close();
                    script = stringBuilder.toString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void saveScript(){
        OutputStreamWriter outputStreamWriter;
        try {
            outputStreamWriter = new OutputStreamWriter(context.openFileOutput(name+".js", MODE_PRIVATE));
            outputStreamWriter.write(script);
            outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        SharedPreferences mPrefs = context.getSharedPreferences("test", MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        prefsEditor.remove(uri);
        prefsEditor.apply();
        prefsEditor.putString(uri, name+".js");
        prefsEditor.apply();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        saveScript();
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public ArrayList<Program> getPrograms() {
        return programs;
    }

    public void setPrograms(ArrayList<Program> programs) {
        this.programs = programs;
    }
}
