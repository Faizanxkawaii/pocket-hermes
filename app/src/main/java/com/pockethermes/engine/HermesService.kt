package com.pockethermes.engine

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.pockethermes.App
import com.pockethermes.MainActivity
import com.pockethermes.R

class HermesService : Service() {

    private val binder = HermesBinder()
    var hermesProcess: HermesProcess? = null
        private set

    inner class HermesBinder : Binder() {
        fun getService(): HermesService = this@HermesService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        hermesProcess = HermesProcess(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)
        hermesProcess?.start()
        return START_STICKY
    }

    override fun onDestroy() {
        hermesProcess?.stop()
        super.onDestroy()
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, App.CHANNEL_ID)
            .setContentTitle("Hermes Agent")
            .setContentText("Running in background")
            .setSmallIcon(R.drawable.ic_hermes)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
