package dev.personal.app.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private data class PermissionSpec(
    val title: String,
    val permission: String,
    val request: List<String>,
    val icon: ImageVector,
    val explanation: String,
)

private val permissionSpecs = listOf(
    PermissionSpec("Camera", Manifest.permission.CAMERA, listOf(Manifest.permission.CAMERA), Icons.Default.PhotoCamera,
        "Allow camera access while the app is in use? Granting access does not take a photo or start video."),
    PermissionSpec("Microphone", Manifest.permission.RECORD_AUDIO, listOf(Manifest.permission.RECORD_AUDIO), Icons.Default.Mic,
        "Allow microphone access while the app is in use? Granting access does not start recording."),
    PermissionSpec("Approximate location", Manifest.permission.ACCESS_COARSE_LOCATION,
        listOf(Manifest.permission.ACCESS_COARSE_LOCATION), Icons.Default.MyLocation,
        "Allow approximate location access while the app is in use? This does not grant background access or start tracking."),
    PermissionSpec("Precise location", Manifest.permission.ACCESS_FINE_LOCATION,
        listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), Icons.Default.GpsFixed,
        "Allow precise location access while the app is in use? You can choose approximate access instead. This does not start tracking."),
    PermissionSpec("Notifications", Manifest.permission.POST_NOTIFICATIONS,
        listOf(Manifest.permission.POST_NOTIFICATIONS), Icons.Default.Notifications,
        "Allow app notifications? This does not grant access to the camera, microphone, location or other apps."),
)

private fun readAccess(context: Context, activity: Activity): Map<String, PermissionAccess> {
    val history = context.getSharedPreferences("permission_requests", Context.MODE_PRIVATE)
    return permissionSpecs.associate { spec ->
        spec.permission to permissionAccess(
            granted = context.checkSelfPermission(spec.permission) == PackageManager.PERMISSION_GRANTED,
            requestedBefore = history.getBoolean(spec.permission, false),
            showRationale = activity.shouldShowRequestPermissionRationale(spec.permission),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun PermissionCenter(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = requireNotNull(LocalActivity.current)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var access by remember(context, activity) { mutableStateOf(readAccess(context, activity)) }
    var requesting by rememberSaveable { mutableStateOf(false) }
    var explanation by remember { mutableStateOf<PermissionSpec?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val editor = context.getSharedPreferences("permission_requests", Context.MODE_PRIVATE).edit()
        result.keys.forEach { editor.putBoolean(it, true) }
        editor.apply()
        requesting = false
        access = readAccess(context, activity)
    }
    DisposableEffect(lifecycle, context, activity) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) access = readAccess(context, activity)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    fun openSettings() {
        error = null
        try {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)))
        } catch (_: RuntimeException) {
            error = "App settings could not be opened. Open this app from Android Settings."
        }
    }
    BackHandler(onBack = onBack)
    Scaffold(topBar = {
        TopAppBar(title = { Text("Permissions", style = MaterialTheme.typography.titleLarge) }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
        }, actions = {
            IconButton(onClick = ::openSettings, enabled = !requesting) { Icon(Icons.Default.Settings, "Open app settings") }
        })
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp).widthIn(max = 720.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            permissionSpecs.forEach { spec ->
                val state = access.getValue(spec.permission)
                val settings = state == PermissionAccess.GRANTED || state == PermissionAccess.SETTINGS
                val action = if (settings) "Open ${spec.title.lowercase()} settings" else "Request ${spec.title.lowercase()} access"
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(spec.icon, null, tint = if (state == PermissionAccess.GRANTED) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(spec.title, style = MaterialTheme.typography.titleMedium)
                        Text(when (state) {
                            PermissionAccess.GRANTED -> "Allowed"
                            PermissionAccess.REQUESTABLE -> "Not requested"
                            PermissionAccess.RATIONALE -> "Denied"
                            PermissionAccess.SETTINGS -> "Not allowed; review Settings"
                        }, style = MaterialTheme.typography.bodyMedium)
                    }
                    TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                        tooltip = { PlainTooltip { Text(action) } }, state = rememberTooltipState()) {
                        IconButton(onClick = {
                            if (settings) openSettings() else explanation = spec
                        }, enabled = !requesting, modifier = Modifier.size(48.dp)) {
                            Icon(if (settings) Icons.Default.Settings else Icons.Default.LockOpen, action)
                        }
                    }
                }
                HorizontalDivider()
            }
        }
    }
    explanation?.let { spec ->
        AlertDialog(onDismissRequest = { explanation = null }, title = { Text(spec.title) },
            text = { Text(spec.explanation) },
            confirmButton = { TextButton(onClick = {
                explanation = null
                error = null
                requesting = true
                try {
                    launcher.launch(spec.request.toTypedArray())
                } catch (_: RuntimeException) {
                    requesting = false
                    error = "The permission request could not be opened."
                    access = readAccess(context, activity)
                }
            }) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { explanation = null }) { Text("Not now") } })
    }
}