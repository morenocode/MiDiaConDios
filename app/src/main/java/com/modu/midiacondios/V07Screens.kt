package com.modu.midiacondios

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class V7Plan(val id: String, val title: String, val days: Int, val description: String, val emoji: String)

@Composable
fun V7HomeScreen(
    contentPadding: PaddingValues,
    onPrayers: () -> Unit,
    onGratitude: () -> Unit,
    onFavorites: () -> Unit,
    onChurch: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val today = remember { LocalDate.now() }
    val fallback = remember(today) { DevotionalRepository.forDate(today) }
    var devotional by remember(today) { mutableStateOf(fallback) }
    var remoteState by remember { mutableStateOf<Boolean?>(null) }
    var refreshToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(today, refreshToken) {
        remoteState = null
        FirebaseDevotionalSource.loadForDate(context, today) { remote ->
            devotional = remote ?: fallback
            remoteState = remote != null
        }
    }

    val prefs = remember { context.getSharedPreferences("devotional", Context.MODE_PRIVATE) }
    val todayKey = today.toString()
    val yesterdayKey = today.minusDays(1).toString()
    val savedLast = remember { prefs.getString("lastCompleted", "").orEmpty() }
    val savedStreak = remember { prefs.getInt("streak", 0) }
    var completed by remember { mutableStateOf(savedLast == todayKey) }
    var streak by remember { mutableIntStateOf(if (savedLast == todayKey || savedLast == yesterdayKey) savedStreak else 0) }
    var best by remember { mutableIntStateOf(prefs.getInt("bestStreak", savedStreak)) }
    var favorite by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(devotional.reference) { favorite = FavoriteStore.contains(context, devotional.reference) }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            kotlinx.coroutines.delay(1800)
            feedback = null
        }
    }

    val dateLabel = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "PE")))
            .replaceFirstChar { it.titlecase(Locale("es", "PE")) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Column(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(scheme.primaryContainer, scheme.surfaceVariant)))
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("MI DÍA CON DIOS", color = scheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Un momento de paz para hoy",
                            fontSize = 28.sp,
                            lineHeight = 33.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.semantics { heading() }
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(dateLabel, color = scheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { refreshToken++ }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar devocional", tint = scheme.primary)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(50), color = scheme.surface.copy(alpha = .82f)) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                        when (remoteState) {
                            null -> {
                                CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Buscando el contenido de hoy…", fontSize = 12.sp)
                            }
                            true -> {
                                Icon(Icons.Filled.CloudDone, contentDescription = null, Modifier.size(17.dp), tint = scheme.secondary)
                                Spacer(Modifier.width(8.dp))
                                Text("Actualizado desde Internet", fontSize = 12.sp, color = scheme.secondary, fontWeight = FontWeight.SemiBold)
                            }
                            false -> {
                                Icon(Icons.Outlined.CloudOff, contentDescription = null, Modifier.size(17.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Contenido disponible sin conexión", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(16.dp)) {
                V7VerseCard(devotional)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = {
                            favorite = FavoriteStore.toggle(context, devotional)
                            feedback = if (favorite) "Guardado en favoritos" else "Quitado de favoritos"
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (favorite) "Guardado" else "Favorito")
                    }
                    OutlinedButton(
                        onClick = { shareV7Devotional(context, devotional) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Compartir")
                    }
                }
                Spacer(Modifier.height(12.dp))
                V7ReflectionCard(devotional)
                Spacer(Modifier.height(12.dp))
                V7PrayerCard(devotional)
                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (!completed) {
                            val previous = prefs.getString("lastCompleted", "").orEmpty()
                            val stored = prefs.getInt("streak", 0)
                            val newStreak = if (previous == yesterdayKey) stored + 1 else 1
                            val newBest = maxOf(best, newStreak)
                            streak = newStreak
                            best = newBest
                            completed = true
                            prefs.edit()
                                .putString("lastCompleted", todayKey)
                                .putInt("streak", newStreak)
                                .putInt("bestStreak", newBest)
                                .apply()
                            feedback = "¡Devocional completado!"
                        }
                    },
                    enabled = !completed,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (completed) "COMPLETADO POR HOY" else "COMPLETAR DEVOCIONAL", fontWeight = FontWeight.Bold)
                }

                feedback?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = scheme.secondary, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(14.dp))
                V7StreakCard(streak, best)
                Spacer(Modifier.height(20.dp))
                Text("Continúa tu momento", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                Text("Tus espacios personales, siempre a mano.", color = scheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    V7QuickCard("Oraciones", "🙏", onPrayers, Modifier.weight(1f))
                    V7QuickCard("Gratitud", "❤️", onGratitude, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    V7QuickCard("Favoritos", "⭐", onFavorites, Modifier.weight(1f))
                    V7QuickCard("Iglesia", "⛪", onChurch, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun V7VerseCard(devotional: Devotional) {
    val scheme = MaterialTheme.colorScheme
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = scheme.primaryContainer, shape = CircleShape) {
                    Icon(Icons.Outlined.AutoStories, contentDescription = null, modifier = Modifier.padding(10.dp).size(21.dp), tint = scheme.primary)
                }
                Spacer(Modifier.width(11.dp))
                Column {
                    Text("Palabra de hoy", color = scheme.primary, fontWeight = FontWeight.Bold)
                    Text(devotional.reference, color = scheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("“${devotional.verse}”", fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun V7ReflectionCard(devotional: Devotional) {
    val scheme = MaterialTheme.colorScheme
    Card(colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer.copy(alpha = .72f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("✨ Reflexión", color = scheme.onPrimaryContainer, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(devotional.reflection, lineHeight = 23.sp, color = scheme.onPrimaryContainer)
            Spacer(Modifier.height(8.dp))
            Text("Lectura breve · 2 min", color = scheme.onPrimaryContainer.copy(alpha = .72f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun V7PrayerCard(devotional: Devotional) {
    val scheme = MaterialTheme.colorScheme
    Card(colors = CardDefaults.cardColors(containerColor = scheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("🙏 Oración de hoy", color = scheme.onSecondaryContainer, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(devotional.prayer, lineHeight = 23.sp, color = scheme.onSecondaryContainer)
        }
    }
}

@Composable
private fun V7StreakCard(streak: Int, best: Int) {
    val scheme = MaterialTheme.colorScheme
    Card(colors = CardDefaults.cardColors(containerColor = scheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("🔥", fontSize = 31.sp)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("Racha actual", color = scheme.onTertiaryContainer.copy(alpha = .75f), fontSize = 12.sp)
                Text("$streak ${if (streak == 1) "día" else "días"}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = scheme.onTertiaryContainer)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Mejor racha", color = scheme.onTertiaryContainer.copy(alpha = .75f), fontSize = 12.sp)
                Text("$best días", fontWeight = FontWeight.Bold, color = scheme.onTertiaryContainer)
            }
        }
    }
}

@Composable
private fun V7QuickCard(label: String, emoji: String, onClick: () -> Unit, modifier: Modifier) {
    Card(onClick = onClick, modifier = modifier.height(102.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center) {
            Text(emoji, fontSize = 27.sp)
            Spacer(Modifier.height(5.dp))
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun V7PrayerScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(EntryStore.load(context, "prayers")) }
    var text by remember { mutableStateOf("") }
    var answered by remember { mutableStateOf(false) }

    fun persist(updated: List<SavedEntry>) {
        entries = updated.sortedByDescending { it.createdAt }
        EntryStore.save(context, "prayers", entries)
    }

    V7Screen(contentPadding, "Mis oraciones", "Guarda tus peticiones y celebra las respuestas.") {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("¿Por qué quieres orar hoy?") },
            minLines = 2,
            shape = RoundedCornerShape(18.dp)
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                val clean = text.trim()
                if (clean.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    persist(listOf(SavedEntry(now, clean, now)) + entries)
                    text = ""
                }
            },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar petición") }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = !answered, onClick = { answered = false }, label = { Text("Pendientes") })
            FilterChip(selected = answered, onClick = { answered = true }, label = { Text("Respondidas") })
        }
        Spacer(Modifier.height(10.dp))
        val visible = entries.filter { it.completed == answered }
        if (visible.isEmpty()) {
            V7Empty(if (answered) "✨" else "🙏", if (answered) "Aún no marcaste respuestas" else "Aún no hay peticiones", "Tus registros aparecerán aquí.")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(visible, key = { it.id }) { entry ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(15.dp)) {
                            Text(entry.text, lineHeight = 21.sp)
                            Text(formatV7Date(entry.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { persist(entries.map { if (it.id == entry.id) it.copy(completed = !it.completed) else it }) }) {
                                    Icon(if (entry.completed) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle, contentDescription = null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(5.dp))
                                    Text(if (entry.completed) "Respondida" else "Marcar respondida")
                                }
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { persist(entries.filterNot { it.id == entry.id }) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Eliminar petición")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun V7GratitudeScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(EntryStore.load(context, "gratitude")) }
    var text by remember { mutableStateOf("") }

    fun persist(updated: List<SavedEntry>) {
        entries = updated.sortedByDescending { it.createdAt }
        EntryStore.save(context, "gratitude", entries)
    }

    V7Screen(contentPadding, "Gratitud", "Un pequeño agradecimiento puede cambiar la forma de mirar el día.") {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Hoy doy gracias por…") },
            minLines = 2,
            shape = RoundedCornerShape(18.dp)
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = {
                val clean = text.trim()
                if (clean.isNotEmpty()) {
                    val now = System.currentTimeMillis()
                    persist(listOf(SavedEntry(now, clean, now)) + entries)
                    text = ""
                }
            },
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar agradecimiento") }
        Spacer(Modifier.height(14.dp))
        if (entries.isEmpty()) {
            V7Empty("❤️", "Tu diario está listo", "Tu primer agradecimiento aparecerá aquí.")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(entries, key = { it.id }) { entry ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
                            Text("❤️", fontSize = 21.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(entry.text, lineHeight = 21.sp)
                                Text(formatV7Date(entry.createdAt), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                            IconButton(onClick = { persist(entries.filterNot { it.id == entry.id }) }) {
                                Icon(Icons.Outlined.Delete, contentDescription = "Eliminar agradecimiento")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun V7FavoritesScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    var favorites by remember { mutableStateOf(FavoriteStore.all(context)) }
    V7Screen(contentPadding, "Favoritos", "Tus devocionales guardados permanecen disponibles en este teléfono.") {
        if (favorites.isEmpty()) {
            V7Empty("⭐", "Todavía no tienes favoritos", "Guarda un devocional desde Inicio.")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(favorites, key = { it.reference }) { item ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(17.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.reference, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { shareV7Devotional(context, item) }) {
                                    Icon(Icons.Outlined.Share, contentDescription = "Compartir ${item.reference}")
                                }
                                IconButton(onClick = {
                                    FavoriteStore.remove(context, item.reference)
                                    favorites = FavoriteStore.all(context)
                                }) { Icon(Icons.Filled.Favorite, contentDescription = "Quitar de favoritos", tint = MaterialTheme.colorScheme.primary) }
                            }
                            Text("“${item.verse}”", fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(item.reflection, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp, lineHeight = 19.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun V7PlansScreen(contentPadding: PaddingValues) {
    val plans = listOf(
        V7Plan("fe7", "7 días de fe", 7, "Pequeños pasos para fortalecer tu confianza en Dios.", "🌱"),
        V7Plan("salmos30", "Salmos en 30 días", 30, "Un recorrido diario para orar y reflexionar con los Salmos.", "📖"),
        V7Plan("oracion21", "21 días de oración", 21, "Construye un hábito sencillo de conversación diaria con Dios.", "🙏")
    )
    val context = LocalContext.current
    V7Screen(contentPadding, "Planes de lectura", "Avanza a tu ritmo. El progreso se guarda en tu teléfono.") {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(plans, key = { it.id }) { plan ->
                var current by remember(plan.id) { mutableIntStateOf(PlanProgressStore.get(context, plan.id)) }
                val progress = (current.toFloat() / plan.days.toFloat()).coerceIn(0f, 1f)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(17.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(plan.emoji, fontSize = 29.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(plan.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text("$current de ${plan.days} días", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(plan.description, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(7.dp))
                        Spacer(Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = {
                                if (current < plan.days) {
                                    current++
                                    PlanProgressStore.set(context, plan.id, current)
                                }
                            },
                            enabled = current < plan.days,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text(if (current >= plan.days) "Plan completado ✓" else "Marcar día ${current + 1} como leído") }
                    }
                }
            }
        }
    }
}

@Composable
fun V7ChurchScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    var remoteState by remember { mutableStateOf<Boolean?>(null) }
    var events by remember { mutableStateOf<List<ChurchEvent>>(emptyList()) }
    var announcements by remember { mutableStateOf<List<ChurchAnnouncement>>(emptyList()) }
    var refresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(refresh) {
        remoteState = null
        FirebaseCommunitySource.load(context) { remoteEvents, remoteAnnouncements, success ->
            events = remoteEvents
            announcements = remoteAnnouncements
            remoteState = success
        }
    }

    val shownEvents = if (events.isNotEmpty()) events else listOf(
        ChurchEvent("Culto dominical", "Domingo · 10:00 a. m."),
        ChurchEvent("Noche de oración", "Viernes · 7:30 p. m."),
        ChurchEvent("Reunión de jóvenes", "Sábado · 5:00 p. m.")
    )

    V7Screen(contentPadding, "Iglesia", "Eventos y anuncios para mantenerte conectado con tu congregación.") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when (remoteState) {
                null -> Text("Actualizando…", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                true -> Text("Contenido actualizado", color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                false -> Text("Mostrando contenido disponible", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
            }
            IconButton(onClick = { refresh++ }) { Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar iglesia") }
        }
        Spacer(Modifier.height(6.dp))
        V7Section("📅 Próximas actividades") {
            shownEvents.forEachIndexed { index, event ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(event.title, fontWeight = FontWeight.SemiBold)
                        if (event.subtitle.isNotBlank()) Text(event.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        if (event.date.isNotBlank()) Text(event.date, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }
                if (index != shownEvents.lastIndex) HorizontalDivider(Modifier.padding(vertical = 10.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        V7Section("📣 Anuncios") {
            if (announcements.isEmpty()) {
                Text("Aún no hay anuncios publicados.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                announcements.forEachIndexed { index, item ->
                    Text(item.title, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(3.dp))
                    Text(item.body, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
                    if (index != announcements.lastIndex) HorizontalDivider(Modifier.padding(vertical = 10.dp))
                }
            }
        }
    }
}

@Composable
fun V7ProfileScreen(
    contentPadding: PaddingValues,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onAdmin: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var reminderEnabled by remember { mutableStateOf(prefs.getBoolean("dailyReminder", false)) }
    var hour by remember { mutableIntStateOf(prefs.getInt("reminderHour", 8)) }
    var minute by remember { mutableIntStateOf(prefs.getInt("reminderMinute", 0)) }

    fun saveReminder() {
        scheduleDailyReminder(context, hour, minute)
        reminderEnabled = true
        prefs.edit().putBoolean("dailyReminder", true).putInt("reminderHour", hour).putInt("reminderMinute", minute).apply()
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) saveReminder() }
    val picker = remember(hour, minute) {
        TimePickerDialog(context, { _, h, m ->
            hour = h
            minute = m
            if (reminderEnabled) saveReminder()
        }, hour, minute, false)
    }

    V7Screen(contentPadding, "Perfil", "Personaliza tu experiencia y tus recordatorios.") {
        V7Section("🔔 Rutina diaria") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Recordatorio diario", fontWeight = FontWeight.Bold)
                    Text("A las ${formatV7Time(hour, minute)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else saveReminder()
                        } else {
                            cancelDailyReminder(context)
                            reminderEnabled = false
                            prefs.edit().putBoolean("dailyReminder", false).apply()
                        }
                    }
                )
            }
            TextButton(onClick = { picker.show() }, enabled = reminderEnabled) {
                Icon(Icons.Outlined.Schedule, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Cambiar hora")
            }
        }
        Spacer(Modifier.height(12.dp))
        V7Section("🌙 Apariencia") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Modo oscuro", fontWeight = FontWeight.Bold)
                    Text("Reduce el brillo para lectura nocturna.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
                Switch(checked = darkMode, onCheckedChange = onDarkModeChange)
            }
        }
        Spacer(Modifier.height(12.dp))
        V7Section("🔒 Privacidad") {
            Text("Tus oraciones, gratitud, favoritos, racha y progreso se guardan localmente en este dispositivo. El contenido público se obtiene desde Firebase.", color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 21.sp)
        }
        Spacer(Modifier.height(12.dp))
        V7Section("🛡️ Administración") {
            Text("Acceso reservado para quien administra el contenido de la iglesia.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = onAdmin, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Abrir panel de administración")
            }
        }
        Spacer(Modifier.height(12.dp))
        V7Section("ℹ️ Aplicación") {
            Text("Mi Día con Dios", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Versión 0.7.0 · prueba", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(5.dp))
            Text("Lectura, reflexión, oración, gratitud y comunidad en una experiencia sencilla y tranquila.", color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun V7Screen(contentPadding: PaddingValues, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(contentPadding).padding(18.dp)) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 20.sp)
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun V7Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(17.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun V7Empty(icon: String, title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 42.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

private fun shareV7Devotional(context: Context, devotional: Devotional) {
    val text = buildString {
        append("📖 ${devotional.reference}\n")
        append("“${devotional.verse}”\n\n")
        append("✨ ${devotional.reflection}\n\n")
        append("🙏 ${devotional.prayer}\n\n")
        append("Mi Día con Dios")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Devocional de hoy")
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir devocional"))
}

private fun formatV7Date(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "PE")))
}

private fun formatV7Time(hour: Int, minute: Int): String {
    val suffix = if (hour < 12) "a. m." else "p. m."
    val h = when (val value = hour % 12) { 0 -> 12 else -> value }
    return "%d:%02d %s".format(h, minute, suffix)
}
