package com.lkl.ipredict.data

import android.content.Context
import com.lkl.ipredict.util.DateUtils
import org.json.JSONArray
import org.json.JSONObject

data class TrackerEvent(
    val name: String,
    val dates: List<String>,
    val tip: String
)

data class ImportMergeResult(
    val addedEvents: Int,
    val replacedEvents: Int
)

object EventRepository {
    private const val PREF_NAME = "ipredict_prefs"
    private const val KEY_EVENTS = "events_json"
    private const val KEY_CURRENT_EVENT = "current_event"

    // Legacy keys for migration.
    private const val LEGACY_KEY_DATES = "event_dates"
    private const val LEGACY_KEY_TIP = "tip_text"

    private const val DEFAULT_EVENT_NAME = "周期时间"
    private const val DEFAULT_TIP = "当你的身体处于特殊时，切记不可剧烈运动。\n保持心情舒畅，才能更快恢复元气。"

    fun getEvents(context: Context): List<TrackerEvent> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        migrateIfNeeded(context)

        val raw = prefs.getString(KEY_EVENTS, null)
        val parsed = raw?.let { parseEvents(it) } ?: emptyList()
        if (parsed.isEmpty()) {
            val defaultEvent = TrackerEvent(DEFAULT_EVENT_NAME, emptyList(), DEFAULT_TIP)
            saveEvents(context, listOf(defaultEvent))
            return listOf(defaultEvent)
        }
        return parsed
    }

    fun getCurrentEventName(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val current = prefs.getString(KEY_CURRENT_EVENT, null)
        val events = getEvents(context)
        return if (!current.isNullOrBlank() && events.any { it.name == current }) {
            current
        } else {
            events.first().name
        }
    }

    fun setCurrentEvent(context: Context, name: String): Boolean {
        val exists = getEvents(context).any { it.name == name }
        if (!exists) return false
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CURRENT_EVENT, name)
            .apply()
        return true
    }

    fun addEvent(context: Context, name: String): Boolean {
        val normalized = name.trim()
        if (normalized.isEmpty()) return false
        val events = getEvents(context).toMutableList()
        if (events.any { it.name == normalized }) return false

        events.add(TrackerEvent(normalized, emptyList(), DEFAULT_TIP))
        saveEvents(context, events)
        setCurrentEvent(context, normalized)
        return true
    }

    fun renameEvent(context: Context, oldName: String, newName: String): Boolean {
        val normalized = newName.trim()
        if (normalized.isEmpty()) return false
        val events = getEvents(context).toMutableList()
        val index = events.indexOfFirst { it.name == oldName }
        if (index < 0) return false
        if (events.any { it.name == normalized && it.name != oldName }) return false

        val updated = events[index].copy(name = normalized)
        events[index] = sanitizeEvent(updated)
        saveEvents(context, events)

        if (getCurrentEventName(context) == oldName) {
            setCurrentEvent(context, normalized)
        }
        return true
    }

    fun deleteEvent(context: Context, name: String): Boolean {
        val events = getEvents(context).toMutableList()
        if (events.size <= 1) return false
        val removed = events.removeAll { it.name == name }
        if (!removed) return false
        saveEvents(context, events)
        return true
    }

    fun mergeImportedEvents(context: Context, imported: List<TrackerEvent>): ImportMergeResult {
        if (imported.isEmpty()) return ImportMergeResult(0, 0)

        val current = getEvents(context).toMutableList()
        var added = 0
        var replaced = 0

        imported.forEach { incoming ->
            val index = current.indexOfFirst { it.name == incoming.name }
            if (index >= 0) {
                current[index] = sanitizeEvent(incoming)
                replaced++
            } else {
                current.add(sanitizeEvent(incoming))
                added++
            }
        }

        saveEvents(context, current)
        if (added > 0 || replaced > 0) {
            setCurrentEvent(context, imported.first().name)
        }
        return ImportMergeResult(addedEvents = added, replacedEvents = replaced)
    }

    fun getDates(context: Context): List<String> {
        return getCurrentEvent(context).dates
    }

    fun addDate(context: Context, date: String): Boolean {
        if (!DateUtils.isValidDate(date)) return false
        val event = getCurrentEvent(context)
        if (event.dates.contains(date)) return false

        val updated = event.copy(dates = DateUtils.safeSortDescending(event.dates + date))
        upsertEvent(context, updated)
        return true
    }

    fun addDates(context: Context, dates: List<String>): Int {
        val event = getCurrentEvent(context)
        val merged = event.dates.toMutableList()
        val before = merged.size

        dates.forEach { date ->
            if (DateUtils.isValidDate(date) && !merged.contains(date)) {
                merged.add(date)
            }
        }

        val updated = event.copy(dates = DateUtils.safeSortDescending(merged))
        upsertEvent(context, updated)
        return updated.dates.size - before
    }

    fun removeDate(context: Context, date: String): Boolean {
        val event = getCurrentEvent(context)
        if (!event.dates.contains(date)) return false

        val updated = event.copy(dates = event.dates.filter { it != date })
        upsertEvent(context, updated)
        return true
    }

    fun updateDate(context: Context, oldDate: String, newDate: String): Boolean {
        if (!DateUtils.isValidDate(newDate)) return false
        val event = getCurrentEvent(context)
        if (!event.dates.contains(oldDate)) return false
        if (oldDate != newDate && event.dates.contains(newDate)) return false

        val updatedDates = event.dates.toMutableList().apply {
            remove(oldDate)
            add(newDate)
        }
        val updated = event.copy(dates = DateUtils.safeSortDescending(updatedDates))
        upsertEvent(context, updated)
        return true
    }

    fun clearAllDates(context: Context) {
        val event = getCurrentEvent(context)
        upsertEvent(context, event.copy(dates = emptyList()))
    }

    fun replaceDates(context: Context, dates: List<String>) {
        val event = getCurrentEvent(context)
        upsertEvent(context, event.copy(dates = DateUtils.safeSortDescending(dates)))
    }

    fun exportText(context: Context): String {
        val event = getCurrentEvent(context)
        return event.dates.joinToString("\n")
    }

    fun getTipText(context: Context): String {
        return getCurrentEvent(context).tip
    }

    fun saveTipText(context: Context, value: String) {
        val event = getCurrentEvent(context)
        upsertEvent(context, event.copy(tip = value.trim().ifBlank { DEFAULT_TIP }))
    }

    private fun getCurrentEvent(context: Context): TrackerEvent {
        val currentName = getCurrentEventName(context)
        return getEvents(context).firstOrNull { it.name == currentName }
            ?: TrackerEvent(DEFAULT_EVENT_NAME, emptyList(), DEFAULT_TIP)
    }

    private fun upsertEvent(context: Context, updatedEvent: TrackerEvent) {
        val events = getEvents(context).toMutableList()
        val index = events.indexOfFirst { it.name == updatedEvent.name }
        if (index >= 0) {
            events[index] = sanitizeEvent(updatedEvent)
        } else {
            events.add(sanitizeEvent(updatedEvent))
        }
        saveEvents(context, events)
    }

    private fun sanitizeEvent(event: TrackerEvent): TrackerEvent {
        return event.copy(
            name = event.name.trim(),
            dates = DateUtils.safeSortDescending(event.dates),
            tip = event.tip.trim().ifBlank { DEFAULT_TIP }
        )
    }

    private fun saveEvents(context: Context, events: List<TrackerEvent>) {
        val sanitized = events.map { sanitizeEvent(it) }.filter { it.name.isNotBlank() }
        val jsonArray = JSONArray()

        sanitized.forEach { event ->
            val json = JSONObject().apply {
                put("name", event.name)
                put("tip", event.tip)
                put("dates", JSONArray(event.dates))
            }
            jsonArray.put(json)
        }

        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_EVENTS, jsonArray.toString()).apply()

        val current = prefs.getString(KEY_CURRENT_EVENT, null)
        if (current.isNullOrBlank() || sanitized.none { it.name == current }) {
            sanitized.firstOrNull()?.let {
                prefs.edit().putString(KEY_CURRENT_EVENT, it.name).apply()
            }
        }
    }

    private fun parseEvents(raw: String): List<TrackerEvent> {
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        val result = mutableListOf<TrackerEvent>()

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val name = item.optString("name").trim()
            if (name.isBlank()) continue

            val tip = item.optString("tip", DEFAULT_TIP)
            val datesJson = item.optJSONArray("dates") ?: JSONArray()
            val dates = mutableListOf<String>()
            for (j in 0 until datesJson.length()) {
                val date = datesJson.optString(j)
                if (DateUtils.isValidDate(date)) dates.add(date)
            }

            result.add(TrackerEvent(name, DateUtils.safeSortDescending(dates), tip))
        }

        return result
    }

    private fun migrateIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_EVENTS)) return

        val legacyRawDates = prefs.getString(LEGACY_KEY_DATES, null)
        val legacyTip = prefs.getString(LEGACY_KEY_TIP, DEFAULT_TIP) ?: DEFAULT_TIP

        val initialDates = if (!legacyRawDates.isNullOrBlank()) {
            val json = runCatching { JSONArray(legacyRawDates) }.getOrNull()
            buildList {
                if (json != null) {
                    for (i in 0 until json.length()) {
                        val date = json.optString(i)
                        if (DateUtils.isValidDate(date)) add(date)
                    }
                }
            }
        } else {
            emptyList()
        }

        val migrated = TrackerEvent(
            name = DEFAULT_EVENT_NAME,
            dates = DateUtils.safeSortDescending(initialDates),
            tip = legacyTip
        )

        saveEvents(context, listOf(migrated))
        prefs.edit()
            .remove(LEGACY_KEY_DATES)
            .remove(LEGACY_KEY_TIP)
            .apply()
    }
}
