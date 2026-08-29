package com.modu.midiacondios

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private data class V15Mood(val emoji: String, val label: String)

private data class V15Scripture(
    val reference: String,
    val theme: String,
    val reflection: String,
    val prayer: String,
    val gratitude: String,
    val action: String
)

private val v15Moods = listOf(
    V15Mood("😌", "En paz"),
    V15Mood("❤️", "Agradecido"),
    V15Mood("🙏", "Necesito dirección"),
    V15Mood("😟", "Preocupado"),
    V15Mood("😔", "Triste")
)

private fun v15Situations(mood: String): List<String> = when (mood) {
    "En paz" -> listOf("Cuidar mi paz", "Familia", "Trabajo", "Descanso", "Ayudar a alguien")
    "Agradecido" -> listOf("Familia", "Salud", "Trabajo", "Oración respondida", "Lo cotidiano")
    "Necesito dirección" -> listOf("Decisión importante", "Trabajo", "Familia", "Finanzas", "Propósito")
    "Preocupado" -> listOf("Trabajo", "Familia", "Salud", "Dinero", "Futuro")
    "Triste" -> listOf("Soledad", "Pérdida", "Familia", "Desilusión", "No sé por qué")
    else -> emptyList()
}

@Composable
fun V15HomeScreen(
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
    val initialLast = remember(todayKey, resetToken) { prefs.getString("lastCompleted", "").orEmpty() }
    val storedStreak = remember(todayKey, resetToken) { prefs.getInt("streak", 0) }

    var completed by remember(todayKey, resetToken) {
        mutableStateOf(initialLast == todayKey || prefs.getBoolean("completed_$todayKey", false))
    }
    var streak by remember(todayKey, resetToken) {
        mutableIntStateOf(if (initialLast == todayKey || initialLast == yesterdayKey) storedStreak else 0)
    }
    var best by remember(todayKey, resetToken) { mutableIntStateOf(prefs.getInt("bestStreak", storedStreak)) }
    var mood by remember(todayKey, resetToken) { mutableStateOf(prefs.getString("mood_$todayKey", "").orEmpty()) }
    var situation by remember(todayKey, resetToken) { mutableStateOf(prefs.getString("situation_$todayKey", "").orEmpty()) }
    var started by remember(todayKey, resetToken) { mutableStateOf(false) }
    var step by remember(todayKey, resetToken) { mutableIntStateOf(0) }
    var gratitudeText by remember(todayKey, resetToken) { mutableStateOf("") }
    var confirmReset by remember { mutableStateOf(false) }
    var feedback by remember(todayKey, resetToken) { mutableStateOf<String?>(null) }

    var bubbleId by remember { mutableStateOf<String?>(null) }
    var bubbleMessage by remember { mutableStateOf("") }
    var bubbleToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(bubbleToken) {
        if (bubbleToken > 0) {
            delay(3300)
            bubbleId = null
        }
    }

    fun showBubble(id: String, message: String) {
        bubbleId = id
        bubbleMessage = message
        bubbleToken++
    }

    val scripture = remember(mood, situation) { v15ScriptureFor(mood, situation) }
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
            }
        }

        item {
            Column(Modifier.padding(16.dp)) {
                if (!started) {
                    Text("¿Cómo está tu corazón hoy?", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(v15Moods) { option ->
                            val id = "mood:${option.label}"
                            Box {
                                FilterChip(
                                    selected = mood == option.label,
                                    onClick = {
                                        mood = option.label
                                        situation = ""
                                        prefs.edit()
                                            .putString("mood_$todayKey", option.label)
                                            .remove("situation_$todayKey")
                                            .apply()
                                        showBubble(id, v15MoodMessage(option.label))
                                    },
                                    label = { Text("${option.emoji} ${option.label}") }
                                )
                                V15AnchoredSpeechBubble(
                                    visible = bubbleId == id,
                                    message = bubbleMessage
                                )
                            }
                        }
                    }

                    if (mood.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text("¿Qué estás viviendo?", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(7.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(v15Situations(mood)) { option ->
                                val id = "situation:$mood:$option"
                                Box {
                                    FilterChip(
                                        selected = situation == option,
                                        onClick = {
                                            situation = option
                                            prefs.edit().putString("situation_$todayKey", option).apply()
                                            showBubble(id, v15SituationMessage(mood, option))
                                        },
                                        label = { Text(option) }
                                    )
                                    V15AnchoredSpeechBubble(
                                        visible = bubbleId == id,
                                        message = bubbleMessage
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
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
                            V15PulsingStartButton(
                                enabled = mood.isNotBlank(),
                                completed = completed,
                                onClick = {
                                    step = 0
                                    started = true
                                    bubbleId = null
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
                    Card(
                        colors = CardDefaults.cardColors(containerColor = scheme.tertiaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🔥 $streak ${if (streak == 1) "día" else "días"}", fontWeight = FontWeight.Bold)
                            Text("🏆 Récord: $best")
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Text("Tus espacios", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        V15QuickCard("Oraciones", "🙏", onPrayers, Modifier.weight(1f))
                        V15QuickCard("Gratitud", "❤️", onGratitude, Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        V15QuickCard("Favoritos", "⭐", onFavorites, Modifier.weight(1f))
                        V15QuickCard("Iglesia", "⛪", onChurch, Modifier.weight(1f))
                    }
                } else {
                    V15Ritual(
                        scripture = scripture,
                        mood = mood,
                        situation = situation,
                        step = step,
                        gratitudeText = gratitudeText,
                        onGratitudeChange = { gratitudeText = it },
                        completed = completed,
                        onBack = { if (step > 0) step-- else started = false },
                        onNext = { if (step < 4) step++ },
                        onFinish = {
                            if (!completed) {
                                val previous = prefs.getString("lastCompleted", "").orEmpty()
                                val previousStreak = prefs.getInt("streak", 0)
                                val newStreak = if (previous == yesterdayKey) previousStreak + 1 else 1
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
                            started = false
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
                    resetV15Today(prefs)
                    confirmReset = false
                    resetToken++
                    started = false
                    bubbleId = null
                    gratitudeText = ""
                }) { Text("Reiniciar") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun V15AnchoredSpeechBubble(visible: Boolean, message: String) {
    if (!visible) return

    val scheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val gapPx = with(density) { 1.dp.roundToPx() }
    val edgePx = with(density) { 8.dp.roundToPx() }
    val provider = remember(gapPx, edgePx) { V15BubblePositionProvider(gapPx, edgePx) }

    Popup(
        popupPositionProvider = provider,
        onDismissRequest = {},
        properties = PopupProperties(
            focusable = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            clippingEnabled = false
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.widthIn(min = 176.dp, max = 270.dp),
                shape = RoundedCornerShape(24.dp),
                color = scheme.surface,
                shadowElevation = 8.dp,
                border = BorderStroke(3.dp, scheme.onSurface)
            ) {
                Text(
                    message,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Canvas(Modifier.width(28.dp).height(15.dp)) {
                val tail = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width / 2f, size.height)
                    lineTo(size.width, 0f)
                    close()
                }
                drawPath(tail, scheme.surface)
                drawPath(
                    tail,
                    scheme.onSurface,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                )
                drawLine(
                    color = scheme.surface,
                    start = androidx.compose.ui.geometry.Offset(3.dp.toPx(), 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width - 3.dp.toPx(), 0f),
                    strokeWidth = 4.dp.toPx()
                )
            }
        }
    }
}

private class V15BubblePositionProvider(
    private val gapPx: Int,
    private val edgePx: Int
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val preferredX = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
        val maxX = (windowSize.width - popupContentSize.width - edgePx).coerceAtLeast(edgePx)
        val x = preferredX.coerceIn(edgePx, maxX)

        val y = (anchorBounds.top - popupContentSize.height - gapPx).coerceAtLeast(edgePx)
        return IntOffset(x, y)
    }
}

@Composable
private fun V15PulsingStartButton(enabled: Boolean, completed: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val pulse = rememberInfiniteTransition(label = "v15_start_pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.026f,
        animationSpec = infiniteRepeatable(tween(850), repeatMode = RepeatMode.Reverse),
        label = "v15_scale"
    )
    val glow by pulse.animateFloat(
        initialValue = .10f,
        targetValue = .38f,
        animationSpec = infiniteRepeatable(tween(850), repeatMode = RepeatMode.Reverse),
        label = "v15_glow"
    )
    val shape = RoundedCornerShape(20.dp)

    Box(
        Modifier.fillMaxWidth()
            .graphicsLayer {
                scaleX = if (enabled) scale else 1f
                scaleY = if (enabled) scale else 1f
            }
            .background(if (enabled) scheme.primary.copy(alpha = glow) else Color.Transparent, shape)
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
private fun V15Ritual(
    scripture: V15Scripture,
    mood: String,
    situation: String,
    step: Int,
    gratitudeText: String,
    onGratitudeChange: (String) -> Unit,
    completed: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
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
                    Text("📖 ${scripture.reference}", color = scheme.primary, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Nueva Traducción Viviente (NTV)", color = scheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(Modifier.height(13.dp))
                    Surface(color = scheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(15.dp)) {
                            Text("Para tu momento de hoy", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(5.dp))
                            Text(scripture.theme, lineHeight = 22.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { openV15Ntv(context, scripture.reference) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) { Text("LEER EL PASAJE EN NTV") }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "La lectura completa se abre en BibleGateway.",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = scheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }

                1 -> {
                    Text("💭 Reflexión breve", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(9.dp))
                    Text(scripture.reflection, lineHeight = 23.sp)
                }

                2 -> {
                    Text("🙏 Oración", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(9.dp))
                    Text(scripture.prayer, lineHeight = 23.sp)
                }

                3 -> {
                    Text(scripture.gratitude, lineHeight = 21.sp)
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
                    Card(
                        colors = CardDefaults.cardColors(containerColor = scheme.secondaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(scripture.action, modifier = Modifier.padding(16.dp), fontSize = 17.sp, lineHeight = 24.sp)
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
private fun V15QuickCard(label: String, emoji: String, onClick: () -> Unit, modifier: Modifier) {
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

private fun v15MoodMessage(mood: String): String = when (mood) {
    "En paz" -> "Qué bueno sentir calma. Cuida esa paz y deja que también alcance a otros."
    "Agradecido" -> "Hoy tienes motivos para agradecer. Hazlos visibles y no los dejes pasar."
    "Necesito dirección" -> "No necesitas ver todo el camino. Busca claridad para el siguiente paso."
    "Preocupado" -> "No cargues todo de una vez. Entrega una preocupación y avanza paso a paso."
    "Triste" -> "Puedes acercarte a Dios tal como estás. Tu tristeza también puede ser escuchada."
    else -> "Dedica estos minutos a escuchar, orar y caminar con calma."
}

private fun v15SituationMessage(mood: String, situation: String): String {
    val key = "$mood|$situation"
    return when (key) {
        "En paz|Cuidar mi paz" -> "Protege aquello que hoy está dando calma a tu corazón."
        "En paz|Familia" -> "Tu paz puede convertirse en paciencia y ternura para tu familia."
        "En paz|Trabajo" -> "Lleva esa calma a tus responsabilidades sin vivir apresurado."
        "En paz|Descanso" -> "Descansar también es reconocer que no todo depende de ti."
        "En paz|Ayudar a alguien" -> "Tal vez tu paz sea justo el ánimo que alguien necesita hoy."
        "Agradecido|Familia" -> "Hoy puede ser un buen día para decir: gracias por estar conmigo."
        "Agradecido|Salud" -> "Reconoce con gratitud lo que tu cuerpo sí puede hacer hoy."
        "Agradecido|Trabajo" -> "Mira una oportunidad, aprendizaje o provisión que tu trabajo te ha dado."
        "Agradecido|Oración respondida" -> "Recuerda cómo empezó esa oración y agradece el camino recorrido."
        "Agradecido|Lo cotidiano" -> "Lo sencillo también puede convertirse en una razón para agradecer."
        "Necesito dirección|Decisión importante" -> "No decidas solo por presión. Busca verdad, sabiduría y paz."
        "Necesito dirección|Trabajo" -> "Piensa en el paso que combine responsabilidad, paz y propósito."
        "Necesito dirección|Familia" -> "Antes de responder, escucha. A veces la dirección empieza allí."
        "Necesito dirección|Finanzas" -> "La claridad financiera suele empezar con orden, paciencia y límites."
        "Necesito dirección|Propósito" -> "Tu propósito también se construye siendo fiel en lo pequeño."
        "Preocupado|Trabajo" -> "Haz bien lo de hoy antes de cargar con todo lo de mañana."
        "Preocupado|Familia" -> "No puedes controlar a todos, pero sí amar y responder con sabiduría."
        "Preocupado|Salud" -> "Cuida lo que está en tus manos y busca apoyo cuando sea necesario."
        "Preocupado|Dinero" -> "Ordena un paso concreto. El miedo no tiene que decidir por ti."
        "Preocupado|Futuro" -> "El futuro no se resuelve hoy completo. Camina este día con fidelidad."
        "Triste|Soledad" -> "No te aísles por completo. Una conversación puede abrir un poco de luz."
        "Triste|Pérdida" -> "El dolor necesita tiempo. No tienes que apresurarte a sentirte bien."
        "Triste|Familia" -> "Puedes buscar paz sin negar lo que te ha dolido."
        "Triste|Desilusión" -> "Una decepción no define toda tu historia ni todo tu futuro."
        "Triste|No sé por qué" -> "Aunque no sepas explicarlo, lo que sientes puede ser llevado a Dios."
        else -> v15MoodMessage(mood)
    }
}

private fun v15ScriptureFor(mood: String, situation: String): V15Scripture {
    val key = "$mood|$situation"
    return when (key) {
        "En paz|Cuidar mi paz" -> V15Scripture(
            "Colosenses 3:15",
            "Permite que la paz de Cristo oriente tus reacciones y decisiones.",
            "Este pasaje invita a no tratar la paz como algo accidental. Puedes cuidarla eligiendo qué pensamientos alimentas y cómo respondes a lo que ocurre.",
            "Señor, gracias por la paz que hoy puedo reconocer. Ayúdame a protegerla y a responder con calma y sabiduría. Amén.",
            "¿Qué hizo posible este momento de paz?",
            "Identifica una cosa que protege tu paz y hazle espacio hoy."
        )
        "En paz|Familia" -> V15Scripture(
            "Romanos 12:18",
            "Haz lo que esté de tu parte para vivir en paz con los demás.",
            "No controlas las reacciones de tu familia, pero sí tus palabras, tu tono y la forma en que buscas reconciliación.",
            "Dios, dame paciencia para amar a mi familia y sabiduría para construir paz desde mis propias decisiones. Amén.",
            "¿Qué valoras hoy de tu familia?",
            "Ten hoy una conversación en la que escuches más de lo que respondes."
        )
        "En paz|Trabajo" -> V15Scripture(
            "Colosenses 3:23",
            "Vive tus tareas con entrega y propósito, sin medir tu valor solo por el resultado.",
            "La paz también puede acompañar el trabajo. Hacer bien lo que corresponde hoy evita que la ansiedad por el resultado gobierne todo.",
            "Señor, ayúdame a trabajar con un corazón tranquilo, responsable y agradecido. Amén.",
            "¿Qué oportunidad o aprendizaje agradeces de tu trabajo?",
            "Elige una tarea importante y hazla con atención, sin prisa innecesaria."
        )
        "En paz|Descanso" -> V15Scripture(
            "Mateo 11:28",
            "Jesús invita a acercarse a Él cuando el cansancio y las cargas pesan.",
            "Descansar no es abandonar tus responsabilidades. También es reconocer tus límites y permitir que tu mente y tu cuerpo se recuperen.",
            "Jesús, enséñame a descansar sin culpa y a dejar contigo las cargas que hoy no necesito seguir llevando. Amén.",
            "¿Qué descanso o cuidado puedes agradecer hoy?",
            "Reserva un momento breve sin pantallas ni pendientes y descansa de verdad."
        )
        "En paz|Ayudar a alguien" -> V15Scripture(
            "Mateo 5:9",
            "La paz que recibes puede convertirse en una influencia de bien para otra persona.",
            "No siempre ayudar significa resolver. A veces basta escuchar, acompañar o decir una palabra que devuelva esperanza.",
            "Dios, muéstrame a quién puedo llevar ánimo y paz hoy. Amén.",
            "¿Quién ha sido una presencia de paz para ti?",
            "Envía un mensaje de ánimo a alguien que esté pasando un momento difícil."
        )
        "Agradecido|Familia" -> V15Scripture(
            "Filipenses 1:3",
            "Recordar a las personas con gratitud cambia la forma en que valoras su presencia.",
            "La costumbre puede volver invisible lo valioso. Hoy vuelve a mirar a tu familia como un regalo que merece ser reconocido.",
            "Señor, gracias por mi familia. Ayúdame a valorar lo bueno y expresar mi gratitud con hechos. Amén.",
            "¿Por qué persona de tu familia agradeces especialmente hoy?",
            "Dile a alguien de tu familia una razón concreta por la que agradeces su vida."
        )
        "Agradecido|Salud" -> V15Scripture(
            "Salmo 103:2-5",
            "Recordar los beneficios recibidos ayuda a cultivar un corazón agradecido.",
            "Agradecer por la salud no exige ignorar limitaciones. También es reconocer cada capacidad y cuidado que hoy sí están presentes.",
            "Dios, gracias por la vida y por cada fortaleza que hoy tengo. Dame sabiduría para cuidar mi cuerpo. Amén.",
            "¿Qué capacidad de tu cuerpo agradeces hoy?",
            "Haz una acción sencilla de cuidado: agua, descanso, movimiento o una consulta pendiente."
        )
        "Agradecido|Trabajo" -> V15Scripture(
            "Colosenses 3:17",
            "Las tareas diarias también pueden vivirse con gratitud y propósito.",
            "Incluso un trabajo exigente puede contener provisión, aprendizaje y oportunidades para servir a otros.",
            "Señor, gracias por lo que mi trabajo me permite aprender y aportar. Ayúdame a vivirlo con responsabilidad. Amén.",
            "¿Qué aspecto de tu trabajo agradeces hoy?",
            "Reconoce y agradece el esfuerzo de una persona con la que trabajas."
        )
        "Agradecido|Oración respondida" -> V15Scripture(
            "Salmo 66:19-20",
            "Reconocer una oración escuchada fortalece la memoria espiritual.",
            "No olvides el camino recorrido mientras esperabas. La respuesta también puede enseñarte paciencia y confianza.",
            "Gracias, Dios, por escucharme. Ayúdame a recordar esta respuesta cuando vuelva a tener que esperar. Amén.",
            "Escribe cómo reconoces esa respuesta.",
            "Comparte este testimonio con alguien que necesite esperanza."
        )
        "Agradecido|Lo cotidiano" -> V15Scripture(
            "Salmo 118:24",
            "El día presente puede recibirse como una oportunidad para agradecer.",
            "La gratitud crece cuando dejamos de esperar solo grandes acontecimientos y aprendemos a reconocer el valor de lo sencillo.",
            "Señor, gracias por este día y por los regalos pequeños que muchas veces paso por alto. Amén.",
            "¿Qué detalle sencillo quieres recordar de hoy?",
            "Anota tres cosas pequeñas por las que estás agradecido."
        )
        "Necesito dirección|Decisión importante" -> V15Scripture(
            "Proverbios 3:5-6",
            "La dirección comienza confiando en Dios y reconociéndolo en el camino.",
            "No siempre tendrás toda la información. Una buena decisión puede requerir oración, consejo y humildad para reconocer tus límites.",
            "Dios, dame claridad para esta decisión. Corrige mis motivaciones y guíame hacia lo verdadero y responsable. Amén.",
            "¿Qué persona sabia puede ayudarte a mirar esta decisión mejor?",
            "Escribe dos opciones y una consecuencia realista de cada una."
        )
        "Necesito dirección|Trabajo" -> V15Scripture(
            "Proverbios 16:3",
            "Pon tus proyectos delante de Dios y ordénalos con propósito.",
            "Antes de preguntarte solo qué opción conviene más, considera también cuál es responsable, sostenible y coherente con tus valores.",
            "Señor, pongo delante de ti mi trabajo y mis planes. Dame sabiduría para avanzar sin ansiedad. Amén.",
            "¿Qué aprendizaje o recurso ya tienes para avanzar?",
            "Define un solo siguiente paso concreto para tu trabajo."
        )
        "Necesito dirección|Familia" -> V15Scripture(
            "Santiago 1:19",
            "Escuchar con atención y responder con calma puede transformar una conversación.",
            "En asuntos familiares, la dirección no siempre llega mediante una respuesta rápida. A veces comienza escuchando mejor.",
            "Dios, ayúdame a escuchar antes de responder y a buscar una solución que cuide la verdad y a las personas. Amén.",
            "¿Qué persona de tu familia agradeces aunque hoy exista tensión?",
            "Haz una pregunta sincera y escucha la respuesta sin interrumpir."
        )
        "Necesito dirección|Finanzas" -> V15Scripture(
            "Proverbios 21:5",
            "La planificación constante suele dar mejores frutos que actuar con prisa.",
            "La claridad financiera se construye con decisiones pequeñas: ordenar, priorizar, esperar y evitar impulsos que luego pesan.",
            "Señor, dame disciplina y sabiduría para administrar lo que tengo. Amén.",
            "¿Qué provisión o recurso sí tienes hoy?",
            "Revisa un gasto y decide conscientemente si realmente lo necesitas."
        )
        "Necesito dirección|Propósito" -> V15Scripture(
            "Efesios 2:10",
            "Tu propósito también se descubre caminando en las buenas obras que tienes delante.",
            "El propósito no siempre llega como una revelación inmediata. Muchas veces se reconoce sirviendo, aprendiendo y siendo fiel en lo pequeño.",
            "Dios, muéstrame cómo usar lo que soy y lo que tengo para hacer el bien. Guíame paso a paso. Amén.",
            "¿Qué capacidad tuya agradeces y podrías poner al servicio de otros?",
            "Haz hoy una acción pequeña relacionada con aquello que sientes llamado a desarrollar."
        )
        "Preocupado|Trabajo" -> V15Scripture(
            "Mateo 6:34",
            "No cargues hoy con todos los problemas que podrían aparecer mañana.",
            "Pensar constantemente en lo que podría salir mal consume energía que necesitas para responder bien a lo que sí está delante de ti.",
            "Señor, ayúdame a trabajar con calma y a no vivir hoy los problemas de mañana. Amén.",
            "¿Qué oportunidad o apoyo tienes en tu trabajo hoy?",
            "Concéntrate en una tarea que sí puedes resolver hoy."
        )
        "Preocupado|Familia" -> V15Scripture(
            "1 Pedro 5:7",
            "Puedes poner tus preocupaciones delante de Dios porque tu vida le importa.",
            "Amar a tu familia no significa poder controlar todo lo que ocurre. Puedes cuidar, acompañar y también reconocer tus límites.",
            "Dios, te entrego lo que me preocupa de mi familia. Muéstrame qué me corresponde hacer y qué debo soltar. Amén.",
            "¿Qué gesto de amor de tu familia puedes agradecer hoy?",
            "Haz una acción de cuidado sin intentar controlar la respuesta de los demás."
        )
        "Preocupado|Salud" -> V15Scripture(
            "Salmo 46:1",
            "Dios puede ser refugio y fortaleza cuando atraviesas incertidumbre.",
            "La fe no sustituye el cuidado médico. Puede acompañarte mientras haces lo que está en tus manos y enfrentas lo que todavía no sabes.",
            "Señor, dame fortaleza, paz y sabiduría para cuidar mi salud y buscar la ayuda adecuada. Amén.",
            "¿Qué persona, cuidado o fortaleza agradeces hoy?",
            "Haz una acción concreta de cuidado y busca atención profesional si la necesitas."
        )
        "Preocupado|Dinero" -> V15Scripture(
            "Mateo 6:31-33",
            "No dejes que la preocupación por las necesidades gobierne todo tu corazón.",
            "La preocupación financiera es real, pero el miedo puede empeorar las decisiones. Empieza por ordenar lo que sí puedes controlar hoy.",
            "Dios, dame provisión, serenidad y sabiduría para administrar bien lo que tengo. Amén.",
            "¿Qué provisión, recurso o ayuda sí puedes reconocer hoy?",
            "Revisa un gasto o deuda y toma un paso concreto y responsable."
        )
        "Preocupado|Futuro" -> V15Scripture(
            "Salmo 37:5",
            "Entrega tu camino a Dios mientras sigues avanzando con responsabilidad.",
            "No necesitas conocer cada detalle del futuro para dar un buen paso hoy. La dirección suele aclararse mientras caminas.",
            "Señor, pongo mi futuro delante de ti. Dame paz para lo que no controlo y valentía para lo que sí debo hacer. Amén.",
            "¿Qué oportunidad del presente agradeces hoy?",
            "Define una acción pequeña que mejore tu mañana sin intentar resolver todo el futuro."
        )
        "Triste|Soledad" -> V15Scripture(
            "Isaías 41:10",
            "La presencia y la ayuda de Dios pueden sostenerte cuando te sientes solo.",
            "Sentirte solo no significa que debas permanecer aislado. Puedes buscar a Dios y permitir que personas confiables se acerquen.",
            "Dios, acompáñame en esta soledad y dame fuerzas para acercarme a alguien de confianza. Amén.",
            "¿Quién ha estado presente para ti, aunque sea de una forma pequeña?",
            "Escribe o llama hoy a una persona de confianza."
        )
        "Triste|Pérdida" -> V15Scripture(
            "Mateo 5:4",
            "Jesús reconoce el dolor de quienes lloran y anuncia consuelo.",
            "El duelo no necesita ser apresurado. Llorar una pérdida es parte de amar lo que fue importante y aprender a caminar con la ausencia.",
            "Señor, recibe mi dolor. Consuélame y dame fuerzas para atravesar esta pérdida sin fingir que estoy bien. Amén.",
            "¿Qué recuerdo valioso puedes agradecer de aquello que perdiste?",
            "Date permiso para recordar, llorar o hablar con alguien de confianza."
        )
        "Triste|Familia" -> V15Scripture(
            "Salmo 147:3",
            "Dios se acerca al corazón herido y acompaña los procesos de restauración.",
            "Las heridas familiares pueden ser profundas. Sanar puede requerir tiempo, límites saludables, conversación y apoyo.",
            "Dios, conoce lo que me duele en mi familia. Dame sabiduría para sanar y actuar con amor. Amén.",
            "¿Qué persona o recuerdo bueno de tu familia todavía puedes agradecer?",
            "Identifica un límite o una conversación saludable que necesites dar."
        )
        "Triste|Desilusión" -> V15Scripture(
            "Salmo 42:11",
            "En medio del desánimo, puedes volver a orientar tu esperanza hacia Dios.",
            "Una decepción puede ocupar todo el horizonte por un momento, pero no tiene por qué convertirse en la definición de tu futuro.",
            "Señor, hoy estoy desilusionado. Ayúdame a procesar lo ocurrido y a recuperar esperanza. Amén.",
            "¿Qué sigue siendo bueno aunque esto no haya salido como esperabas?",
            "Escribe una expectativa que debes soltar y un nuevo paso que sí puedes dar."
        )
        "Triste|No sé por qué" -> V15Scripture(
            "Salmo 13:1-2,5-6",
            "La Biblia también da espacio a preguntas, cansancio y emociones difíciles de explicar.",
            "No necesitas entender perfectamente lo que sientes para acercarte a Dios. Puedes expresar confusión, tristeza y esperanza en la misma oración.",
            "Dios, no sé explicar bien cómo me siento, pero tú conoces mi corazón. Quédate conmigo hoy. Amén.",
            "¿Qué pequeño bien sigue presente hoy?",
            "Haz algo suave por ti: caminar, descansar o conversar con alguien seguro."
        )
        else -> when (mood) {
            "En paz" -> V15Scripture("Filipenses 4:7", "La paz de Dios puede guardar tu corazón y tu mente.", "La paz puede recibirse y también cuidarse. Presta atención a lo que alimenta tu calma.", "Dios, gracias por la paz de hoy. Guarda mi mente y mis decisiones. Amén.", "¿Qué hizo posible esta paz?", "Comparte ánimo con alguien que lo necesite.")
            "Agradecido" -> V15Scripture("1 Tesalonicenses 5:18", "La gratitud puede practicarse aun en días imperfectos.", "Agradecer no niega las dificultades; te ayuda a reconocer lo bueno que sigue presente.", "Señor, abre mis ojos para reconocer y agradecer lo bueno de este día. Amén.", "¿Qué quieres recordar especialmente de hoy?", "Expresa tu gratitud a una persona.")
            "Necesito dirección" -> V15Scripture("Santiago 1:5", "Cuando te falta sabiduría, puedes pedirla a Dios.", "La dirección puede llegar por medio de la oración, el consejo y decisiones responsables.", "Dios, necesito sabiduría. Ayúdame a escuchar y avanzar sin prisa ni miedo. Amén.", "¿Qué recurso o persona puede orientarte?", "Busca una segunda perspectiva confiable antes de decidir.")
            "Preocupado" -> V15Scripture("Filipenses 4:6-7", "Puedes llevar tus preocupaciones a Dios en oración.", "No se trata de negar lo que te preocupa, sino de dejar de cargarlo solo y avanzar con claridad.", "Señor, te entrego lo que pesa en mi mente. Dame paz para hacer lo que me corresponde. Amén.", "¿Qué apoyo o pequeño bien sigue presente?", "Elige una sola acción concreta que sí puedas realizar hoy.")
            "Triste" -> V15Scripture("Salmo 34:18", "Dios se acerca a quienes tienen el corazón herido.", "La tristeza no te hace menos espiritual. Puedes reconocerla y permitirte avanzar con paciencia.", "Dios, acércate a mi corazón hoy. Dame consuelo, fuerza y compañía. Amén.", "¿Qué pequeño detalle te recuerda que no estás completamente solo?", "Acércate hoy a una persona de confianza.")
            else -> V15Scripture("Salmo 119:105", "La palabra de Dios puede iluminar el siguiente paso.", "No necesitas resolver toda tu vida hoy. Busca claridad para lo que tienes delante.", "Dios, ilumina mi camino y ayúdame a vivir este día con sabiduría. Amén.", "¿Qué agradeces hoy?", "Haz una acción pequeña de amor y servicio.")
        }
    }
}

private fun openV15Ntv(context: Context, reference: String) {
    val url = "https://www.biblegateway.com/passage/?search=${Uri.encode(reference)}&version=NTV"
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

private fun resetV15Today(prefs: SharedPreferences) {
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
