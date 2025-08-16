package com.drgia.golcast

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat

class TestNotifService : Service() {
    companion object {
        const val CH = "test_channel"
        const val ID = 999
    }
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val ch = NotificationChannel(CH, "Prueba", NotificationManager.IMPORTANCE_DEFAULT)
            nm.createNotificationChannel(ch)
        }
        val content = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(this, CH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("GolCast")
            .setContentText("Notificación de prueba en foreground")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(content)
            .build()

        // ⚠️ clave: ponemos el servicio en foreground de inmediato
        startForeground(ID, n)
    }
}
