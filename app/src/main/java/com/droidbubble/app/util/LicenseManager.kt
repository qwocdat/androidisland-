package com.droidbubble.app.util

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest
import java.util.Locale

object LicenseManager {
    private const val PREFS_NAME = "digitalxiao_license_prefs"
    private const val KEY_IS_ACTIVATED = "is_license_activated"
    private const val KEY_SAVED_LICENSE = "saved_license_key"
    private const val SECRET_SALT = "DIGITALXIAO_ANDROIDISLAND_SECRET_2026"

    // Danh sách SHA-256 của các Master Key cố định:
    // - DIGITALXIAO-VIP
    // - DIGITALXIAO-2026
    // - ANDROIDISLAND-VIP
    // - DIGITALVOLUME-VIP
    private val MASTER_KEY_HASHES = setOf(
        "ad355610e8d142d7f9d0f35de17ab31f8ae44c978374d4aed69cc3cf6a58cf4f", // DIGITALXIAO-VIP
        "57abc24acc5b0e190ee193affc8c5f3a64c1a15a14049dbbb9e300f7604fdacf", // DIGITALXIAO-2026
        "7aa7f46f75e3fbf81b05e82daa889064efc03b1ffd458c194735380d2854cc45", // ANDROIDISLAND-VIP
        "872689f97ed9c92c1395f8cf238270fa14bdc525c533f04ff707ea96cf809040"  // DIGITALVOLUME-VIP
    )

    fun isActivated(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val activated = prefs.getBoolean(KEY_IS_ACTIVATED, false)
        val savedKey = prefs.getString(KEY_SAVED_LICENSE, null)
        if (!activated || savedKey.isNullOrBlank()) return false
        return isValidKey(context, savedKey)
    }

    fun getSavedKey(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_SAVED_LICENSE, null)
    }

    fun getDeviceId(context: Context): String {
        val rawId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull() ?: "UNKNOWN"
        return rawId.uppercase(Locale.ROOT).take(8).padEnd(8, '0')
    }

    fun generateDeviceKey(deviceId: String): String {
        val cleanId = deviceId.trim().uppercase(Locale.ROOT)
        val digest = hashSha256("$cleanId:$SECRET_SALT").uppercase(Locale.ROOT)
        val part1 = digest.substring(0, 4)
        val part2 = digest.substring(4, 8)
        return "DX-$part1-$part2"
    }

    fun isValidKey(context: Context, inputKey: String): Boolean {
        val cleanKey = inputKey.trim().uppercase(Locale.ROOT)
        if (cleanKey.isBlank()) return false

        // 1. Kiểm tra Master Key qua SHA-256
        val inputHash = hashSha256(cleanKey)
        if (MASTER_KEY_HASHES.contains(inputHash)) {
            return true
        }

        // 2. Kiểm tra Key theo Device ID của máy (DX-XXXX-YYYY)
        val deviceId = getDeviceId(context)
        val expectedDeviceKey = generateDeviceKey(deviceId)
        if (cleanKey == expectedDeviceKey) {
            return true
        }

        // 3. Kiểm tra Key thuật toán chữ ký tổng quát DX-<4 ký tự>-<4 ký tự checksum>
        val parts = cleanKey.split("-")
        if (parts.size == 3 && (parts[0] == "DX" || parts[0] == "DV" || parts[0] == "AI") && parts[1].length == 4 && parts[2].length == 4) {
            val seed = parts[1]
            val expectedChecksum = hashSha256("UNIVERSAL:$seed:$SECRET_SALT").uppercase(Locale.ROOT).substring(0, 4)
            if (parts[2] == expectedChecksum) {
                return true
            }
        }

        return false
    }

    fun verifyAndActivate(context: Context, inputKey: String): Boolean {
        val cleanKey = inputKey.trim().uppercase(Locale.ROOT)
        if (isValidKey(context, cleanKey)) {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .putBoolean(KEY_IS_ACTIVATED, true)
                .putString(KEY_SAVED_LICENSE, cleanKey)
                .apply()
            return true
        }
        return false
    }

    fun deactivate(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .clear()
            .apply()
    }

    private fun hashSha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
