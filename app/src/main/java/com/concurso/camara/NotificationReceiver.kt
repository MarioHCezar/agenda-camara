package com.concurso.camara

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_OPEN_QCONCURSOS = "com.concurso.camara.ACTION_OPEN_QCONCURSOS"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmScheduler.scheduleAll(context)
            return
        }

        if (intent.action == ACTION_OPEN_QCONCURSOS) {
            val notifId = intent.getIntExtra("ALERT_ID", 1)
            val url = intent.getStringExtra("TARGET_URL") ?: AlertRepository.QCONCURSOS_URL

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(notifId)

            QConcursosLauncher.open(context, url.ifBlank { AlertRepository.QCONCURSOS_URL })
            return
        }

        val alertId = intent.getIntExtra("ALERT_ID", 1)
        val subject = intent.getStringExtra("SUBJECT") ?: "Hora do Simulado!"
        val hour = intent.getIntExtra("HOUR", 8)
        val customMsg = intent.getStringExtra("CUSTOM_MESSAGE") ?: ""
        val targetUrl = intent.getStringExtra("TARGET_URL") ?: AlertRepository.QCONCURSOS_URL

        val randomMessage = AlertRepository.getRandomNotificationMessage(context, hour, customMsg)
        val currentMood = AlertRepository.getCurrentMascotMood(context)

        showNotification(context, alertId, subject, randomMessage, targetUrl, currentMood)

        val currentAlerts = AlertRepository.getAlerts(context)
        currentAlerts.find { it.id == alertId }?.let { alert ->
            if (alert.enabled) {
                AlarmScheduler.scheduleAlert(context, alert)
            }
        }
    }

    private fun showNotification(
        context: Context,
        id: Int,
        subject: String,
        message: String,
        targetUrl: String,
        mood: MascotMood
    ) {
        val channelId = "camara_study_channel_v2"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Simulados & Alertas QConcursos",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações com redirecionamento direto para o QConcursos"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val clickIntent = Intent(context, NotificationReceiver::class.java).apply {
            action = ACTION_OPEN_QCONCURSOS
            putExtra("ALERT_ID", id)
            putExtra("TARGET_URL", targetUrl)
        }

        val clickPendingIntent = PendingIntent.getBroadcast(
            context,
            id + 5000,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mascotBitmap = MascotBitmapGenerator.createMoodBitmap(mood)
        val expandedContent = RemoteViews(context.packageName, R.layout.notification_expanded).apply {
            setImageViewBitmap(R.id.notification_mascot, mascotBitmap)
            setTextViewText(R.id.notification_title, subject)
            setTextViewText(R.id.notification_message, message)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setLargeIcon(mascotBitmap)
            .setContentTitle("📚 $subject")
            .setContentText(message)
            .setSubText(mood.title)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomBigContentView(expandedContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                android.R.drawable.ic_media_play,
                "Ir para QConcursos",
                clickPendingIntent
            )
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }
}
