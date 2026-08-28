package com.modu.midiacondios

import android.content.Context
import android.net.Uri

/** Small local persistence helpers used by the MVP. */
data class SavedEntry(
    val id: Long,
    val text: String,
    val createdAt: Long,
    val completed: Boolean = false
)

object EntryStore {
    private const val PREFS = "journal_entries_v2"
    private const val RECORD = "\u001E"
    private const val FIELD = "\u001F"

    fun load(context: Context, key: String): List<SavedEntry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(key, "")
            .orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(RECORD).mapNotNull { row ->
            val fields = row.split(FIELD)
            if (fields.size != 4) return@mapNotNull null
            runCatching {
                SavedEntry(
                    id = fields[0].toLong(),
                    text = Uri.decode(fields[1]),
                    createdAt = fields[2].toLong(),
                    completed = fields[3].toBooleanStrictOrNull() ?: false
                )
            }.getOrNull()
        }.sortedByDescending { it.createdAt }
    }

    fun save(context: Context, key: String, entries: List<SavedEntry>) {
        val raw = entries.joinToString(RECORD) {
            listOf(
                it.id.toString(),
                Uri.encode(it.text),
                it.createdAt.toString(),
                it.completed.toString()
            ).joinToString(FIELD)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(key, raw).apply()
    }
}

object FavoriteStore {
    private const val PREFS = "favorite_devotionals_v2"
    private const val KEY = "items"
    private const val RECORD = "\u001E"
    private const val FIELD = "\u001F"

    fun all(context: Context): List<Devotional> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, "")
            .orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(RECORD).mapNotNull { row ->
            val fields = row.split(FIELD)
            if (fields.size != 4) return@mapNotNull null
            runCatching {
                Devotional(
                    Uri.decode(fields[0]),
                    Uri.decode(fields[1]),
                    Uri.decode(fields[2]),
                    Uri.decode(fields[3])
                )
            }.getOrNull()
        }
    }

    fun contains(context: Context, reference: String): Boolean =
        all(context).any { it.reference == reference }

    fun toggle(context: Context, devotional: Devotional): Boolean {
        val list = all(context).toMutableList()
        val existing = list.indexOfFirst { it.reference == devotional.reference }
        val nowSaved = existing < 0
        if (existing >= 0) {
            list.removeAt(existing)
        } else {
            list.add(0, Devotional(devotional.reference, devotional.verse, devotional.reflection, devotional.prayer))
        }
        save(context, list)
        return nowSaved
    }

    fun remove(context: Context, reference: String) {
        save(context, all(context).filterNot { it.reference == reference })
    }

    private fun save(context: Context, items: List<Devotional>) {
        val raw = items.joinToString(RECORD) {
            listOf(
                Uri.encode(it.reference),
                Uri.encode(it.verse),
                Uri.encode(it.reflection),
                Uri.encode(it.prayer)
            ).joinToString(FIELD)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, raw).apply()
    }
}

object PlanProgressStore {
    private const val PREFS = "reading_plan_progress"

    fun get(context: Context, planId: String): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(planId, 0)

    fun set(context: Context, planId: String, day: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(planId, day).apply()
    }
}
