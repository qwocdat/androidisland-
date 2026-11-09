package com.droidbubble.app.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
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

    private const val FIREBASE_PROJECT_ID = "digitalvolume-keygate"
    private const val FIREBASE_API_KEY = "AIzaSyDigitalXiaoKeyGatePublicConfigKey"
    private const val FIRESTORE_DOC_URL =
        "https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/app_config/androidisland_update?key=$FIREBASE_API_KEY"

    private val DEFAULT_RAW_URLS = listOf(
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
            val j = JSONObject(jsonStr)
            if (j.has("fields")) {
                val fields = j.getJSONObject("fields")
                val rawPayload = fields.optJSONObject("configJson")?.optString("stringValue", "") ?: ""
                if (rawPayload.isNotBlank()) {
                    return parseConfigJson(rawPayload)
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
            conn.responseCode in 200..299
        } catch (_: Exception) {
            false
        }
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
