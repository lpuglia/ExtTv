package com.android.exttv.service;

import android.app.job.JobParameters
import android.app.job.JobService
import android.util.Log
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.android.exttv.model.manager.AddonManager
import com.android.exttv.model.manager.FavouriteManager
import com.android.exttv.model.manager.FavouriteManager.getAllFavouriteCards
import com.android.exttv.model.manager.PythonManager
import com.android.exttv.model.manager.StatusManager
import com.android.exttv.util.TvContract
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            scheduleSyncJob(context)
        }
    }
}

fun scheduleSyncJob(context: Context) {
    val componentName = ComponentName(context, SyncProgramsJobService::class.java)
    val jobInfo = JobInfo.Builder(123, componentName)
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY) // Example condition: Any network required
        .setPersisted(true)  // Persist across reboots (requires RECEIVE_BOOT_COMPLETED permission)
        .setPeriodic(15 * 60 * 1000)  // Minimum interval is 15 minutes
//        .setOverrideDeadline(1000)
        .build()

    val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    val result = jobScheduler.schedule(jobInfo)
    if (result == JobScheduler.RESULT_SUCCESS) {
        Log.d("SyncProgramsJobService", "Job successfully scheduled")
    } else {
        Log.d("SyncProgramsJobService", "Job scheduling failed")
    }
}

class SyncProgramsJobService : JobService() {
    
    override fun onCreate() {
        super.onCreate()
        Log.d("SyncProgramsJobService", "Service Created")
        StatusManager.init(null, applicationContext)
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            Log.d("SyncProgramsJobService", "Service Started")
            TvContract.createOrUpdateChannel(getAllFavouriteCards())
        }
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        Log.d("SyncProgramsJobService", "Service Stopped")
        return true
    }
}
