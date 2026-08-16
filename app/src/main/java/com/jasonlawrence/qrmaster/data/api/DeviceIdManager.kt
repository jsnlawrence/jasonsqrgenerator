package com.jasonlawrence.qrmaster.data.api

import android.content.Context
import android.provider.Settings
import java.util.UUID

/**
 * Returns a stable device identifier that survives app reinstalls.
 * Uses Android's ANDROID_ID (unique per device + app signing key combo).
 * Falls back to a persisted UUID if ANDROID_ID is unavailable.
 */
object DeviceIdManager {

    private const val PREFS_NAME = "qrmaster_prefs"
    private const val KEY_DEVICE_ID = "device_id"

    private var cachedId: String? = null

    fun getDeviceId(context: Context): String {
        cachedId?.let { return it }

        // Try ANDROID_ID first (survives reinstalls, tied to device + signing key)
        val androidId = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )

        if (!androidId.isNullOrBlank() && androidId != "9774d56d682e549c") {
            // Valid ANDROID_ID (the check filters out a known broken value on some old devices)
            cachedId = androidId
            return androidId
        }

        // Fallback: persisted UUID (for emulators or broken ANDROID_ID devices)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)

        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        }

        cachedId = deviceId
        return deviceId
    }
}
