package com.android.exttv.util;

import com.android.exttv.scrapers.ScriptEngine;

import org.json.JSONArray;
import org.json.JSONObject;

public class ProxyProvider {
    static String bestProxy = null;

    public static String getBest(ScriptEngine scriptEngine){
        if(bestProxy!=null) return bestProxy;

        try {
            String apiUrl = "https://api.nordvpn.com/v1/servers/recommendations?limit=10&filters%5Bcountry_id%5D=106";
            String response = scriptEngine.getResponse(apiUrl);

            JSONArray servers = new JSONArray(response);
            for (int i=0; i < servers.length(); i++) {
                JSONObject server = servers.getJSONObject(i);
                String hostname = server.getString("hostname");
                if(hostname.equals("it189.nordvpn.com")) continue; // doesn't work with some providers
                if(hostname.equals("it193.nordvpn.com")) continue; // doesn't work with some providers
                if(hostname.equals("it212.nordvpn.com")) continue; // doesn't work with some providers
                if(hostname.equals("it210.nordvpn.com")) continue; // doesn't work with some providers
                if(hostname.equals("it211.nordvpn.com")) continue; // doesn't work with some providers

                JSONArray technologies = server.getJSONArray("technologies");
                JSONObject t = null;
                for (int j=0; j < technologies.length(); j++) {
                    t = technologies.getJSONObject(j);
                    if(t.getString("name").equals("HTTP Proxy (SSL)")){
                        break;
                    }
                }
                if(t.getJSONObject("pivot").getString("status").equals("offline"))
                    continue;
                bestProxy = hostname;
                return bestProxy;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
