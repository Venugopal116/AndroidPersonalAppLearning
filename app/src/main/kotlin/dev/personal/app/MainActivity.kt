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
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

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
            val client = OkHttpClient.Builder()
                .followRedirects(false).followSslRedirects(false)
                .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
                .callTimeout(40, TimeUnit.SECONDS).build()
            val api = Retrofit.Builder().baseUrl(EndpointPolicy.normalize(endpoint)).client(client)
                .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
                .build().create(IdentityApi::class.java)
            val repository = RemoteDeviceRepository(api, sessions)
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