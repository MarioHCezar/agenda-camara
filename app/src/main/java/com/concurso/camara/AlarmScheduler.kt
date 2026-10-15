package com.concurso.camara

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlarmScheduler {

    fun scheduleDailyAlerts(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        AlertCatalog.alerts.forEach { alert ->
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                action = "com.concurso.camara.ACTION_STUDY_ALARM"
                putExtra("ALERT_ID", alert.id)
                putExtra("ALERT_TITLE", alert.title)
                putExtra("ALERT_MESSAGE", alert.message)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alert.id,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, alert.hour)
                set(Calendar.MINUTE, alert.minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                // Se o horário de hoje já passou, agenda para o dia seguinte
                if (timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            } catch (e: SecurityException) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
    }
}