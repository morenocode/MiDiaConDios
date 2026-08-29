package com.modu.midiacondios

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.zIndex
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private data class V14Mood(val emoji: String, val label: String)

private data class V14Guidance(
    val gratitudePrompt: String,
    val mission: String
)

private data class V14Scripture(
    val reference: String,
    val theme: String,
    val reflection: String,
    val prayer: String
)

private val v14Moods = listOf(
    V14Mood("😌", "En paz"),
    V14Mood("❤️", "Agradecido"),
    V14Mood("🙏", "Necesito dirección"),
    V14Mood("😟", "Preocupado"),
    V14Mood("😔", "Triste")
)

private fun v14SituationsFor(mood: String): List<String> = when (mood) {
    "En paz" -> listOf("Cuidar mi paz", "Familia", "Trabajo", "Descanso", "Ayudar a alguien")
    "Agradecido" -> listOf("Familia", "Salud", "Trabajo", "Oración respondida", "Lo cotidiano")
    "Necesito dirección" -> listOf("Decisión importante", "Trabajo", "Familia", "Finanzas", "Propósito")
    "Preocupado" -> listOf("Trabajo", "Familia", "Salud", "Dinero", "Futuro")
    "Triste" -> listOf("Soledad", "Pérdida", "Familia", "Desilusión", "No sé por qué")
    else -> emptyList()
}

@Composable
fun V14HomeScreen(
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

    var cloudMessage by remember { mutableStateOf<String?>(null) }
    var cloudToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(cloudToken) {
        if (cloudToken > 0) {
            delay(3400)
            cloudMessage = null
        }
    }

    val scripture = remember(mood, situation) { v14ScriptureFor(mood, situation) }
    val guidance = remember(mood, situation) { v14GuidanceFor(mood, situation) }

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

    Box(Modifier.fillMaxSize()) {
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
                    if (!ritualStarted) {
                        Text("¿Cómo está tu corazón hoy?", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(v14Moods) { option ->
                                FilterChip(
                                    selected = mood == option.label,
                                    onClick = {
                                        mood = option.label
                                        situation = ""
                                        prefs.edit()
                                            .putString("mood_$todayKey", option.label)
                                            .remove("situation_$todayKey")
                                            .apply()
                                        cloudMessage = v14MoodCloud(option.label)
                                        cloudToken++
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
                                items(v14SituationsFor(mood)) { option ->
                                    FilterChip(
                                        selected = situation == option,
                                        onClick = {
                                            val newSituation = if (situation == option) "" else option
                                            situation = newSituation
                                            val editor = prefs.edit()
                                            if (newSituation.isBlank()) editor.remove("situation_$todayKey")
                                            else editor.putString("situation_$todayKey", newSituation)
                                            editor.apply()
                                            cloudMessage = if (newSituation.isBlank()) {
                                                v14MoodCloud(mood)
                                            } else {
                                                v14SituationCloud(mood, newSituation)
                                            }
                                            cloudToken++
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
                                V14PulsingStartButton(
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
                            V14QuickCard("Oraciones", "🙏", onPrayers, Modifier.weight(1f))
                            V14QuickCard("Gratitud", "❤️", onGratitude, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            V14QuickCard("Favoritos", "⭐", onFavorites, Modifier.weight(1f))
                            V14QuickCard("Iglesia", "⛪", onChurch, Modifier.weight(1f))
                        }
                    } else {
                        V14Ritual(
                            scripture = scripture,
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

        AnimatedVisibility(
            visible = cloudMessage != null,
            enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 2 },
            exit = fadeOut(tween(180)) + slideOutVertically(tween(220)) { -it / 2 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp, start = 18.dp, end = 18.dp)
                .zIndex(4f)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = scheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    "☁️  ${cloudMessage.orEmpty()}",
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
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
                    resetV14Today(prefs)
                    confirmReset = false
                    resetToken++
                }) { Text("Reiniciar") }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun V14PulsingStartButton(enabled: Boolean, completed: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val pulse = rememberInfiniteTransition(label = "v14_start_button_pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(animation = tween(850), repeatMode = RepeatMode.Reverse),
        label = "v14_start_scale"
    )
    val glow by pulse.animateFloat(
        initialValue = .10f,
        targetValue = .38f,
        animationSpec = infiniteRepeatable(animation = tween(850), repeatMode = RepeatMode.Reverse),
        label = "v14_start_glow"
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
private fun V14Ritual(
    scripture: V14Scripture,
    mood: String,
    situation: String,
    guidance: V14Guidance,
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
                    Text("📖 ${scripture.reference}", color = scheme.primary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(5.dp))
                    Text("Lectura sugerida · Nueva Traducción Viviente (NTV)", color = scheme.onSurfaceVariant, fontSize = 12.sp)
                    Spacer(Modifier.height(14.dp))
                    Surface(color = scheme.surfaceVariant, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(15.dp)) {
                            Text("Idea central", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(scripture.theme, lineHeight = 22.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { openNtvPassage(context, scripture.reference) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("LEER TEXTO COMPLETO EN NTV")
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "El texto completo se abre en BibleGateway para respetar la edición NTV.",
                        color = scheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
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
private fun V14QuickCard(label: String, emoji: String, onClick: () -> Unit, modifier: Modifier) {
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

private fun v14MoodCloud(mood: String): String = when (mood) {
    "En paz" -> "Qué bueno. Cuida esa paz y compártela hoy."
    "Agradecido" -> "La gratitud hace visible lo bueno que a veces damos por sentado."
    "Necesito dirección" -> "No tienes que resolverlo todo hoy. Busca sabiduría para el siguiente paso."
    "Preocupado" -> "Una carga a la vez. Hoy concéntrate en lo que sí puedes hacer."
    "Triste" -> "Puedes venir tal como estás. No necesitas esconder lo que sientes."
    else -> "Este momento puede ayudarte a detenerte y escuchar con calma."
}

private fun v14SituationCloud(mood: String, situation: String): String {
    val key = "$mood|$situation"
    return when (key) {
        "En paz|Cuidar mi paz" -> "Protege lo que hoy trae calma a tu corazón."
        "En paz|Familia" -> "Tu paz también puede convertirse en paciencia para tu familia."
        "En paz|Trabajo" -> "Lleva esa calma a tus responsabilidades de hoy."
        "En paz|Descanso" -> "Descansar también es reconocer que no todo depende de ti."
        "En paz|Ayudar a alguien" -> "Tal vez tu paz sea justo el ánimo que otra persona necesita."
        "Agradecido|Familia" -> "Hoy puede ser un buen día para decir en voz alta: gracias por ustedes."
        "Agradecido|Salud" -> "Agradece lo que tu cuerpo sí puede hacer hoy."
        "Agradecido|Trabajo" -> "Reconoce una oportunidad, aprendizaje o provisión de tu trabajo."
        "Agradecido|Oración respondida" -> "Recuerda cómo comenzó esa oración y reconoce el camino recorrido."
        "Agradecido|Lo cotidiano" -> "Lo sencillo también puede ser una bendición."
        "Necesito dirección|Decisión importante" -> "No decidas solo por presión; busca claridad, verdad y paz."
        "Necesito dirección|Trabajo" -> "Piensa en el paso que combina responsabilidad con propósito."
        "Necesito dirección|Familia" -> "Escuchar bien puede ser tan importante como tener una respuesta."
        "Necesito dirección|Finanzas" -> "La sabiduría financiera suele comenzar con orden y paciencia."
        "Necesito dirección|Propósito" -> "Tu propósito se construye también en las pequeñas decisiones de hoy."
        "Preocupado|Trabajo" -> "Haz bien lo de hoy antes de cargar con todo lo de mañana."
        "Preocupado|Familia" -> "No puedes controlar a todos, pero sí puedes amar y responder con sabiduría."
        "Preocupado|Salud" -> "Cuida lo que está en tus manos y busca apoyo cuando lo necesites."
        "Preocupado|Dinero" -> "Primero ordena un paso concreto; el miedo no tiene que decidir por ti."
        "Preocupado|Futuro" -> "El futuro no se resuelve en una sola noche. Camina el día de hoy."
        "Triste|Soledad" -> "No te aísles por completo. Una conversación puede abrir un poco de luz."
        "Triste|Pérdida" -> "El dolor necesita tiempo. No tienes que apresurarte a sentirte bien."
        "Triste|Familia" -> "Puedes buscar paz sin negar lo que te ha dolido."
        "Triste|Desilusión" -> "Una decepción no define toda tu historia."
        "Triste|No sé por qué" -> "Aunque no sepas explicarlo, lo que sientes sigue siendo real."
        else -> v14MoodCloud(mood)
    }
}

private fun v14ScriptureFor(mood: String, situation: String): V14Scripture {
    val key = "$mood|$situation"
    return when (key) {
        "En paz|Cuidar mi paz" -> V14Scripture(
            "Colosenses 3:15",
            "La paz de Cristo puede dirigir la manera en que reaccionas y tomas decisiones.",
            "La paz no es solo ausencia de problemas; también puede ser una guía interior para responder con calma, gratitud y dominio propio.",
            "Señor, gracias por la paz que hoy puedo reconocer. Ayúdame a cuidarla y a no perderla por cosas que no merecen gobernar mi corazón. Amén."
        )
        "En paz|Familia" -> V14Scripture(
            "Romanos 12:18",
            "Busca vivir en paz con los demás en todo lo que dependa de ti.",
            "No puedes controlar las reacciones de tu familia, pero sí puedes elegir tus palabras, tu tono y la disposición con la que te acercas.",
            "Dios, dame paciencia para amar a mi familia con sabiduría y para construir paz desde mis propias decisiones. Amén."
        )
        "En paz|Trabajo" -> V14Scripture(
            "Colosenses 3:23",
            "Tu trabajo puede hacerse con entrega y propósito, incluso en las tareas pequeñas.",
            "La paz también se expresa trabajando sin desesperación, haciendo lo que corresponde con excelencia y sin medir tu valor solo por los resultados.",
            "Señor, ayúdame a trabajar con un corazón tranquilo, responsable y agradecido. Que hoy haga bien lo que está delante de mí. Amén."
        )
        "En paz|Descanso" -> V14Scripture(
            "Mateo 11:28",
            "Jesús invita a acercarse a Él cuando el cansancio y las cargas pesan demasiado.",
            "Descansar no significa abandonar responsabilidades; significa reconocer que tu cuerpo, tu mente y tu alma también necesitan detenerse.",
            "Jesús, enséñame a descansar sin culpa y a dejar contigo las cargas que hoy no necesito seguir llevando. Amén."
        )
        "En paz|Ayudar a alguien" -> V14Scripture(
            "Mateo 5:9",
            "Quien promueve la paz puede convertirse en una influencia de bien para otros.",
            "La calma que hoy tienes puede transformarse en escucha, reconciliación, ánimo o una palabra amable para alguien que está pasando un momento difícil.",
            "Dios, muéstrame a quién puedo llevar paz hoy y dame palabras sencillas que ayuden en lugar de herir. Amén."
        )
        "Agradecido|Familia" -> V14Scripture(
            "Filipenses 1:3",
            "Recordar a las personas con gratitud cambia la forma en que valoramos su presencia.",
            "La familiaridad puede hacer que dejemos de notar lo valioso. Hoy puedes volver a mirar a tu familia como personas por las que vale la pena agradecer.",
            "Señor, gracias por mi familia. Ayúdame a valorar lo bueno, sanar lo necesario y expresar mi gratitud con hechos. Amén."
        )
        "Agradecido|Salud" -> V14Scripture(
            "Salmo 103:2-5",
            "Recordar los beneficios recibidos ayuda a cultivar un corazón agradecido.",
            "Agradecer por la salud no exige ignorar limitaciones; también significa reconocer cada capacidad, cuidado y oportunidad que hoy sí está presente.",
            "Dios, gracias por la vida y por cada fortaleza que hoy tengo. Dame sabiduría para cuidar mi cuerpo y valorar este día. Amén."
        )
        "Agradecido|Trabajo" -> V14Scripture(
            "Colosenses 3:17",
            "Las tareas diarias pueden vivirse con gratitud y una intención que honre a Dios.",
            "El trabajo puede tener días difíciles, pero también puede ser espacio de provisión, aprendizaje, servicio y crecimiento.",
            "Señor, gracias por lo que mi trabajo me permite aprender y aportar. Ayúdame a vivirlo con gratitud y responsabilidad. Amén."
        )
        "Agradecido|Oración respondida" -> V14Scripture(
            "Salmo 66:19-20",
            "Reconocer una oración escuchada fortalece la memoria espiritual y la gratitud.",
            "No olvides el camino recorrido mientras esperabas. La respuesta también puede enseñarte paciencia, confianza y perseverancia.",
            "Gracias, Dios, por escucharme. Ayúdame a no olvidar esta respuesta y a convertir mi gratitud en confianza para el futuro. Amén."
        )
        "Agradecido|Lo cotidiano" -> V14Scripture(
            "Salmo 118:24",
            "El día presente puede recibirse como una oportunidad para agradecer y alegrarse.",
            "La gratitud crece cuando dejamos de esperar solo grandes acontecimientos y aprendemos a reconocer el valor de lo sencillo.",
            "Señor, gracias por este día, por lo pequeño y por lo cotidiano. Abre mis ojos para reconocer tus regalos sencillos. Amén."
        )
        "Necesito dirección|Decisión importante" -> V14Scripture(
            "Proverbios 3:5-6",
            "La dirección comienza confiando en Dios y reconociéndolo en cada camino.",
            "No siempre tendrás toda la información. Una buena decisión puede requerir confianza, consejo sabio y humildad para reconocer que tu perspectiva es limitada.",
            "Dios, dame claridad para esta decisión. Corrige mis motivaciones y guíame hacia lo que sea verdadero, responsable y bueno. Amén."
        )
        "Necesito dirección|Trabajo" -> V14Scripture(
            "Proverbios 16:3",
            "Puedes poner tus proyectos delante de Dios y ordenar tus planes con propósito.",
            "Antes de preguntarte solo qué opción te conviene, considera también qué decisión es responsable, sostenible y coherente con tus valores.",
            "Señor, pongo delante de ti mi trabajo y mis planes. Dame sabiduría para actuar con responsabilidad y sin ansiedad. Amén."
        )
        "Necesito dirección|Familia" -> V14Scripture(
            "Santiago 1:19",
            "Escuchar con atención y responder con calma puede transformar conversaciones difíciles.",
            "En asuntos familiares, la dirección no siempre llega mediante una respuesta rápida. A veces comienza escuchando mejor y reaccionando menos.",
            "Dios, ayúdame a escuchar antes de responder y a buscar una solución que cuide la verdad y también a las personas. Amén."
        )
        "Necesito dirección|Finanzas" -> V14Scripture(
            "Proverbios 21:5",
            "La planificación constante suele producir mejores frutos que actuar con prisa.",
            "La claridad financiera normalmente se construye con decisiones pequeñas: ordenar, priorizar, esperar y evitar impulsos que después pesan.",
            "Señor, dame disciplina y sabiduría para administrar lo que tengo. Ayúdame a no decidir por miedo ni por impulso. Amén."
        )
        "Necesito dirección|Propósito" -> V14Scripture(
            "Efesios 2:10",
            "Tu vida puede orientarse hacia obras buenas y un propósito que se construye caminando.",
            "El propósito no siempre aparece como una revelación inmediata. Muchas veces se descubre sirviendo, aprendiendo y siendo fiel en lo que ya tienes delante.",
            "Dios, muéstrame cómo usar lo que soy y lo que tengo para hacer el bien. Guíame paso a paso en mi propósito. Amén."
        )
        "Preocupado|Trabajo" -> V14Scripture(
            "Mateo 6:34",
            "Jesús invita a no cargar hoy con todas las preocupaciones del mañana.",
            "Pensar en todo lo que podría salir mal agota antes de tiempo. Hoy puedes concentrarte en la responsabilidad que sí tienes delante.",
            "Señor, ayúdame a trabajar con calma y a no vivir hoy los problemas de mañana. Dame claridad para el siguiente paso. Amén."
        )
        "Preocupado|Familia" -> V14Scripture(
            "1 Pedro 5:7",
            "Puedes poner tus preocupaciones delante de Dios porque tu vida le importa.",
            "Amar a tu familia no significa poder controlar todo lo que ocurre. Puedes cuidar, acompañar y también reconocer tus límites.",
            "Dios, te entrego lo que me preocupa de mi familia. Muéstrame qué me corresponde hacer y qué necesito dejar en tus manos. Amén."
        )
        "Preocupado|Salud" -> V14Scripture(
            "Salmo 46:1",
            "Dios puede ser refugio y fortaleza cuando atraviesas momentos de incertidumbre.",
            "La fe no sustituye el cuidado médico, pero puede acompañarte mientras haces lo que está en tus manos y enfrentas lo que todavía no sabes.",
            "Señor, dame fortaleza, paz y sabiduría para cuidar mi salud y buscar la ayuda adecuada cuando sea necesario. Amén."
        )
        "Preocupado|Dinero" -> V14Scripture(
            "Mateo 6:31-33",
            "Jesús invita a no dejar que la preocupación por las necesidades gobierne todo el corazón.",
            "La preocupación financiera es real, pero el miedo puede empeorar las decisiones. Empieza por ordenar lo que sí puedes controlar hoy.",
            "Dios, dame provisión, sabiduría y serenidad. Ayúdame a administrar bien lo que tengo y a tomar decisiones responsables. Amén."
        )
        "Preocupado|Futuro" -> V14Scripture(
            "Salmo 37:5",
            "Puedes entregar tu camino a Dios mientras sigues avanzando con confianza y responsabilidad.",
            "No necesitas conocer cada detalle del futuro para dar un buen paso hoy. La dirección suele aclararse mientras caminas con fidelidad.",
            "Señor, pongo mi futuro delante de ti. Dame paz para lo que no puedo controlar y valentía para lo que sí debo hacer. Amén."
        )
        "Triste|Soledad" -> V14Scripture(
            "Isaías 41:10",
            "La presencia y la ayuda de Dios pueden sostenerte cuando te sientes solo o débil.",
            "Sentirte solo no significa que debas permanecer aislado. Puedes buscar a Dios y también permitir que personas confiables se acerquen a ti.",
            "Dios, acompáñame en esta soledad. Dame fuerzas para abrirme a tu presencia y también para acercarme a alguien de confianza. Amén."
        )
        "Triste|Pérdida" -> V14Scripture(
            "Mateo 5:4",
            "Jesús reconoce el dolor de quienes lloran y anuncia consuelo para ellos.",
            "El duelo no necesita ser apresurado. Llorar una pérdida es parte de amar lo que fue importante y de aprender a caminar con la ausencia.",
            "Señor, recibe mi dolor. Consuélame y dame fuerzas para atravesar esta pérdida sin tener que fingir que estoy bien. Amén."
        )
        "Triste|Familia" -> V14Scripture(
            "Salmo 147:3",
            "Dios se acerca al corazón herido y puede acompañar procesos de restauración.",
            "Las heridas familiares pueden doler profundamente. Sanar puede requerir tiempo, límites saludables, conversación y, en algunos casos, ayuda externa.",
            "Dios, conoce lo que me duele en mi familia. Dame sabiduría para sanar, poner límites cuando sean necesarios y actuar con amor. Amén."
        )
        "Triste|Desilusión" -> V14Scripture(
            "Salmo 42:11",
            "En medio del desánimo, el salmista conversa con su propia alma y vuelve a orientar su esperanza hacia Dios.",
            "Una decepción puede ocupar todo el horizonte por un momento, pero no tiene por qué convertirse en la definición de tu futuro.",
            "Señor, hoy estoy desilusionado. Ayúdame a procesar lo ocurrido y a recuperar esperanza sin negar lo que siento. Amén."
        )
        "Triste|No sé por qué" -> V14Scripture(
            "Salmo 13:1-2,5-6",
            "La Biblia también da espacio a preguntas, cansancio y emociones que no se entienden de inmediato.",
            "No necesitas tener una explicación perfecta para acercarte a Dios. Puedes expresar confusión, tristeza y esperanza en la misma oración.",
            "Dios, no sé explicar bien cómo me siento, pero tú conoces mi corazón. Quédate conmigo y ayúdame a atravesar este día. Amén."
        )
        else -> when (mood) {
            "En paz" -> V14Scripture(
                "Filipenses 4:7",
                "La paz de Dios puede guardar el corazón y la mente aun cuando no todo esté resuelto.",
                "La paz puede ser algo que recibes y también algo que proteges. Cuida lo que alimenta tu calma y evita entregar tu mente a preocupaciones innecesarias.",
                "Dios, gracias por la paz de hoy. Guarda mi mente y ayúdame a vivir este día con calma, gratitud y sabiduría. Amén."
            )
            "Agradecido" -> V14Scripture(
                "1 Tesalonicenses 5:18",
                "La gratitud puede practicarse en toda circunstancia sin negar las dificultades.",
                "Agradecer no significa decir que todo es perfecto. Significa reconocer que incluso dentro de un día normal todavía existen motivos para valorar y recordar.",
                "Señor, abre mis ojos para reconocer lo bueno que hoy está presente y enséñame a expresar mi gratitud con acciones. Amén."
            )
            "Necesito dirección" -> V14Scripture(
                "Santiago 1:5",
                "Cuando falta sabiduría, puedes pedirla a Dios con confianza.",
                "La dirección no siempre llega como una respuesta instantánea. Puede venir a través de la oración, el consejo, la reflexión y decisiones responsables.",
                "Dios, necesito sabiduría. Ayúdame a escuchar, pensar con claridad y avanzar sin prisa ni miedo. Amén."
            )
            "Preocupado" -> V14Scripture(
                "Filipenses 4:6-7",
                "La preocupación puede llevarse a Dios mediante oración, petición y gratitud.",
                "No se trata de negar lo que te preocupa, sino de dejar de cargarlo solo. Ora, identifica lo que sí puedes hacer y permite que la paz acompañe el proceso.",
                "Señor, te entrego lo que hoy pesa en mi mente. Dame paz y claridad para hacer lo que me corresponde sin vivir dominado por el miedo. Amén."
            )
            "Triste" -> V14Scripture(
                "Salmo 34:18",
                "Dios se acerca especialmente a quienes tienen el corazón herido.",
                "La tristeza no te vuelve menos espiritual. Puedes reconocerla, pedir compañía y permitirte avanzar con paciencia mientras vuelves a encontrar fuerzas.",
                "Dios, acércate a mi corazón hoy. Dame consuelo, fuerza y personas con quienes pueda caminar este momento. Amén."
            )
            else -> V14Scripture(
                "Salmo 119:105",
                "La palabra de Dios puede iluminar el siguiente paso aunque no muestre todo el camino de una vez.",
                "No necesitas resolver toda tu vida hoy. Busca claridad para el paso que tienes delante y camina con paciencia.",
                "Dios, ilumina mi camino y ayúdame a vivir este día con sabiduría, fe y amor. Amén."
            )
        }
    }
}

private fun v14GuidanceFor(mood: String, situation: String): V14Guidance {
    val key = "$mood|$situation"
    return when (key) {
        "Preocupado|Dinero" -> V14Guidance(
            "¿Qué provisión, recurso o ayuda sí puedes reconocer hoy?",
            "Revisa un solo gasto o decisión económica y toma un paso concreto y responsable."
        )
        "Preocupado|Salud" -> V14Guidance(
            "¿Qué cuidado, persona o fortaleza agradeces hoy?",
            "Haz una acción concreta de cuidado y busca apoyo profesional si lo necesitas."
        )
        "Triste|Soledad" -> V14Guidance(
            "¿Quién ha estado presente para ti, aunque sea de una manera pequeña?",
            "Escribe o llama hoy a una persona de confianza. No te aísles."
        )
        "Necesito dirección|Decisión importante" -> V14Guidance(
            "¿Qué persona, experiencia o recurso puede ayudarte a decidir mejor?",
            "Anota dos opciones y una consecuencia realista de cada una antes de decidir."
        )
        "Agradecido|Oración respondida" -> V14Guidance(
            "Escribe cómo reconoces la respuesta que recibiste.",
            "Comparte ese testimonio con una persona que necesite esperanza."
        )
        else -> when (mood) {
            "Preocupado" -> V14Guidance(
                "¿Qué apoyo o pequeño bien sigue presente hoy?",
                "Elige una sola acción concreta que sí puedas realizar hoy."
            )
            "Triste" -> V14Guidance(
                "¿Qué pequeño detalle te recuerda que no estás completamente solo?",
                "Acércate hoy a una persona de confianza."
            )
            "Necesito dirección" -> V14Guidance(
                "¿Qué recursos o personas ya tienes para orientarte?",
                "Antes de decidir, detente y busca una segunda perspectiva confiable."
            )
            "Agradecido" -> V14Guidance(
                "¿Qué quieres recordar especialmente de este día?",
                "Expresa hoy tu gratitud a una persona."
            )
            "En paz" -> V14Guidance(
                "¿Qué hizo posible este momento de paz?",
                "Comparte ánimo con alguien que hoy esté preocupado."
            )
            else -> V14Guidance(
                "¿Qué agradeces hoy?",
                "Haz una acción pequeña de amor y servicio."
            )
        }
    }
}

private fun openNtvPassage(context: Context, reference: String) {
    val url = "https://www.biblegateway.com/passage/?search=${Uri.encode(reference)}&version=NTV"
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

private fun resetV14Today(prefs: SharedPreferences) {
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
