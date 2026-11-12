package dev.personal.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.personal.app.data.*
import dev.personal.app.device.DeviceMetadata
import dev.personal.app.security.KeystoreSessionStore
import dev.personal.app.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val settings = getSharedPreferences("server_configuration", MODE_PRIVATE)
        val endpoint = settings.getString("endpoint", "").orEmpty()
        val sessions = KeystoreSessionStore(applicationContext)
        val metadata = DeviceMetadata(applicationContext)
        val model = if (endpoint.isNotBlank()) {
            val repository = RemoteDeviceRepository(createIdentityApi(endpoint), sessions)
            ViewModelProvider(this, object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = FoundationViewModel(repository) as T
            })[FoundationViewModel::class.java]
        } else null
        setContent {
            PersonalTheme {
                if (model == null) ServerSetup { value ->
                    check(settings.edit().putString("endpoint", value).commit())
                    recreate()
                } else FoundationScreen(model, metadata, endpoint) {
                    sessions.clear()
                    check(settings.edit().remove("endpoint").commit())
                    viewModelStore.clear()
                    recreate()
                }
            }
        }
    }
}