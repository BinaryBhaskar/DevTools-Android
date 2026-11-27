package com.amurot.devtools.widget

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.amurot.devtools.core.AndroidSettingsStore
import com.amurot.devtools.core.DevController

/**
 * Keeps widgets in sync when a setting changes outside DevTools (in Settings, over adb, etc.)
 * without a running process: a content-trigger job fires on each change, refreshes the
 * widgets and re-arms itself.
 */
class SettingsWatchJob : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        WidgetRefresher.refreshAll(this)
        schedule(this)
        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean = false

    companion object {
        private const val JOB_ID = 4201

        fun schedule(context: Context) {
            val scheduler = context.getSystemService(JobScheduler::class.java) ?: return
            if (!WidgetRefresher.hasAnyWidget(context)) {
                scheduler.cancel(JOB_ID)
                return
            }
            val builder = JobInfo.Builder(JOB_ID, ComponentName(context, SettingsWatchJob::class.java))
                .setTriggerContentUpdateDelay(150)
                .setTriggerContentMaxDelay(1_000)
            DevController.watchedKeys.forEach {
                builder.addTriggerContentUri(JobInfo.TriggerContentUri(AndroidSettingsStore.uriFor(it), 0))
            }
            runCatching { scheduler.schedule(builder.build()) }
        }
    }
}

/** Content-trigger jobs can't be persisted, so re-arm after boot and app updates. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        WidgetRefresher.refreshAll(context)
        SettingsWatchJob.schedule(context)
    }
}
