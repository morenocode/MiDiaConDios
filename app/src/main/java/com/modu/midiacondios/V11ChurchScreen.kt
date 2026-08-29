package com.modu.midiacondios

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun V11ChurchScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    var personalActivities by remember { mutableStateOf(PersonalChurchStore.loadActivities(context)) }
    var personalAnnouncements by remember { mutableStateOf(PersonalChurchStore.loadAnnouncements(context)) }
    var updates by remember { mutableStateOf<List<ChurchUpdate>>(emptyList()) }
    var updatesState by remember { mutableStateOf<Boolean?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    var showActivityDialog by remember { mutableStateOf(false) }
    var editingActivity by remember { mutableStateOf<PersonalChurchActivity?>(null) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var editingAnnouncement by remember { mutableStateOf<PersonalChurchAnnouncement?>(null) }

    LaunchedEffect(refresh) {
        updatesState = null
        FirebaseCommunitySource.loadUpdates(context) { items, success ->
            updates = items
            updatesState = success
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(contentPadding),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text("Iglesia", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Tu comunidad y tu agenda.", color = scheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        editingActivity = null
                        showActivityDialog = true
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("＋ Actividad") }
                OutlinedButton(
                    onClick = {
                        editingAnnouncement = null
                        showAnnouncementDialog = true
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("＋ Anuncio") }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Mis actividades", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }

        if (personalActivities.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Text("Aún no agregaste actividades.", modifier = Modifier.padding(15.dp), color = scheme.onSurfaceVariant)
                }
            }
        } else {
            items(personalActivities, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
                        Text("📅", fontSize = 21.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold)
                            if (item.detail.isNotBlank()) Text(item.detail, color = scheme.onSurfaceVariant, fontSize = 13.sp)
                            if (item.date.isNotBlank()) Text(item.date, color = scheme.primary, fontSize = 12.sp)
                        }
                        IconButton(onClick = {
                            editingActivity = item
                            showActivityDialog = true
                        }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Editar actividad")
                        }
                        IconButton(onClick = {
                            personalActivities = PersonalChurchStore.deleteActivity(context, item.id)
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Eliminar actividad")
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(5.dp))
            Text("Mis anuncios", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }

        if (personalAnnouncements.isEmpty()) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Text("Aún no agregaste anuncios.", modifier = Modifier.padding(15.dp), color = scheme.onSurfaceVariant)
                }
            }
        } else {
            items(personalAnnouncements, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
                        Text("📣", fontSize = 21.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.title, fontWeight = FontWeight.Bold)
                            Text(item.body, color = scheme.onSurfaceVariant, fontSize = 13.sp)
                        }
                        IconButton(onClick = {
                            editingAnnouncement = item
                            showAnnouncementDialog = true
                        }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Editar anuncio")
                        }
                        IconButton(onClick = {
                            personalAnnouncements = PersonalChurchStore.deleteAnnouncement(context, item.id)
                        }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Eliminar anuncio")
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Lo nuevo", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { refresh++ }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar Lo nuevo")
                }
            }
        }

        if (updates.isEmpty()) {
            item {
                V13DemoChurchFlyer()
                if (updatesState == false) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Vista demo · las publicaciones reales aparecerán aquí.",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = scheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            items(updates, key = { it.id }) { item ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = item.title.ifBlank { "Publicación de la iglesia" },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(4f / 5f)
                                .background(scheme.surfaceVariant),
                            contentScale = ContentScale.Fit
                        )
                        if (item.title.isNotBlank() || item.body.isNotBlank()) {
                            Column(Modifier.padding(14.dp)) {
                                if (item.title.isNotBlank()) {
                                    Text(item.title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                }
                                if (item.body.isNotBlank()) {
                                    if (item.title.isNotBlank()) Spacer(Modifier.height(4.dp))
                                    Text(item.body, color = scheme.onSurfaceVariant, lineHeight = 20.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showActivityDialog) {
        V11ActivityDialog(
            initial = editingActivity,
            onDismiss = { showActivityDialog = false },
            onSave = { title, detail, date ->
                val item = PersonalChurchActivity(
                    id = editingActivity?.id ?: System.currentTimeMillis(),
                    title = title,
                    detail = detail,
                    date = date
                )
                personalActivities = PersonalChurchStore.upsertActivity(context, item)
                showActivityDialog = false
            }
        )
    }

    if (showAnnouncementDialog) {
        V11AnnouncementDialog(
            initial = editingAnnouncement,
            onDismiss = { showAnnouncementDialog = false },
            onSave = { title, body ->
                val item = PersonalChurchAnnouncement(
                    id = editingAnnouncement?.id ?: System.currentTimeMillis(),
                    title = title,
                    body = body
                )
                personalAnnouncements = PersonalChurchStore.upsertAnnouncement(context, item)
                showAnnouncementDialog = false
            }
        )
    }
}

@Composable
private fun V13DemoChurchFlyer() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 5f)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF080B12),
                            Color(0xFF111827),
                            Color(0xFF3A220F),
                            Color(0xFF0B0D12)
                        )
                    )
                )
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "VISTA DEMO",
                        color = Color(0xFFD9B477),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "CELEBREMOS JUNTOS",
                        color = Color(0xFFD9B477),
                        fontSize = 12.sp,
                        letterSpacing = 2.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "ANIVERSARIO",
                        color = Color(0xFFF3D6A6),
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "DE NUESTRA IGLESIA",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 3.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("✦", color = Color(0xFFD9B477), fontSize = 27.sp)
                    Text(
                        "CONCIERTO",
                        color = Color.White,
                        fontSize = 39.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "ALABANZA  •  ADORACIÓN  •  UNIDAD",
                        color = Color(0xFFE9C38A),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Una noche para celebrar la fidelidad de Dios",
                        color = Color.White.copy(alpha = .9f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "SÁBADO  •  6:00 P. M.",
                        color = Color(0xFFF3D6A6),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "VEN CON TU FAMILIA Y AMIGOS",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun V11ActivityDialog(
    initial: PersonalChurchActivity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var detail by remember(initial?.id) { mutableStateOf(initial?.detail.orEmpty()) }
    var date by remember(initial?.id) { mutableStateOf(initial?.date.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva actividad" else "Editar actividad") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Actividad") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("Detalle") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Fecha u hora") },
                    placeholder = { Text("Ej. Sábado · 6:00 p. m.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title.trim(), detail.trim(), date.trim()) },
                enabled = title.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun V11AnnouncementDialog(
    initial: PersonalChurchAnnouncement?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember(initial?.id) { mutableStateOf(initial?.title.orEmpty()) }
    var body by remember(initial?.id) { mutableStateOf(initial?.body.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo anuncio" else "Editar anuncio") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Anuncio") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title.trim(), body.trim()) },
                enabled = title.isNotBlank() && body.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
