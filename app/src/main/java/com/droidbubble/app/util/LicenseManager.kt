package com.droidbubble.app.util

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.droidbubble.app.service.IslandAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.Executors
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * UID-based Device Authorization, Real-Time Accessibility Telemetry & Bug Report Manager
 * Integrated with Firebase Project `ideo-dd376` (both Realtime Database & Cloud Firestore):
 *  - apiKey: "AIzaSyAVuB8m14zkMPs-JTLAKB0Bp18QBKAMBt4"
 *  - databaseURL: "https://ideo-dd376-default-rtdb.asia-southeast1.firebasedatabase.app"
 *  - projectId: "ideo-dd376"
 */
object LicenseManager {
    private const val PREF_FILE = "digital_volume_keygate_secure"
    private const val KEY_UID = "user_uid"
    private const val KEY_ACTIVATED_PAYLOAD = "activated_payload"
    private const val KEY_ACTIVATED_HMAC = "activated_hmac"
    private const val KEY_SKIP_ACTIVATION_PROMPT = "skip_activation_prompt_v2"
    private const val KEYSTORE_ALIAS = "DigitalVolumeKeyGateHmacKey"

    const val DEFAULT_API_BASE = "https://getkey.digitalsmod.site/api"
    const val FIREBASE_API_KEY = "AIzaSyAVuB8m14zkMPs-JTLAKB0Bp18QBKAMBt4"
    const val FIREBASE_AUTH_DOMAIN = "ideo-dd376.firebaseapp.com"
    const val FIREBASE_DATABASE_URL = "https://ideo-dd376-default-rtdb.asia-southeast1.firebasedatabase.app"
    const val FIREBASE_PROJECT_ID = "ideo-dd376"
    const val FIREBASE_STORAGE_BUCKET = "ideo-dd376.firebasestorage.app"
    const val FIREBASE_MESSAGING_SENDER_ID = "689163185"
    const val FIREBASE_APP_ID = "1:689163185:web:f4e11f000a5afc3a364b90"
    const val FIREBASE_MEASUREMENT_ID = "G-6Z3NDBKPFH"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    data class VerifyResult(
        val success: Boolean,
        val message: String,
        val uid: String = "",
        val status: String = "pending" // "approved", "pending", "blocked"
    )

    data class DeviceUidRecord(
        val uid: String,
        val deviceName: String,
        val approved: Boolean,
        val status: String,
        val accessibilityActive: Boolean = false,
        val overlayActive: Boolean = false,
        val appVersion: String = "2.0.0",
        val requestedAt: Long = 0L,
        val lastActiveAt: Long = 0L
    )

    data class BugReportRecord(
        val reportId: String,
        val uid: String,
        val deviceName: String,
        val androidVersion: String = Build.VERSION.RELEASE ?: "14",
        val appVersion: String,
        val type: String, // "BUG" or "FEATURE"
        val title: String,
        val description: String,
        val accessibilityActive: Boolean,
        val status: String, // "open" or "resolved"
        val createdAt: Long
    )

    private fun securePrefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    fun isSkipActivationPrompt(context: Context): Boolean {
        return securePrefs(context).getBoolean(KEY_SKIP_ACTIVATION_PROMPT, true)
    }

    fun setSkipActivationPrompt(context: Context, skip: Boolean) {
        securePrefs(context).edit()
            .putBoolean(KEY_SKIP_ACTIVATION_PROMPT, skip)
            .apply()
    }

    fun isActivated(context: Context): Boolean {
        return hasValidLocalSeal(context) && isSkipActivationPrompt(context)
    }

    fun hasValidLocalSeal(context: Context): Boolean {
        val prefs = securePrefs(context)
        val payload = prefs.getString(KEY_ACTIVATED_PAYLOAD, null) ?: return false
        val savedHmac = prefs.getString(KEY_ACTIVATED_HMAC, null) ?: return false
        val expectedHmac = computeKeystoreHmac(context, payload) ?: return false
        if (!MessageDigest.isEqual(savedHmac.toByteArray(), expectedHmac.toByteArray())) {
            clearLicense(context)
            return false
        }
        return try {
            val json = JSONObject(payload)
            val uid = json.optString("uid", "")
            val activated = json.optBoolean("activated", false)
            activated && uid == getUserUid(context)
        } catch (_: Exception) {
            false
        }
    }

    fun getUserUid(context: Context): String {
        val prefs = securePrefs(context)
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
        val cleanAndroidId = androidId.lowercase(Locale.ROOT).trim()
        val deterministicUid = "uid_" + sha256Hex("dxiao_device_$cleanAndroidId").take(16)
        val existing = prefs.getString(KEY_UID, null)
        if (!existing.isNullOrBlank() && existing.startsWith("uid_")) {
            return existing
        }
        prefs.edit().putString(KEY_UID, deterministicUid).apply()
        return deterministicUid
    }

    fun getDeviceId(context: Context): String = getUserUid(context)

    fun getDeviceModelName(): String {
        val manufacturer = Build.MANUFACTURER?.replaceFirstChar { it.uppercase() } ?: "Android"
        val model = Build.MODEL ?: "Device"
        val modelClean = if (model.startsWith(manufacturer, ignoreCase = true)) model else "$manufacturer $model"
        return "$modelClean (Android ${Build.VERSION.RELEASE})"
    }

    fun isAccessibilityEnabledOnDevice(context: Context): Boolean {
        if (IslandAccessibilityService.instance != null) return true
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            enabledServices.contains(context.packageName, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    fun getSavedKey(context: Context): String? {
        return if (hasValidLocalSeal(context)) getUserUid(context) else null
    }

    fun clearLicense(context: Context) {
        securePrefs(context).edit()
            .remove(KEY_ACTIVATED_PAYLOAD)
            .remove(KEY_ACTIVATED_HMAC)
            .apply()
    }

    fun deactivate(context: Context) {
        clearLicense(context)
    }

    fun saveVerifiedSeal(context: Context, uid: String, activatedAt: Long = System.currentTimeMillis()) {
        val payload = JSONObject().apply {
            put("uid", uid)
            put("activated", true)
            put("licenseType", "lifetime")
            put("keyId", "UID_APPROVED")
            put("activatedAt", activatedAt)
        }.toString()
        val hmac = computeKeystoreHmac(context, payload) ?: return
        securePrefs(context).edit()
            .putString(KEY_ACTIVATED_PAYLOAD, payload)
            .putString(KEY_ACTIVATED_HMAC, hmac)
            .apply()
    }

    /**
     * Registers the device on Firebase (Realtime Database + Firestore) once when the activation dialog opens,
     * preserving existing approval status if already approved by Admin.
     */
    fun registerDeviceOnCloud(context: Context) {
        val uid = getUserUid(context)
        val devName = getDeviceModelName()
        val a11yActive = isAccessibilityEnabledOnDevice(context)
        val overlayActive = Settings.canDrawOverlays(context)
        val appVer = UpdateManager.getCurrentVersion(context)
        executor.execute {
            val existingRtdb = fetchDeviceStatusFromRtdb(uid)
            if (existingRtdb != null && (existingRtdb.first || existingRtdb.second == "approved")) {
                return@execute
            }
            registerPendingDeviceOnFirebase(uid, devName, a11yActive, overlayActive, appVer)
        }
    }

    fun syncDeviceTelemetry(context: Context, accessibilityOverride: Boolean? = null, overlayOverride: Boolean? = null) {
        if (!hasValidLocalSeal(context)) return
        val uid = getUserUid(context)
        val devName = getDeviceModelName()
        val a11yActive = accessibilityOverride ?: isAccessibilityEnabledOnDevice(context)
        val overlayActive = overlayOverride ?: Settings.canDrawOverlays(context)
        val appVer = UpdateManager.getCurrentVersion(context)
        executor.execute {
            approveUidOnFirebaseInternal(
                uid = uid,
                deviceName = devName,
                approved = true,
                accessibilityActive = a11yActive,
                overlayActive = overlayActive,
                appVersion = appVer
            )
        }
    }

    private fun fetchDeviceStatusFromRtdb(uid: String): Pair<Boolean, String>? {
        return try {
            val rtdbUrl = URL("$FIREBASE_DATABASE_URL/androidisland/uid_devices/$uid.json?_t=${System.currentTimeMillis()}")
            val conn = (rtdbUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                if (body.isNotEmpty() && body != "null") {
                    val json = JSONObject(body)
                    val approved = json.optBoolean("approved", false)
                    val status = json.optString("status", if (approved) "approved" else "pending")
                    return Pair(approved || status == "approved", status)
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    fun checkAndActivateByUid(context: Context, callback: (VerifyResult) -> Unit) {
        val uid = getUserUid(context)
        val deviceName = getDeviceModelName()
        val a11yActive = isAccessibilityEnabledOnDevice(context)
        val overlayActive = Settings.canDrawOverlays(context)
        val appVer = UpdateManager.getCurrentVersion(context)

        executor.execute {
            try {
                // 1. Check OnlineConfigManager (Firebase RTDB app_config / Firestore / online_update.json)
                val onlineCfg = try {
                    kotlinx.coroutines.runBlocking {
                        val cfg = OnlineConfigManager.fetchOnlineConfig(context)
                        DynamicFeatureEngine.applyDynamicFeatures(context, cfg)
                        cfg
                    }
                } catch (_: Exception) {
                    OnlineConfigManager.getCachedConfig(context)
                }

                if (onlineCfg.allowAllDevices || onlineCfg.allowedUids.any { it.equals(uid, ignoreCase = true) }) {
                    saveVerifiedSeal(context, uid)
                    approveUidOnFirebaseInternal(uid, deviceName, true, a11yActive, overlayActive, appVer)
                    mainHandler.post {
                        callback(VerifyResult(true, "Thiết bị ($uid) đã được cấp phép truy cập!", uid, "approved"))
                    }
                    return@execute
                }

                // 2. Check Firebase Realtime Database (`ideo-dd376-default-rtdb`: `/androidisland/uid_devices/{uid}.json`)
                val rtdbStatus = fetchDeviceStatusFromRtdb(uid)
                if (rtdbStatus != null) {
                    val (approved, status) = rtdbStatus
                    if (approved) {
                        saveVerifiedSeal(context, uid)
                        approveUidOnFirebaseInternal(uid, deviceName, true, a11yActive, overlayActive, appVer)
                        mainHandler.post {
                            callback(VerifyResult(true, "Xác thực UID thành công! Thiết bị đã được cấp phép.", uid, "approved"))
                        }
                        return@execute
                    } else if (status == "blocked") {
                        clearLicense(context)
                        mainHandler.post {
                            callback(VerifyResult(false, "Thiết bị ($uid) đã bị từ chối hoặc khóa quyền truy cập.", uid, "blocked"))
                        }
                        return@execute
                    }
                }

                // 3. Check Cloud Firestore (`ideo-dd376`: `uid_devices/{uid}`)
                val docUrl = URL("https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/uid_devices/$uid?key=$FIREBASE_API_KEY")
                val getConn = (docUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("Accept", "application/json")
                }
                val code = getConn.responseCode
                if (code == 200) {
                    val body = BufferedReader(InputStreamReader(getConn.inputStream)).use { it.readText() }
                    val json = JSONObject(body)
                    val fields = json.optJSONObject("fields")
                    val approved = fields?.optJSONObject("approved")?.optBoolean("booleanValue", false) ?: false
                    val status = fields?.optJSONObject("status")?.optString("stringValue", if (approved) "approved" else "pending") ?: "pending"

                    if (approved || status == "approved") {
                        saveVerifiedSeal(context, uid)
                        approveUidOnFirebaseInternal(uid, deviceName, true, a11yActive, overlayActive, appVer)
                        mainHandler.post {
                            callback(VerifyResult(true, "Xác thực UID thành công! Thiết bị đã được cấp phép.", uid, "approved"))
                        }
                        return@execute
                    } else if (status == "blocked") {
                        clearLicense(context)
                        mainHandler.post {
                            callback(VerifyResult(false, "Thiết bị ($uid) đã bị từ chối hoặc khóa quyền truy cập.", uid, "blocked"))
                        }
                        return@execute
                    }
                }

                // Đảm bảo thiết bị đang chờ duyệt được ghi lên cả RTDB & Firestore
                registerPendingDeviceOnFirebase(uid, deviceName, a11yActive, overlayActive, appVer)

                mainHandler.post {
                    callback(
                        VerifyResult(
                            success = false,
                            message = "UID của bạn chưa được duyệt. Hãy gửi mã UID cho Admin duyệt trên Web rồi bấm lại Xác minh!",
                            uid = uid,
                            status = "pending"
                        )
                    )
                }
            } catch (_: Exception) {
                mainHandler.post {
                    callback(
                        VerifyResult(
                            success = false,
                            message = "Không thể kết nối máy chủ xác thực UID. Vui lòng kiểm tra mạng Internet.",
                            uid = uid,
                            status = "pending"
                        )
                    )
                }
            }
        }
    }

    private fun registerPendingDeviceOnFirebase(
        uid: String,
        deviceName: String,
        accessibilityActive: Boolean = false,
        overlayActive: Boolean = false,
        appVersion: String = "2.0.0"
    ) {
        val nowMs = System.currentTimeMillis()

        // 1. Ghi lên Firebase Realtime Database
        try {
            val rtdbUrl = URL("$FIREBASE_DATABASE_URL/androidisland/uid_devices/$uid.json")
            val conn = (rtdbUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val rtdbPayload = JSONObject().apply {
                put("uid", uid)
                put("deviceName", deviceName)
                put("approved", false)
                put("status", "pending")
                put("accessibilityActive", accessibilityActive)
                put("overlayActive", overlayActive)
                put("appVersion", appVersion)
                put("requestedAt", nowMs)
                put("lastActiveAt", nowMs)
                put("appName", "androidisland")
            }
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(rtdbPayload.toString()) }
            conn.responseCode
        } catch (_: Exception) {
        }

        // 2. Ghi lên Cloud Firestore
        try {
            val docUrl = URL("https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/uid_devices/$uid?key=$FIREBASE_API_KEY")
            val conn = (docUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val payload = JSONObject().apply {
                put("fields", JSONObject().apply {
                    put("uid", JSONObject().put("stringValue", uid))
                    put("deviceName", JSONObject().put("stringValue", deviceName))
                    put("approved", JSONObject().put("booleanValue", false))
                    put("status", JSONObject().put("stringValue", "pending"))
                    put("accessibilityActive", JSONObject().put("booleanValue", accessibilityActive))
                    put("overlayActive", JSONObject().put("booleanValue", overlayActive))
                    put("appVersion", JSONObject().put("stringValue", appVersion))
                    put("requestedAt", JSONObject().put("integerValue", nowMs.toString()))
                    put("lastActiveAt", JSONObject().put("integerValue", nowMs.toString()))
                    put("appName", JSONObject().put("stringValue", "androidisland"))
                })
            }
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(payload.toString()) }
            conn.responseCode
        } catch (_: Exception) {
        }
    }

    private fun approveUidOnFirebaseInternal(
        uid: String,
        deviceName: String,
        approved: Boolean,
        accessibilityActive: Boolean = false,
        overlayActive: Boolean = false,
        appVersion: String = "2.0.0"
    ) {
        val nowMs = System.currentTimeMillis()
        val statusStr = if (approved) "approved" else "blocked"

        // 1. Ghi lên Firebase Realtime Database
        try {
            val rtdbUrl = URL("$FIREBASE_DATABASE_URL/androidisland/uid_devices/$uid.json")
            val conn = (rtdbUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val rtdbPayload = JSONObject().apply {
                put("uid", uid)
                put("deviceName", deviceName)
                put("approved", approved)
                put("status", statusStr)
                put("accessibilityActive", accessibilityActive)
                put("overlayActive", overlayActive)
                put("appVersion", appVersion)
                put("requestedAt", nowMs)
                put("lastActiveAt", nowMs)
                put("appName", "androidisland")
            }
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(rtdbPayload.toString()) }
            conn.responseCode
        } catch (_: Exception) {
        }

        // 2. Ghi lên Cloud Firestore
        try {
            val docUrl = URL("https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/uid_devices/$uid?key=$FIREBASE_API_KEY")
            val conn = (docUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val payload = JSONObject().apply {
                put("fields", JSONObject().apply {
                    put("uid", JSONObject().put("stringValue", uid))
                    put("deviceName", JSONObject().put("stringValue", deviceName))
                    put("approved", JSONObject().put("booleanValue", approved))
                    put("status", JSONObject().put("stringValue", statusStr))
                    put("accessibilityActive", JSONObject().put("booleanValue", accessibilityActive))
                    put("overlayActive", JSONObject().put("booleanValue", overlayActive))
                    put("appVersion", JSONObject().put("stringValue", appVersion))
                    put("requestedAt", JSONObject().put("integerValue", nowMs.toString()))
                    put("lastActiveAt", JSONObject().put("integerValue", nowMs.toString()))
                    put("appName", JSONObject().put("stringValue", "androidisland"))
                })
            }
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(payload.toString()) }
            conn.responseCode
        } catch (_: Exception) {
        }
    }

    suspend fun fetchAllDeviceUidsFromFirestore(): List<DeviceUidRecord> = withContext(Dispatchers.IO) {
        val mapByUid = linkedMapOf<String, DeviceUidRecord>()

        // 1. Đọc từ Firebase Realtime Database (`ideo-dd376-default-rtdb`)
        try {
            val rtdbUrl = URL("$FIREBASE_DATABASE_URL/androidisland/uid_devices.json?_t=${System.currentTimeMillis()}")
            val conn = (rtdbUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                if (body.isNotEmpty() && body != "null") {
                    val json = JSONObject(body)
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val d = json.optJSONObject(key) ?: continue
                        val uid = d.optString("uid", key)
                        val approved = d.optBoolean("approved", false)
                        val status = d.optString("status", if (approved) "approved" else "pending")
                        mapByUid[uid] = DeviceUidRecord(
                            uid = uid,
                            deviceName = d.optString("deviceName", "Thiết bị Android"),
                            approved = approved || status == "approved",
                            status = status,
                            accessibilityActive = d.optBoolean("accessibilityActive", false),
                            overlayActive = d.optBoolean("overlayActive", false),
                            appVersion = d.optString("appVersion", "2.0.0"),
                            requestedAt = d.optLong("requestedAt", 0L),
                            lastActiveAt = d.optLong("lastActiveAt", 0L)
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        // 2. Đọc bổ sung từ Cloud Firestore (`ideo-dd376`)
        try {
            val listUrl = URL("https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/uid_devices?pageSize=200&key=$FIREBASE_API_KEY&_t=${System.currentTimeMillis()}")
            val conn = (listUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == 200) {
                val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val json = JSONObject(body)
                val docs = json.optJSONArray("documents")
                if (docs != null) {
                    for (i in 0 until docs.length()) {
                        val doc = docs.getJSONObject(i)
                        val namePath = doc.optString("name", "")
                        val fallbackUid = namePath.substringAfterLast("/")
                        val fields = doc.optJSONObject("fields") ?: continue
                        val uid = fields.optJSONObject("uid")?.optString("stringValue", fallbackUid) ?: fallbackUid
                        if (mapByUid.containsKey(uid)) continue
                        val devName = fields.optJSONObject("deviceName")?.optString("stringValue", "Thiết bị Android") ?: "Thiết bị Android"
                        val approved = fields.optJSONObject("approved")?.optBoolean("booleanValue", false) ?: false
                        val status = fields.optJSONObject("status")?.optString("stringValue", if (approved) "approved" else "pending") ?: "pending"
                        val a11y = fields.optJSONObject("accessibilityActive")?.optBoolean("booleanValue", false) ?: false
                        val ovl = fields.optJSONObject("overlayActive")?.optBoolean("booleanValue", false) ?: false
                        val ver = fields.optJSONObject("appVersion")?.optString("stringValue", "2.0.0") ?: "2.0.0"
                        val reqAt = fields.optJSONObject("requestedAt")?.optString("integerValue", "0")?.toLongOrNull() ?: 0L
                        val lastAt = fields.optJSONObject("lastActiveAt")?.optString("integerValue", reqAt.toString())?.toLongOrNull() ?: reqAt
                        mapByUid[uid] = DeviceUidRecord(
                            uid = uid,
                            deviceName = devName,
                            approved = approved || status == "approved",
                            status = status,
                            accessibilityActive = a11y,
                            overlayActive = ovl,
                            appVersion = ver,
                            requestedAt = reqAt,
                            lastActiveAt = lastAt
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        mapByUid.values.sortedByDescending { maxOf(it.lastActiveAt, it.requestedAt) }
    }

    suspend fun setDeviceUidApproval(uid: String, deviceName: String, approved: Boolean): Boolean = withContext(Dispatchers.IO) {
        val cleanUid = uid.trim()
        if (cleanUid.isEmpty()) return@withContext false
        approveUidOnFirebaseInternal(cleanUid, deviceName.ifBlank { "Thiết bị được Admin cấp phép" }, approved)
        true
    }

    suspend fun submitBugReportToFirestore(
        context: Context,
        type: String,
        title: String,
        description: String
    ): Boolean = withContext(Dispatchers.IO) {
        val uid = getUserUid(context)
        val devName = getDeviceModelName()
        val androidVer = Build.VERSION.RELEASE ?: "14"
        val appVer = UpdateManager.getCurrentVersion(context)
        val a11y = isAccessibilityEnabledOnDevice(context)
        val nowMs = System.currentTimeMillis()
        val reportId = "rep_${nowMs}_${uid.takeLast(6)}"
        val cleanTitle = title.ifBlank { if (type == "BUG") "Báo cáo lỗi" else "Đề xuất tính năng" }
        var okAny = false

        // 1. Ghi lên Firebase Realtime Database
        try {
            val rtdbUrl = URL("$FIREBASE_DATABASE_URL/androidisland/bug_reports/$reportId.json")
            val conn = (rtdbUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "PUT"
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val rtdbPayload = JSONObject().apply {
                put("reportId", reportId)
                put("uid", uid)
                put("deviceName", devName)
                put("androidVersion", androidVer)
                put("appVersion", appVer)
                put("type", type)
                put("title", cleanTitle)
                put("description", description.trim())
                put("accessibilityActive", a11y)
                put("status", "open")
                put("createdAt", nowMs)
            }
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use {
                it.write(rtdbPayload.toString())
            }
            if (conn.responseCode in 200..299) okAny = true
        } catch (_: Exception) {
        }

        // 2. Ghi lên Cloud Firestore
        try {
            val docUrl = URL("https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/bug_reports/$reportId?key=$FIREBASE_API_KEY")
            val conn = (docUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                doOutput = true
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val payload = JSONObject().apply {
                put("fields", JSONObject().apply {
                    put("reportId", JSONObject().put("stringValue", reportId))
                    put("uid", JSONObject().put("stringValue", uid))
                    put("deviceName", JSONObject().put("stringValue", devName))
                    put("androidVersion", JSONObject().put("stringValue", androidVer))
                    put("appVersion", JSONObject().put("stringValue", appVer))
                    put("type", JSONObject().put("stringValue", type))
                    put("title", JSONObject().put("stringValue", cleanTitle))
                    put("description", JSONObject().put("stringValue", description.trim()))
                    put("accessibilityActive", JSONObject().put("booleanValue", a11y))
                    put("status", JSONObject().put("stringValue", "open"))
                    put("createdAt", JSONObject().put("integerValue", nowMs.toString()))
                })
            }
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use {
                it.write(payload.toString())
            }
            if (conn.responseCode in 200..299) okAny = true
        } catch (_: Exception) {
        }

        okAny
    }

    suspend fun fetchAllBugReportsFromFirestore(): List<BugReportRecord> = withContext(Dispatchers.IO) {
        val mapById = linkedMapOf<String, BugReportRecord>()

        // 1. Đọc từ Firebase Realtime Database
        try {
            val rtdbUrl = URL("$FIREBASE_DATABASE_URL/androidisland/bug_reports.json?_t=${System.currentTimeMillis()}")
            val conn = (rtdbUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode in 200..299) {
                val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }.trim()
                if (body.isNotEmpty() && body != "null") {
                    val json = JSONObject(body)
                    val keys = json.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val r = json.optJSONObject(key) ?: continue
                        val repId = r.optString("reportId", key)
                        mapById[repId] = BugReportRecord(
                            reportId = repId,
                            uid = r.optString("uid", ""),
                            deviceName = r.optString("deviceName", "Thiết bị Android"),
                            androidVersion = r.optString("androidVersion", "14"),
                            appVersion = r.optString("appVersion", "2.0.0"),
                            type = r.optString("type", "BUG"),
                            title = r.optString("title", "Báo lỗi"),
                            description = r.optString("description", ""),
                            accessibilityActive = r.optBoolean("accessibilityActive", false),
                            status = r.optString("status", "open"),
                            createdAt = r.optLong("createdAt", 0L)
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        // 2. Đọc từ Cloud Firestore
        try {
            val listUrl = URL("https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/bug_reports?pageSize=100&key=$FIREBASE_API_KEY&_t=${System.currentTimeMillis()}")
            val conn = (listUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode == 200) {
                val body = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val json = JSONObject(body)
                val docs = json.optJSONArray("documents")
                if (docs != null) {
                    for (i in 0 until docs.length()) {
                        val doc = docs.getJSONObject(i)
                        val namePath = doc.optString("name", "")
                        val fallbackId = namePath.substringAfterLast("/")
                        val fields = doc.optJSONObject("fields") ?: continue
                        val repId = fields.optJSONObject("reportId")?.optString("stringValue", fallbackId) ?: fallbackId
                        if (mapById.containsKey(repId)) continue
                        val uid = fields.optJSONObject("uid")?.optString("stringValue", "") ?: ""
                        val devName = fields.optJSONObject("deviceName")?.optString("stringValue", "Thiết bị Android") ?: "Thiết bị Android"
                        val androidVer = fields.optJSONObject("androidVersion")?.optString("stringValue", "14") ?: "14"
                        val ver = fields.optJSONObject("appVersion")?.optString("stringValue", "2.0.0") ?: "2.0.0"
                        val type = fields.optJSONObject("type")?.optString("stringValue", "BUG") ?: "BUG"
                        val title = fields.optJSONObject("title")?.optString("stringValue", "Báo lỗi") ?: "Báo lỗi"
                        val desc = fields.optJSONObject("description")?.optString("stringValue", "") ?: ""
                        val a11y = fields.optJSONObject("accessibilityActive")?.optBoolean("booleanValue", false) ?: false
                        val status = fields.optJSONObject("status")?.optString("stringValue", "open") ?: "open"
                        val createdAt = fields.optJSONObject("createdAt")?.optString("integerValue", "0")?.toLongOrNull() ?: 0L
                        mapById[repId] = BugReportRecord(
                            reportId = repId,
                            uid = uid,
                            deviceName = devName,
                            androidVersion = androidVer,
                            appVersion = ver,
                            type = type,
                            title = title,
                            description = desc,
                            accessibilityActive = a11y,
                            status = status,
                            createdAt = createdAt
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }

        mapById.values.sortedByDescending { it.createdAt }
    }

    suspend fun deleteBugReportFromFirestore(reportId: String): Boolean = withContext(Dispatchers.IO) {
        var okAny = false
        try {
            val rtdbUrl = URL("$FIREBASE_DATABASE_URL/androidisland/bug_reports/$reportId.json")
            val conn = (rtdbUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 5000
                readTimeout = 5000
            }
            if (conn.responseCode in 200..299) okAny = true
        } catch (_: Exception) {
        }
        try {
            val docUrl = URL("https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/bug_reports/$reportId?key=$FIREBASE_API_KEY")
            val conn = (docUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 5000
                readTimeout = 5000
            }
            if (conn.responseCode in 200..299) okAny = true
        } catch (_: Exception) {
        }
        okAny
    }

    fun verifyAdminPasscodeAndSelfApprove(context: Context, passcode: String): Boolean {
        val clean = passcode.trim()
        if (clean.equals("digitalxiao", ignoreCase = true) ||
            clean.equals("DIGITALXIAO-VIP", ignoreCase = true) ||
            clean.equals("admin2026", ignoreCase = true)
        ) {
            val uid = getUserUid(context)
            val devName = getDeviceModelName()
            val a11y = isAccessibilityEnabledOnDevice(context)
            val ovl = Settings.canDrawOverlays(context)
            val ver = UpdateManager.getCurrentVersion(context)
            saveVerifiedSeal(context, uid)
            setSkipActivationPrompt(context, true)
            executor.execute {
                approveUidOnFirebaseInternal(uid, "$devName [Admin]", true, a11y, ovl, ver)
            }
            return true
        }
        return false
    }

    fun sha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        val sb = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            sb.append(String.format("%02x", b))
        }
        return sb.toString()
    }

    private fun getOrCreateHardwareSecretKey(): SecretKey? {
        return try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val existing = ks.getEntry(KEYSTORE_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (existing != null) return existing.secretKey

            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                KEYSTORE_ALIAS,
                KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
            ).build()
            keyGenerator.init(spec)
            keyGenerator.generateKey()
        } catch (_: Throwable) {
            null
        }
    }

    private fun computeKeystoreHmac(context: Context, data: String): String? {
        return try {
            val secretKey = getOrCreateHardwareSecretKey()
            if (secretKey != null) {
                val mac = Mac.getInstance("HmacSHA256")
                mac.init(secretKey)
                val sig = mac.doFinal(data.toByteArray(StandardCharsets.UTF_8))
                Base64.encodeToString(sig, Base64.NO_WRAP)
            } else {
                val fallbackId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "dxiao"
                sha256Hex("FALLBACK_HMAC_${fallbackId}_$data")
            }
        } catch (_: Throwable) {
            null
        }
    }
}
