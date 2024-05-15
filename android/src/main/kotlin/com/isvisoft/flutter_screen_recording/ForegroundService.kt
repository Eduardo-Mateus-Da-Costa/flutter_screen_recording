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
import com.isvisoft.flutter_screen_recording.FlutterScreenRecordingPlugin
import com.isvisoft.flutter_screen_recording.R






import android.R.drawable
android.content.pm.ServiceInfo
import androidx.core.app.ServiceCompat


class ForegroundService : Service() {
    private val CHANNEL_ID = "general_notification_channel"
    companion object {
        fun startService(context: Context, title: String, content: String) {
            var notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.icon).build()
            ServiceCompat.startForegroundService(
                context,
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        }


        fun stopService(context: Context) {
            ServiceCompat.stopForeground(context, ServiceInfo.MEDIA_PROJECTION_SERVICE)
        }

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

            var title = intent?.getStringExtra("titleExtra")
            if (title == null) {
                title = "Flutter Screen Recording";
            }
            var message = intent?.getStringExtra("messageExtra")
            if (message == null) {
                message = ""
            }

            createNotificationChannel()
            val notificationIntent = Intent(this, FlutterScreenRecordingPlugin::class.java)

            val pendingIntent = PendingIntent.getActivity(
                this,
                0, notificationIntent, PendingIntent.FLAG_MUTABLE
            )
            var notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pendingIntent).build()

            ServiceCompat.startForegroundService(
                context,
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )

            return START_NOT_STICKY
        }

        override fun onBind(intent: Intent): IBinder? {
            return null
        }

        private fun createNotificationChannel() {
            var notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
            var channel = NotificationChannel(CHANNEL_ID, "General", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }
    }
}