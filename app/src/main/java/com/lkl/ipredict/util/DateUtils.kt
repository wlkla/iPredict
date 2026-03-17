package com.lkl.ipredict.util

import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

object DateUtils {
    private const val DATE_PATTERN = "yyyy-MM-dd"
    private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    fun today(): String {
        return format(Date())
    }

    fun format(date: Date): String {
        return dateFormat().format(date)
    }

    fun fromPicker(year: Int, monthZeroBased: Int, dayOfMonth: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, monthZeroBased + 1, dayOfMonth)
    }

    fun parse(value: String): Date? {
        return try {
            dateFormat().parse(value)
        } catch (_: ParseException) {
            null
        }
    }

    fun isValidDate(value: String): Boolean {
        return parse(value) != null
    }

    fun diffDays(older: String, newer: String): Int {
        val olderDate = parse(older) ?: return 0
        val newerDate = parse(newer) ?: return 0
        val diff = (newerDate.time - olderDate.time) / DAY_MILLIS
        return diff.toInt()
    }

    fun addDays(date: String, days: Int): String {
        val parsed = parse(date) ?: return date
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.time = parsed
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return format(calendar.time)
    }

    fun safeSortDescending(values: List<String>): List<String> {
        return values
            .filter { isValidDate(it) }
            .distinct()
            .sortedWith { left, right ->
                val l = parse(left)?.time ?: 0L
                val r = parse(right)?.time ?: 0L
                when {
                    l == r -> 0
                    l > r -> -1
                    else -> 1
                }
            }
    }

    fun absDays(from: String, to: String): Int {
        return abs(diffDays(from, to))
    }

    private fun dateFormat(): SimpleDateFormat {
        return SimpleDateFormat(DATE_PATTERN, Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }
}
