package com.modu.midiacondios

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private data class V11Mood(val emoji: String, val label: String)

private data class V11Guidance(
    val focus: String,
    val reflectionQuestion: String,
    val prayerPrompt: String,
    val gratitudePrompt: String,
    val mission: String
)

private val v11Moods = listOf(
    V11Mood("😌", "En paz"),
    V11Mood("❤️", "Agradecido"),
    V11Mood("🙏", "Necesito dirección"),
    V11Mood("😟", "Preocupado"),
    V11Mood("😔", "Triste")
)

private fun v11SituationsFor(mood: String): List<String> = when (mood) {
    "En paz" -> listOf("Cuidar mi paz", "Familia", "Trabajo", "Descanso", "Ayudar a alguien")
    "Agradecido" -> listOf("Familia", "Salud", "Trabajo", "Oración respondida", "Lo cotidiano")
    "Necesito dirección" -> listOf("Decisión importante", "Trabajo", "Familia", "Finanzas", "Propósito")
    "Preocupado" -> listOf("Trabajo", "Familia", "Salud", "Dinero", "Futuro")
    "Triste" -> listOf("Soledad", "Pérdida", "Familia", "Desilusión", "No sé por qué")
    else -> emptyList()
}

@Composable
fun V11HomeScreen(
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
    var completed by remember(todayKey, resetToken) {
        mutableStateOf(initialLast == todayKey || prefs.getBoolean("completed_$todayKey", false))
    }
    var streak by remember(todayKey, resetToken) {
        mutableIntStateOf(if (initialLast == todayKey || initialLast == yesterdayKey) initialStoredStreak else 0)
    }
    var best by remember(todayKey, resetToken) {
        mutableIntStateOf(prefs.getInt("bestStreak", initialStoredStreak))
    }
    var mood by remember(todayKey, resetToken) {
        mutableStateOf(prefs.getString("mood_$todayKey", "").orEmpty())
    }
    var situation by remember(todayKey, resetToken) {
        mutableStateOf(prefs.getString("situation_$todayKey", "").orEmpty())
    }
    var ritualStarted by remember(todayKey, resetToken) { mutableStateOf(false) }
    var step by remember(todayKey, resetToken) { mutableIntStateOf(0) }
    var gratitudeText by remember(todayKey, resetToken) { mutableStateOf("") }
    var feedback by remember(todayKey, resetToken) { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }

    val guidance = remember(mood, situation) { v11GuidanceFor(mood, situation) }
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
                    .background(scheme.primaryContainer)
                    .padding(horizontal = 20.dp, vertical = 20.dp)
            ) {
                Text("MI DÍA CON DIOS", color = scheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text("$greeting 👋", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Text(dateLabel, color = scheme.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Surface(shape = RoundedCornerShape(50), color = scheme.surface.copy(alpha = .82f)) {
                    Text(
                        when (remoteState) {
                            null -> "⏳ Preparando…"
                            true -> "☁️ Actualizado"
                            false -> "📱 Disponible sin conexión"
                        },
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        fontSize = 12.sp
                    )
                }
            }
        }

        item {
            Column(Modifier.padding(16.dp)) {
                if (!ritualStarted) {
                    Text("¿Cómo está tu corazón hoy?", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(v11Moods) { option ->
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
                        Spacer(Modifier.height(14.dp))
                        Text("¿Qué estás viviendo?", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(7.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(v11SituationsFor(mood)) { option ->
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
                    }

                    Spacer(Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = scheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(18.dp)) {
                            Text("✨ Mi momento con Dios", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            if (mood.isNotBlank()) {
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    if (situation.isBlank()) mood else "$mood · $situation",
                                    color = scheme.onPrimaryContainer.copy(alpha = .75f),
                                    fontSize = 13.sp
                                )
                            }
                            Spacer(Modifier.height(13.dp))
                            V11PulsingStartButton(
                                enabled = mood.isNotBlank(),
                                completed = completed,
                                onClick = {
                                    step = 0
                                    ritualStarted = true
                                }
                            )
                            if (completed) {
                                Spacer(Modifier.height(6.dp))
                                TextButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                                    Text("↻ Reiniciar mi momento de hoy")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Card(colors = CardDefaults.cardColors(containerColor = scheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔥 $streak ${if (streak == 1) "día" else "días"}", fontWeight = FontWeight.Bold, color = scheme.onTertiaryContainer)
                            Text("🏆 Récord: $best", color = scheme.onTertiaryContainer)
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Text("Tus espacios", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        V11QuickCard("Oraciones", "🙏", onPrayers, Modifier.weight(1f))
                        V11QuickCard("Gratitud", "❤️", onGratitude, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        V11QuickCard("Favoritos", "⭐", onFavorites, Modifier.weight(1f))
                        V11QuickCard("Iglesia", "⛪", onChurch, Modifier.weight(1f))
                    }
                } else {
                    V11Ritual(
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

                                if (gratitudeText.isNotBlank() && !prefs.getBoolean("gratitudeSaved_$todayKey", false)) {
                                    val now = System.currentTimeMillis()
                                    val existing = EntryStore.load(context, "gratitude")
                                    EntryStore.save(
                                        context,
                                        "gratitude",
                                        listOf(SavedEntry(now, gratitudeText.trim(), now)) + existing
                                    )
                                    prefs.edit().putBoolean("gratitudeSaved_$todayKey", true).apply()
                                }

                                prefs.edit()
                                    .putString("lastCompleted", todayKey)
                                    .putInt("streak", newStreak)
                                    .putInt("bestStreak", newBest)
                                    .putBoolean("completed_$todayKey", true)
                                    .apply()

                                streak = newStreak
                                best = newBest
                                completed = true
                                feedback = "✨ Momento completado"
                            }
                            ritualStarted = false
                        }
                    )
                }

                feedback?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        it,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = scheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("¿Reiniciar hoy?") },
            text = { Text("Volverás a empezar desde cómo está tu corazón. Tus oraciones, favoritos y gratitud no se borrarán.") },
            confirmButton = {
                TextButton(onClick = {
                    resetV11Today(prefs)
                    confirmReset = false
                    resetToken++
                }) { Text("Reiniciar") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun V11PulsingStartButton(enabled: Boolean, completed: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val pulse = rememberInfiniteTransition(label = "start_button_pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(animation = tween(850), repeatMode = RepeatMode.Reverse),
        label = "start_scale"
    )
    val glow by pulse.animateFloat(
        initialValue = .10f,
        targetValue = .38f,
        animationSpec = infiniteRepeatable(animation = tween(850), repeatMode = RepeatMode.Reverse),
        label = "start_glow"
    )
    val shape = RoundedCornerShape(20.dp)

    Box(
        Modifier.fillMaxWidth()
            .graphicsLayer {
                scaleX = if (enabled) scale else 1f
                scaleY = if (enabled) scale else 1f
            }
            .background(
                if (enabled) scheme.primary.copy(alpha = glow) else Color.Transparent,
                shape
            )
            .padding(4.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                if (completed) "REVISAR MI MOMENTO DE HOY" else "COMENZAR MI MOMENTO CON DIOS",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun V11Ritual(
    devotional: Devotional,
    mood: String,
    situation: String,
    guidance: V11Guidance,
    step: Int,
    gratitudeText: String,
    onGratitudeChange: (String) -> Unit,
    completed: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val titles = listOf("Palabra", "Reflexiona", "Ora", "Agradece", "Acción")

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("MI MOMENTO CON DIOS", color = scheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Text("${step + 1}/5 · ${titles[step]}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(if (situation.isBlank()) mood else "$mood · $situation", color = scheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(progress = { (step + 1) / 5f }, modifier = Modifier.fillMaxWidth().height(7.dp))
            Spacer(Modifier.height(18.dp))

            when (step) {
                0 -> {
                    Text("📖 ${devotional.reference}", color = scheme.primary, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    Text("“${devotional.verse}”", fontSize = 22.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    Text("Lee pensando en ${guidance.focus}.", color = scheme.onSurfaceVariant)
                }
                1 -> {
                    Text(devotional.reflection, lineHeight = 23.sp)
                    Spacer(Modifier.height(12.dp))
                    Surface(color = scheme.surfaceVariant, shape = RoundedCornerShape(14.dp)) {
                        Text(guidance.reflectionQuestion, modifier = Modifier.padding(14.dp), lineHeight = 21.sp)
                    }
                }
                2 -> {
                    Text(devotional.prayer, lineHeight = 23.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(guidance.prayerPrompt, color = scheme.onSurfaceVariant, lineHeight = 21.sp)
                }
                3 -> {
                    Text(guidance.gratitudePrompt, lineHeight = 21.sp)
                    Spacer(Modifier.height(9.dp))
                    OutlinedTextField(
                        value = gratitudeText,
                        onValueChange = onGratitudeChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Hoy agradezco por…") },
                        minLines = 3,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
                else -> {
                    Card(colors = CardDefaults.cardColors(containerColor = scheme.secondaryContainer), modifier = Modifier.fillMaxWidth()) {
                        Text(guidance.mission, modifier = Modifier.padding(16.dp), fontSize = 17.sp, lineHeight = 24.sp)
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            HorizontalDivider()
            Spacer(Modifier.height(11.dp))
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
private fun V11QuickCard(label: String, emoji: String, onClick: () -> Unit, modifier: Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.height(94.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center) {
            Text(emoji, fontSize = 25.sp)
            Spacer(Modifier.height(4.dp))
            Text(label, fontWeight = FontWeight.Bold)
        }
    }
}

private fun v11GuidanceFor(mood: String, situation: String): V11Guidance {
    val specific = "$mood|$situation"
    return when (specific) {
        "Preocupado|Dinero" -> V11Guidance(
            focus = "calma y sabiduría con tus finanzas",
            reflectionQuestion = "¿Qué gasto o decisión económica sí puedes ordenar hoy sin intentar resolver todo de una vez?",
            prayerPrompt = "Pide paz para no decidir desde el miedo y sabiduría para usar bien lo que tienes.",
            gratitudePrompt = "¿Qué recurso, ayuda o provisión sí tienes hoy?",
            mission = "Revisa un solo gasto de hoy y toma una decisión concreta y responsable."
        )
        "Preocupado|Salud" -> V11Guidance(
            focus = "paz, cuidado y esperanza",
            reflectionQuestion = "¿Qué está bajo tu cuidado hoy y qué parte de la situación no puedes controlar?",
            prayerPrompt = "Ora por fortaleza y por sabiduría para buscar la ayuda adecuada cuando la necesites.",
            gratitudePrompt = "¿Qué persona, cuidado o pequeña fortaleza agradeces hoy?",
            mission = "Haz hoy una acción concreta de cuidado y evita cargar mentalmente con todo el futuro."
        )
        "Triste|Soledad" -> V11Guidance(
            focus = "consuelo y compañía",
            reflectionQuestion = "¿Qué necesitas hoy: ser escuchado, acompañado o simplemente descansar cerca de alguien de confianza?",
            prayerPrompt = "Habla con Dios con tus propias palabras, sin intentar ocultar la tristeza.",
            gratitudePrompt = "¿Quién ha estado presente para ti, aunque sea de una manera pequeña?",
            mission = "Escribe o llama hoy a una persona de confianza. No te aísles."
        )
        "Necesito dirección|Decisión importante" -> V11Guidance(
            focus = "sabiduría y discernimiento",
            reflectionQuestion = "¿Qué opción refleja mejor amor, verdad, responsabilidad y paz a largo plazo?",
            prayerPrompt = "Pide claridad para decidir sin prisa y humildad para escuchar consejo sabio.",
            gratitudePrompt = "¿Qué persona o experiencia puede ayudarte a decidir mejor?",
            mission = "Anota dos opciones y una consecuencia realista de cada una antes de decidir."
        )
        "Agradecido|Oración respondida" -> V11Guidance(
            focus = "gratitud y memoria",
            reflectionQuestion = "¿Qué aprendiste durante el tiempo de espera antes de ver esta respuesta?",
            prayerPrompt = "Da gracias con nombres y detalles concretos.",
            gratitudePrompt = "Escribe cómo viste la respuesta a tu oración.",
            mission = "Comparte este testimonio con una persona que necesite esperanza."
        )
        else -> when (mood) {
            "Preocupado" -> V11Guidance(
                focus = "confianza y un paso posible",
                reflectionQuestion = "¿Qué sí puedes hacer hoy y qué necesitas dejar de intentar controlar?",
                prayerPrompt = "Nombra aquello que te preocupa y pide claridad para el siguiente paso.",
                gratitudePrompt = "¿Qué apoyo o pequeño bien sigue presente hoy?",
                mission = "Elige una sola acción concreta que sí puedas realizar hoy."
            )
            "Triste" -> V11Guidance(
                focus = "consuelo y esperanza",
                reflectionQuestion = "¿Qué es lo que más te duele hoy y qué tipo de apoyo necesitas?",
                prayerPrompt = "Ora con honestidad. No necesitas palabras perfectas.",
                gratitudePrompt = "¿Qué pequeño detalle te recuerda que no estás solo?",
                mission = "Acércate hoy a una persona de confianza."
            )
            "Necesito dirección" -> V11Guidance(
                focus = "sabiduría y paciencia",
                reflectionQuestion = "¿Estás decidiendo desde la paz y la responsabilidad o desde la presión del momento?",
                prayerPrompt = "Pide sabiduría, paciencia y apertura para escuchar buen consejo.",
                gratitudePrompt = "¿Qué recursos o personas ya tienes para orientarte?",
                mission = "Antes de decidir, detente y busca una segunda perspectiva confiable."
            )
            "Agradecido" -> V11Guidance(
                focus = "gratitud que se convierte en acción",
                reflectionQuestion = "¿Qué bendición has normalizado y hoy quieres volver a valorar?",
                prayerPrompt = "Da gracias por algo concreto y por una persona concreta.",
                gratitudePrompt = "¿Qué quieres recordar especialmente de este día?",
                mission = "Expresa hoy tu gratitud a una persona."
            )
            "En paz" -> V11Guidance(
                focus = "cuidar y compartir la paz",
                reflectionQuestion = "¿Qué está ayudando a tu corazón a permanecer en paz hoy?",
                prayerPrompt = "Da gracias por esta calma y pide sabiduría para conservarla.",
                gratitudePrompt = "¿Qué hizo posible este momento de paz?",
                mission = "Comparte ánimo con alguien que hoy esté preocupado."
            )
            else -> V11Guidance(
                focus = "tu día de hoy",
                reflectionQuestion = "¿Qué palabra necesitas llevar contigo hoy?",
                prayerPrompt = "Ora con sencillez sobre lo que estás viviendo.",
                gratitudePrompt = "¿Qué agradeces hoy?",
                mission = "Haz una acción pequeña de amor y servicio."
            )
        }
    }
}

private fun resetV11Today(prefs: SharedPreferences) {
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

    editor.apply()
}
