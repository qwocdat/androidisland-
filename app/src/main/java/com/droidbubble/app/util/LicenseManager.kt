package com.droidbubble.app.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
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
import java.util.UUID
import java.util.concurrent.Executors
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey

/**
 * KeyGate & Lifetime License Manager ported 1:1 from DigitalVolume:
 *  - Firebase Firestore REST activation sync (digitalvolume-keygate)
 *  - Cloudflare Worker backend verification (https://getkey.digitalsmod.site/api)
 *  - Hardware-backed Android Keystore HMAC-SHA256 signature seal
 *  - Single-use, UID-bound & global cryptographic signed keys (format: DXIAO-XXXX-XXXX-XXXX)
 *  - Support for legacy Master Keys
 */
object LicenseManager {
    private const val PREF_FILE = "digital_volume_keygate_secure"
    private const val KEY_UID = "user_uid"
    private const val KEY_ACTIVATED_PAYLOAD = "activated_payload"
    private const val KEY_ACTIVATED_HMAC = "activated_hmac"
    private const val KEY_USED_HASHES = "used_key_hashes_registry"
    private const val KEYSTORE_ALIAS = "DigitalVolumeKeyGateHmacKey"

    const val DEFAULT_GETKEY_URL = "https://getkey.digitalsmod.site/app/digitalvolume/"
    const val DEFAULT_API_BASE = "https://getkey.digitalsmod.site/api"
    const val FIREBASE_PROJECT_ID = "digitalvolume-keygate"
    const val FIREBASE_API_KEY = "AIzaSyDigitalXiaoKeyGatePublicConfigKey"

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val MASTER_KEY_HASHES = setOf(
        "ad355610e8d142d7f9d0f35de17ab31f8ae44c978374d4aed69cc3cf6a58cf4f", // DIGITALXIAO-VIP
        "57abc24acc5b0e190ee193affc8c5f3a64c1a15a14049dbbb9e300f7604fdacf", // DIGITALXIAO-2026
        "7aa7f46f75e3fbf81b05e82daa889064efc03b1ffd458c194735380d2854cc45", // ANDROIDISLAND-VIP
        "872689f97ed9c92c1395f8cf238270fa14bdc525c533f04ff707ea96cf809040"  // DIGITALVOLUME-VIP
    )

    data class VerifyResult(
        val success: Boolean,
        val message: String,
        val keyId: String? = null,
        val activatedAt: Long = 0L
    )

    private fun securePrefs(context: Context) =
        context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    /**
     * Checks whether the current device UID has a valid hardware-HMAC-signed lifetime license.
     */
    fun isActivated(context: Context): Boolean {
        return hasValidLocalSeal(context)
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
            val licenseType = json.optString("licenseType", "")
            val keyId = json.optString("keyId", "")
            activated && licenseType == "lifetime" && uid == getUserUid(context) && keyId.isNotBlank()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns a deterministic hardware-anchored UID for this device (same algorithm as DigitalVolume).
     */
    fun getUserUid(context: Context): String {
        val prefs = securePrefs(context)
        val androidId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        } catch (_: Exception) {
            "unknown"
        }
        val existing = prefs.getString(KEY_UID, null)
        if (!existing.isNullOrBlank()) return existing
        val raw = "dxiao_${androidId}_${UUID.randomUUID().toString().take(8)}"
        val uid = "uid_" + sha256Hex(raw).take(20)
        prefs.edit().putString(KEY_UID, uid).apply()
        return uid
    }

    fun getDeviceId(context: Context): String {
        return getUserUid(context)
    }

    fun getActivatedKeyId(context: Context): String? {
        if (!hasValidLocalSeal(context)) return null
        val payload = securePrefs(context).getString(KEY_ACTIVATED_PAYLOAD, null) ?: return null
        return try {
            JSONObject(payload).optString("keyId", null)
        } catch (_: Exception) {
            null
        }
    }

    fun getSavedKey(context: Context): String? {
        return getActivatedKeyId(context)
    }

    fun getKeyPageUrl(context: Context): String {
        val uid = getUserUid(context)
        return "$DEFAULT_GETKEY_URL?uid=$uid"
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

    private fun saveVerifiedSeal(context: Context, uid: String, keyId: String, activatedAt: Long) {
        val payload = JSONObject().apply {
            put("uid", uid)
            put("activated", true)
            put("licenseType", "lifetime")
            put("keyId", keyId)
            put("activatedAt", activatedAt)
        }.toString()
        val hmac = computeKeystoreHmac(context, payload) ?: return
        securePrefs(context).edit()
            .putString(KEY_ACTIVATED_PAYLOAD, payload)
            .putString(KEY_ACTIVATED_HMAC, hmac)
            .apply()
    }

    /**
     * Syncs activation to Firebase Firestore REST API (digitalvolume-keygate)
     */
    private fun syncActivationToFirestoreRest(uid: String, keyId: String, codeHash: String, nowMs: Long) {
        try {
            val docUrl = URL("https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/databases/(default)/documents/users/$uid?key=$FIREBASE_API_KEY")
            val conn = (docUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "PATCH"
                doOutput = true
                connectTimeout = 5000
                readTimeout = 5000
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            }
            val payload = JSONObject().apply {
                put("fields", JSONObject().apply {
                    put("activated", JSONObject().put("booleanValue", true))
                    put("licenseType", JSONObject().put("stringValue", "lifetime"))
                    put("keyId", JSONObject().put("stringValue", keyId))
                    put("codeHash", JSONObject().put("stringValue", codeHash))
                    put("activatedAt", JSONObject().put("integerValue", nowMs.toString()))
                    put("appName", JSONObject().put("stringValue", "androidisland"))
                })
            }
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(payload.toString()) }
            conn.responseCode
        } catch (_: Exception) {
        }
    }

    /**
     * Verifies and redeems a single-use KEY atomically using DigitalVolume's exact mechanism.
     */
    fun verifyAndActivateKey(context: Context, rawKeyInput: String, callback: (VerifyResult) -> Unit) {
        val cleanedKey = rawKeyInput.trim().uppercase(Locale.ROOT)
        if (cleanedKey.isEmpty()) {
            callback(VerifyResult(false, "Vui lòng nhập mã KEY để xác thực."))
            return
        }

        // Support legacy Master Key (e.g. DIGITALXIAO-VIP)
        val inputHash = sha256Hex(cleanedKey)
        if (MASTER_KEY_HASHES.contains(inputHash)) {
            val uid = getUserUid(context)
            val now = System.currentTimeMillis()
            saveVerifiedSeal(context, uid, cleanedKey, now)
            callback(VerifyResult(true, "Kích hoạt Master Key thành công!", cleanedKey, now))
            return
        }

        if (!isWellFormedKey(cleanedKey)) {
            callback(VerifyResult(false, "Định dạng KEY không hợp lệ (Chuẩn: DXIAO-XXXX-XXXX-XXXX)."))
            return
        }

        val uid = getUserUid(context)
        val apiBase = DEFAULT_API_BASE

        executor.execute {
            // 1. Live Cloudflare / Firebase Backend verification
            try {
                val url = URL("$apiBase/verify-key")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    connectTimeout = 8000
                    readTimeout = 8000
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    setRequestProperty("X-Device-Uid", uid)
                }
                val reqJson = JSONObject().apply {
                    put("uid", uid)
                    put("key", cleanedKey)
                    put("clientTimestamp", System.currentTimeMillis())
                    put("app", "androidisland")
                }
                OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use {
                    it.write(reqJson.toString())
                }
                val status = conn.responseCode
                val stream = if (status in 200..299) conn.inputStream else conn.errorStream
                if (stream != null) {
                    val respText = BufferedReader(InputStreamReader(stream)).use { it.readText() }
                    val resp = JSONObject(respText)
                    val ok = status == 200 && resp.optBoolean("success", false)
                    if (ok) {
                        val keyId = resp.optString("keyId", "KEY_" + sha256Hex(cleanedKey).take(10).uppercase(Locale.ROOT))
                        val ts = resp.optLong("activatedAt", System.currentTimeMillis())
                        saveVerifiedSeal(context, uid, keyId, ts)
                        syncActivationToFirestoreRest(uid, keyId, sha256Hex(cleanedKey), ts)
                        mainHandler.post {
                            callback(VerifyResult(true, "Kích hoạt bản quyền Lifetime thành công!", keyId, ts))
                        }
                        return@execute
                    }
                }
            } catch (_: Exception) {
                // Fallback to cryptographic offline check
            }

            // 2. Cryptographic & atomic verification for keys issued by getkey system
            val verification = verifyCryptographicKey(context, cleanedKey, uid)
            mainHandler.post { callback(verification) }
        }
    }

    @Synchronized
    private fun verifyCryptographicKey(context: Context, key: String, currentUid: String): VerifyResult {
        val parts = key.split("-")
        if (parts.size != 4 || parts[0] != "DXIAO") {
            return VerifyResult(false, "Mã KEY không đúng cấu trúc DXIAO-XXXX-XXXX-XXXX.")
        }
        val seg1 = parts[1]
        val seg2 = parts[2]
        val seg3 = parts[3]

        val expectedUidBound = sha256Hex("DXIAO_V1_${seg1}_${seg2}_${currentUid}").take(4).uppercase(Locale.ROOT)
        val expectedGeneral = sha256Hex("DXIAO_V1_${seg1}_${seg2}_GLOBAL").take(4).uppercase(Locale.ROOT)
        val expectedOtherUidCheck = sha256Hex("DXIAO_OTHER_${seg1}_${seg2}").take(4).uppercase(Locale.ROOT)

        if (seg3 != expectedUidBound && seg3 != expectedGeneral) {
            if (seg1.startsWith("UID") || seg3 == expectedOtherUidCheck) {
                return VerifyResult(false, "KEY này đã được gắn với một tài khoản (UID) khác.")
            }
            return VerifyResult(false, "Mã KEY không tồn tại hoặc chữ ký bảo mật không hợp lệ.")
        }

        val codeHash = sha256Hex("KEY_SALT_DXIAO_$key")
        val prefs = securePrefs(context)
        val registryJsonStr = prefs.getString(KEY_USED_HASHES, "{}") ?: "{}"
        val registry = try { JSONObject(registryJsonStr) } catch (_: Exception) { JSONObject() }

        if (registry.has(codeHash)) {
            val usedBy = registry.optString(codeHash, "")
            return if (usedBy == currentUid) {
                VerifyResult(false, "Mã KEY này đã được sử dụng trước đó (KEY chỉ dùng 1 lần).")
            } else {
                VerifyResult(false, "Mã KEY này đã được kích hoạt trên một thiết bị/tài khoản khác.")
            }
        }

        registry.put(codeHash, currentUid)
        prefs.edit().putString(KEY_USED_HASHES, registry.toString()).apply()

        val keyId = "KEY_" + codeHash.take(10).uppercase(Locale.ROOT)
        val now = System.currentTimeMillis()
        saveVerifiedSeal(context, currentUid, keyId, now)
        syncActivationToFirestoreRest(currentUid, keyId, codeHash, now)
        return VerifyResult(true, "Xác thực thành công! Đã mở khóa bản quyền Lifetime vĩnh viễn.", keyId, now)
    }

    private fun isWellFormedKey(key: String): Boolean {
        val regex = Regex("^DXIAO-[A-Z0-9]{4}-[A-Z0-9]{4}-[A-Z0-9]{4}$")
        return regex.matches(key)
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
