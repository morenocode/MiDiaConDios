package com.modu.midiacondios

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

object FirebaseAdminSource {
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    fun currentUser(): FirebaseUser? = auth.currentUser

    fun signIn(email: String, password: String, onResult: (Boolean, String?) -> Unit) {
        auth.signInWithEmailAndPassword(email.trim(), password)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { error -> onResult(false, error.localizedMessage ?: "No se pudo iniciar sesión") }
    }

    fun signOut() = auth.signOut()

    fun checkAdmin(onResult: (Boolean) -> Unit) {
        val user = auth.currentUser ?: run {
            onResult(false)
            return
        }
        db.collection("admins").document(user.uid).get()
            .addOnSuccessListener { onResult(it.exists()) }
            .addOnFailureListener { onResult(false) }
    }

    fun publishDevotional(
        date: String,
        reference: String,
        verse: String,
        reflection: String,
        prayer: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val payload = mapOf(
            "reference" to reference.trim(),
            "verse" to verse.trim(),
            "reflection" to reflection.trim(),
            "prayer" to prayer.trim()
        )
        db.collection("devotionals").document(date.trim()).set(payload)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.localizedMessage ?: "No se pudo publicar") }
    }

    fun publishEvent(
        title: String,
        subtitle: String,
        date: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val payload = mapOf(
            "title" to title.trim(),
            "subtitle" to subtitle.trim(),
            "date" to date.trim(),
            "active" to true
        )
        db.collection("events").document().set(payload)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.localizedMessage ?: "No se pudo publicar") }
    }

    fun publishAnnouncement(
        title: String,
        body: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val payload = mapOf(
            "title" to title.trim(),
            "body" to body.trim(),
            "active" to true
        )
        db.collection("announcements").document().set(payload)
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { onResult(false, it.localizedMessage ?: "No se pudo publicar") }
    }
}
