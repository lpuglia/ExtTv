package com.android.exttv.model;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

public class Episode implements Comparable<Episode> {
    private String title;
    private String description;
    private String thumbURL;
    private String pageURL;
    private String duration;
    private long durationLong;
    private Bitmap thumb;
    private GregorianCalendar airDate;

    public Episode(String json) {
        if(json.equals("")) return;
        try {
            JSONObject jObject = new JSONObject(json);

            Calendar c = Calendar.getInstance();
            c.setTime(new Date(jObject.getLong("AirDate")));

            durationLong = 0;
            if (jObject.has("Duration") && !jObject.isNull("Duration")) {
                durationLong = jObject.getLong("Duration");
            }

            @SuppressLint("SimpleDateFormat") SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
            this.setPageURL(jObject.getString("PageURL"))
                    .setThumbURL(jObject.getString("ThumbURL"))
                    .setAirDate((GregorianCalendar) c)
                    .setDescription(jObject.getString("Description"))
                    .setDuration(df.format(durationLong))
                    .setTitle(jObject.getString("Title"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    public Bitmap getThumb() {
        return thumb;
    }

    public Episode setThumb(Bitmap thumb) {
        this.thumb = thumb;
        return this;
    }

    public String getDuration() {
        return duration;
    }

    public Episode setDuration(String duration) {
        this.duration = duration;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public Episode setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public Episode setDescription(String description) {
        this.description = description;
        return this;
    }

    public GregorianCalendar getAirDate() {
        return airDate;
    }

    public Episode setAirDate(GregorianCalendar airDate) {
        this.airDate = airDate;
        return this;
    }

    public String getThumbURL() {
        return thumbURL;
    }

    public Episode setThumbURL(String thumbURL) {
        this.thumbURL = thumbURL;
        return this;
    }

    public String getPageURL() {
        return pageURL;
    }

    public Episode setPageURL(String pageURL) {
        this.pageURL = pageURL;
        return this;
    }

    public long getDurationLong() {
        return durationLong;
    }

    public Episode setDurationLong(long durationLong) {
        this.durationLong = durationLong;
        return this;
    }

    @Override
    public int compareTo(Episode episode) {
        return -airDate.compareTo(episode.airDate);
    }

}
