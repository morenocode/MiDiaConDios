package com.modu.midiacondios

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(onBack: () -> Unit) {
    var user by remember { mutableStateOf(FirebaseAdminSource.currentUser()) }
    var adminState by remember { mutableStateOf<Boolean?>(if (user == null) false else null) }
    var authRefresh by remember { mutableIntStateOf(0) }

    LaunchedEffect(user?.uid, authRefresh) {
        if (user == null) {
            adminState = false
        } else {
            adminState = null
            FirebaseAdminSource.checkAdmin { adminState = it }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Administración") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "Volver")
                }
            }
        )

        when {
            user == null -> AdminLogin(
                onSignedIn = {
                    user = FirebaseAdminSource.currentUser()
                    authRefresh++
                }
            )
            adminState == null -> Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Verificando permisos de administrador…")
            }
            adminState == false -> NotAuthorized(
                email = user?.email.orEmpty(),
                uid = user?.uid.orEmpty(),
                onRetry = { authRefresh++ },
                onLogout = {
                    FirebaseAdminSource.signOut()
                    user = null
                    adminState = false
                }
            )
            else -> AdminWorkspace(
                email = user?.email.orEmpty(),
                onLogout = {
                    FirebaseAdminSource.signOut()
                    user = null
                    adminState = false
                }
            )
        }
    }
}

@Composable
private fun AdminLogin(onSignedIn: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Panel privado", fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Inicia sesión con la cuenta autorizada. Firebase comprobará tus permisos antes de permitir una publicación.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(22.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Correo del administrador") },
            singleLine = true
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        message?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                loading = true
                message = null
                FirebaseAdminSource.signIn(email, password) { success, error ->
                    loading = false
                    if (success) onSignedIn() else message = error ?: "No se pudo iniciar sesión"
                }
            },
            enabled = email.isNotBlank() && password.length >= 6 && !loading,
            modifier = Modifier.fillMaxWidth().height(54.dp)
        ) {
            if (loading) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.height(20.dp).width(20.dp))
            else Text("Iniciar sesión")
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Por seguridad, esta pantalla no permite crear cuentas nuevas ni convertir una cuenta en administrador.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun NotAuthorized(email: String, uid: String, onRetry: () -> Unit, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())) {
        Text("Cuenta sin autorización", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "La sesión es válida, pero esta cuenta todavía no figura como administradora.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(18.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Cuenta", fontWeight = FontWeight.Bold)
                Text(email.ifBlank { "Sin correo" }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Text("UID que debes autorizar en Firebase", fontWeight = FontWeight.Bold)
                SelectionContainer { Text(uid, color = MaterialTheme.colorScheme.primary) }
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Volver a comprobar permisos") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.Logout, contentDescription = null)
            Spacer(Modifier.width(7.dp))
            Text("Cerrar sesión")
        }
    }
}

@Composable
private fun AdminWorkspace(email: String, onLogout: () -> Unit) {
    var section by remember { mutableIntStateOf(0) }

    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp).verticalScroll(rememberScrollState())) {
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Administrador autorizado", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Text(email, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                }
                IconButton(onClick = onLogout) { Icon(Icons.Outlined.Logout, contentDescription = "Cerrar sesión") }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text("¿Qué quieres publicar?", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            FilterChip(
                selected = section == 0,
                onClick = { section = 0 },
                label = { Text("Devocional") },
                leadingIcon = { Icon(Icons.Outlined.MenuBook, contentDescription = null) }
            )
            FilterChip(
                selected = section == 1,
                onClick = { section = 1 },
                label = { Text("Evento") },
                leadingIcon = { Icon(Icons.Outlined.Event, contentDescription = null) }
            )
            FilterChip(
                selected = section == 2,
                onClick = { section = 2 },
                label = { Text("Lo nuevo") },
                leadingIcon = { Icon(Icons.Outlined.Campaign, contentDescription = null) }
            )
        }
        Spacer(Modifier.height(14.dp))
        when (section) {
            0 -> DevotionalEditor()
            1 -> EventEditor()
            else -> ChurchUpdateEditor()
        }
        Spacer(Modifier.height(30.dp))
    }
}

@Composable
private fun DevotionalEditor() {
    var date by remember { mutableStateOf(LocalDate.now().toString()) }
    var reference by remember { mutableStateOf("") }
    var verse by remember { mutableStateOf("") }
    var reflection by remember { mutableStateOf("") }
    var prayer by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val validDate = runCatching { LocalDate.parse(date.trim()) }.isSuccess
    val valid = validDate && reference.isNotBlank() && verse.isNotBlank() && reflection.isNotBlank() && prayer.isNotBlank()

    AdminEditorCard("Devocional diario", "El ID se guarda con formato AAAA-MM-DD. Publicar la misma fecha actualiza ese devocional.") {
        OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Fecha · AAAA-MM-DD") }, singleLine = true, isError = date.isNotBlank() && !validDate)
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(reference, { reference = it }, Modifier.fillMaxWidth(), label = { Text("Referencia · Ej. Salmo 118:24") }, singleLine = true)
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(verse, { verse = it }, Modifier.fillMaxWidth(), label = { Text("Versículo") }, minLines = 2)
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(reflection, { reflection = it }, Modifier.fillMaxWidth(), label = { Text("Reflexión") }, minLines = 4)
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(prayer, { prayer = it }, Modifier.fillMaxWidth(), label = { Text("Oración") }, minLines = 3)
        message?.let { Spacer(Modifier.height(9.dp)); AdminMessage(it) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                loading = true
                message = null
                FirebaseAdminSource.publishDevotional(date, reference, verse, reflection, prayer) { ok, error ->
                    loading = false
                    message = if (ok) "✓ Devocional publicado correctamente" else "Error: ${error ?: "No se pudo publicar"}"
                }
            },
            enabled = valid && !loading,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (loading) "Publicando…" else "Publicar devocional") }
    }
}

@Composable
private fun EventEditor() {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    AdminEditorCard("Nuevo evento", "Puedes escribir la fecha como deseas que aparezca a los usuarios.") {
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Título") }, singleLine = true)
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(subtitle, { subtitle = it }, Modifier.fillMaxWidth(), label = { Text("Horario o descripción breve") })
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Fecha · Ej. Domingo 30 de agosto") })
        message?.let { Spacer(Modifier.height(9.dp)); AdminMessage(it) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                loading = true
                message = null
                FirebaseAdminSource.publishEvent(title, subtitle, date) { ok, error ->
                    loading = false
                    if (ok) {
                        message = "✓ Evento publicado correctamente"
                        title = ""; subtitle = ""; date = ""
                    } else message = "Error: ${error ?: "No se pudo publicar"}"
                }
            },
            enabled = title.isNotBlank() && !loading,
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (loading) "Publicando…" else "Publicar evento") }
    }
}

@Composable
private fun ChurchUpdateEditor() {
    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            imageUri = uri
            message = null
        }
    }

    AdminEditorCard(
        "Lo nuevo",
        "Sube una fotografía o flyer. Los usuarios podrán verlo en Iglesia, pero no editarlo ni eliminarlo."
    ) {
        OutlinedButton(
            onClick = { picker.launch("image/*") },
            enabled = !loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (imageUri == null) "Elegir fotografía o flyer" else "Cambiar imagen")
        }

        imageUri?.let { uri ->
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = uri,
                    contentDescription = "Vista previa de la publicación",
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Título opcional") },
            singleLine = true
        )
        Spacer(Modifier.height(9.dp))
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Texto breve opcional") },
            minLines = 2
        )

        message?.let { Spacer(Modifier.height(9.dp)); AdminMessage(it) }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val selected = imageUri ?: return@Button
                loading = true
                message = null
                val contentType = context.contentResolver.getType(selected)
                FirebaseAdminSource.publishChurchUpdate(selected, contentType, title, body) { ok, error ->
                    loading = false
                    if (ok) {
                        message = "✓ Publicado en Lo nuevo"
                        imageUri = null
                        title = ""
                        body = ""
                    } else {
                        message = "Error: ${error ?: "No se pudo publicar"}"
                    }
                }
            },
            enabled = imageUri != null && !loading,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            if (loading) Text("Subiendo…") else Text("Publicar en Lo nuevo")
        }
    }
}

@Composable
private fun AdminEditorCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

@Composable
private fun AdminMessage(message: String) {
    val success = message.startsWith("✓")
    Text(
        message,
        color = if (success) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
        fontWeight = FontWeight.SemiBold
    )
}
