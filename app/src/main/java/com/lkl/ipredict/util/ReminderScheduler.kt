package com.lkl.ipredict.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.getSystemService
import com.lkl.ipredict.ReminderReceiver
import com.lkl.ipredict.data.EventRepository
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.max

data class ReminderConfig(
    val enabled: Boolean,
    val leadDays: Int,
    val hour: Int,
    val minute: Int
)

object ReminderScheduler {

    private const val PREF_NAME = "ipredict_reminder_prefs"
    private const val KEY_ENABLED_PREFIX = "enabled_"
    private const val KEY_LEAD_PREFIX = "lead_"
    private const val KEY_HOUR_PREFIX = "hour_"
    private const val KEY_MINUTE_PREFIX = "minute_"

    private const val DEFAULT_LEAD = 1
    private const val DEFAULT_HOUR = 9
    private const val DEFAULT_MINUTE = 0

    fun getConfig(context: Context, eventName: String): ReminderConfig {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = sanitize(eventName)
        return ReminderConfig(
            enabled = prefs.getBoolean(KEY_ENABLED_PREFIX + key, false),
            leadDays = prefs.getInt(KEY_LEAD_PREFIX + key, DEFAULT_LEAD),
            hour = prefs.getInt(KEY_HOUR_PREFIX + key, DEFAULT_HOUR),
            minute = prefs.getInt(KEY_MINUTE_PREFIX + key, DEFAULT_MINUTE)
        )
    }

    fun saveConfig(context: Context, eventName: String, config: ReminderConfig) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = sanitize(eventName)
        prefs.edit()
            .putBoolean(KEY_ENABLED_PREFIX + key, config.enabled)
            .putInt(KEY_LEAD_PREFIX + key, config.leadDays)
            .putInt(KEY_HOUR_PREFIX + key, config.hour)
            .putInt(KEY_MINUTE_PREFIX + key, config.minute)
            .apply()
    }

    fun updateSchedule(context: Context) {
        val eventName = EventRepository.getCurrentEventName(context)
        val config = getConfig(context, eventName)
        cancel(context, eventName)
        if (!config.enabled) return

        val dates = EventRepository.getDates(context)
        val state = CycleCalculator.countdownState(dates) ?: return

        val alarmManager: AlarmManager = context.getSystemService() ?: return

        val targetDate = computeNextTriggerDate(state, config)
        val triggerMillis = dateTimeMillis(targetDate, config, context)

        if (triggerMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_EVENT_NAME, eventName)
            putExtra(ReminderReceiver.EXTRA_NEXT_DATE, state.nextDate)
            putExtra(ReminderReceiver.EXTRA_LEAD_DAYS, config.leadDays)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(eventName),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pending
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pending)
        }
    }

    fun cancel(context: Context, eventName: String) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            requestCode(eventName),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pending != null) {
            val alarmManager: AlarmManager = context.getSystemService() ?: return
            alarmManager.cancel(pending)
            pending.cancel()
        }
    }

    private fun computeNextTriggerDate(state: CountdownState, config: ReminderConfig): String {
        // Aim for (nextDate - leadDays) at the configured time. If that moment is in the past,
        // roll forward by whole cycles until it lands in the future.
        var nextDate = state.nextDate
        repeat(6) { // cap to avoid infinite loops
            val triggerDate = DateUtils.addDays(nextDate, -config.leadDays)
            val millis = dateTimeMillis(triggerDate, config, null)
            if (millis > System.currentTimeMillis()) return triggerDate
            val cycle = max(state.averageCycle, 1)
            nextDate = DateUtils.addDays(nextDate, cycle)
        }
        return nextDate
    }

    private fun dateTimeMillis(date: String, config: ReminderConfig, context: Context?): Long {
        val parsed = DateUtils.parse(date) ?: return 0L
        val tz = if (context != null) TimeZone.getDefault() else TimeZone.getTimeZone("UTC")
        val cal = Calendar.getInstance(tz).apply {
            time = parsed
            set(Calendar.HOUR_OF_DAY, config.hour)
            set(Calendar.MINUTE, config.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun requestCode(eventName: String): Int {
        return sanitize(eventName).hashCode()
    }

    private fun sanitize(value: String): String = value.trim().ifBlank { "default" }
}

