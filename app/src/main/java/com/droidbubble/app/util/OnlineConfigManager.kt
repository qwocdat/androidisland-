package com.droidbubble.app.util

import android.content.Context
import com.droidbubble.app.data.SettingsRepository
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.IslandSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class OnlineAppConfig(
    val announcementId: String = "v2.5.0_liquid_release",
    val showAnnouncement: Boolean = true,
    val announcementTitle: String = "Thông Báo Video & Cập Nhật Mới",
    val announcementSubtitle: String = "Androidisland Liquid Edition • digitalxiao",
    val announcementBannerTitle: String = "Đã ra mắt Video hướng dẫn & Bản cập nhật lớn",
    val announcementBannerDesc: String = "Xem ngay video trên TikTok để biết cách cài đặt, xác minh UID và cập nhật tính năng trực tuyến mới nhất.",
    val changelogItems: List<String> = listOf(
        "Giao diện Liquid Island 5 Tab mới: Tích hợp thanh điều hướng Liquid Glass Tabbar chuẩn vật lý.",
        "Đảo động đen nguyên bản: Mặc định hiển thị viên thuốc đen tuyền ôm khít camera trước, chống bị thanh trạng thái che khuất.",
        "Ẩn HyperIsland hệ điều hành: Tắt và che phủ đảo gốc của Xiaomi HyperOS, ColorOS, OriginOS, MagicOS.",
        "Xác thực thiết bị qua UID: Cấp phép trực tiếp theo mã UID thiết bị trên hệ thống Cloud và tích chọn Không hiển thị lại.",
        "Cập nhật Tính năng & Fix lỗi Online (OTA): Đẩy tính năng mới, chỉnh sửa kích thước Đảo động và vá lỗi trực tiếp từ Web không cần cài lại APK."
    ),
    val tiktokVideoUrl: String = "https://vt.tiktok.com/ZSpBjvVcP/",
    val tiktokButtonText: String = "Mở Video TikTok & Thả Tim",
    val latestVersion: String = "2.0.0",
    val latestVersionCode: Int = 25,
    val apkDownloadUrl: String = "https://github.com/qwocdat/androidisland-/releases/latest/download/Androidisland.apk",
    val updateNotes: String = "Bản cập nhật Liquid Edition: Xác thực thiết bị trực tiếp qua UID, cập nhật tính năng & fix lỗi trực tuyến (OTA) và ẩn HyperIsland.",
    val forceUpdate: Boolean = false,
    val allowAllDevices: Boolean = false,
    val allowedUids: List<String> = emptyList(),
    val patchVersion: Int = 1,
    val remoteToastMessage: String = "",
    val onlineFeatureSwitches: Map<String, Boolean> = emptyMap(),
    val onlineDimensionOverrides: Map<String, Int> = emptyMap()
)

object OnlineConfigManager {
    private const val PREFS_NAME = "androidisland_online_config_prefs"
    private const val KEY_CACHED_JSON = "cached_online_config_json"
    private const val KEY_CUSTOM_CONFIG_URL = "custom_online_config_url"

    // Cấu hình Firebase chính thức của dự án (ideo-dd376)
    const val FIREBASE_API_KEY = "AIzaSyAVuB8m14zkMPs-JTLAKB0Bp18QBKAMBt4"
    const val FIREBASE_AUTH_DOMAIN = "ideo-dd376.firebaseapp.com"
    const val FIREBASE_DATABASE_URL = "https://ideo-dd376-default-rtdb.asia-southeast1.firebasedatabase.app"
    const val FIREBASE_PROJECT_ID = "ideo-dd376"
    const val FIREBASE_STORAGE_BUCKET = "ideo-dd376.firebasestorage.app"
    const val FIREBASE_MESSAGING_SENDER_ID = "689163185"
    const val FIREBASE_APP_ID = "1:689163185:web:f4e11f000a5afc3a364b90"
    const val FIREBASE_MEASUREMENT_ID = "G-6Z3NDBKPFH"

    private const val RTDB_CONFIG_URL =
        "$FIREBASE_DATABASE_URL/androidisland/app_config.json"

    private const val FIRESTORE_DOC_URL =
        "https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/app_config/androidisland_update?key=$FIREBASE_API_KEY"

    private val DEFAULT_RAW_URLS = listOf(
        "https://getkey.digitalsmod.site/online_update.json",
        "https://getkey.digitalsmod.site/api/app-config?app=androidisland",
        "https://raw.githubusercontent.com/qwocdat/androidisland-/main/online_update.json",
        "https://raw.githubusercontent.com/digitalxiao/androidisland/main/online_update.json"
    )

    fun getCachedConfig(context: Context): OnlineAppConfig {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_CACHED_JSON, null) ?: return OnlineAppConfig()
        return parseConfigJson(raw) ?: OnlineAppConfig()
    }

    fun getCustomConfigUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_CUSTOM_CONFIG_URL, "") ?: ""
    }

    fun setCustomConfigUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_CUSTOM_CONFIG_URL, url.trim())
            .apply()
    }

    fun saveLocalOverrideConfig(context: Context, config: OnlineAppConfig) {
        saveCachedConfig(context, config)
    }

    private fun saveCachedConfig(context: Context, config: OnlineAppConfig) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_CACHED_JSON, toConfigJsonString(config))
            .apply()
    }

    fun toJsonString(config: OnlineAppConfig): String = toConfigJsonString(config)

    fun toConfigJsonString(config: OnlineAppConfig): String {
        return JSONObject().apply {
            put("announcementId", config.announcementId)
            put("showAnnouncement", config.showAnnouncement)
            put("announcementTitle", config.announcementTitle)
            put("announcementSubtitle", config.announcementSubtitle)
            put("announcementBannerTitle", config.announcementBannerTitle)
            put("announcementBannerDesc", config.announcementBannerDesc)
            put("changelogItems", JSONArray(config.changelogItems))
            put("tiktokVideoUrl", config.tiktokVideoUrl)
            put("tiktokButtonText", config.tiktokButtonText)
            put("latestVersion", config.latestVersion)
            put("latestVersionCode", config.latestVersionCode)
            put("apkDownloadUrl", config.apkDownloadUrl)
            put("updateNotes", config.updateNotes)
            put("forceUpdate", config.forceUpdate)
            put("allowAllDevices", config.allowAllDevices)
            put("allowedUids", JSONArray(config.allowedUids))
            put("patchVersion", config.patchVersion)
            put("remoteToastMessage", config.remoteToastMessage)
            put("onlineFeatureSwitches", JSONObject().apply {
                config.onlineFeatureSwitches.forEach { (k, v) -> put(k, v) }
            })
            put("onlineDimensionOverrides", JSONObject().apply {
                config.onlineDimensionOverrides.forEach { (k, v) -> put(k, v) }
            })
        }.toString(2)
    }

    fun parseConfigJson(jsonStr: String): OnlineAppConfig? {
        return try {
            val trimmed = jsonStr.trim()
            if (trimmed.isEmpty() || trimmed == "null") return null
            val j = JSONObject(trimmed)
            if (j.has("fields")) {
                val fields = j.getJSONObject("fields")
                val rawPayload = fields.optJSONObject("configJson")?.optString("stringValue", "") ?: ""
                if (rawPayload.isNotBlank()) {
                    return parseConfigJson(rawPayload)
                }
            }
            if (j.has("configJson") && j.opt("configJson") is String) {
                val inner = j.optString("configJson", "")
                if (inner.isNotBlank()) {
                    return parseConfigJson(inner)
                }
            }
            val defaults = OnlineAppConfig()
            val itemsArr = j.optJSONArray("changelogItems")
            val items = if (itemsArr != null && itemsArr.length() > 0) {
                buildList {
                    for (i in 0 until itemsArr.length()) {
                        val s = itemsArr.optString(i, "").trim()
                        if (s.isNotEmpty()) add(s)
                    }
                }
            } else {
                defaults.changelogItems
            }
            val uidsArr = j.optJSONArray("allowedUids")
            val uids = if (uidsArr != null && uidsArr.length() > 0) {
                buildList {
                    for (i in 0 until uidsArr.length()) {
                        val u = uidsArr.optString(i, "").trim()
                        if (u.isNotEmpty()) add(u)
                    }
                }
            } else {
                emptyList()
            }

            val switchesMap = mutableMapOf<String, Boolean>()
            val switchesObj = j.optJSONObject("onlineFeatureSwitches")
            if (switchesObj != null) {
                val keys = switchesObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (!switchesObj.isNull(k)) {
                        switchesMap[k] = switchesObj.optBoolean(k)
                    }
                }
            }

            val dimsMap = mutableMapOf<String, Int>()
            val dimsObj = j.optJSONObject("onlineDimensionOverrides")
            if (dimsObj != null) {
                val keys = dimsObj.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    if (!dimsObj.isNull(k)) {
                        val v = dimsObj.optInt(k, Int.MIN_VALUE)
                        if (v != Int.MIN_VALUE) {
                            dimsMap[k] = v
                        }
                    }
                }
            }

            OnlineAppConfig(
                announcementId = j.optString("announcementId", defaults.announcementId),
                showAnnouncement = j.optBoolean("showAnnouncement", defaults.showAnnouncement),
                announcementTitle = j.optString("announcementTitle", defaults.announcementTitle),
                announcementSubtitle = j.optString("announcementSubtitle", defaults.announcementSubtitle),
                announcementBannerTitle = j.optString("announcementBannerTitle", defaults.announcementBannerTitle),
                announcementBannerDesc = j.optString("announcementBannerDesc", defaults.announcementBannerDesc),
                changelogItems = items,
                tiktokVideoUrl = j.optString("tiktokVideoUrl", defaults.tiktokVideoUrl),
                tiktokButtonText = j.optString("tiktokButtonText", defaults.tiktokButtonText),
                latestVersion = j.optString("latestVersion", defaults.latestVersion),
                latestVersionCode = j.optInt("latestVersionCode", defaults.latestVersionCode),
                apkDownloadUrl = j.optString("apkDownloadUrl", defaults.apkDownloadUrl),
                updateNotes = j.optString("updateNotes", defaults.updateNotes),
                forceUpdate = j.optBoolean("forceUpdate", defaults.forceUpdate),
                allowAllDevices = j.optBoolean("allowAllDevices", defaults.allowAllDevices),
                allowedUids = uids,
                patchVersion = j.optInt("patchVersion", defaults.patchVersion),
                remoteToastMessage = j.optString("remoteToastMessage", ""),
                onlineFeatureSwitches = switchesMap,
                onlineDimensionOverrides = dimsMap
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun fetchOnlineConfig(context: Context): OnlineAppConfig = withContext(Dispatchers.IO) {
        val customUrl = getCustomConfigUrl(context)
        val urlsToTry = buildList {
            if (customUrl.startsWith("http")) add(customUrl)
            add(RTDB_CONFIG_URL)
            add(FIRESTORE_DOC_URL)
            addAll(DEFAULT_RAW_URLS)
        }

        for (endpoint in urlsToTry) {
            try {
                val separator = if (endpoint.contains("?")) "&" else "?"
                val bustedUrl = "${endpoint}${separator}_t=${System.currentTimeMillis()}"
                val conn = (URL(bustedUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Cache-Control", "no-cache")
                }
                if (conn.responseCode in 200..299) {
                    val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                    val parsed = parseConfigJson(body)
                    if (parsed != null) {
                        saveCachedConfig(context, parsed)
                        return@withContext parsed
                    }
                }
            } catch (_: Exception) {
            }
        }

        getCachedConfig(context)
    }

    suspend fun publishConfigToFirebase(config: OnlineAppConfig): Boolean = withContext(Dispatchers.IO) {
        val configJsonStr = toConfigJsonString(config)
        var successAny = false

        // 1. Đồng bộ lên Firebase Realtime Database (ideo-dd376-default-rtdb)
        try {
            val rtdbConn = (URL(RTDB_CONFIG_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            OutputStreamWriter(rtdbConn.outputStream, StandardCharsets.UTF_8).use {
                it.write(configJsonStr)
            }
            if (rtdbConn.responseCode in 200..299) {
                successAny = true
            }
        } catch (_: Exception) {
        }

        // 2. Đồng bộ lên Cloud Firestore (ideo-dd376)
        try {
            val conn = (URL(FIRESTORE_DOC_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val payload = JSONObject().apply {
                put("fields", JSONObject().apply {
                    put("configJson", JSONObject().put("stringValue", configJsonStr))
                    put("updatedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
                    put("latestVersion", JSONObject().put("stringValue", config.latestVersion))
                    put("announcementId", JSONObject().put("stringValue", config.announcementId))
                    put("patchVersion", JSONObject().put("integerValue", config.patchVersion.toString()))
                })
            }
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use {
                it.write(payload.toString())
            }
            if (conn.responseCode in 200..299) {
                successAny = true
            }
        } catch (_: Exception) {
        }

        successAny
    }

    suspend fun publishOnlineConfig(context: Context, config: OnlineAppConfig): Boolean = withContext(Dispatchers.IO) {
        saveCachedConfig(context, config)
        val okFirebase = publishConfigToFirebase(config)
        val configJsonStr = toConfigJsonString(config)

        val customUrl = getCustomConfigUrl(context)
        if (customUrl.startsWith("http")) {
            try {
                val conn = (URL(customUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use {
                    it.write(configJsonStr)
                }
                conn.responseCode
            } catch (_: Exception) {
            }
        }

        okFirebase
    }
}

/**
 * Dynamic Feature & Bug Fix Engine (OTA Live Patch):
 * Tự động đồng bộ các công tắc tính năng và thông số sửa lỗi kích thước Đảo động từ OnlineConfigManager.
 */
object DynamicFeatureEngine {

    private const val PREFS_NAME = "dynamic_features_prefs"
    private const val KEY_LAST_TOAST = "last_shown_remote_toast"
    private const val KEY_LAST_PATCH_SIGNATURE = "last_applied_patch_signature"

    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isEngineRunning = false

    fun start(context: Context) {
        if (isEngineRunning) return
        isEngineRunning = true
        val appCtx = context.applicationContext

        engineScope.launch {
            while (isActive) {
                try {
                    val config = OnlineConfigManager.fetchOnlineConfig(appCtx)
                    applyDynamicFeatures(appCtx, config)
                } catch (_: Throwable) {
                }
                delay(30_000L)
            }
        }
    }

    fun syncNow(context: Context) {
        val appCtx = context.applicationContext
        engineScope.launch {
            try {
                val config = OnlineConfigManager.fetchOnlineConfig(appCtx)
                applyDynamicFeatures(appCtx, config, forceApply = true)
            } catch (_: Throwable) {
            }
        }
    }

    suspend fun applyDynamicFeatures(
        context: Context,
        config: OnlineAppConfig,
        forceApply: Boolean = false
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val patchSignature = buildString {
            append("v=${config.patchVersion};")
            append("sw=${config.onlineFeatureSwitches.entries.sortedBy { it.key }.joinToString(",") { "${it.key}=${it.value}" }};")
            append("dim=${config.onlineDimensionOverrides.entries.sortedBy { it.key }.joinToString(",") { "${it.key}=${it.value}" }}")
        }

        val lastSignature = prefs.getString(KEY_LAST_PATCH_SIGNATURE, "")
        val shouldApplySettings = forceApply || (patchSignature != lastSignature &&
                (config.onlineFeatureSwitches.isNotEmpty() || config.onlineDimensionOverrides.isNotEmpty()))

        if (shouldApplySettings) {
            val repo = SettingsRepository(context)
            val current = try {
                repo.settingsFlow.first()
            } catch (_: Throwable) {
                IslandSettings()
            }

            var updated = current
            var hasChanges = false

            val sw = config.onlineFeatureSwitches
            if (sw.isNotEmpty()) {
                sw["alwaysShowPill"]?.let {
                    if (updated.alwaysShowPill != it) { updated = updated.copy(alwaysShowPill = it); hasChanges = true }
                }
                sw["hideNativeOsIsland"]?.let {
                    if (updated.hideNativeOsIsland != it) { updated = updated.copy(hideNativeOsIsland = it); hasChanges = true }
                }
                sw["bubbleEffectEnabled"]?.let {
                    if (updated.bubbleEffectEnabled != it) { updated = updated.copy(bubbleEffectEnabled = it); hasChanges = true }
                }
                sw["showCalls"]?.let {
                    if (updated.showCalls != it) { updated = updated.copy(showCalls = it); hasChanges = true }
                }
                sw["showMedia"]?.let {
                    if (updated.showMedia != it) { updated = updated.copy(showMedia = it); hasChanges = true }
                }
                sw["showNotifications"]?.let {
                    if (updated.showNotifications != it) { updated = updated.copy(showNotifications = it); hasChanges = true }
                }
                sw["showCharging"]?.let {
                    if (updated.showCharging != it) { updated = updated.copy(showCharging = it); hasChanges = true }
                }
                sw["showFlashlight"]?.let {
                    if (updated.showFlashlight != it) { updated = updated.copy(showFlashlight = it); hasChanges = true }
                }
                sw["showFps"]?.let {
                    if (updated.showFps != it) { updated = updated.copy(showFps = it); hasChanges = true }
                }
                sw["showWeather"]?.let {
                    if (updated.showWeather != it) { updated = updated.copy(showWeather = it); hasChanges = true }
                }
                sw["showNetworkSpeed"]?.let {
                    if (updated.showNetworkSpeed != it) { updated = updated.copy(showNetworkSpeed = it); hasChanges = true }
                }
                sw["showAlarm"]?.let {
                    if (updated.showAlarm != it) { updated = updated.copy(showAlarm = it); hasChanges = true }
                }
                sw["droidbarEnabled"]?.let {
                    if (updated.droidbarEnabled != it) { updated = updated.copy(droidbarEnabled = it); hasChanges = true }
                }
                sw["hapticsEnabled"]?.let {
                    if (updated.hapticsEnabled != it) { updated = updated.copy(hapticsEnabled = it); hasChanges = true }
                }
            }

            val dims = config.onlineDimensionOverrides
            if (dims.isNotEmpty()) {
                dims["widthDp"]?.let {
                    val safe = it.coerceIn(80, 260)
                    if (updated.widthDp != safe) { updated = updated.copy(widthDp = safe); hasChanges = true }
                }
                dims["heightDp"]?.let {
                    val safe = it.coerceIn(24, 64)
                    if (updated.heightDp != safe) { updated = updated.copy(heightDp = safe); hasChanges = true }
                }
                dims["topOffsetDp"]?.let {
                    val safe = it.coerceIn(0, 60)
                    if (updated.topOffsetDp != safe) { updated = updated.copy(topOffsetDp = safe); hasChanges = true }
                }
                dims["horizontalOffsetDp"]?.let {
                    val safe = it.coerceIn(-120, 120)
                    if (updated.horizontalOffsetDp != safe) { updated = updated.copy(horizontalOffsetDp = safe); hasChanges = true }
                }
                dims["cornerRadiusDp"]?.let {
                    val safe = it.coerceIn(10, 50)
                    if (updated.cornerRadiusDp != safe) { updated = updated.copy(cornerRadiusDp = safe); hasChanges = true }
                }
            }

            if (hasChanges) {
                repo.updateSettings { updated }
            }
            prefs.edit().putString(KEY_LAST_PATCH_SIGNATURE, patchSignature).apply()
        }

        if (config.remoteToastMessage.isNotBlank()) {
            val toastKey = "${config.patchVersion}_${config.remoteToastMessage}"
            val lastToast = prefs.getString(KEY_LAST_TOAST, "")
            if (lastToast != toastKey) {
                prefs.edit().putString(KEY_LAST_TOAST, toastKey).apply()
                IslandStateManager.triggerTaskConfirmation("sparkle", config.remoteToastMessage)
            }
        }
    }
}
