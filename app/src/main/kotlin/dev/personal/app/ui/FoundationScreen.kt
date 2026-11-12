package dev.personal.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.personal.app.data.ServerConnector
import dev.personal.app.data.userMessage
import dev.personal.app.device.DeviceMetadata
import dev.personal.contracts.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

@Composable fun PersonalTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) darkColorScheme(primary = Color(0xFF79D4BD), secondary = Color(0xFFE9C170))
    else lightColorScheme(primary = Color(0xFF006B58), secondary = Color(0xFF795900))
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable fun ServerSetup(onSave: (String) -> Unit) {
    var endpoint by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var connecting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val connector = remember { ServerConnector() }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().padding(24.dp).widthIn(max = 640.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Personal Device Lab", style = MaterialTheme.typography.headlineMedium)
            Text("Server", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(endpoint, { endpoint = it }, label = { Text("HTTPS server address") }, singleLine = true,
                enabled = !connecting, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth())
            if (connecting) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                if (!connecting) {
                    connecting = true
                    error = null
                    scope.launch {
                        try {
                            val verified = withContext(Dispatchers.IO) { connector.verify(endpoint) }
                            onSave(verified)
                        } catch (failure: CancellationException) {
                            throw failure
                        } catch (failure: Exception) {
                            error = userMessage(failure)
                        } finally {
                            connecting = false
                        }
                    }
                }
            }, enabled = !connecting && endpoint.isNotBlank()) { Text(if (connecting) "Connecting" else "Connect") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun FoundationScreen(model: FoundationViewModel, metadata: DeviceMetadata, endpoint: String, onChangeServer: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    var confirmRevoke by remember { mutableStateOf<Device?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        TopAppBar(title = { Text("Personal Device Lab", style = MaterialTheme.typography.titleLarge) }, actions = {
            if (state.signedIn) {
                IconButton(onClick = model::refresh, enabled = !state.busy) { Icon(Icons.Default.Refresh, "Refresh devices") }
                IconButton(onClick = model::logout, enabled = !state.busy) { Icon(Icons.Default.ExitToApp, "Sign out") }
            } else IconButton(onClick = onChangeServer) { Icon(Icons.Default.Settings, "Change server") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(endpoint, style = MaterialTheme.typography.bodySmall)
            if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            if (!state.signedIn) SignInForm(state.busy, model::signIn)
            else {
                val current = state.devices.firstOrNull { it.installationId == metadata.installationId }
                Text("This device", style = MaterialTheme.typography.titleLarge)
                SelectionContainer { Text(metadata.installationId, style = MaterialTheme.typography.bodySmall) }
                if (current == null) EnrollmentForm(state.busy) { name, code -> model.enroll(metadata.snapshot(name), code) }
                else {
                    Text(current.name, style = MaterialTheme.typography.titleMedium)
                    Text("${current.manufacturer} ${current.model} | Android ${current.androidVersion}")
                    Button(onClick = { model.synchronize(current.id, metadata.snapshot(current.name)) }, enabled = !state.busy && current.revokedAt == null) {
                        Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Synchronize status")
                    }
                }
                HorizontalDivider()
                Text("Registered devices", style = MaterialTheme.typography.titleLarge)
                if (state.devices.isEmpty()) Text("No registered devices.")
                state.devices.forEach { device -> DeviceRow(device, !state.busy) { confirmRevoke = device } }
                HorizontalDivider()
                PairingForm(state.busy, model::pairing)
                TextButton(onClick = { confirmDelete = true }, enabled = !state.busy,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("Delete account and server data")
                }
            }
        }
    }
    confirmRevoke?.let { device -> AlertDialog(onDismissRequest = { confirmRevoke = null },
        title = { Text("Revoke ${device.name}?") },
        text = { Text("Its device sessions will stop working. This installation cannot register again. Existing metadata remains until account deletion.") },
        confirmButton = { TextButton(onClick = { confirmRevoke = null; model.revoke(device.id) }) { Text("Revoke") } },
        dismissButton = { TextButton(onClick = { confirmRevoke = null }) { Text("Cancel") } }) }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete account?") },
        text = { Text("Permanently delete your account, device records, pairing codes, sessions and activity records from the active database. Backup copies expire under the server retention policy.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; model.deleteAccount() }) { Text("Delete permanently") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })
    state.pairing?.let { pairing -> AlertDialog(onDismissRequest = model::clearPairing,
        title = { Text("One-time pairing code") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SelectionContainer { Text(pairing.code) }
            Text("Expires ${formatTime(pairing.expiresAt)}")
        } }, confirmButton = { TextButton(onClick = model::clearPairing) { Text("Close") } }) }
}

@Composable private fun SignInForm(busy: Boolean, onSubmit: (Credentials, Boolean) -> Unit) {
    var register by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var invite by remember { mutableStateOf("") }
    Text(if (register) "Create account" else "Sign in", style = MaterialTheme.typography.headlineSmall)
    OutlinedTextField(email, { email = it }, label = { Text("Email") }, enabled = !busy, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), modifier = Modifier.fillMaxWidth())
    OutlinedTextField(password, { password = it }, label = { Text("Password") }, enabled = !busy, singleLine = true,
        visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
    if (register) OutlinedTextField(invite, { invite = it }, label = { Text("Registration invitation") }, enabled = !busy, singleLine = true,
        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    Button(onClick = {
        onSubmit(Credentials(email.trim(), password, invite), register)
        password = ""; invite = ""
    }, enabled = !busy && email.isNotBlank() && password.length in (if (register) 12 else 1)..128 && (!register || invite.isNotBlank())) {
        Icon(Icons.Default.Lock, null); Spacer(Modifier.width(8.dp)); Text(if (register) "Create account" else "Sign in")
    }
    TextButton(onClick = { register = !register; password = ""; invite = "" }, enabled = !busy) {
        Text(if (register) "Use an existing account" else "Create an account")
    }
}

@Composable private fun EnrollmentForm(busy: Boolean, onEnroll: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    OutlinedTextField(name, { name = it }, label = { Text("Device name") }, enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(code, { code = it }, label = { Text("Pairing code (optional)") }, enabled = !busy, singleLine = true,
        visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    Button(onClick = { onEnroll(name, code); code = "" }, enabled = !busy && name.trim().length in 1..100) {
        Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Approve device registration")
    }
}

@Composable private fun PairingForm(busy: Boolean, onCreate: (String) -> Unit) {
    var installation by remember { mutableStateOf("") }
    Text("Pair another device", style = MaterialTheme.typography.titleLarge)
    OutlinedTextField(installation, { installation = it }, label = { Text("Other installation ID") },
        enabled = !busy, singleLine = true, modifier = Modifier.fillMaxWidth())
    OutlinedButton(onClick = { onCreate(installation) }, enabled = !busy && validInstallationId(installation.trim())) {
        Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Create pairing code")
    }
}

@Composable private fun DeviceRow(device: Device, enabled: Boolean, onRevoke: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(device.name, style = MaterialTheme.typography.titleMedium)
            Text("${device.manufacturer} ${device.model} | Android ${device.androidVersion}")
            val recent = System.currentTimeMillis() - device.lastSeenAt in 0..300_000
            Text(when { device.revokedAt != null -> "Revoked"; recent -> "Recently seen"; else -> "Not recently seen" })
            Text("Battery: ${device.batteryPercent?.let { "$it%" } ?: "Unavailable"} | ${device.connectivity}")
            Text("Last synchronization: ${formatTime(device.lastSeenAt)}", style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onRevoke, enabled = enabled && device.revokedAt == null) {
                Icon(Icons.Default.Delete, null); Spacer(Modifier.width(8.dp)); Text("Revoke access")
            }
        }
    }
}

private fun formatTime(value: Long): String = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(value))