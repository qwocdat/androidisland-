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

// Remote toast on Dynamic Island disabled per user request
    }
}
