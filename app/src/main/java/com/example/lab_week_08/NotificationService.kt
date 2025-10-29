package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class NotificationService : Service() {

    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var serviceHandler: Handler

    override fun onBind(intent: Intent): IBinder? = null

    override fun onCreate() {
        super.onCreate()

        notificationBuilder = startForegroundNotification()

        val handlerThread = HandlerThread("SecondThread").apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    private fun startForegroundNotification(): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()

        // ✅ Channel ID fix → selalu punya ID
        val channelId = createNotificationChannel()

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Second Worker Running")
            .setContentText("Please wait...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)

        startForeground(NOTIFICATION_ID, builder.build())
        return builder
    }

    private fun getPendingIntent(): PendingIntent {
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) FLAG_IMMUTABLE else 0
        return PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), flag)
    }

    private fun createNotificationChannel(): String {
        val channelId = "work_channel_001" // ✅ gunakan ID tetap

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Worker Notification",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        return channelId // ✅ TIDAK pernah return ""
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra(EXTRA_ID)
            ?: "UNKNOWN"   // ✅ Jangan crash → punya default

        serviceHandler.post {
            countDownFromTenToZero(notificationBuilder)
            notifyCompletion(id)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun countDownFromTenToZero(builder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        for (i in 10 downTo 0) {
            Thread.sleep(1000L)
            builder.setContentText("$i seconds remaining")
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun notifyCompletion(id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = id
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1001

        // ✅ Samakan key dengan MainActivity
        const val EXTRA_ID = "ID"

        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}
