package com.modu.midiacondios

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun V11ChurchScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme

    var personalActivities by remember { mutableStateOf(PersonalChurchStore.loadActivities(context)) }
    var personalAnnouncements by remember { mutableStateOf(PersonalChurchStore.loadAnnouncements(context)) }

    var remoteEvents by remember { mutableStateOf<List<ChurchEvent>>(emptyList()) }
    var remoteAnnouncements by remember { mutableStateOf<List<ChurchAnnouncement>>(emptyList()) }
    var remoteState by remember { mutableStateOf<Boolean?>(null) }
    var refresh by remember { mutableIntStateOf(0) }

    var showActivityDialog by remember { mutableStateOf(false) }
    var editingActivity by remember { mutableStateOf<PersonalChurchActivity?>(null) }
    var showAnnouncementDialog by remember { mutableStateOf(false) }
    var editingAnnouncement by remember { mutableStateOf<PersonalChurchAnnouncement?>(null) }

    LaunchedEffect(refresh) {
        remoteState = null
        FirebaseCommunitySource.load(context) { events, announcements, success ->
            remoteEvents = events
            remoteAnnouncements = announcements
            remoteState = success
        }
    }

    val shownRemoteEvents = if (remoteEvents.isNotEmpty()) remoteEvents else listOf(
        ChurchEvent("Culto dominical", "Domingo · 10:00 a. m."),
        ChurchEvent("Noche de oración", "Viernes · 7:30 p. m."),
        ChurchEvent("Reunión de jóvenes", "Sábado · 5:00 p. m.")
    )

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
                Column(Modifier.weight(1f)) {
                    Text("De tu iglesia", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        when (remoteState) {
                            null -> "Actualizando…"
                            true -> "Contenido publicado"
                            false -> "Contenido disponible"
                        },
                        color = scheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = { refresh++ }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar")
                }
            }
        }

        item {
            Text("Próximas actividades", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        items(shownRemoteEvents) { event ->
            Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Event, contentDescription = null, tint = scheme.primary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(event.title, fontWeight = FontWeight.SemiBold)
                        if (event.subtitle.isNotBlank()) Text(event.subtitle, color = scheme.onSurfaceVariant, fontSize = 13.sp)
                        if (event.date.isNotBlank()) Text(event.date, color = scheme.primary, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Anuncios de la iglesia", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }

        if (remoteAnnouncements.isEmpty()) {
            item {
                Text("Aún no hay anuncios publicados.", color = scheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
            }
        } else {
            items(remoteAnnouncements) { item ->
                Card(colors = CardDefaults.cardColors(containerColor = scheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("📣 ${item.title}", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(3.dp))
                        Text(item.body, color = scheme.onSurfaceVariant, fontSize = 13.sp)
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
