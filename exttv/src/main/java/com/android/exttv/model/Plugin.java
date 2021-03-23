package com.android.exttv.model;

import android.content.Context;
import android.util.Log;
import android.webkit.URLUtil;

import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Scanner;

public class Plugin {
    private String name;
    private String script;
    private Context context;
    private ArrayList<Program> programs;

    public Plugin(String uri, Context context) {
        this.context = context;
        try {
            // check if uri is an URL
            if(URLUtil.isValidUrl(uri)){
                try {
                    URLConnection conn = new URL(uri).openConnection();
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String str;
                    script = in.readLine();
                    while ((str = in.readLine()) != null) {
                        script+="\n"+str;
                    }
                    in.close();
                    return;
                }catch(IOException e){
                    e.printStackTrace();
                }
            } else {
                Log.d("Plugin", "URI is not an URL, attempting to read file...");
            }
            //otherwise uri is a PATH
            InputStream inputStream = null;
            inputStream = context.openFileInput(uri);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }

                inputStream.close();
                script = stringBuilder.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void saveScript(){
        OutputStreamWriter outputStreamWriter = null;
        try {
            outputStreamWriter = new OutputStreamWriter(context.openFileOutput(name+".js", Context.MODE_PRIVATE));
            outputStreamWriter.write(script);
            outputStreamWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
