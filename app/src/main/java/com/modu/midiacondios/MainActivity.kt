package com.modu.midiacondios

import android.Manifest
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Purple = Color(0xFF6D4AE8)
private val Ink = Color(0xFF1E2550)
private val SoftBg = Color(0xFFF8F7FC)
private val Green = Color(0xFF42A85A)
private val Orange = Color(0xFFFF9200)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MiDiaConDiosApp() }
    }
}

data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun MiDiaConDiosApp() {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Purple,
            background = SoftBg,
            surface = Color.White,
            onPrimary = Color.White,
            onBackground = Ink,
            onSurface = Ink
        )
    ) {
        val nav = rememberNavController()
        val tabs = listOf(
            NavItem("inicio", "Inicio", Icons.Filled.Home),
            NavItem("planes", "Planes", Icons.Outlined.MenuBook),
            NavItem("oraciones", "Oraciones", Icons.Outlined.VolunteerActivism),
            NavItem("iglesia", "Iglesia", Icons.Outlined.Groups),
            NavItem("perfil", "Perfil", Icons.Outlined.AccountCircle)
        )

        Scaffold(
            containerColor = SoftBg,
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
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
                        onEvents = { nav.navigate("eventos") }
                    )
                }
                composable("planes") { PlansScreen() }
                composable("oraciones") { NotesScreen("Mis oraciones", "Escribe una petición de oración") }
                composable("iglesia") { ChurchScreen() }
                composable("perfil") { ProfileScreen() }
                composable("gratitud") { NotesScreen("Gratitud", "¿Por qué agradeces hoy?") }
                composable("favoritos") { FavoritesScreen() }
                composable("eventos") {
                    SimpleListScreen(
                        "Eventos",
                        listOf(
                            "Culto dominical" to "Domingo · 10:00 a. m.",
                            "Noche de oración" to "Viernes · 7:30 p. m.",
                            "Reunión de jóvenes" to "Sábado · 5:00 p. m."
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    onPrayers: () -> Unit,
    onGratitude: () -> Unit,
    onFavorites: () -> Unit,
    onEvents: () -> Unit
) {
    val context = LocalContext.current
    val today = remember { LocalDate.now() }
    val devotional = remember(today) { DevotionalRepository.forDate(today) }
    val prefs = remember { context.getSharedPreferences("devotional", Context.MODE_PRIVATE) }
    val favoritePrefs = remember { context.getSharedPreferences("favorites", Context.MODE_PRIVATE) }
    val todayKey = today.toString()
    val yesterdayKey = today.minusDays(1).toString()
    val savedLastCompleted = remember { prefs.getString("lastCompleted", "") ?: "" }
    val savedStreak = remember { prefs.getInt("streak", 0) }

    var completed by remember { mutableStateOf(savedLastCompleted == todayKey) }
    var streak by remember {
        mutableIntStateOf(
            if (savedLastCompleted == todayKey || savedLastCompleted == yesterdayKey) savedStreak else 0
        )
    }
    var bestStreak by remember { mutableIntStateOf(prefs.getInt("bestStreak", savedStreak)) }
    var favorite by remember(devotional.reference) {
        mutableStateOf(favoritePrefs.getStringSet("refs", emptySet())?.contains(devotional.reference) == true)
    }

    val date = remember(today) {
        today.format(DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", Locale("es", "PE")))
    }

    fun toggleFavorite() {
        val refs = favoritePrefs.getStringSet("refs", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (favorite) refs.remove(devotional.reference) else refs.add(devotional.reference)
        favoritePrefs.edit().putStringSet("refs", HashSet(refs)).apply()
        favorite = !favorite
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFFF3EEFF),
                                Color(0xFFFFF4DC),
                                Color(0xFFE7F4FF)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Text("☀️ ¡Buenos días!", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(date, color = Color(0xFF565B73))
                Spacer(Modifier.height(3.dp))
                Text("Que tengas un día lleno de la presencia de Dios. 💜", color = Color(0xFF565B73))
            }
        }

        item {
            Column(Modifier.padding(16.dp)) {
                FeatureCard("📖 Palabra de hoy", Color.White) {
                    Text(
                        "“${devotional.verse}”",
                        fontWeight = FontWeight.Bold,
                        fontSize = 21.sp,
                        color = Ink
                    )
                    Text(devotional.reference, color = Color(0xFF5B6074), fontSize = 17.sp)
                    Spacer(Modifier.height(6.dp))
                    Text("El contenido cambia cada día", color = Color.Gray, fontSize = 12.sp)
                }

                Spacer(Modifier.height(12.dp))

                FeatureCard("✨ Reflexión de hoy", Color(0xFFF7F2FF)) {
                    Text(devotional.reflection, lineHeight = 22.sp)
                    Spacer(Modifier.height(7.dp))
                    Text("Lectura breve · 2 min", color = Color.Gray)
                }

                Spacer(Modifier.height(12.dp))

                FeatureCard("🙏 Oración de hoy", Color(0xFFF2FBF4)) {
                    Text(devotional.prayer, lineHeight = 22.sp)
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { toggleFavorite() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (favorite) "Guardado en favoritos" else "Guardar en favoritos")
                }

                Spacer(Modifier.height(10.dp))

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
                        }
                    },
                    enabled = !completed,
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple,
                        disabledContainerColor = Green
                    )
                ) {
                    Icon(Icons.Filled.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (completed) "DEVOCIONAL COMPLETADO" else "COMPLETAR DEVOCIONAL",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(12.dp))

                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8EB)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("🔥 Racha actual", color = Orange, fontWeight = FontWeight.Bold)
                            Text(
                                "$streak ${if (streak == 1) "día" else "días"}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Mejor racha", color = Color.Gray, fontSize = 12.sp)
                            Text("$bestStreak días", fontWeight = FontWeight.Bold, color = Orange)
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Quick("🙏\nOraciones", onPrayers, Modifier.weight(1f))
                    Quick("❤️\nGratitud", onGratitude, Modifier.weight(1f))
                    Quick("⭐\nFavoritos", onFavorites, Modifier.weight(1f))
                    Quick("📅\nEventos", onEvents, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    bg: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = bg),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = Purple, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun Quick(label: String, onClick: () -> Unit, modifier: Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.height(94.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(
            Modifier.fillMaxSize().padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = Ink
            )
        }
    }
}

@Composable
private fun NotesScreen(title: String, placeholder: String) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("notes", Context.MODE_PRIVATE) }
    val key = "notes_" + title.lowercase(Locale.getDefault())
    var value by remember { mutableStateOf("") }
    var notes by remember {
        mutableStateOf(
            prefs.getString(key, "")
                ?.split("\u001F")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        )
    }

    fun save(updated: List<String>) {
        notes = updated
        prefs.edit().putString(key, updated.joinToString("\u001F")).apply()
    }

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            if (title == "Gratitud") "Guarda pequeños motivos para agradecer cada día."
            else "Tus notas se guardan solamente en este dispositivo.",
            color = Color.Gray
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(placeholder) },
            minLines = 3,
            shape = RoundedCornerShape(18.dp)
        )

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = {
                if (value.isNotBlank()) {
                    save(listOf(value.trim()) + notes)
                    value = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Guardar")
        }

        Spacer(Modifier.height(16.dp))

        if (notes.isEmpty()) {
            EmptyState(
                icon = if (title == "Gratitud") "❤️" else "🙏",
                title = "Aún no hay registros",
                subtitle = "Escribe el primero cuando quieras."
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(notes) { index, note ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(
                            Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(note, modifier = Modifier.weight(1f))
                            IconButton(onClick = { save(notes.filterIndexed { i, _ -> i != index }) }) {
                                Icon(Icons.Outlined.Delete, "Eliminar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FavoritesScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("favorites", Context.MODE_PRIVATE) }
    var refs by remember {
        mutableStateOf(prefs.getStringSet("refs", emptySet())?.toSet() ?: emptySet())
    }
    val favorites = DevotionalRepository.all().filter { it.reference in refs }

    fun remove(reference: String) {
        val updated = refs.toMutableSet().apply { remove(reference) }
        refs = updated
        prefs.edit().putStringSet("refs", HashSet(updated)).apply()
    }

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text("Favoritos", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Text("Tus palabras y reflexiones guardadas.", color = Color.Gray)
        Spacer(Modifier.height(16.dp))

        if (favorites.isEmpty()) {
            EmptyState("⭐", "Todavía no guardaste favoritos", "Desde Inicio puedes guardar la palabra del día.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(favorites, key = { it.reference }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(Modifier.padding(17.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    item.reference,
                                    modifier = Modifier.weight(1f),
                                    color = Purple,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = { remove(item.reference) }) {
                                    Icon(Icons.Filled.Favorite, "Quitar de favoritos", tint = Purple)
                                }
                            }
                            Text("“${item.verse}”", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(8.dp))
                            Text(item.reflection, color = Color(0xFF5F6477), fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(icon: String, title: String, subtitle: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 42.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, fontSize = 42.sp)
        Spacer(Modifier.height(10.dp))
        Text(title, fontWeight = FontWeight.Bold, color = Ink, textAlign = TextAlign.Center)
        Text(subtitle, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PlansScreen() = SimpleListScreen(
    "Planes de lectura",
    listOf(
        "7 días de fe" to "Fortalece tu confianza en Dios.",
        "Salmos en 30 días" to "Un salmo y reflexión cada día.",
        "21 días de oración" to "Forma un hábito de oración diaria."
    )
)

@Composable
private fun ChurchScreen() = SimpleListScreen(
    "Iglesia",
    listOf(
        "Próxima reunión" to "Domingo · 10:00 a. m.",
        "Transmisión" to "Aquí enlazaremos YouTube o Facebook Live.",
        "Anuncios" to "Novedades de la congregación."
    )
)

@Composable
private fun ProfileScreen() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    var reminderEnabled by remember { mutableStateOf(prefs.getBoolean("dailyReminder", false)) }

    fun enableReminder() {
        scheduleDailyReminder(context)
        reminderEnabled = true
        prefs.edit().putBoolean("dailyReminder", true).apply()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) enableReminder()
    }

    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(18.dp))
            Icon(
                Icons.Outlined.AccountCircle,
                null,
                modifier = Modifier.size(82.dp),
                tint = Purple
            )
            Text("Mi perfil", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
            Text("Mi Día con Dios · versión 0.2", color = Color.Gray)
        }

        Spacer(Modifier.height(28.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.NotificationsActive, null, tint = Purple)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Recordatorio diario", fontWeight = FontWeight.Bold)
                    Text("Alrededor de las 8:00 a. m.", color = Color.Gray, fontSize = 13.sp)
                }
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) {
                            if (
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED
                            ) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                enableReminder()
                            }
                        } else {
                            cancelDailyReminder(context)
                            reminderEnabled = false
                            prefs.edit().putBoolean("dailyReminder", false).apply()
                        }
                    }
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F2FF)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("🔒 Privacidad", fontWeight = FontWeight.Bold, color = Purple)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Por ahora, tus oraciones, gratitud, racha y favoritos se guardan localmente en tu teléfono.",
                    color = Color(0xFF5F6477)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Text(
            "Contenido devocional original para acompañar la lectura bíblica. En próximas versiones podremos sincronizar contenido y eventos de la iglesia.",
            color = Color.Gray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SimpleListScreen(title: String, entries: List<Pair<String, String>>) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(entries) { _, entry ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(Modifier.padding(17.dp)) {
                        Text(entry.first, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(entry.second, color = Color(0xFF5F6477))
                    }
                }
            }
        }
    }
}
