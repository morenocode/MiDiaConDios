package com.modu.midiacondios

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Purple = Color(0xFF6650D8)
private val DeepPurple = Color(0xFF3F2B96)
private val Ink = Color(0xFF1D2440)
private val Muted = Color(0xFF697089)
private val SoftBg = Color(0xFFF7F7FB)
private val SoftPurple = Color(0xFFF1EEFF)
private val SoftGreen = Color(0xFFEDF8F0)
private val Green = Color(0xFF2F9B5F)
private val Orange = Color(0xFFE88918)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MiDiaConDiosApp() }
    }
}

data class NavItem(val route: String, val label: String, val icon: ImageVector)

data class ReadingPlan(
    val id: String,
    val title: String,
    val days: Int,
    val description: String,
    val emoji: String
)

@Composable
fun MiDiaConDiosApp() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Purple,
            secondary = Green,
            background = SoftBg,
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = Ink,
            onSurface = Ink
        ),
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(26.dp)
        )
    ) {
        val nav = rememberNavController()
        val tabs = listOf(
            NavItem("inicio", "Inicio", Icons.Filled.Home),
            NavItem("planes", "Planes", Icons.Outlined.MenuBook),
            NavItem("oraciones", "Oraciones", Icons.Outlined.VolunteerActivism),
            NavItem("perfil", "Perfil", Icons.Outlined.AccountCircle)
        )

        Scaffold(
            containerColor = SoftBg,
            bottomBar = {
                NavigationBar(containerColor = Color.White, tonalElevation = 4.dp) {
                    val current = nav.currentBackStackEntryAsState().value?.destination?.route
                    tabs.forEach { item ->
                        NavigationBarItem(
                            selected = current == item.route,
                            onClick = {
                                nav.navigate(item.route) {
                                    popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, item.label) },
                            label = { Text(item.label, fontSize = 11.sp) }
                        )
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = "inicio",
                modifier = Modifier.padding(padding)
            ) {
                composable("inicio") {
                    HomeScreen(
                        onPrayers = { nav.navigate("oraciones") },
                        onGratitude = { nav.navigate("gratitud") },
                        onFavorites = { nav.navigate("favoritos") },
                        onChurch = { nav.navigate("iglesia") }
                    )
                }
                composable("planes") { PlansScreen() }
                composable("oraciones") { PrayerScreen() }
                composable("perfil") { ProfileScreen() }
                composable("gratitud") { GratitudeScreen() }
                composable("favoritos") { FavoritesScreen() }
                composable("iglesia") { ChurchScreen() }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    onPrayers: () -> Unit,
    onGratitude: () -> Unit,
    onFavorites: () -> Unit,
    onChurch: () -> Unit
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val fallback = remember(today) { DevotionalRepository.forDate(today) }
    var devotional by remember(today) { mutableStateOf(fallback) }
    var remoteState by remember { mutableStateOf<Boolean?>(null) }
    var refreshToken by remember { mutableIntStateOf(0) }

    LaunchedEffect(today, refreshToken) {
        remoteState = null
        FirebaseDevotionalSource.loadForDate(context, today) { remote ->
            if (remote != null) {
                devotional = remote
                remoteState = true
            } else {
                devotional = fallback
                remoteState = false
            }
        }
    }

    val prefs = remember { context.getSharedPreferences("devotional", Context.MODE_PRIVATE) }
    val todayKey = today.toString()
    val yesterdayKey = today.minusDays(1).toString()
    val savedLastCompleted = remember { prefs.getString("lastCompleted", "") ?: "" }
    val savedStreak = remember { prefs.getInt("streak", 0) }

    var completed by remember { mutableStateOf(savedLastCompleted == todayKey) }
    var streak by remember {
        mutableIntStateOf(if (savedLastCompleted == todayKey || savedLastCompleted == yesterdayKey) savedStreak else 0)
    }
    var bestStreak by remember { mutableIntStateOf(prefs.getInt("bestStreak", savedStreak)) }
    var favorite by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(devotional.reference) {
        favorite = FavoriteStore.contains(context, devotional.reference)
    }
    LaunchedEffect(feedback) {
        if (feedback != null) {
            kotlinx.coroutines.delay(1800)
            feedback = null
        }
    }

    val date = remember(today) {
        today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "PE")))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("es", "PE")) else it.toString() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFEDE8FF), Color(0xFFFFF3DF), Color(0xFFEAF6FF))
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Mi Día con Dios", fontSize = 13.sp, color = DeepPurple, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text("Un momento de paz para hoy", fontSize = 27.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Spacer(Modifier.height(4.dp))
                            Text(date, color = Muted)
                        }
                        IconButton(
                            onClick = { refreshToken++ },
                            modifier = Modifier.background(Color.White.copy(alpha = .72f), CircleShape)
                        ) {
                            Icon(Icons.Outlined.Refresh, "Actualizar", tint = Purple)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        color = Color.White.copy(alpha = .72f),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.wrapContentWidth()
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            when (remoteState) {
                                null -> {
                                    CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(7.dp))
                                    Text("Actualizando contenido…", fontSize = 12.sp, color = Muted)
                                }
                                true -> {
                                    Icon(Icons.Filled.CloudDone, null, Modifier.size(16.dp), tint = Green)
                                    Spacer(Modifier.width(7.dp))
                                    Text("Devocional actualizado", fontSize = 12.sp, color = Green, fontWeight = FontWeight.SemiBold)
                                }
                                false -> {
                                    Icon(Icons.Outlined.CloudOff, null, Modifier.size(16.dp), tint = Muted)
                                    Spacer(Modifier.width(7.dp))
                                    Text("Disponible sin conexión", fontSize = 12.sp, color = Muted)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                VerseCard(devotional)
                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = {
                        favorite = FavoriteStore.toggle(context, devotional)
                        feedback = if (favorite) "Guardado en favoritos" else "Quitado de favoritos"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (favorite) "Guardado en favoritos" else "Guardar este devocional")
                }

                Spacer(Modifier.height(12.dp))
                ReflectionCard(devotional)
                Spacer(Modifier.height(12.dp))
                PrayerCard(devotional)
                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = {
                        if (!completed) {
                            val previous = prefs.getString("lastCompleted", "") ?: ""
                            val stored = prefs.getInt("streak", 0)
                            val newStreak = if (previous == yesterdayKey) stored + 1 else 1
                            val newBest = maxOf(bestStreak, newStreak)
                            streak = newStreak
                            bestStreak = newBest
                            prefs.edit()
                                .putInt("streak", newStreak)
                                .putInt("bestStreak", newBest)
                                .putString("lastCompleted", todayKey)
                                .apply()
                            completed = true
                            feedback = "¡Devocional completado!"
                        }
                    },
                    enabled = !completed,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Purple, disabledContainerColor = Green)
                ) {
                    Icon(Icons.Filled.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (completed) "COMPLETADO POR HOY" else "COMPLETAR DEVOCIONAL", fontWeight = FontWeight.Bold)
                }

                if (feedback != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(feedback!!, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, color = Green, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(14.dp))
                StreakCard(streak, bestStreak)
                Spacer(Modifier.height(18.dp))

                Text("Continúa tu momento", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text("Accesos rápidos a lo que usas cada día.", color = Muted, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickCard("Oraciones", "🙏", SoftPurple, onPrayers, Modifier.weight(1f))
                    QuickCard("Gratitud", "❤️", Color(0xFFFFF0F3), onGratitude, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    QuickCard("Favoritos", "⭐", Color(0xFFFFF7E8), onFavorites, Modifier.weight(1f))
                    QuickCard("Iglesia", "⛪", Color(0xFFECF7F1), onChurch, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun VerseCard(devotional: Devotional) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = SoftPurple, shape = CircleShape) {
                    Icon(Icons.Outlined.AutoStories, null, Modifier.padding(9.dp).size(20.dp), tint = Purple)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Palabra de hoy", color = Purple, fontWeight = FontWeight.Bold)
                    Text(devotional.reference, color = Muted, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("“${devotional.verse}”", fontSize = 22.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold, color = Ink)
        }
    }
}

@Composable
private fun ReflectionCard(devotional: Devotional) {
    Card(colors = CardDefaults.cardColors(containerColor = SoftPurple), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("✨ Reflexión", color = DeepPurple, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(devotional.reflection, lineHeight = 23.sp, color = Ink)
            Spacer(Modifier.height(8.dp))
            Text("Lectura breve · 2 min", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun PrayerCard(devotional: Devotional) {
    Card(colors = CardDefaults.cardColors(containerColor = SoftGreen), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text("🙏 Oración de hoy", color = Green, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(Modifier.height(8.dp))
            Text(devotional.prayer, lineHeight = 23.sp, color = Ink)
        }
    }
}

@Composable
private fun StreakCard(streak: Int, best: Int) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7E9)), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0xFFFFE5BC), shape = CircleShape) {
                Text("🔥", fontSize = 28.sp, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Racha actual", color = Muted, fontSize = 12.sp)
                Text("$streak ${if (streak == 1) "día" else "días"}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Ink)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Mejor racha", color = Muted, fontSize = 12.sp)
                Text("$best días", color = Orange, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun QuickCard(label: String, emoji: String, bg: Color, onClick: () -> Unit, modifier: Modifier) {
    Card(onClick = onClick, modifier = modifier.height(102.dp), colors = CardDefaults.cardColors(containerColor = bg)) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.Center) {
            Text(emoji, fontSize = 27.sp)
            Spacer(Modifier.height(5.dp))
            Text(label, fontWeight = FontWeight.Bold, color = Ink)
        }
    }
}

@Composable
private fun PrayerScreen() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(EntryStore.load(context, "prayers")) }
    var text by remember { mutableStateOf("") }
    var showAnswered by remember { mutableStateOf(false) }

    fun persist(updated: List<SavedEntry>) {
        entries = updated.sortedByDescending { it.createdAt }
        EntryStore.save(context, "prayers", entries)
    }

    ScreenColumn("Mis oraciones", "Guarda tus peticiones y marca las respuestas que quieras recordar.") {
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
            FilterChip(selected = !showAnswered, onClick = { showAnswered = false }, label = { Text("Pendientes") })
            FilterChip(selected = showAnswered, onClick = { showAnswered = true }, label = { Text("Respondidas") })
        }
        Spacer(Modifier.height(10.dp))

        val visible = entries.filter { it.completed == showAnswered }
        if (visible.isEmpty()) {
            EmptyState(if (showAnswered) "✨" else "🙏", if (showAnswered) "Aún no marcaste respuestas" else "Aún no hay peticiones", "Tus registros aparecerán aquí.")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(visible, key = { it.id }) { entry ->
                    EntryCard(
                        entry = entry,
                        completedLabel = if (entry.completed) "Respondida" else "Marcar respondida",
                        onToggle = { persist(entries.map { if (it.id == entry.id) it.copy(completed = !it.completed) else it }) },
                        onDelete = { persist(entries.filterNot { it.id == entry.id }) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GratitudeScreen() {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(EntryStore.load(context, "gratitude")) }
    var text by remember { mutableStateOf("") }

    fun persist(updated: List<SavedEntry>) {
        entries = updated.sortedByDescending { it.createdAt }
        EntryStore.save(context, "gratitude", entries)
    }

    ScreenColumn("Gratitud", "Anota algo bueno de tu día, incluso si parece pequeño.") {
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
            EmptyState("❤️", "Tu diario está listo", "Tu primer agradecimiento aparecerá aquí.")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(entries, key = { it.id }) { entry ->
                    GratitudeCard(entry) { persist(entries.filterNot { it.id == entry.id }) }
                }
            }
        }
    }
}

@Composable
private fun EntryCard(entry: SavedEntry, completedLabel: String, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Text(entry.text, lineHeight = 21.sp)
            Spacer(Modifier.height(8.dp))
            Text(formatEntryDate(entry.createdAt), color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onToggle) {
                    Icon(if (entry.completed) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(completedLabel)
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Eliminar", tint = Muted) }
            }
        }
    }
}

@Composable
private fun GratitudeCard(entry: SavedEntry, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.Top) {
            Text("❤️", fontSize = 21.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.text, lineHeight = 21.sp)
                Spacer(Modifier.height(5.dp))
                Text(formatEntryDate(entry.createdAt), color = Muted, fontSize = 11.sp)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Eliminar", tint = Muted) }
        }
    }
}

@Composable
private fun FavoritesScreen() {
    val context = LocalContext.current
    var favorites by remember { mutableStateOf(FavoriteStore.all(context)) }

    ScreenColumn("Favoritos", "Aquí quedan guardados completos, incluso los devocionales recibidos desde Internet.") {
        if (favorites.isEmpty()) {
            EmptyState("⭐", "Todavía no tienes favoritos", "Guarda un devocional desde Inicio para verlo aquí.")
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(favorites, key = { it.reference }) { item ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(17.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.reference, modifier = Modifier.weight(1f), color = Purple, fontWeight = FontWeight.Bold)
                                IconButton(onClick = {
                                    FavoriteStore.remove(context, item.reference)
                                    favorites = FavoriteStore.all(context)
                                }) { Icon(Icons.Filled.Favorite, "Quitar", tint = Purple) }
                            }
                            Text("“${item.verse}”", fontWeight = FontWeight.SemiBold, lineHeight = 22.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(item.reflection, color = Muted, fontSize = 13.sp, lineHeight = 19.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlansScreen() {
    val plans = listOf(
        ReadingPlan("fe7", "7 días de fe", 7, "Pequeños pasos para fortalecer tu confianza en Dios.", "🌱"),
        ReadingPlan("salmos30", "Salmos en 30 días", 30, "Un recorrido diario para orar y reflexionar con los Salmos.", "📖"),
        ReadingPlan("oracion21", "21 días de oración", 21, "Construye un hábito sencillo de conversación diaria con Dios.", "🙏")
    )
    val context = LocalContext.current

    ScreenColumn("Planes de lectura", "Avanza a tu ritmo. Tu progreso se guarda en este teléfono.") {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(plans, key = { it.id }) { plan ->
                var current by remember(plan.id) { mutableIntStateOf(PlanProgressStore.get(context, plan.id)) }
                val progress = if (plan.days == 0) 0f else current.toFloat() / plan.days.toFloat()
                Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(17.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(plan.emoji, fontSize = 30.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(plan.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("$current de ${plan.days} días", color = Muted, fontSize = 12.sp)
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(plan.description, color = Muted, lineHeight = 20.sp)
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(7.dp), color = Purple, trackColor = SoftPurple)
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
private fun ChurchScreen() {
    ScreenColumn("Iglesia", "Un espacio para mantener cerca la vida de tu congregación.") {
        SectionCard("📅 Próximas actividades") {
            EventLine("Culto dominical", "Domingo · 10:00 a. m.")
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            EventLine("Noche de oración", "Viernes · 7:30 p. m.")
            HorizontalDivider(Modifier.padding(vertical = 10.dp))
            EventLine("Reunión de jóvenes", "Sábado · 5:00 p. m.")
        }
        Spacer(Modifier.height(12.dp))
        SectionCard("📣 Anuncios") {
            Text("Esta sección ya está preparada visualmente. En la siguiente etapa conectaremos eventos y anuncios a Firebase para administrarlos sin actualizar la APK.", color = Muted, lineHeight = 21.sp)
        }
    }
}

@Composable
private fun ProfileScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var reminderEnabled by remember { mutableStateOf(prefs.getBoolean("dailyReminder", false)) }
    var hour by remember { mutableIntStateOf(prefs.getInt("reminderHour", 8)) }
    var minute by remember { mutableIntStateOf(prefs.getInt("reminderMinute", 0)) }

    fun saveReminder() {
        scheduleDailyReminder(context, hour, minute)
        reminderEnabled = true
        prefs.edit()
            .putBoolean("dailyReminder", true)
            .putInt("reminderHour", hour)
            .putInt("reminderMinute", minute)
            .apply()
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) saveReminder()
    }

    val picker = remember(hour, minute) {
        TimePickerDialog(context, { _, h, m ->
            hour = h
            minute = m
            if (reminderEnabled) saveReminder()
        }, hour, minute, false)
    }

    ScreenColumn("Perfil", "Personaliza tu rutina diaria y revisa cómo se guardan tus datos.") {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = SoftPurple, shape = CircleShape) {
                        Icon(Icons.Outlined.NotificationsActive, null, Modifier.padding(10.dp), tint = Purple)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Recordatorio diario", fontWeight = FontWeight.Bold)
                        Text("A las ${formatTime(hour, minute)}", color = Muted, fontSize = 13.sp)
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
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { picker.show() }, enabled = reminderEnabled) {
                    Icon(Icons.Outlined.Schedule, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Cambiar hora")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        SectionCard("🔒 Tus datos") {
            Text("Tus oraciones, gratitud, favoritos, racha y progreso de planes se guardan localmente en este teléfono. Los devocionales públicos se descargan desde Firebase.", color = Muted, lineHeight = 21.sp)
        }
        Spacer(Modifier.height(12.dp))
        SectionCard("ℹ️ Aplicación") {
            Text("Mi Día con Dios", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Versión 0.5.0 · prueba", color = Muted)
            Spacer(Modifier.height(6.dp))
            Text("Diseñada para crear un hábito diario sencillo de lectura, reflexión, oración y gratitud.", color = Muted, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun ScreenColumn(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Muted, lineHeight = 20.sp)
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(17.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Ink)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun EventLine(title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Event, null, tint = Purple)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EmptyState(icon: String, title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 42.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, color = Muted, textAlign = TextAlign.Center)
    }
}

private fun formatEntryDate(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "PE")))
}

private fun formatTime(hour: Int, minute: Int): String {
    val suffix = if (hour < 12) "a. m." else "p. m."
    val h = when (val value = hour % 12) { 0 -> 12 else -> value }
    return "%d:%02d %s".format(h, minute, suffix)
}
