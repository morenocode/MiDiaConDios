package com.modu.midiacondios

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private val V7Purple = Color(0xFF6750A4)
private val V7Green = Color(0xFF2E7D5B)
private val V7Gold = Color(0xFF9A6500)

class V07Activity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent { MiDiaConDiosV07App() }
    }
}

private data class V7NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun MiDiaConDiosV07App() {
    val context = LocalContext.current
    val settings = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
    val systemDark = isSystemInDarkTheme()
    var darkMode by rememberSaveable { mutableStateOf(settings.getBoolean("darkMode", systemDark)) }

    val light = lightColorScheme(
        primary = V7Purple,
        secondary = V7Green,
        tertiary = V7Gold,
        background = Color(0xFFF9F7FC),
        surface = Color(0xFFFFFBFF),
        surfaceVariant = Color(0xFFF1EDF7),
        primaryContainer = Color(0xFFEADDFF),
        secondaryContainer = Color(0xFFD6F2E4),
        tertiaryContainer = Color(0xFFFFDEA6),
        onBackground = Color(0xFF1D1B20),
        onSurface = Color(0xFF1D1B20)
    )
    val dark = darkColorScheme(
        primary = Color(0xFFD0BCFF),
        secondary = Color(0xFF9FD5BB),
        tertiary = Color(0xFFF5BD67),
        background = Color(0xFF121116),
        surface = Color(0xFF1A191F),
        surfaceVariant = Color(0xFF29262F),
        primaryContainer = Color(0xFF4F378B),
        secondaryContainer = Color(0xFF174F3B),
        tertiaryContainer = Color(0xFF5D4200),
        onBackground = Color(0xFFE7E1E8),
        onSurface = Color(0xFFE7E1E8)
    )

    MaterialTheme(
        colorScheme = if (darkMode) dark else light,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(28.dp)
        )
    ) {
        val nav = rememberNavController()
        val entry by nav.currentBackStackEntryAsState()
        val current = entry?.destination?.route
        val tabs = listOf(
            V7NavItem("inicio", "Inicio", Icons.Filled.Home),
            V7NavItem("planes", "Planes", Icons.Outlined.MenuBook),
            V7NavItem("oraciones", "Oraciones", Icons.Outlined.VolunteerActivism),
            V7NavItem("perfil", "Perfil", Icons.Outlined.AccountCircle)
        )

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (current != "admin") {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 5.dp) {
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
                                icon = { Icon(item.icon, contentDescription = item.label) },
                                label = { Text(item.label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            NavHost(
                navController = nav,
                startDestination = "inicio",
                modifier = Modifier.then(Modifier)
            ) {
                composable("inicio") {
                    V7HomeScreen(
                        contentPadding = padding,
                        onPrayers = { nav.navigate("oraciones") },
                        onGratitude = { nav.navigate("gratitud") },
                        onFavorites = { nav.navigate("favoritos") },
                        onChurch = { nav.navigate("iglesia") }
                    )
                }
                composable("planes") { V7PlansScreen(padding) }
                composable("oraciones") { V7PrayerScreen(padding) }
                composable("perfil") {
                    V7ProfileScreen(
                        contentPadding = padding,
                        darkMode = darkMode,
                        onDarkModeChange = { enabled ->
                            darkMode = enabled
                            settings.edit().putBoolean("darkMode", enabled).apply()
                        },
                        onAdmin = { nav.navigate("admin") }
                    )
                }
                composable("gratitud") { V7GratitudeScreen(padding) }
                composable("favoritos") { V7FavoritesScreen(padding) }
                composable("iglesia") { V7ChurchScreen(padding) }
                composable("admin") { AdminPanelScreen(onBack = { nav.popBackStack() }) }
            }
        }
    }
}
