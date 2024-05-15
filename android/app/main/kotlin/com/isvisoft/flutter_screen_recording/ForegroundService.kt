package android.app.main.kotlin.com.isvisoft.flutter_screen_recording

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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.R.drawable
import android.telephony.mbms.ServiceInfo
import androidx.core.app.ServiceCompat


class ForegroundService : Service() {
    private val NOTIFICATION_CHANNEL_ID = "general_notification_channel"
    companion object {
        fun startService(context: Context, title: String, content: String) {
            var notificationManager = context.getSystemService(Service.NOTIFICATION_SERVICE) as NotificationManager
            var channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, "General", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
            var notification = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setForegroundServiceBehavior(ServiceInfo.FOREGROUND_SERVICE_IMMEDIATE).build()
            ServiceCompat.startForegroundService(
                context,
                1,
                notification,
                ServiceInfo.MEDIA_PROJECTION_SERVICE
            )
        }


        fun stopService(context: Context) {
            ServiceCompat.stopForeground(context, ServiceInfo.MEDIA_PROJECTION_SERVICE)
        }
    }
}