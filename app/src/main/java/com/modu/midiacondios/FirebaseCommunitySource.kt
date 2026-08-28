package com.modu.midiacondios

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore

data class ChurchEvent(
    val title: String,
    val subtitle: String,
    val date: String = ""
)

data class ChurchAnnouncement(
    val title: String,
    val body: String
)

object FirebaseCommunitySource {

    fun load(
        context: Context,
        onResult: (List<ChurchEvent>, List<ChurchAnnouncement>, Boolean) -> Unit
    ) {
        if (FirebaseApp.getApps(context).isEmpty()) {
            onResult(emptyList(), emptyList(), false)
            return
        }

        val db = FirebaseFirestore.getInstance()
        var events: List<ChurchEvent>? = null
        var announcements: List<ChurchAnnouncement>? = null
        var failed = false

        fun finishIfReady() {
            if (events != null && announcements != null) {
                onResult(events.orEmpty(), announcements.orEmpty(), !failed)
            }
        }

        db.collection("events")
            .get()
            .addOnSuccessListener { snapshot ->
                events = snapshot.documents.mapNotNull { doc ->
                    val active = doc.getBoolean("active") ?: true
                    val title = doc.getString("title").orEmpty().trim()
                    val subtitle = doc.getString("subtitle").orEmpty().trim()
                    val date = doc.getString("date").orEmpty().trim()
                    if (!active || title.isBlank()) null else ChurchEvent(title, subtitle, date)
                }.sortedBy { it.date }
                finishIfReady()
            }
            .addOnFailureListener {
                failed = true
                events = emptyList()
                finishIfReady()
            }

        db.collection("announcements")
            .get()
            .addOnSuccessListener { snapshot ->
                announcements = snapshot.documents.mapNotNull { doc ->
                    val active = doc.getBoolean("active") ?: true
                    val title = doc.getString("title").orEmpty().trim()
                    val body = doc.getString("body").orEmpty().trim()
                    if (!active || title.isBlank() || body.isBlank()) null else ChurchAnnouncement(title, body)
                }
                finishIfReady()
            }
            .addOnFailureListener {
                failed = true
                announcements = emptyList()
                finishIfReady()
            }
    }
}
