package com.isvisoft.flutter_screen_recording

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.isvisoft.flutter_screen_recording.R
import android.R.drawable
import android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat
android.app.Notification

import com.isvisoft.flutter_screen_recording.FlutterScreenRecordingPlugin




class ForegroundService : Service() {
    private val CHANNEL_ID = "general_notification_channel"
    companion object {
        fun startService(context: Context, title: String, content: String) {
            val builder: Notification.Builder = Builder(context)
            val nfIntent: Intent = Intent(context, FlutterScreenRecordingPlugin::class.java)

            builder.setContentIntent(PendingIntent.getActivity(context, 0, nfIntent, 0))
                .setLargeIcon(R.drawable.icon)
                .setSmallIcon(R.drawable.icon)
                .setContentText("is running......")
                .setWhen(java.lang.System.currentTimeMillis())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId("notification_id")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                val channel: NotificationChannel =
                    NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW)
                notificationManager.createNotificationChannel(channel)
            }

            val notification: Notification = builder.build()
            notification.defaults = Notification.DEFAULT_SOUND
            startForeground(110, notification)
        }


        fun stopService(context: Context) {
            val stopIntent = Intent(context, ForegroundService::class.java)
            context.stopService(stopIntent)
        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        createNotificationChannel()

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    private fun createNotificationChannel() {
        val builder: Notification.Builder = Builder(this.getApplicationContext())
        val nfIntent: Intent = Intent(this, FlutterScreenRecordingPlugin::class.java)

        builder.setContentIntent(PendingIntent.getActivity(this, 0, nfIntent, 0))
            .setLargeIcon(R.drawable.icon)
            .setSmallIcon(R.drawable.icon)
            .setContentText("is running......")
            .setWhen(java.lang.System.currentTimeMillis())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId("notification_id")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val channel: NotificationChannel =
                NotificationChannel("notification_id", "notification_name", NotificationManager.IMPORTANCE_LOW)
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = builder.build()
        notification.defaults = Notification.DEFAULT_SOUND
        startForeground(110, notification)
    }
}