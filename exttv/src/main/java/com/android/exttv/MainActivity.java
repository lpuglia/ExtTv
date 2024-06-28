package com.android.exttv;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;

import androidx.tvprovider.media.tv.TvContractCompat;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import com.android.exttv.model.Subscription;
import com.android.exttv.util.AppLinkHelper;
import com.android.exttv.util.TvUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * Displays subscriptions that can be added to the main launcher's channels.
 */
public class MainActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

//        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
//        StrictMode.setThreadPolicy(policy);
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        Python py = Python.getInstance();
        PyObject pyObject = py.getModule("kodi");
//        PyObject result = pyObject.callAttr("your_python_function", "ciao");
//        String output = result.toString();
//        Log.d("Python Output", output);
////        new AddChannelTask(this).execute();
//        finish();
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
            List<Subscription> subscriptions = new ArrayList<>();
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

            List<Long> ids = new ArrayList<>();
            for (Subscription subscription : subscriptions) {
                long channelId = TvUtil.createChannel(mContext, subscription);
//                subscription.setChannelId(channelId);
//                setPrograms(subscription.getName(), channelId);
                TvContractCompat.requestChannelBrowsable(mContext, channelId);
                ids.add(channelId);
            }
            TvUtil.scheduleSyncingProgramsForChannel(mContext, ids);

        }
    }

}