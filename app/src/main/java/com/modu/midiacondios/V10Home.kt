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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

private data class V10Mood(val emoji: String, val label: String)
private data class V10Guidance(
    val intro: String,
    val focus: String,
    val reflectionQuestion: String,
    val prayerPrompt: String,
    val gratitudePrompt: String,
    val mission: String
)

private val v10Moods = listOf(
    V10Mood("😌", "En paz"),
    V10Mood("❤️", "Agradecido"),
    V10Mood("🙏", "Necesito dirección"),
    V10Mood("😟", "Preocupado"),
    V10Mood("😔", "Triste")
)

private fun v10SituationsFor(mood: String): List<String> = when (mood) {
    "En paz" -> listOf("Quiero cuidar esta paz", "Familia", "Trabajo o estudios", "Descansar", "Ayudar a alguien")
    "Agradecido" -> listOf("Familia", "Salud", "Trabajo o estudios", "Oración respondida", "Lo cotidiano")
    "Necesito dirección" -> listOf("Decisión importante", "Trabajo o estudios", "Familia o relación", "Finanzas", "Propósito")
    "Preocupado" -> listOf("Trabajo o estudios", "Familia", "Salud", "Dinero", "Futuro")
    "Triste" -> listOf("Soledad", "Pérdida", "Familia", "Desilusión", "No sé por qué")
    else -> emptyList()
}

@Composable
fun V10HomeScreen(
    contentPadding: PaddingValues,
    onPrayers: () -> Unit,
    onGratitude: () -> Unit,
    onFavorites: () -> Unit,
    onChurch: () -> Unit
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val prefs = remember { context.getSharedPreferences("devotional", Context.MODE_PRIVATE) }
    var today by remember { mutableStateOf(LocalDate.now()) }
    var resetToken by remember { mutableIntStateOf(0) }

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

    LaunchedEffect(today, resetToken) {
        remoteState = null
        FirebaseDevotionalSource.loadForDate(context, today) { remote ->
            devotional = remote ?: fallback
            remoteState = remote != null
        }
    }

    val initialLast = remember(todayKey, resetToken) { prefs.getString("lastCompleted", "").orEmpty() }
    val initialStoredStreak = remember(todayKey, resetToken) { prefs.getInt("streak", 0) }
    var completed by remember(todayKey, resetToken) { mutableStateOf(initialLast == todayKey || prefs.getBoolean("completed_$todayKey", false)) }
    var streak by remember(todayKey, resetToken) {
        mutableIntStateOf(if (initialLast == todayKey || initialLast == yesterdayKey) initialStoredStreak else 0)
    }
    var best by remember(todayKey, resetToken) { mutableIntStateOf(prefs.getInt("bestStreak", initialStoredStreak)) }
    var weekCompleted by remember(todayKey, resetToken) { mutableIntStateOf(v10CountRecentDays(prefs, today)) }
    var mood by remember(todayKey, resetToken) { mutableStateOf(prefs.getString("mood_$todayKey", "").orEmpty()) }
    var situation by remember(todayKey, resetToken) { mutableStateOf(prefs.getString("situation_$todayKey", "").orEmpty()) }
    var ritualStarted by remember(todayKey, resetToken) { mutableStateOf(false) }
    var step by remember(todayKey, resetToken) { mutableIntStateOf(0) }
    var gratitudeText by remember(todayKey, resetToken) { mutableStateOf("") }
    var feedback by remember(todayKey, resetToken) { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    val prayerCount = EntryStore.load(context, "prayers").size
    var gratitudeCount by remember(todayKey, resetToken) { mutableIntStateOf(EntryStore.load(context, "gratitude").size) }
    val favoriteCount = FavoriteStore.all(context).size
    val guidance = remember(mood, situation) { v10GuidanceFor(mood, situation) }

    val greeting = remember(today) {
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
                Text("$greeting 👋", fontSize = 28.sp, lineHeight = 33.sp, fontWeight = FontWeight.Bold, modifier = Modifier.semantics { heading() })
                Text("Hoy no tiene que sentirse igual que ayer.", color = scheme.onSurfaceVariant, fontSize = 15.sp)
                Spacer(Modifier.height(5.dp))
                Text(dateLabel, color = scheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(11.dp))
                Surface(shape = RoundedCornerShape(50), color = scheme.surface.copy(alpha = .84f)) {
                    Text(
                        when (remoteState) {
                            null -> "⏳ Preparando el contenido de hoy…"
                            true -> "☁️ Devocional actualizado"
                            false -> "📱 Disponible sin conexión"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                if (!ritualStarted) {
                    Text("¿Cómo está tu corazón hoy?", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text("Elige una emoción. Esto cambiará el enfoque de tu momento.", color = scheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(v10Moods) { option ->
                            FilterChip(
                                selected = mood == option.label,
                                onClick = {
                                    mood = option.label
                                    situation = ""
                                    prefs.edit()
                                        .putString("mood_$todayKey", option.label)
                                        .remove("situation_$todayKey")
                                        .apply()
                                },
                                label = { Text("${option.emoji} ${option.label}") }
                            )
                        }
                    }

                    if (mood.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text("¿Qué estás viviendo?", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Es opcional. No necesitas escribir nada.", color = scheme.onSurfaceVariant, fontSize = 12.sp)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(v10SituationsFor(mood)) { option ->
                                FilterChip(
                                    selected = situation == option,
                                    onClick = {
                                        situation = if (situation == option) "" else option
                                        val editor = prefs.edit()
                                        if (situation.isBlank()) editor.remove("situation_$todayKey")
                                        else editor.putString("situation_$todayKey", situation)
                                        editor.apply()
                                    },
                                    label = { Text(option) }
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        V10SmartResponseCard(mood, situation, guidance)
                    }

                    Spacer(Modifier.height(14.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(18.dp)) {
                            Text(if (completed) "✨ Tu momento de hoy está completo" else "✨ Mi momento con Dios", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            Text(
                                if (mood.isBlank()) "Primero cuéntale a la app cómo está tu corazón para personalizar el recorrido."
                                else "5 pasos · Palabra · Reflexión · Oración · Gratitud · Acción",
                                color = scheme.onPrimaryContainer.copy(alpha = .78f), lineHeight = 21.sp
                            )
                            Spacer(Modifier.height(13.dp))
                            Button(
                                onClick = { step = 0; ritualStarted = true },
                                enabled = mood.isNotBlank(),
                                modifier = Modifier.fillMaxWidth().height(54.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(if (completed) "REVISAR MI MOMENTO DE HOY" else "COMENZAR MI MOMENTO CON DIOS", fontWeight = FontWeight.Bold)
                            }
                            if (completed) {
                                Spacer(Modifier.height(8.dp))
                                TextButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("↻ Reiniciar mi momento de hoy")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    V10StreakCard(streak, best, weekCompleted)
                    Spacer(Modifier.height(12.dp))
                    V10WeekCard(weekCompleted, prayerCount, gratitudeCount, favoriteCount)
                    Spacer(Modifier.height(20.dp))
                    Text("Tus espacios", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Todo lo que vas viviendo con Dios, en un solo lugar.", color = scheme.onSurfaceVariant, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        V10QuickCard("Mis oraciones", "🙏", onPrayers, Modifier.weight(1f))
                        V10QuickCard("Gratitud", "❤️", onGratitude, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        V10QuickCard("Favoritos", "⭐", onFavorites, Modifier.weight(1f))
                        V10QuickCard("Mi iglesia", "⛪", onChurch, Modifier.weight(1f))
                    }
                } else {
                    V10Ritual(
                        devotional = devotional,
                        mood = mood,
                        situation = situation,
                        guidance = guidance,
                        step = step,
                        gratitudeText = gratitudeText,
                        onGratitudeChange = { gratitudeText = it },
                        completed = completed,
                        onBack = { if (step > 0) step-- else ritualStarted = false },
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
                                    EntryStore.save(context, "gratitude", listOf(SavedEntry(now, gratitudeText.trim(), now)) + existing)
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
                                weekCompleted = v10CountRecentDays(prefs, today)
                                if (savedGratitude) gratitudeCount++
                                feedback = "✨ Completaste un momento preparado para cómo estás hoy"
                            }
                            ritualStarted = false
                        }
                    )
                }

                feedback?.let {
                    Spacer(Modifier.height(12.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = scheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
                        Text(it, modifier = Modifier.fillMaxWidth().padding(16.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = scheme.onSecondaryContainer)
                    }
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("¿Reiniciar el momento de hoy?") },
            text = { Text("Volverás a elegir cómo está tu corazón y qué estás viviendo. Tus oraciones, favoritos y agradecimientos guardados no se borrarán.") },
            confirmButton = {
                TextButton(onClick = {
                    resetV10Today(prefs)
                    confirmReset = false
                    resetToken++
                }) { Text("Reiniciar") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun V10SmartResponseCard(mood: String, situation: String, guidance: V10Guidance) {
    val scheme = MaterialTheme.colorScheme
    Card(colors = CardDefaults.cardColors(containerColor = scheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("✨ Un momento preparado para ti", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = scheme.onSecondaryContainer)
            Spacer(Modifier.height(6.dp))
            Text(guidance.intro, color = scheme.onSecondaryContainer, lineHeight = 22.sp)
            Spacer(Modifier.height(8.dp))
            Text("Enfoque de hoy: ${guidance.focus}", color = scheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold)
            if (situation.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("$mood · $situation", color = scheme.onSecondaryContainer.copy(alpha = .72f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun V10Ritual(
    devotional: Devotional,
    mood: String,
    situation: String,
    guidance: V10Guidance,
    step: Int,
    gratitudeText: String,
    onGratitudeChange: (String) -> Unit,
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
            Text("${step + 1} de 5 · ${titles[step]}", fontSize = 23.sp, fontWeight = FontWeight.Bold)
            if (situation.isNotBlank()) Text("$mood · $situation", color = scheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { (step + 1) / 5f }, modifier = Modifier.fillMaxWidth().height(7.dp))
            Spacer(Modifier.height(18.dp))

            when (step) {
                0 -> {
                    Text("📖 ${devotional.reference}", color = scheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text("“${devotional.verse}”", fontSize = 22.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(13.dp))
                    Surface(color = scheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
                        Text("Lee esta palabra teniendo presente: ${guidance.focus}.", modifier = Modifier.padding(14.dp), lineHeight = 21.sp)
                    }
                }
                1 -> {
                    Text("💭 Reflexiona", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    Text(devotional.reflection, lineHeight = 24.sp)
                    Spacer(Modifier.height(14.dp))
                    Surface(color = scheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
                        Text(guidance.reflectionQuestion, modifier = Modifier.padding(14.dp), lineHeight = 21.sp)
                    }
                }
                2 -> {
                    Text("🙏 Ora desde lo que estás viviendo", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    Text(devotional.prayer, lineHeight = 24.sp)
                    Spacer(Modifier.height(14.dp))
                    Text(guidance.prayerPrompt, color = scheme.onSurfaceVariant, lineHeight = 21.sp)
                }
                3 -> {
                    Text("❤️ Agradece en medio de tu realidad", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(guidance.gratitudePrompt, color = scheme.onSurfaceVariant, lineHeight = 21.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = gratitudeText,
                        onValueChange = onGratitudeChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Escribe algo por lo que agradeces hoy") },
                        minLines = 3,
                        shape = RoundedCornerShape(16.dp)
                    )
                    Spacer(Modifier.height(7.dp))
                    Text("Esto sí se guarda en tu diario de gratitud. La situación elegida solo personaliza el recorrido.", color = scheme.onSurfaceVariant, fontSize = 11.sp)
                }
                else -> {
                    Text("✨ Tu acción de hoy", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = scheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
                        Text(guidance.mission, modifier = Modifier.padding(16.dp), fontSize = 17.sp, lineHeight = 24.sp, color = scheme.onSecondaryContainer)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Una acción pequeña puede ayudarte a llevar lo reflexionado a tu vida cotidiana.", color = scheme.onSurfaceVariant, lineHeight = 21.sp)
                }
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text(if (step == 0) "Salir" else "Atrás") }
                if (step < 4) Button(onClick = onNext, modifier = Modifier.weight(1f)) { Text("Continuar") }
                else Button(onClick = onFinish, modifier = Modifier.weight(1f)) { Text(if (completed) "Finalizar" else "Completar") }
            }
        }
    }
}

@Composable
private fun V10StreakCard(streak: Int, best: Int, weekCompleted: Int) {
    val scheme = MaterialTheme.colorScheme
    val stage = when {
        streak >= 100 -> "🌳 Camino firme"
        streak >= 30 -> "🌿 Raíces profundas"
        streak >= 7 -> "🌱 Constancia"
        else -> "🌱 Sembrando el hábito"
    }
    Card(colors = CardDefaults.cardColors(containerColor = scheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("🔥 $streak ${if (streak == 1) "día" else "días"} caminando con Dios", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = scheme.onTertiaryContainer)
            Text(stage, color = scheme.onTertiaryContainer.copy(alpha = .78f), fontSize = 13.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Últimos 7 días: $weekCompleted/7", color = scheme.onTertiaryContainer)
                Text("Récord: $best días", color = scheme.onTertiaryContainer, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun V10WeekCard(days: Int, prayers: Int, gratitude: Int, favorites: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(17.dp)) {
            Text("Tu semana con Dios", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                V10MiniStat("📖", "$days/7", "días")
                V10MiniStat("🙏", "$prayers", "oraciones")
                V10MiniStat("❤️", "$gratitude", "gracias")
                V10MiniStat("⭐", "$favorites", "guardados")
            }
        }
    }
}

@Composable
private fun V10MiniStat(emoji: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(emoji, fontSize = 21.sp)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun V10QuickCard(label: String, emoji: String, onClick: () -> Unit, modifier: Modifier) {
    Card(onClick = onClick, modifier = modifier.height(100.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center) {
            Text(emoji, fontSize = 27.sp)
            Spacer(Modifier.height(5.dp))
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}

private fun v10GuidanceFor(mood: String, situation: String): V10Guidance {
    val situationText = if (situation.isBlank()) "lo que estás viviendo hoy" else situation.lowercase()
    val base = when (mood) {
        "Preocupado" -> V10Guidance(
            intro = "No necesitas resolver todo de una vez. Este momento te ayudará a ordenar lo que pesa y a distinguir entre lo que puedes hacer y lo que necesitas soltar.",
            focus = "confianza, calma y un paso posible frente a $situationText",
            reflectionQuestion = "¿Qué parte de esta preocupación sí requiere una acción tuya hoy y qué parte estás intentando controlar sin poder hacerlo?",
            prayerPrompt = "Después de la oración propuesta, nombra con tus propias palabras aquello que más te preocupa y pide claridad para el siguiente paso, no para tener todo resuelto de inmediato.",
            gratitudePrompt = "Incluso dentro de esta preocupación, ¿qué apoyo, capacidad o pequeño bien sigue presente hoy?",
            mission = "Escribe mentalmente una sola acción concreta que sí puedas realizar hoy respecto a $situationText. Haz esa acción y deja el resto para mañana."
        )
        "Triste" -> V10Guidance(
            intro = "Hoy no necesitas fingir que todo está bien. Puedes recorrer este momento con honestidad, sin apresurarte a cambiar lo que sientes.",
            focus = "consuelo, compañía y esperanza en medio de $situationText",
            reflectionQuestion = "¿Qué es lo que más te duele de esta situación y qué necesitarías recibir hoy: compañía, descanso, escucha o esperanza?",
            prayerPrompt = "Habla con Dios de manera sencilla y honesta. No necesitas palabras perfectas; puedes nombrar la tristeza tal como la estás sintiendo.",
            gratitudePrompt = "Sin negar tu tristeza, ¿hay una persona, recuerdo o pequeño detalle que hoy te haga sentir acompañado?",
            mission = "No te aísles. Contacta hoy a una persona de confianza y comparte, aunque sea brevemente, cómo estás viviendo $situationText."
        )
        "Necesito dirección" -> V10Guidance(
            intro = "Cuando hay decisiones importantes, la claridad suele crecer paso a paso. Este momento está pensado para ayudarte a mirar la situación con calma y sabiduría.",
            focus = "discernimiento y sabiduría para $situationText",
            reflectionQuestion = "¿Qué opción refleja mejor amor, verdad, responsabilidad y paz a largo plazo, aunque no sea la más fácil ahora?",
            prayerPrompt = "Pide sabiduría para ver con claridad, paciencia para no decidir por impulso y humildad para escuchar consejo confiable.",
            gratitudePrompt = "¿Qué recursos, personas o experiencias ya tienes que pueden ayudarte a decidir mejor?",
            mission = "Antes de decidir sobre $situationText, escribe dos opciones y una consecuencia realista de cada una. Luego conversa con una persona sabia de confianza."
        )
        "Agradecido" -> V10Guidance(
            intro = "La gratitud se vuelve más profunda cuando la reconocemos con intención. Hoy el recorrido te ayudará a detenerte y valorar lo que estás recibiendo.",
            focus = "reconocer y compartir la gratitud por $situationText",
            reflectionQuestion = "¿Qué cambió en ti o en tu vida gracias a aquello por lo que hoy estás agradecido?",
            prayerPrompt = "Convierte tu agradecimiento en una oración concreta: nombra personas, momentos y detalles en lugar de quedarte solo con una sensación general.",
            gratitudePrompt = "¿Qué detalle específico de $situationText quieres recordar cuando llegue un día difícil?",
            mission = "Expresa hoy tu gratitud a una persona relacionada con $situationText. Sé específico sobre por qué agradeces su presencia o ayuda."
        )
        else -> V10Guidance(
            intro = "La paz también necesita ser cuidada. Este momento puede ayudarte a disfrutarla con conciencia y convertirla en algo que también alcance a otros.",
            focus = "cuidar la paz y vivirla con intención en $situationText",
            reflectionQuestion = "¿Qué está favoreciendo tu paz hoy y qué hábito podrías cuidar para no perderla innecesariamente?",
            prayerPrompt = "Agradece por este momento de calma y pide sabiduría para conservarla sin desconectarte de las necesidades de quienes te rodean.",
            gratitudePrompt = "¿Qué tres cosas sencillas están contribuyendo hoy a tu paz?",
            mission = "Haz algo que proteja esta paz y compártela con alguien: escucha, anima o acompaña a una persona que lo necesite."
        )
    }

    return when (situation) {
        "Salud" -> base.copy(
            focus = if (mood == "Agradecido") "reconocer el regalo de la salud con gratitud" else "afrontar la preocupación por la salud sin anticipar más de lo que sabes",
            mission = if (mood == "Agradecido") "Cuida hoy tu cuerpo con una acción sencilla y agradece por una capacidad física que muchas veces das por sentada."
            else "Evita buscar respuestas compulsivamente. Anota tus dudas de salud y busca orientación profesional adecuada cuando corresponda."
        )
        "Dinero", "Finanzas" -> base.copy(
            focus = "sabiduría, responsabilidad y calma frente a las finanzas",
            mission = "Revisa hoy un solo aspecto concreto de tus finanzas —un gasto, una deuda o un presupuesto— y toma una decisión pequeña pero responsable."
        )
        "Decisión importante" -> base.copy(
            focus = "claridad y paciencia antes de una decisión importante",
            mission = "No decidas solo por presión. Escribe qué ganarías, qué arriesgarías y qué valor quieres proteger con tu decisión."
        )
        "Soledad" -> base.copy(
            focus = "sentirte acompañado y dar un paso fuera del aislamiento",
            mission = "Envía un mensaje o llama a alguien con quien puedas ser tú mismo. No necesitas tener una conversación perfecta; solo abrir una puerta al contacto."
        )
        "Pérdida" -> base.copy(
            focus = "dar espacio al duelo y recordar con amor",
            mission = "Reserva unos minutos para recordar algo valioso de aquello o de quien perdiste. Si puedes, compártelo con alguien de confianza."
        )
        "Oración respondida" -> base.copy(
            focus = "recordar con gratitud una oración respondida",
            mission = "Escribe brevemente cómo llegó esa respuesta y qué aprendiste durante la espera. Guárdalo como testimonio para el futuro."
        )
        else -> base
    }
}

private fun v10CountRecentDays(prefs: android.content.SharedPreferences, today: LocalDate): Int {
    val last = prefs.getString("lastCompleted", "").orEmpty()
    return (0L..6L).count { offset ->
        val key = today.minusDays(offset).toString()
        prefs.getBoolean("completed_$key", false) || last == key
    }
}

private fun resetV10Today(prefs: android.content.SharedPreferences) {
    val today = LocalDate.now()
    val todayKey = today.toString()
    val yesterdayKey = today.minusDays(1).toString()
    val lastCompleted = prefs.getString("lastCompleted", "").orEmpty()
    val currentStreak = prefs.getInt("streak", 0)

    val editor = prefs.edit()
        .remove("completed_$todayKey")
        .remove("mood_$todayKey")
        .remove("situation_$todayKey")

    if (lastCompleted == todayKey) {
        val previousStreak = (currentStreak - 1).coerceAtLeast(0)
        editor.putInt("streak", previousStreak)
        if (previousStreak > 0) editor.putString("lastCompleted", yesterdayKey)
        else editor.remove("lastCompleted")
    }

    // The historical best and saved gratitude entry are intentionally preserved.
    editor.apply()
}
