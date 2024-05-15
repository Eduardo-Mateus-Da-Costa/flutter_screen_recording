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


class ForegroundService : Service() {
    private const val NOTIFICATION_CHANNEL_ID = "general_notification_channel"

    private fun startService(context: Context, title: String, content: String) {
        Notification.Builder(this, NOTIFICATION_CHANNEL_ID).apply {
            setContentTitle(title)
            setOngoing(true)
            setContentText(content)
            setSmallIcon(R.drawable.ic_notifcation_icon)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            NotificationChannel(NOTIFICATION_CHANNEL_ID, "General", importance).apply {
                description = "General Notifications"
                with((this@LocationService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)) {
                    createNotificationChannel(this@apply)
                }
            }
            //need core 1.12 and higher and SDK 29 and higher
            ServiceCompat.startForeground(
                this@LocationService, 1, this.build(),
                ServiceInfo.MEDIA_PROJECTION_SERVICE
            )
            //this@LocationService.startForeground(1, this.build())
        }
    }
}