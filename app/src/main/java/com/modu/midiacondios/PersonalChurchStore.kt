package com.modu.midiacondios

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PersonalChurchActivity(
    val id: Long,
    val title: String,
    val detail: String,
    val date: String
)

data class PersonalChurchAnnouncement(
    val id: Long,
    val title: String,
    val body: String
)

object PersonalChurchStore {
    private const val PREFS = "personal_church"
    private const val KEY_ACTIVITIES = "activities"
    private const val KEY_ANNOUNCEMENTS = "announcements"

    fun loadActivities(context: Context): List<PersonalChurchActivity> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ACTIVITIES, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val id = item.optLong("id", 0L)
                    val title = item.optString("title").trim()
                    if (id == 0L || title.isBlank()) continue
                    add(
                        PersonalChurchActivity(
                            id = id,
                            title = title,
                            detail = item.optString("detail").trim(),
                            date = item.optString("date").trim()
                        )
                    )
                }
            }.sortedByDescending { it.id }
        }.getOrDefault(emptyList())
    }

    fun saveActivities(context: Context, items: List<PersonalChurchActivity>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("detail", item.detail)
                    .put("date", item.date)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ACTIVITIES, array.toString()).apply()
    }

    fun upsertActivity(context: Context, item: PersonalChurchActivity): List<PersonalChurchActivity> {
        val current = loadActivities(context).toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) current[index] = item else current.add(item)
        val updated = current.sortedByDescending { it.id }
        saveActivities(context, updated)
        return updated
    }

    fun deleteActivity(context: Context, id: Long): List<PersonalChurchActivity> {
        val updated = loadActivities(context).filterNot { it.id == id }
        saveActivities(context, updated)
        return updated
    }

    fun loadAnnouncements(context: Context): List<PersonalChurchAnnouncement> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ANNOUNCEMENTS, "[]").orEmpty()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val id = item.optLong("id", 0L)
                    val title = item.optString("title").trim()
                    val body = item.optString("body").trim()
                    if (id == 0L || title.isBlank() || body.isBlank()) continue
                    add(PersonalChurchAnnouncement(id = id, title = title, body = body))
                }
            }.sortedByDescending { it.id }
        }.getOrDefault(emptyList())
    }

    fun saveAnnouncements(context: Context, items: List<PersonalChurchAnnouncement>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("title", item.title)
                    .put("body", item.body)
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ANNOUNCEMENTS, array.toString()).apply()
    }

    fun upsertAnnouncement(context: Context, item: PersonalChurchAnnouncement): List<PersonalChurchAnnouncement> {
        val current = loadAnnouncements(context).toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        if (index >= 0) current[index] = item else current.add(item)
        val updated = current.sortedByDescending { it.id }
        saveAnnouncements(context, updated)
        return updated
    }

    fun deleteAnnouncement(context: Context, id: Long): List<PersonalChurchAnnouncement> {
        val updated = loadAnnouncements(context).filterNot { it.id == id }
        saveAnnouncements(context, updated)
        return updated
    }
}
