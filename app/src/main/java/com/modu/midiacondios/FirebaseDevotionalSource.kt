package com.modu.midiacondios

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate

/**
 * Fuente opcional de contenido remoto.
 *
 * Mientras app/google-services.json no exista, la aplicación continúa usando
 * DevotionalRepository local y esta clase simplemente devuelve null.
 *
 * Colección esperada en Firestore: devotionals
 * Documento: YYYY-MM-DD (por ejemplo 2026-08-28)
 * Campos: reference, verse, reflection, prayer
 */
object FirebaseDevotionalSource {

    fun isConfigured(context: Context): Boolean =
        FirebaseApp.getApps(context).isNotEmpty()

    fun loadForDate(
        context: Context,
        date: LocalDate,
        onResult: (Devotional?) -> Unit
    ) {
        if (!isConfigured(context)) {
            onResult(null)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("devotionals")
            .document(date.toString())
            .get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    onResult(null)
                    return@addOnSuccessListener
                }

                val reference = document.getString("reference").orEmpty().trim()
                val verse = document.getString("verse").orEmpty().trim()
                val reflection = document.getString("reflection").orEmpty().trim()
                val prayer = document.getString("prayer").orEmpty().trim()

                if (reference.isBlank() || verse.isBlank() || reflection.isBlank() || prayer.isBlank()) {
                    onResult(null)
                } else {
                    onResult(
                        Devotional(
                            reference = reference,
                            verse = verse,
                            reflection = reflection,
                            prayer = prayer
                        )
                    )
                }
            }
            .addOnFailureListener {
                // Si Internet o Firebase falla, la UI conserva el contenido local.
                onResult(null)
            }
    }
}
