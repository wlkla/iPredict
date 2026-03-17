package com.lkl.ipredict.util

import com.lkl.ipredict.data.TrackerEvent
import org.json.JSONArray
import org.json.JSONObject

data class ExchangePayload(
    val events: List<TrackerEvent>
)

object ExchangeFormat {
    private const val FORMAT_NAME = "IPREDICT_EXPORT"
    private const val VERSION = 2

    fun serialize(events: List<TrackerEvent>): String {
        val eventArray = JSONArray()
        events.forEach { event ->
            val item = JSONObject().apply {
                put("name", event.name)
                put("tip", event.tip)
                put("dates", JSONArray(event.dates))
            }
            eventArray.put(item)
        }

        val json = JSONObject().apply {
            put("format", FORMAT_NAME)
            put("version", VERSION)
            put("events", eventArray)
            put("exportedAt", System.currentTimeMillis())
        }
        return json.toString(2)
    }

    fun parse(content: String): ExchangePayload? {
        val json = runCatching { JSONObject(content) }.getOrNull() ?: return null
        if (json.optString("format") != FORMAT_NAME) return null

        val eventsArray = json.optJSONArray("events")
        if (eventsArray != null) {
            val events = parseEvents(eventsArray)
            return if (events.isEmpty()) null else ExchangePayload(events)
        }

        // Backward compatibility for v1 single-event export.
        val datesArray = json.optJSONArray("dates") ?: return null
        val dates = parseDates(datesArray)
        if (dates.isEmpty()) return null
        val tip = json.optString("tip", "")
        return ExchangePayload(
            listOf(
                TrackerEvent(
                    name = "周期时间",
                    dates = DateUtils.safeSortDescending(dates),
                    tip = tip
                )
            )
        )
    }

    private fun parseEvents(array: JSONArray): List<TrackerEvent> {
        val events = mutableListOf<TrackerEvent>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val name = item.optString("name").trim()
            if (name.isBlank()) continue

            val datesArray = item.optJSONArray("dates") ?: JSONArray()
            val dates = parseDates(datesArray)
            val tip = item.optString("tip", "")

            events.add(
                TrackerEvent(
                    name = name,
                    dates = DateUtils.safeSortDescending(dates),
                    tip = tip
                )
            )
        }
        return events
    }

    private fun parseDates(array: JSONArray): List<String> {
        val dates = mutableListOf<String>()
        for (i in 0 until array.length()) {
            val date = array.optString(i)
            if (DateUtils.isValidDate(date)) {
                dates.add(date)
            }
        }
        return dates
    }
}
