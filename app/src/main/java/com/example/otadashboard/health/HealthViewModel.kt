package com.example.otadashboard.health

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DeviceHealth(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val batteryTempC: Float?,
    val availRamMb: Long,
    val totalRamMb: Long,
    val availStorageGb: Double,
    val totalStorageGb: Double,
    val androidVersion: String,
    val securityPatch: String,
    val deviceModel: String,
    val networkType: String
)

class HealthViewModel(context: Context) {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(readHealth())
    val state: StateFlow<DeviceHealth> = _state.asStateFlow()

    fun refresh() {
        _state.value = readHealth()
    }

    private fun readHealth(): DeviceHealth {
        val battery = readBattery()
        val ram = readRam()
        val storage = readStorage()

        return DeviceHealth(
            batteryPercent = battery.percent,
            isCharging = battery.charging,
            batteryTempC = battery.tempC,
            availRamMb = ram.availMb,
            totalRamMb = ram.totalMb,
            availStorageGb = storage.availGb,
            totalStorageGb = storage.totalGb,
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            securityPatch = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Build.VERSION.SECURITY_PATCH
            } else {
                "Bilinmiyor"
            },
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            networkType = readNetworkType()
        )
    }

    private data class BatteryReading(val percent: Int, val charging: Boolean, val tempC: Float?)
    private data class RamReading(val availMb: Long, val totalMb: Long)
    private data class StorageReading(val availGb: Double, val totalGb: Double)

    private fun readBattery(): BatteryReading {
        val intent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else 0
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
        val tempC = if (tempTenths != null && tempTenths != Int.MIN_VALUE) tempTenths / 10f else null
        return BatteryReading(percent, charging, tempC)
    }

    private fun readRam(): RamReading {
        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val mem = ActivityManager.MemoryInfo()
        am.getMemoryInfo(mem)
        return RamReading(
            availMb = mem.availMem / (1024 * 1024),
            totalMb = mem.totalMem / (1024 * 1024)
        )
    }

    private fun readStorage(): StorageReading {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        val total = stat.blockCountLong * stat.blockSizeLong
        val avail = stat.availableBlocksLong * stat.blockSizeLong
        val gb = 1024.0 * 1024.0 * 1024.0
        return StorageReading(avail / gb, total / gb)
    }

    private fun readNetworkType(): String {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return "Bilinmiyor"
        val network = cm.activeNetwork ?: return "Çevrimdışı"
        val caps = cm.getNetworkCapabilities(network) ?: return "Çevrimdışı"
        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Mobil veri"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Bağlı"
        }
    }
}
