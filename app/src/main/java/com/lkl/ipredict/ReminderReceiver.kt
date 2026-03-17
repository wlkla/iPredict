package com.lkl.ipredict

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.app.PendingIntent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.lkl.ipredict.data.EventRepository

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val eventName = intent.getStringExtra(EXTRA_EVENT_NAME) ?: "周期时间"
        val nextDate = intent.getStringExtra(EXTRA_NEXT_DATE) ?: "--"
        val leadDays = intent.getIntExtra(EXTRA_LEAD_DAYS, 1)

        createChannelIfNeeded(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val contentPending = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "周期提醒"
        val subtitle = "距离事件“$eventName”还有${leadDays}天，预计日期 $nextDate"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bottom_line_chart)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(subtitle))
            .setContentIntent(contentPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "ipredict_reminder"
        private const val NOTIFICATION_ID = 20201
        const val EXTRA_EVENT_NAME = "event_name"
        const val EXTRA_NEXT_DATE = "next_date"
        const val EXTRA_LEAD_DAYS = "lead_days"

        fun createChannelIfNeeded(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing != null) return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "周期提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "事件倒计时提醒"
            }
            manager.createNotificationChannel(channel)
        }
    }
}
