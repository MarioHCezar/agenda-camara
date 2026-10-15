package com.concurso.camara

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Se o celular reiniciar, reagenda todos os alertas
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmScheduler.scheduleDailyAlerts(context)
            return
        }

        val alertId = intent.getIntExtra("ALERT_ID", 1)
        val title = intent.getStringExtra("ALERT_TITLE") ?: "Hora de Estudar!"
        val message = intent.getStringExtra("ALERT_MESSAGE") ?: "O Cebraspe não espera. Abra seu material!"

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "cd_study_alerts_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Lembretes Concurso Câmara",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas diários intensivos de estudo para o Cebraspe"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(alertId, notification)

        // Reagenda todos os alertas para garantir a repetição nos próximos dias
        AlarmScheduler.scheduleDailyAlerts(context)
    }
}