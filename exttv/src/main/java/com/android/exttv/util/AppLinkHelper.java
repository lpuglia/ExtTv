package com.android.exttv.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.annotation.StringDef;

import com.android.exttv.model.Episode;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

/** Builds and parses uris for deep linking within the app. */
public class AppLinkHelper {

    private static final String SCHEMA_URI_PREFIX = "tvrecommendation://app/";
    public static final String PLAYBACK = "playback";
    public static final String BROWSE = "browse";
    private static final String URI_PLAY = SCHEMA_URI_PREFIX + PLAYBACK;
    private static final String URI_VIEW = SCHEMA_URI_PREFIX + BROWSE;
    private static final int URI_INDEX_OPTION = 0;
    private static final int URI_INDEX_CHANNEL = 1;
    private static final int URI_INDEX_MOVIE = 2;
    private static final int URI_INDEX_POSITION = 3;
    public static final int DEFAULT_POSITION = -1;


    public static void setEpisodeCursor(Long position, Episode episode, Context context){
        if(episode==null) return;
        SharedPreferences mPrefs = context.getSharedPreferences("test", MODE_PRIVATE);
        Gson gson = new Gson();
        HashMap<String, Long> cursorPositions;
        if(mPrefs.contains("cursorPositions")){
            String json = mPrefs.getString("cursorPositions", "");
            java.lang.reflect.Type type = new TypeToken<HashMap<String, Long>>(){}.getType();
            cursorPositions  = gson.fromJson(json, type);
        }else{
            cursorPositions = new LinkedHashMap<>();
        }
        cursorPositions.put(episode.getPageURL(), position);
        SharedPreferences.Editor prefsEditor = mPrefs.edit();
        prefsEditor.putString("cursorPositions", gson.toJson(cursorPositions));
        prefsEditor.apply();
    }

    public static Long getEpisodeCursor(Episode episode, Context context){
        if(episode==null) return 0L;
        SharedPreferences mPrefs = context.getSharedPreferences("test", MODE_PRIVATE);
        Gson gson = new Gson();
        HashMap<String, Long> cursorPositions;
        if(mPrefs.contains("cursorPositions")) {
            String json = mPrefs.getString("cursorPositions", "");
            java.lang.reflect.Type type = new TypeToken<HashMap<String, Long>>() {}.getType();
            cursorPositions = gson.fromJson(json, type);
            if(cursorPositions.containsKey(episode.getPageURL()))
                return cursorPositions.get(episode.getPageURL());
        }
        return 0L;
    }

    /**
     * Builds a {@link Uri} for deep link into playing a movie from the beginning.
     *
     * @param channelId - id of the channel the movie is in.
     * @param movieId - id of the movie.
     * @return a uri.
     */
    public static Uri buildPlaybackUri(long channelId, long movieId) {
        return buildPlaybackUri(channelId, movieId, DEFAULT_POSITION);
    }

    /**
     * Builds a {@link Uri} to deep link into continue playing a movie from a position.
     *
     * @param channelId - id of the channel the movie is in.
     * @param movieId - id of the movie.
     * @param position - position to continue playing.
     * @return a uri.
     */
    public static Uri buildPlaybackUri(long channelId, long movieId, long position) {
        return Uri.parse(URI_PLAY)
                .buildUpon()
                .appendPath(String.valueOf(channelId))
                .appendPath(String.valueOf(movieId))
                .appendPath(String.valueOf(position))
                .build();
    }

    /**
     * Builds a {@link Uri} to deep link into viewing a subscription.
     *
     * @param subscriptionName - name of the subscription.
     * @return a uri.
     */
    public static Uri buildBrowseUri(String subscriptionName) {
        return Uri.parse(URI_VIEW).buildUpon().appendPath(subscriptionName).build();
    }

    /**
     * Returns an {@link AppLinkAction} for the given Uri.
     *
     * @param uri to determine the intended action.
     * @return an action.
     */
    public static AppLinkAction extractAction(Uri uri) {
        if (isPlaybackUri(uri)) {
            return new PlaybackAction(
                    extractChannelId(uri), extractMovieId(uri), extractPosition(uri));
        } else if (isBrowseUri(uri)) {
            return new BrowseAction(extractSubscriptionName(uri));
        }
        throw new IllegalArgumentException("No action found for uri " + uri);
    }

    /**
     * Tests if the {@link Uri} was built for playing a movie.
     *
     * @param uri to examine.
     * @return true if the uri is for playing a movie.
     */
    private static boolean isPlaybackUri(Uri uri) {
        if (uri.getPathSegments().isEmpty()) {
            return false;
        }
        String option = uri.getPathSegments().get(URI_INDEX_OPTION);
        return PLAYBACK.equals(option);
    }

    /**
     * Tests if a {@link Uri} was built for browsing a subscription.
     *
     * @param uri to examine.
     * @return true if the Uri is for browsing a subscription.
     */
    private static boolean isBrowseUri(Uri uri) {
        if (uri.getPathSegments().isEmpty()) {
            return false;
        }
        String option = uri.getPathSegments().get(URI_INDEX_OPTION);
        return BROWSE.equals(option);
    }

    /**
     * Extracts the subscription name from the {@link Uri}.
     *
     * @param uri that contains a subscription name.
     * @return the subscription name.
     */
    private static String extractSubscriptionName(Uri uri) {
        return extract(uri, URI_INDEX_CHANNEL);
    }

    /**
     * Extracts the channel id from the {@link Uri}.
     *
     * @param uri that contains a channel id.
     * @return the channel id.
     */
    public static long extractChannelId(Uri uri) {
        return extractLong(uri, URI_INDEX_CHANNEL);
    }

    /**
     * Extracts the movie id from the {@link Uri}.
     *
     * @param uri that contains a movie id.
     * @return the movie id.
     */
    private static long extractMovieId(Uri uri) {
        return extractLong(uri, URI_INDEX_MOVIE);
    }

    /**
     * Extracts the playback mPosition from the {@link Uri}.
     *
     * @param uri that contains a playback mPosition.
     * @return the playback mPosition.
     */
    private static long extractPosition(Uri uri) {
        return extractLong(uri, URI_INDEX_POSITION);
    }

    private static long extractLong(Uri uri, int index) {
        return Long.parseLong(extract(uri, index));
    }

    private static String extract(Uri uri, int index) {
        List<String> pathSegments = uri.getPathSegments();
        if (pathSegments.isEmpty() || pathSegments.size() < index) {
            return null;
        }
        return pathSegments.get(index);
    }

    @StringDef({BROWSE, PLAYBACK})
    public @interface ActionFlags {}

    /** Action for deep linking. */
    public interface AppLinkAction {
        /** Returns an string representation of the action. */
        @ActionFlags
        String getAction();
    }

    /** Browse a subscription. */
    public static class BrowseAction implements AppLinkAction {

        private final String mSubscriptionName;

        private BrowseAction(String subscriptionName) {
            this.mSubscriptionName = subscriptionName;
        }

        public String getSubscriptionName() {
            return mSubscriptionName;
        }

        @Override
        public String getAction() {
            return BROWSE;
        }
    }

    /** Play a movie. */
    public static class PlaybackAction implements AppLinkAction {

        private final long mChannelId;
        private final long mMovieId;
        private final long mPosition;

        private PlaybackAction(long channelId, long movieId, long position) {
            this.mChannelId = channelId;
            this.mMovieId = movieId;
            this.mPosition = position;
        }

        public long getChannelId() {
            return mChannelId;
        }

        public long getMovieId() {
            return mMovieId;
        }

        public long getPosition() {
            return mPosition;
        }

        @Override
        public String getAction() {
            return PLAYBACK;
        }
    }
}
