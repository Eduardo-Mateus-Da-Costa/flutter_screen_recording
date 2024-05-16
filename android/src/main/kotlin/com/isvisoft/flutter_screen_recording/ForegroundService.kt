package com.isvisoft.flutter_screen_recording

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import android.content.pm.ServiceInfo
import android.util.Log
import androidx.annotation.RequiresApi

import com.isvisoft.flutter_screen_recording.FlutterScreenRecordingPlugin

@RequiresApi(Build.VERSION_CODES.Q)
class ForegroundService : Service() {
    companion object {
        private const val FOREGROUND_SERVICE_ID = 357
        private const val CHANNEL_ID = "flutter_screen_recording"
    }

    override fun onBind(intent: Intent) : IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onDestroy() {
        cleanupService()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(FOREGROUND_SERVICE_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        return START_STICKY
    }

    private fun createNotification(): Notification {
        Log.d("CreateNotification", "Creating notification")
        val nfIntent: Intent = Intent(this, ForegroundService::class.java)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT > 23) flags = flags or PendingIntent.FLAG_IMMUTABLE
        val channel = NotificationChannel(
            CHANNEL_ID
            "Datacertify está gravando a tela",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Datacertify está gravando a tela"
        }
        channel.setShowBadge(false)
        // Register the channel with the system
        val manager = context.getSystemService(NotificationManager::class.java)
        manager!!.createNotificationChannel(channel)

        val pendingIntent  = PendingIntent.getActivity(
            this, 0,
            nfIntent, flags
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Datacertify está gravando a tela")
            .setContentText("Datacertify está gravando a tela")
            .setSmallIcon(R.drawable.icon)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        Log.d("CreateNotification", "Notification created")
        return notification
    }

    fun startFService(context: Context?) {
        var context = context ?: this
        print("Context: $context")
        val nfIntent: Intent = Intent(context, FlutterScreenRecordingPlugin::class.java)
        var flags = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT > 23) flags = flags or PendingIntent.FLAG_IMMUTABLE

        val pendingIntent  = PendingIntent.getActivity(
            context, 0,
            nfIntent, flags
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Datacertify está gravando a tela",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Datacertify está gravando a tela"
            }
            channel.setShowBadge(false)
            // Register the channel with the system
            val manager = context.getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(channel)
        }

        print("P StartForeground 1")
        Log.d("StartForeground", "L StartForeground 1")

        print("P StartForeground 2")
        Log.d("StartForeground", "Foreground service started")
    }
}