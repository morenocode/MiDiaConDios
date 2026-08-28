package com.modu.midiacondios

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private data class V9Mood(val emoji: String, val label: String)

private val v9Moods = listOf(
    V9Mood("😌", "En paz"),
    V9Mood("❤️", "Agradecido"),
    V9Mood("🙏", "Necesito dirección"),
    V9Mood("😟", "Preocupado"),
    V9Mood("😔", "Triste")
)

private val v9Missions = listOf(
    "Escribe a una persona que necesite ánimo y dile algo bueno de corazón.",
    "Aparta cinco minutos sin distracciones y ora por otra persona.",
    "Haz hoy un acto de servicio sin esperar reconocimiento.",
    "Agradece personalmente a alguien que haya sido una bendición para ti.",
    "Lee nuevamente el versículo de hoy antes de dormir y llévalo a oración.",
    "Perdona una pequeña ofensa y decide no alimentar resentimiento.",
    "Comparte una palabra de esperanza con alguien que esté pasando un momento difícil."
)

@Composable
fun V09HomeScreen(
    contentPadding: PaddingValues,
    onPrayers: () -> Unit,
    onGratitude: () -> Unit,
    onFavorites: () -> Unit,
    onChurch: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var today by remember { mutableStateOf(LocalDate.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            today = LocalDate.now()
        }
    }

    val todayKey = today.toString()
    val yesterdayKey = today.minusDays(1).toString()
    val fallback = remember(today) { DevotionalRepository.forDate(today) }
    var devotional by remember(today) { mutableStateOf(fallback) }
    var remoteState by remember(today) { mutableStateOf<Boolean?>(null) }
    var refreshToken by remember(today) { mutableIntStateOf(0) }

    LaunchedEffect(today, refreshToken) {
        remoteState = null
        FirebaseDevotionalSource.loadForDate(context, today) { remote ->
            devotional = remote ?: fallback
            remoteState = remote != null
        }
    }

    val prefs = remember { context.getSharedPreferences("devotional", Context.MODE_PRIVATE) }
    val initialLast = remember(todayKey) { prefs.getString("lastCompleted", "").orEmpty() }
    val initialStoredStreak = remember(todayKey) { prefs.getInt("streak", 0) }
    var completed by remember(todayKey) { mutableStateOf(initialLast == todayKey) }
    var streak by remember(todayKey) {
        mutableIntStateOf(if (initialLast == todayKey || initialLast == yesterdayKey) initialStoredStreak else 0)
    }
    var best by remember(todayKey) { mutableIntStateOf(prefs.getInt("bestStreak", initialStoredStreak)) }
    var weekCompleted by remember(todayKey) { mutableIntStateOf(v9CountRecentDays(prefs, today)) }
    var ritualStarted by remember(todayKey) { mutableStateOf(false) }
    var step by remember(todayKey) { mutableIntStateOf(0) }
    var mood by remember(todayKey) { mutableStateOf(prefs.getString("mood_$todayKey", "").orEmpty()) }
    var gratitudeText by remember(todayKey) { mutableStateOf("") }
    var feedback by remember(todayKey) { mutableStateOf<String?>(null) }

    val prayerCount = EntryStore.load(context, "prayers").size
    val initialGratitudeCount = remember(todayKey) { EntryStore.load(context, "gratitude").size }
    var gratitudeCount by remember(todayKey) { mutableIntStateOf(initialGratitudeCount) }
    val favoriteCount = FavoriteStore.all(context).size

    val greeting = remember {
        when (LocalTime.now().hour) {
            in 5..11 -> "Buenos días"
            in 12..18 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    }
    val dateLabel = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "PE")))
            .replaceFirstChar { it.titlecase(Locale("es", "PE")) }
    }
    val mission = remember(today, mood) { v9MissionFor(mood, today.dayOfYear) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        item {
            Column(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(scheme.primaryContainer, scheme.surfaceVariant)))
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Text("MI DÍA CON DIOS", color = scheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(5.dp))
                Text(
                    "$greeting 👋",
                    fontSize = 28.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() }
                )
                Text("Regálale unos minutos a Dios hoy.", color = scheme.onSurfaceVariant, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                Text(dateLabel, color = scheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                Surface(shape = RoundedCornerShape(50), color = scheme.surface.copy(alpha = .84f)) {
                    Text(
                        when (remoteState) {
                            null -> "⏳ Preparando el contenido de hoy…"
                            true -> "☁️ Devocional actualizado"
                            false -> "📱 Devocional disponible sin conexión"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                Text("¿Cómo está tu corazón hoy?", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("Elige lo que más se acerque a cómo te sientes.", color = scheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(v9Moods) { option ->
                        FilterChip(
                            selected = mood == option.label,
                            onClick = {
                                mood = option.label
                                prefs.edit().putString("mood_$todayKey", option.label).apply()
                            },
                            label = { Text("${option.emoji} ${option.label}") }
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))

                if (!ritualStarted) {
                    V09JourneyCard(
                        completed = completed,
                        mood = mood,
                        onStart = {
                            step = 0
                            ritualStarted = true
                        }
                    )
                    Spacer(Modifier.height(12.dp))
                    V09StreakCard(streak = streak, best = best, weekCompleted = weekCompleted)
                    Spacer(Modifier.height(12.dp))
                    V09WeekCard(
                        completedDays = weekCompleted,
                        prayerCount = prayerCount,
                        gratitudeCount = gratitudeCount,
                        favoriteCount = favoriteCount
                    )
                    Spacer(Modifier.height(20.dp))
                    Text("Tus espacios", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Todo lo que vas viviendo con Dios, en un solo lugar.", color = scheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        V09QuickCard("Mis oraciones", "🙏", onPrayers, Modifier.weight(1f))
                        V09QuickCard("Gratitud", "❤️", onGratitude, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        V09QuickCard("Favoritos", "⭐", onFavorites, Modifier.weight(1f))
                        V09QuickCard("Mi iglesia", "⛪", onChurch, Modifier.weight(1f))
                    }
                } else {
                    V09Ritual(
                        devotional = devotional,
                        step = step,
                        gratitudeText = gratitudeText,
                        onGratitudeChange = { gratitudeText = it },
                        mission = mission,
                        completed = completed,
                        onBack = {
                            if (step > 0) step-- else ritualStarted = false
                        },
                        onNext = { if (step < 4) step++ },
                        onFinish = {
                            if (!completed) {
                                val previous = prefs.getString("lastCompleted", "").orEmpty()
                                val stored = prefs.getInt("streak", 0)
                                val newStreak = if (previous == yesterdayKey) stored + 1 else 1
                                val newBest = maxOf(best, newStreak)

                                var savedGratitude = false
                                if (gratitudeText.isNotBlank() && !prefs.getBoolean("gratitudeSaved_$todayKey", false)) {
                                    val now = System.currentTimeMillis()
                                    val existing = EntryStore.load(context, "gratitude")
                                    EntryStore.save(
                                        context,
                                        "gratitude",
                                        listOf(SavedEntry(now, gratitudeText.trim(), now)) + existing
                                    )
                                    savedGratitude = true
                                }

                                prefs.edit()
                                    .putString("lastCompleted", todayKey)
                                    .putInt("streak", newStreak)
                                    .putInt("bestStreak", newBest)
                                    .putBoolean("completed_$todayKey", true)
                                    .putBoolean("gratitudeSaved_$todayKey", savedGratitude || prefs.getBoolean("gratitudeSaved_$todayKey", false))
                                    .apply()

                                streak = newStreak
                                best = newBest
                                completed = true
                                weekCompleted = v9CountRecentDays(prefs, today)
                                if (savedGratitude) gratitudeCount++
                                feedback = "✨ Completaste tu momento con Dios"
                            }
                            ritualStarted = false
                        }
                    )
                }

                feedback?.let {
                    Spacer(Modifier.height(12.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = scheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
                        Text(
                            it,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun V09JourneyCard(completed: Boolean, mood: String, onStart: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(
        colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(if (completed) "✨ Tu momento de hoy está completo" else "✨ Tu momento con Dios", fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(
                if (completed) "Puedes volver a leerlo cuando quieras. Mañana tendrás un nuevo momento."
                else if (mood.isBlank()) "5 pasos · Palabra · Reflexión · Oración · Gratitud · Acción"
                else "Hoy vienes sintiéndote: $mood. Camina estos cinco pasos con calma.",
                color = scheme.onPrimaryContainer.copy(alpha = .78f),
                lineHeight = 21.sp
            )
            Spacer(Modifier.height(13.dp))
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) {
                Text(if (completed) "REVISAR MI MOMENTO DE HOY" else "COMENZAR MI MOMENTO CON DIOS", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun V09Ritual(
    devotional: Devotional,
    step: Int,
    gratitudeText: String,
    onGratitudeChange: (String) -> Unit,
    mission: String,
    completed: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val titles = listOf("Palabra", "Reflexiona", "Ora", "Agradece", "Acción del día")

    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(18.dp)) {
            Text("MI MOMENTO CON DIOS", color = scheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(5.dp))
            Text("${step + 1} de 5 · ${titles[step]}", fontSize = 23.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { (step + 1) / 5f }, modifier = Modifier.fillMaxWidth().height(7.dp))
            Spacer(Modifier.height(18.dp))

            when (step) {
                0 -> {
                    Text("📖 ${devotional.reference}", color = scheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text("“${devotional.verse}”", fontSize = 22.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(13.dp))
                    Text("Léelo despacio. ¿Qué palabra o frase llama tu atención hoy?", color = scheme.onSurfaceVariant, lineHeight = 21.sp)
                }
                1 -> {
                    Text("💭 Reflexiona", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    Text(devotional.reflection, lineHeight = 24.sp)
                    Spacer(Modifier.height(14.dp))
                    Surface(color = scheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
                        Text("Pregunta para hoy: ¿Qué puedes aplicar de esta palabra en una situación concreta de tu vida?", modifier = Modifier.padding(14.dp), lineHeight = 21.sp)
                    }
                }
                2 -> {
                    Text("🙏 Ora con calma", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    Text(devotional.prayer, lineHeight = 24.sp)
                    Spacer(Modifier.height(14.dp))
                    Text("Puedes hacer una pausa y añadir tus propias palabras antes de continuar.", color = scheme.onSurfaceVariant)
                }
                3 -> {
                    Text("❤️ Hoy agradezco por…", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    OutlinedTextField(
                        value = gratitudeText,
                        onValueChange = onGratitudeChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Escribe algo sencillo por lo que das gracias a Dios hoy") },
                        minLines = 3,
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Es opcional, pero si escribes algo quedará guardado en tu diario de gratitud.", color = scheme.onSurfaceVariant, fontSize = 12.sp)
                }
                else -> {
                    Text("✨ Tu acción de hoy", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = scheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
                        Text(mission, modifier = Modifier.padding(16.dp), fontSize = 17.sp, lineHeight = 24.sp, color = scheme.onSecondaryContainer)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("No se trata de hacer algo grande; se trata de llevar la Palabra a tu vida cotidiana.", color = scheme.onSurfaceVariant, lineHeight = 21.sp)
                }
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text(if (step == 0) "Salir" else "Atrás")
                }
                if (step < 4) {
                    Button(onClick = onNext, modifier = Modifier.weight(1f)) { Text("Continuar") }
                } else {
                    Button(onClick = onFinish, modifier = Modifier.weight(1f)) {
                        Text(if (completed) "Finalizar" else "Completar")
                    }
                }
            }
        }
    }
}

@Composable
private fun V09StreakCard(streak: Int, best: Int, weekCompleted: Int) {
    val scheme = MaterialTheme.colorScheme
    val stage = when {
        streak >= 100 -> "🌳 Camino firme"
        streak >= 30 -> "🌿 Raíces profundas"
        streak >= 7 -> "🌱 Constancia"
        else -> "🌱 Sembrando el hábito"
    }
    val next = when {
        streak < 7 -> "Próximo hito: 7 días"
        streak < 30 -> "Próximo hito: 30 días"
        streak < 100 -> "Próximo hito: 100 días"
        else -> "Sigue caminando un día a la vez"
    }

    Card(colors = CardDefaults.cardColors(containerColor = scheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 34.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("$streak ${if (streak == 1) "día" else "días"} caminando con Dios", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = scheme.onTertiaryContainer)
                    Text(stage, color = scheme.onTertiaryContainer.copy(alpha = .78f), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Últimos 7 días: $weekCompleted/7", color = scheme.onTertiaryContainer)
                Text("Récord: $best días", color = scheme.onTertiaryContainer, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(5.dp))
            Text(next, color = scheme.onTertiaryContainer.copy(alpha = .72f), fontSize = 12.sp)
        }
    }
}

@Composable
private fun V09WeekCard(completedDays: Int, prayerCount: Int, gratitudeCount: Int, favoriteCount: Int) {
    val scheme = MaterialTheme.colorScheme
    Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(17.dp)) {
            Text("Tu semana con Dios", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Mira tu constancia, no para competir, sino para recordar tu camino.", color = scheme.onSurfaceVariant, fontSize = 13.sp)
            Spacer(Modifier.height(13.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                V09MiniStat("📖", "$completedDays/7", "días")
                V09MiniStat("🙏", "$prayerCount", "oraciones")
                V09MiniStat("❤️", "$gratitudeCount", "gracias")
                V09MiniStat("⭐", "$favoriteCount", "guardados")
            }
        }
    }
}

@Composable
private fun V09MiniStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 21.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun V09QuickCard(label: String, emoji: String, onClick: () -> Unit, modifier: Modifier) {
    Card(onClick = onClick, modifier = modifier.height(100.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center) {
            Text(emoji, fontSize = 27.sp)
            Spacer(Modifier.height(5.dp))
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}

private fun v9CountRecentDays(prefs: android.content.SharedPreferences, today: LocalDate): Int {
    val last = prefs.getString("lastCompleted", "").orEmpty()
    return (0L..6L).count { offset ->
        val key = today.minusDays(offset).toString()
        prefs.getBoolean("completed_$key", false) || last == key
    }
}

private fun v9MissionFor(mood: String, dayOfYear: Int): String = when (mood) {
    "Preocupado" -> "Pon por escrito una preocupación concreta, entrégasela a Dios en oración y decide qué pequeño paso sí puedes dar hoy."
    "Triste" -> "Busca a una persona de confianza, no te aísles y comparte una palabra honesta sobre cómo estás. Después oren juntos si es posible."
    "Necesito dirección" -> "Antes de tomar una decisión importante hoy, guarda unos minutos de silencio, ora y pregúntate si tu elección refleja amor, verdad y sabiduría."
    "Agradecido" -> "Convierte tu gratitud en acción: dile hoy a una persona por qué agradeces a Dios por su vida."
    "En paz" -> "Comparte la paz que hoy sientes: anima a alguien que esté cargando una preocupación."
    else -> v9Missions[dayOfYear % v9Missions.size]
}
