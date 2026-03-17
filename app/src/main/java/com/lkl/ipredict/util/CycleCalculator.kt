package com.lkl.ipredict.util

import kotlin.math.roundToInt

data class CountdownState(
    val averageCycle: Int,
    val daysSinceLast: Int,
    val daysLeft: Int,
    val nextDate: String,
    val progress: Int
)

object CycleCalculator {

    fun intervals(datesDesc: List<String>): List<Int> {
        if (datesDesc.size < 2) return emptyList()
        return datesDesc.zipWithNext { newer, older ->
            DateUtils.diffDays(older, newer).coerceAtLeast(0)
        }
    }

    fun intervalForIndex(datesDesc: List<String>, index: Int): Int? {
        if (index < 0 || index >= datesDesc.lastIndex) return null
        return DateUtils.diffDays(datesDesc[index + 1], datesDesc[index]).coerceAtLeast(0)
    }

    fun averageInterval(datesDesc: List<String>): Int? {
        val points = intervals(datesDesc).filter { it > 0 }
        if (points.isEmpty()) return null
        return points.average().roundToInt().coerceAtLeast(1)
    }

    fun countdownState(datesDesc: List<String>, today: String = DateUtils.today()): CountdownState? {
        val lastDate = datesDesc.firstOrNull() ?: return null
        val avg = averageInterval(datesDesc) ?: return null
        val daysSinceLast = DateUtils.diffDays(lastDate, today).coerceAtLeast(0)
        val rawLeft = avg - daysSinceLast
        // Don't coerce to 0, allow negative values to indicate overdue
        val daysLeft = rawLeft
        val next = DateUtils.addDays(lastDate, avg)
        return CountdownState(
            averageCycle = avg,
            daysSinceLast = daysSinceLast,
            daysLeft = daysLeft,
            nextDate = next,
            progress = daysLeft.coerceIn(0, avg)
        )
    }
}
