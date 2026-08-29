package com.modu.midiacondios

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import java.util.UUID

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

    fun publishChurchUpdate(
        imageUri: Uri,
        contentType: String?,
        title: String,
        body: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val user = auth.currentUser ?: run {
            onResult(false, "Debes iniciar sesión como administrador")
            return
        }

        val id = UUID.randomUUID().toString()
        val path = "church_updates/$id"
        val storageRef = FirebaseStorage.getInstance().reference.child(path)
        val metadata = StorageMetadata.Builder().apply {
            if (!contentType.isNullOrBlank()) setContentType(contentType)
            setCustomMetadata("uploadedBy", user.uid)
        }.build()

        storageRef.putFile(imageUri, metadata)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception ?: IllegalStateException("No se pudo subir la imagen")
                storageRef.downloadUrl
            }
            .addOnSuccessListener { downloadUri ->
                val payload = mapOf(
                    "title" to title.trim(),
                    "body" to body.trim(),
                    "imageUrl" to downloadUri.toString(),
                    "storagePath" to path,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "createdBy" to user.uid
                )
                db.collection("church_updates").document(id).set(payload)
                    .addOnSuccessListener { onResult(true, null) }
                    .addOnFailureListener { error ->
                        storageRef.delete()
                        onResult(false, error.localizedMessage ?: "La imagen subió, pero no se pudo publicar")
                    }
            }
            .addOnFailureListener { error ->
                onResult(false, error.localizedMessage ?: "No se pudo subir la imagen")
            }
    }
}
