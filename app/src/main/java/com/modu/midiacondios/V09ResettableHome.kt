package com.modu.midiacondios

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlinx.coroutines.delay

/**
 * Adds a safe "restart today" control around the v0.9 spiritual journey.
 * It resets only today's devotional completion/mood and today's streak contribution.
 * Prayer, favorites and journal entries are intentionally preserved.
 */
@Composable
fun V09ResettableHomeScreen(
    contentPadding: PaddingValues,
    onPrayers: () -> Unit,
    onGratitude: () -> Unit,
    onFavorites: () -> Unit,
    onChurch: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("devotional", Context.MODE_PRIVATE) }
    var resetToken by remember { mutableIntStateOf(0) }
    var completedToday by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }

    LaunchedEffect(resetToken) {
        while (true) {
            val todayKey = LocalDate.now().toString()
            completedToday = prefs.getString("lastCompleted", "").orEmpty() == todayKey ||
                prefs.getBoolean("completed_$todayKey", false)
            delay(500)
        }
    }

    Box(Modifier.fillMaxSize()) {
        key(resetToken) {
            V09HomeScreen(
                contentPadding = contentPadding,
                onPrayers = onPrayers,
                onGratitude = onGratitude,
                onFavorites = onFavorites,
                onChurch = onChurch
            )
        }

        if (completedToday) {
            OutlinedButton(
                onClick = { confirmReset = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 92.dp)
            ) {
                Text("↻ Reiniciar mi momento de hoy")
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("¿Reiniciar el momento de hoy?") },
            text = {
                Text(
                    "Volverás a comenzar desde la pregunta sobre cómo está tu corazón. " +
                        "El día de hoy dejará de contar temporalmente en tu racha. " +
                        "Tus oraciones, favoritos y diario de gratitud no se borrarán."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        resetV09Today(prefs)
                        completedToday = false
                        confirmReset = false
                        resetToken++
                    }
                ) { Text("Reiniciar") }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancelar") }
            }
        )
    }
}

private fun resetV09Today(prefs: android.content.SharedPreferences) {
    val today = LocalDate.now()
    val todayKey = today.toString()
    val yesterdayKey = today.minusDays(1).toString()
    val lastCompleted = prefs.getString("lastCompleted", "").orEmpty()
    val currentStreak = prefs.getInt("streak", 0)

    val editor = prefs.edit()
        .remove("completed_$todayKey")
        .remove("mood_$todayKey")

    if (lastCompleted == todayKey) {
        val previousStreak = (currentStreak - 1).coerceAtLeast(0)
        editor.putInt("streak", previousStreak)
        if (previousStreak > 0) {
            editor.putString("lastCompleted", yesterdayKey)
        } else {
            editor.remove("lastCompleted")
        }
    }

    // Keep bestStreak as the historical record. We also keep gratitudeSaved_* so a
    // previously saved journal item is not duplicated or deleted during a test reset.
    editor.apply()
}
