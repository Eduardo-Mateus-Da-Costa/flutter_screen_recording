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



class ForegroundService : Service() {
    companion object {
        @JvmStatic
        val ACTION_SHUTDOWN = "SHUTDOWN"
        @JvmStatic
        val ACTION_START = "START"
        @JvmStatic
        val CHANNEL_ID = "flutter_screen_recording"
        @JvmStatic
        private val TAG = "ForegroundService"
    }

    override fun onBind(intent: Intent) : IBinder? {
        return null
    }

    override fun onDestroy() {
        cleanupService()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) : Int {
        print("onStartCommand")
        Log.d("StartForeground", "L onStartCommand")
        if (intent?.action == ACTION_SHUTDOWN) {
            cleanupService()
            stopSelf()
        } else if (intent?.action == ACTION_START) {
            print("startService")
            Log.d("StartForeground", "L startService")
            startFService(null)
        }
        print("START_STICKY")
        Log.d("StartForeground", "L START_STICKY")
        return START_STICKY
    }

    private fun cleanupService() {
        stopForeground(true)
    }

    fun startFService(context: Context?) {
        var context = context ?: this
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
            val manager = getSystemService(NotificationManager::class.java)
            manager!!.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Datacertify está gravando a tela")
            .setContentText("Datacertify está gravando a tela")
            .setSmallIcon(R.drawable.icon)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .build()

        print("P StartForeground 1")
        Log.d("StartForeground", "L StartForeground 1")
        startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        print("P StartForeground 2")
        Log.d("StartForeground", "Foreground service started")
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        cleanupService()
        stopSelf()
    }
}