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
 * UID-based Device Authorization, Real-Time Accessibility Telemetry & Bug Report Manager:
 *  - Deterministic hardware-anchored UID (`uid_...`).
 *  - Supports "Don't show again" (`isSkipActivationPrompt` / `setSkipActivationPrompt`) so verified users
 *    can bypass the activation/social prompt on future app launches.
 *  - Tracks activation status (`approved`), live Accessibility Service status (`accessibilityActive`),
 *    overlay status (`overlayActive`), app version, and `lastActiveAt` timestamp on Firebase Firestore (`uid_devices/{uid}`).
 *  - Collects & manages user Bug Reports and Feature Suggestions on Firebase Firestore (`bug_reports/{reportId}`).
 */
object LicenseManager {
    private const val PREF_FILE = "digital_volume_keygate_secure"
    private const val KEY_UID = "user_uid"
    private const val KEY_ACTIVATED_PAYLOAD = "activated_payload"
    private const val KEY_ACTIVATED_HMAC = "activated_hmac"
    private const val KEY_SKIP_ACTIVATION_PROMPT = "skip_activation_prompt_v2"
    private const val KEYSTORE_ALIAS = "DigitalVolumeKeyGateHmacKey"

    const val DEFAULT_API_BASE = "https://getkey.digitalsmod.site/api"
    const val FIREBASE_PROJECT_ID = "digitalvolume-keygate"
    const val FIREBASE_API_KEY = "AIzaSyDigitalXiaoKeyGatePublicConfigKey"

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

    /**
     * Returns true when the device has a valid local cryptographic UID seal AND the user has checked
     * "Không hiển thị lại" (default true once verified). If the user unchecked "Không hiển thị lại",
     * opening the app will show the verification/social screen again.
     */
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
     * Syncs live device usage & Accessibility Service status to Firebase Firestore (`uid_devices/{uid}`)
     * so Admin can see how many activated users are actively using the app via Accessibility Service.
     */
    fun syncDeviceTelemetry(context: Context, accessibilityOverride: Boolean? = null, overlayOverride: Boolean? = null) {
        if (!hasValidLocalSeal(context)) return
        val uid = getUserUid(context)
        val devName = getDeviceModelName()
        val a11yActive = accessibilityOverride ?: isAccessibilityEnabledOnDevice(context)
        val overlayActive = overlayOverride ?: Settings.canDrawOverlays(context)
        val appVer = UpdateManager.getCurrentVersion(context)
        executor.execute {
            approveUidOnFirestoreInternal(
                uid = uid,
                deviceName = devName,
                approved = true,
                accessibilityActive = a11yActive,
                overlayActive = overlayActive,
                appVersion = appVer
            )
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
                // 1. Check OnlineConfigManager (online_update.json / Firebase app_config)
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
                    approveUidOnFirestoreInternal(uid, deviceName, true, a11yActive, overlayActive, appVer)
                    mainHandler.post {
                        callback(VerifyResult(true, "Thiết bị ($uid) đã được cấp phép truy cập!", uid, "approved"))
                    }
                    return@execute
                }

                // 2. Check Firebase Firestore `uid_devices/{uid}`
                val docUrl = URL("https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/uid_devices/$uid?key=$FIREBASE_API_KEY")
                val getConn = (docUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 6000
                    readTimeout = 6000
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
                        approveUidOnFirestoreInternal(uid, deviceName, true, a11yActive, overlayActive, appVer)
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
                    } else {
                        registerPendingDeviceOnFirestore(uid, deviceName, a11yActive, overlayActive, appVer)
                    }
                } else if (code == 404) {
                    registerPendingDeviceOnFirestore(uid, deviceName, a11yActive, overlayActive, appVer)
                }

                // 3. Check legacy `users/{uid}` collection from DigitalVolume
                val userDocUrl = URL("https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/users/$uid?key=$FIREBASE_API_KEY")
                val userConn = (userDocUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 5000
                    readTimeout = 5000
                    setRequestProperty("Accept", "application/json")
                }
                if (userConn.responseCode == 200) {
                    val uBody = BufferedReader(InputStreamReader(userConn.inputStream)).use { it.readText() }
                    val uFields = JSONObject(uBody).optJSONObject("fields")
                    val uActivated = uFields?.optJSONObject("activated")?.optBoolean("booleanValue", false) ?: false
                    if (uActivated) {
                        saveVerifiedSeal(context, uid)
                        approveUidOnFirestoreInternal(uid, deviceName, true, a11yActive, overlayActive, appVer)
                        mainHandler.post {
                            callback(VerifyResult(true, "Đã đồng bộ quyền truy cập UID từ hệ thống DigitalVolume!", uid, "approved"))
                        }
                        return@execute
                    }
                }

                mainHandler.post {
                    callback(
                        VerifyResult(
                            success = false,
                            message = "UID của bạn đã được gửi lên hệ thống. Hãy gửi mã UID này cho Admin rồi bấm nút Xác minh!",
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

    private fun registerPendingDeviceOnFirestore(
        uid: String,
        deviceName: String,
        accessibilityActive: Boolean = false,
        overlayActive: Boolean = false,
        appVersion: String = "2.0.0"
    ) {
        try {
            val nowMs = System.currentTimeMillis()
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

    private fun approveUidOnFirestoreInternal(
        uid: String,
        deviceName: String,
        approved: Boolean,
        accessibilityActive: Boolean = false,
        overlayActive: Boolean = false,
        appVersion: String = "2.0.0"
    ) {
        try {
            val nowMs = System.currentTimeMillis()
            val statusStr = if (approved) "approved" else "blocked"
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
        val result = mutableListOf<DeviceUidRecord>()
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
                        val devName = fields.optJSONObject("deviceName")?.optString("stringValue", "Thiết bị Android") ?: "Thiết bị Android"
                        val approved = fields.optJSONObject("approved")?.optBoolean("booleanValue", false) ?: false
                        val status = fields.optJSONObject("status")?.optString("stringValue", if (approved) "approved" else "pending") ?: "pending"
                        val a11y = fields.optJSONObject("accessibilityActive")?.optBoolean("booleanValue", false) ?: false
                        val ovl = fields.optJSONObject("overlayActive")?.optBoolean("booleanValue", false) ?: false
                        val ver = fields.optJSONObject("appVersion")?.optString("stringValue", "2.0.0") ?: "2.0.0"
                        val reqAt = fields.optJSONObject("requestedAt")?.optString("integerValue", "0")?.toLongOrNull() ?: 0L
                        val lastAt = fields.optJSONObject("lastActiveAt")?.optString("integerValue", reqAt.toString())?.toLongOrNull() ?: reqAt
                        result.add(
                            DeviceUidRecord(
                                uid = uid,
                                deviceName = devName,
                                approved = approved,
                                status = status,
                                accessibilityActive = a11y,
                                overlayActive = ovl,
                                appVersion = ver,
                                requestedAt = reqAt,
                                lastActiveAt = lastAt
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
        result.sortedByDescending { maxOf(it.lastActiveAt, it.requestedAt) }
    }

    suspend fun setDeviceUidApproval(uid: String, deviceName: String, approved: Boolean): Boolean = withContext(Dispatchers.IO) {
        val cleanUid = uid.trim()
        if (cleanUid.isEmpty()) return@withContext false
        approveUidOnFirestoreInternal(cleanUid, deviceName.ifBlank { "Thiết bị được Admin cấp phép" }, approved)
        true
    }

    /**
     * Submits a Bug Report or Feature Suggestion directly to Firebase Firestore (`bug_reports/{reportId}`)
     * so Admin can view and manage all bug reports on the Web Admin Dashboard & In-App Admin Panel.
     */
    suspend fun submitBugReportToFirestore(
        context: Context,
        type: String,
        title: String,
        description: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val uid = getUserUid(context)
            val devName = getDeviceModelName()
            val androidVer = Build.VERSION.RELEASE ?: "14"
            val appVer = UpdateManager.getCurrentVersion(context)
            val a11y = isAccessibilityEnabledOnDevice(context)
            val nowMs = System.currentTimeMillis()
            val reportId = "rep_${nowMs}_${uid.takeLast(6)}"

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
                    put("title", JSONObject().put("stringValue", title.ifBlank { if (type == "BUG") "Báo cáo lỗi" else "Đề xuất tính năng" }))
                    put("description", JSONObject().put("stringValue", description.trim()))
                    put("accessibilityActive", JSONObject().put("booleanValue", a11y))
                    put("status", JSONObject().put("stringValue", "open"))
                    put("createdAt", JSONObject().put("integerValue", nowMs.toString()))
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

    /**
     * Admin function: fetches all Bug Reports and Feature Suggestions from Firebase Firestore (`bug_reports`).
     */
    suspend fun fetchAllBugReportsFromFirestore(): List<BugReportRecord> = withContext(Dispatchers.IO) {
        val result = mutableListOf<BugReportRecord>()
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
                        result.add(
                            BugReportRecord(
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
                        )
                    }
                }
            }
        } catch (_: Exception) {
        }
        result.sortedByDescending { it.createdAt }
    }

    /**
     * Admin function: deletes a bug report from Firebase Firestore (`bug_reports/{reportId}`).
     */
    suspend fun deleteBugReportFromFirestore(reportId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val docUrl = URL("https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/bug_reports/$reportId?key=$FIREBASE_API_KEY")
            val conn = (docUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "DELETE"
                connectTimeout = 5000
                readTimeout = 5000
            }
            conn.responseCode in 200..299
        } catch (_: Exception) {
            false
        }
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
                approveUidOnFirestoreInternal(uid, "$devName [Admin]", true, a11y, ovl, ver)
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
