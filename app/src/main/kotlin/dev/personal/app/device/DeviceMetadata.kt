package dev.personal.app.device

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import dev.personal.contracts.DeviceInput
import java.util.UUID

class DeviceMetadata(private val context: Context) {
    private val preferences = context.getSharedPreferences("installation", Context.MODE_PRIVATE)
    val installationId: String = preferences.getString("id", null) ?: UUID.randomUUID().toString().also {
        check(preferences.edit().putString("id", it).commit())
    }
    fun snapshot(name: String): DeviceInput {
        val battery = context.getSystemService(BatteryManager::class.java).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val network = manager.getNetworkCapabilities(manager.activeNetwork)
        val connectivity = when {
            network == null -> "offline"
            network.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "wifi"
            network.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "cellular"
            network.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ethernet"
            else -> "other"
        }
        return DeviceInput(installationId, name.trim(), Build.MANUFACTURER, Build.MODEL, Build.VERSION.RELEASE,
            battery.takeIf { it in 0..100 }, connectivity)
    }
}