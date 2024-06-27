/*
 * Copyright (c) 2017 Google Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.android.exttv.util;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.media.tv.TvContract;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.tvprovider.media.tv.Channel;
import androidx.tvprovider.media.tv.ChannelLogoUtils;
import androidx.tvprovider.media.tv.TvContractCompat;

import android.os.PersistableBundle;
import android.util.Log;

import com.android.exttv.SyncProgramsJobService;
import com.android.exttv.model.Subscription;

import java.util.List;

/** Manages interactions with the TV Provider. */
public class TvUtil {

    private static final String TAG = "TvUtil";
    private static final long CHANNEL_JOB_ID_OFFSET = 1000;

    private static final String[] CHANNELS_PROJECTION = {
        TvContractCompat.Channels._ID,
        TvContract.Channels.COLUMN_DISPLAY_NAME,
        TvContractCompat.Channels.COLUMN_BROWSABLE
    };

    /**
     * Converts a {@link Subscription} into a {@link Channel} and adds it to the tv provider.
     *
     * @param context used for accessing a content resolver.
     * @param subscription to be converted to a channel and added to the tv provider.
     * @return the id of the channel that the tv provider returns.
     */
    @WorkerThread
    public static long createChannel(Context context, Subscription subscription) {

        // Checks if our subscription has been added to the channels before.
        Cursor cursor = context.getContentResolver().query(TvContractCompat.Channels.CONTENT_URI, CHANNELS_PROJECTION,null,null,null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Channel channel = Channel.fromCursor(cursor);
                if (subscription.getName().equals(channel.getDisplayName())) {
                    Log.d(TAG,"Channel already exists. Returning channel " + channel.getId() + " from TV Provider.");
                    return channel.getId();
                }
            } while (cursor.moveToNext());
        }

        // Create the channel since it has not been added to the TV Provider.
        Uri appLinkIntentUri = Uri.parse(subscription.getAppLinkIntentUri());

        Channel.Builder builder = new Channel.Builder();
        builder.setType(TvContractCompat.Channels.TYPE_PREVIEW)
                .setDisplayName(subscription.getName())
                .setDescription(subscription.getDescription())
                .setAppLinkIntentUri(appLinkIntentUri);

        Log.d(TAG, "Creating channel: " + subscription.getName());
        Uri channelUrl =
                context.getContentResolver()
                        .insert(
                                TvContractCompat.Channels.CONTENT_URI,
                                builder.build().toContentValues());

        Log.d(TAG, "channel insert at " + channelUrl);
        long channelId = ContentUris.parseId(channelUrl);
        Log.d(TAG, "channel id " + channelId);

        Bitmap bitmap = convertToBitmap(context, subscription.getChannelLogo());
        ChannelLogoUtils.storeChannelLogo(context, channelId, bitmap);

        return channelId;
    }

    public static int getNumberOfChannels(Context context) {
        Cursor cursor =
                context.getContentResolver()
                        .query(
                                TvContractCompat.Channels.CONTENT_URI,
                                CHANNELS_PROJECTION,
                                null,
                                null,
                                null);
        return cursor != null ? cursor.getCount() : 0;
    }

    /**
     * Converts a resource into a {@link Bitmap}. If the resource is a vector drawable, it will be
     * drawn into a new Bitmap. Otherwise the {@link BitmapFactory} will decode the resource.
     *
     * @param context used for getting the drawable from resources.
     * @param resourceId of the drawable.
     * @return a bitmap of the resource.
     */
    @NonNull
    public static Bitmap convertToBitmap(Context context, int resourceId) {
        Drawable drawable = context.getDrawable(resourceId);
        if (drawable instanceof VectorDrawable) {
            Bitmap bitmap =
                    Bitmap.createBitmap(
                            drawable.getIntrinsicWidth(),
                            drawable.getIntrinsicHeight(),
                            Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        }

        return BitmapFactory.decodeResource(context.getResources(), resourceId);
    }

    private static int getJobIdForChannelId(long channelId) {
        return (int) (CHANNEL_JOB_ID_OFFSET + channelId);
    }

    public static void scheduleSyncingProgramsForChannel(Context context, List<Long> channelIds) {
        ComponentName componentName = new ComponentName(context, SyncProgramsJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(1, componentName);

        PersistableBundle bundle = new PersistableBundle();
        bundle.putLong("Live", channelIds.get(0));
        bundle.putLong("OnDemand", channelIds.get(1));
        builder.setExtras(bundle);

        builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY);

        JobScheduler scheduler =
                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        Log.d(TAG, "Scheduled channel creation.");
        scheduler.schedule(builder.build());
    }



}
