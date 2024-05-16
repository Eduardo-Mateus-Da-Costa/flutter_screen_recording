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
import android.content.pm.ServiceInfo
import android.util.Log


class ForegroundService : Service() {
    private val CHANNEL_ID = "ForegroundService Kotlin"
    companion object {
        fun startService(context: Context, title: String, message: String) {
            Log.d("ForegroundService", "startService")
            val startIntent = Intent(context, ForegroundService::class.java)
            startIntent.putExtra("messageExtra", message)
            startIntent.putExtra("titleExtra", title)
            Log.d("ForegroundService", "startService")
            ContextCompat.startForegroundService(context, startIntent)
            Log.d("ForegroundService", "startService")
        }
        fun stopService(context: Context) {
            val stopIntent = Intent(context, ForegroundService::class.java)
            context.stopService(stopIntent)
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ForegroundService", "onStartCommand")
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
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.icon)
            .setContentIntent(pendingIntent)
            .build()
        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        Log.d("ForegroundService", "startForeground")
        return START_NOT_STICKY
    }
    override fun onBind(intent: Intent): IBinder? {
        return null
    }
    private fun createNotificationChannel() {
        Log.d("ForegroundService", "createNotificationChannel")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(CHANNEL_ID, "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT)
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(serviceChannel)
            Log.d("ForegroundService", "createdNotificationChannel")
        }
        Log.d("ForegroundService", "endCreateNotificationChannel")
    }
}