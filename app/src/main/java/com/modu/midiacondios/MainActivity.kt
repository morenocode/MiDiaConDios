package com.modu.midiacondios

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
    MaterialTheme(colorScheme = lightColorScheme(primary = Purple, background = SoftBg, surface = Color.White, onPrimary = Color.White, onBackground = Ink, onSurface = Ink)) {
        val nav = rememberNavController()
        val tabs = listOf(
            NavItem("inicio", "Inicio", Icons.Filled.Home),
            NavItem("planes", "Planes", Icons.Outlined.MenuBook),
            NavItem("oraciones", "Oraciones", Icons.Outlined.VolunteerActivism),
            NavItem("iglesia", "Iglesia", Icons.Outlined.Groups),
            NavItem("perfil", "Perfil", Icons.Outlined.AccountCircle)
        )
        Scaffold(containerColor = SoftBg, bottomBar = {
            NavigationBar(containerColor = Color.White) {
                val current = nav.currentBackStackEntryAsState().value?.destination?.route
                tabs.forEach { item ->
                    NavigationBarItem(selected = current == item.route, onClick = {
                        nav.navigate(item.route) {
                            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }, icon = { Icon(item.icon, item.label) }, label = { Text(item.label, fontSize = 11.sp) })
                }
            }
        }) { padding ->
            NavHost(navController = nav, startDestination = "inicio", modifier = Modifier.padding(padding)) {
                composable("inicio") { HomeScreen({ nav.navigate("oraciones") }, { nav.navigate("gratitud") }, { nav.navigate("favoritos") }, { nav.navigate("eventos") }) }
                composable("planes") { PlansScreen() }
                composable("oraciones") { NotesScreen("Mis oraciones", "Escribe una petición de oración") }
                composable("iglesia") { ChurchScreen() }
                composable("perfil") { ProfileScreen() }
                composable("gratitud") { NotesScreen("Gratitud", "¿Por qué agradeces hoy?") }
                composable("favoritos") { SimpleListScreen("Favoritos", listOf("Filipenses 4:13" to "Todo lo puedo en Cristo que me fortalece.", "Salmos 23:1" to "El Señor es mi pastor; nada me faltará.")) }
                composable("eventos") { SimpleListScreen("Eventos", listOf("Culto dominical" to "Domingo · 10:00 a. m.", "Noche de oración" to "Viernes · 7:30 p. m.", "Reunión de jóvenes" to "Sábado · 5:00 p. m.")) }
            }
        }
    }
}

@Composable
private fun HomeScreen(onPrayers: () -> Unit, onGratitude: () -> Unit, onFavorites: () -> Unit, onEvents: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("devotional", Context.MODE_PRIVATE) }
    val today = remember { LocalDate.now() }
    var completed by remember { mutableStateOf(prefs.getString("lastCompleted", "") == today.toString()) }
    var streak by remember { mutableIntStateOf(prefs.getInt("streak", 0)) }
    val date = remember(today) { today.format(DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", Locale("es", "PE"))) }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFFF3EEFF), Color(0xFFFFF4DC), Color(0xFFE7F4FF)))).padding(20.dp)) {
                Text("☀️ ¡Buenos días!", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Ink)
                Text(date, color = Color(0xFF565B73))
                Text("Que tengas un día lleno de la presencia de Dios. 💜", color = Color(0xFF565B73))
            }
        }
        item {
            Column(Modifier.padding(16.dp)) {
                FeatureCard("📖 Versículo del día", Color.White) {
                    Text("“Todo lo puedo en Cristo que me fortalece.”", fontWeight = FontWeight.Bold, fontSize = 21.sp, color = Ink)
                    Text("Filipenses 4:13", color = Color(0xFF5B6074), fontSize = 17.sp)
                }
                Spacer(Modifier.height(12.dp))
                FeatureCard("✨ Reflexión de hoy", Color(0xFFF7F2FF)) {
                    Text("Confía en que Dios está contigo en cada paso que das. Él tiene el control.")
                    Text("2 min de lectura", color = Color.Gray)
                }
                Spacer(Modifier.height(12.dp))
                FeatureCard("🙏 Oración de hoy", Color(0xFFF2FBF4)) {
                    Text("Señor, gracias por este nuevo día. Guíame, protégeme y ayúdame a ser de bendición para otros. Amén.")
                }
                Spacer(Modifier.height(14.dp))
                Button(onClick = {
                    if (!completed) {
                        val previous = prefs.getString("lastCompleted", "")
                        streak = if (previous == today.minusDays(1).toString()) streak + 1 else 1
                        prefs.edit().putInt("streak", streak).putString("lastCompleted", today.toString()).apply()
                        completed = true
                    }
                }, enabled = !completed, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = Purple, disabledContainerColor = Green)) {
                    Icon(Icons.Filled.CheckCircle, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (completed) "DEVOCIONAL COMPLETADO" else "COMPLETAR DEVOCIONAL", fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                FeatureCard("🔥 Racha diaria", Color(0xFFFFF8EB)) { Text("$streak ${if (streak == 1) "día" else "días"}", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Orange) }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Quick("Oraciones", onPrayers, Modifier.weight(1f)); Quick("Gratitud", onGratitude, Modifier.weight(1f)); Quick("Favoritos", onFavorites, Modifier.weight(1f)); Quick("Eventos", onEvents, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(title: String, bg: Color, content: @Composable ColumnScope.() -> Unit) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = bg), elevation = CardDefaults.cardElevation(2.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = Purple, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Spacer(Modifier.height(10.dp)); content()
        }
    }
}

@Composable
private fun Quick(label: String, onClick: () -> Unit, modifier: Modifier) {
    Card(onClick = onClick, modifier = modifier.height(90.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Box(Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) { Text(label, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Ink) }
    }
}

@Composable
private fun NotesScreen(title: String, placeholder: String) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("notes", Context.MODE_PRIVATE) }
    val key = "notes_" + title.lowercase(Locale.getDefault())
    var value by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(prefs.getString(key, "")?.split("\u001F")?.filter { it.isNotBlank() } ?: emptyList()) }
    fun save(updated: List<String>) { notes = updated; prefs.edit().putString(key, updated.joinToString("\u001F")).apply() }
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = value, onValueChange = { value = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text(placeholder) }, minLines = 3, shape = RoundedCornerShape(18.dp))
        Spacer(Modifier.height(10.dp))
        Button(onClick = { if (value.isNotBlank()) { save(listOf(value.trim()) + notes); value = "" } }, modifier = Modifier.fillMaxWidth()) { Text("Guardar") }
        Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(notes) { index, note ->
                Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(note, modifier = Modifier.weight(1f)); IconButton(onClick = { save(notes.filterIndexed { i, _ -> i != index }) }) { Icon(Icons.Outlined.Delete, "Eliminar") }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlansScreen() = SimpleListScreen("Planes de lectura", listOf("7 días de fe" to "Fortalece tu confianza en Dios.", "Salmos en 30 días" to "Un salmo y reflexión cada día.", "21 días de oración" to "Forma un hábito de oración diaria."))

@Composable
private fun ChurchScreen() = SimpleListScreen("Iglesia", listOf("Próxima reunión" to "Domingo · 10:00 a. m.", "Transmisión" to "Aquí enlazaremos YouTube o Facebook Live.", "Anuncios" to "Novedades de la congregación."))

@Composable
private fun ProfileScreen() {
    Column(Modifier.fillMaxSize().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(30.dp)); Icon(Icons.Outlined.AccountCircle, null, modifier = Modifier.size(90.dp), tint = Purple); Text("Mi perfil", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink); Text("Mi Día con Dios · versión 0.1", color = Color.Gray)
    }
}

@Composable
private fun SimpleListScreen(title: String, entries: List<Pair<String, String>>) {
    Column(Modifier.fillMaxSize().padding(18.dp)) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Ink); Spacer(Modifier.height(16.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(entries) { _, entry -> Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp)) { Column(Modifier.padding(17.dp)) { Text(entry.first, fontWeight = FontWeight.Bold, fontSize = 18.sp); Text(entry.second, color = Color(0xFF5F6477)) } } }
        }
    }
}
