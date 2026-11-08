package com.droidbubble.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.droidbubble.app.model.DEFAULT_FEATURE_PRIORITIES
import com.droidbubble.app.model.DEFAULT_MEDIA_LAYERS
import com.droidbubble.app.model.DesignLanguage
import com.droidbubble.app.model.IslandSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "island_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_ENABLED = booleanPreferencesKey("is_enabled")
        val WIDTH_DP = intPreferencesKey("width_dp")
        val HEIGHT_DP = intPreferencesKey("height_dp")
        val CORNER_RADIUS_DP = intPreferencesKey("corner_radius_dp")
        val EXPANDED_CORNER_RADIUS_DP = intPreferencesKey("expanded_corner_radius_dp")
        val TOP_OFFSET_DP = intPreferencesKey("top_offset_dp")
        val VERTICAL_OFFSET_OPENING_DP = intPreferencesKey("vertical_offset_opening_dp")
        val HORIZONTAL_OFFSET_DP = intPreferencesKey("horizontal_offset_dp")
        val IS_LEFT_CUTOUT = booleanPreferencesKey("is_left_cutout")
        val IS_TEARDROP_CUTOUT = booleanPreferencesKey("is_teardrop_cutout")
        val SHOW_OUTLINE = booleanPreferencesKey("show_outline")
        val OUTLINE_COLOR_ARGB = longPreferencesKey("outline_color_argb")
        val OUTLINE_THICKNESS_DP = floatPreferencesKey("outline_thickness_dp")
        val EXPANDED_SCALE = floatPreferencesKey("expanded_scale")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val SHOW_CALLS = booleanPreferencesKey("show_calls")
        val SHOW_VOICE_MEMO = booleanPreferencesKey("show_voice_memo")
        val SHOW_MEDIA = booleanPreferencesKey("show_media")
        val SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        val SHOW_LIVE_PROGRESS = booleanPreferencesKey("show_live_progress")
        val SHOW_SCREEN_RECORDING = booleanPreferencesKey("show_screen_recording")
        val SHOW_FLASHLIGHT = booleanPreferencesKey("show_flashlight")
        val SHOW_BLUETOOTH = booleanPreferencesKey("show_bluetooth")
        val SHOW_CHARGING = booleanPreferencesKey("show_charging")
        val SHOW_RINGER_MODE = booleanPreferencesKey("show_ringer_mode")
        val SHOW_AIRPLANE_MODE = booleanPreferencesKey("show_airplane_mode")
        val SHOW_IN_LANDSCAPE = booleanPreferencesKey("show_in_landscape")
        val ANIMATION_SPEED_MULTIPLIER = floatPreferencesKey("animation_speed_multiplier")
        val BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
        val RESTART_ON_BOOT = booleanPreferencesKey("restart_on_boot")
        val BUBBLE_EFFECT_ENABLED = booleanPreferencesKey("bubble_effect_enabled")
        val DISMISS_MEDIA_ON_PAUSE = booleanPreferencesKey("dismiss_media_on_pause")
        val ALWAYS_SHOW_PILL = booleanPreferencesKey("always_show_pill")
        val SHOW_DUAL_ACTIVITY_IN_COMPACT = booleanPreferencesKey("show_dual_activity_in_compact")
        val SHOW_FPS = booleanPreferencesKey("show_fps")
        val SHOW_WEATHER = booleanPreferencesKey("show_weather")
        val SHOW_NETWORK_SPEED = booleanPreferencesKey("show_network_speed")
        val SHOW_ALARM = booleanPreferencesKey("show_alarm")
        val AUTO_DISMISS_NOTIFICATIONS = booleanPreferencesKey("auto_dismiss_notifications")
        val NOTIFICATION_AUTO_DISMISS_SECONDS = intPreferencesKey("notification_auto_dismiss_seconds")
        val PERSISTENT_CHARGING = booleanPreferencesKey("persistent_charging")
        val NOTIFICATION_MAX_LINES = intPreferencesKey("notification_max_lines")
        val WEATHER_CITY = stringPreferencesKey("weather_city")
        val FONT_KEY = stringPreferencesKey("font_key")
        val SWAP_ACTIVITIES_ON_SWIPE = booleanPreferencesKey("swap_activities_on_swipe")
        val BLURRED_ALBUM_ART_BACKGROUND = booleanPreferencesKey("blurred_album_art_background")
        val DESIGN_LANGUAGE = stringPreferencesKey("design_language")
        val GLASS_ENABLED = booleanPreferencesKey("glass_enabled")
        val GLASS_OPACITY = intPreferencesKey("glass_opacity")
        val MEDIA_WAVE_ANIMATION = booleanPreferencesKey("media_wave_animation")
        val SPINNING_ALBUM_ART = booleanPreferencesKey("spinning_album_art")
        val WAVY_PROGRESS_BAR = booleanPreferencesKey("wavy_progress_bar")
        val HIDE_ON_LOCK_SCREEN = booleanPreferencesKey("hide_on_lock_screen")
        val EXPAND_DAMPING = floatPreferencesKey("expand_damping")
        val EXPAND_STIFFNESS = floatPreferencesKey("expand_stiffness")
        val APPEAR_DAMPING = floatPreferencesKey("appear_damping")
        val APPEAR_STIFFNESS = floatPreferencesKey("appear_stiffness")
        val SPLIT_DAMPING = floatPreferencesKey("split_damping")
        val SPLIT_STIFFNESS = floatPreferencesKey("split_stiffness")
        val FEATURE_PRIORITIES = stringPreferencesKey("feature_priorities")
        val MEDIA_CONTROL_ALIGNMENT = stringPreferencesKey("media_control_alignment")
        val MEDIA_LAYERS_ORDER = stringPreferencesKey("media_layers_order")
        val DROIDBAR_ENABLED = booleanPreferencesKey("droidbar_enabled")
        val DROIDBAR_SHOW_FLASHLIGHT = booleanPreferencesKey("droidbar_show_flashlight")
        val DROIDBAR_SHOW_RING_MODE = booleanPreferencesKey("droidbar_show_ring_mode")
        val DROIDBAR_SHOW_SCREENSHOT = booleanPreferencesKey("droidbar_show_screenshot")
        val DROIDBAR_SHOW_WIFI = booleanPreferencesKey("droidbar_show_wifi")
        val DROIDBAR_SHOW_MOBILE_DATA = booleanPreferencesKey("droidbar_show_mobile_data")
        val DROIDBAR_USE_MOST_USED_APPS = booleanPreferencesKey("droidbar_use_most_used_apps")
        val DROIDBAR_CUSTOM_APPS = stringPreferencesKey("droidbar_custom_apps")
        val DROIDBAR_BACKGROUND_OPACITY = floatPreferencesKey("droidbar_background_opacity")
        val DROIDBAR_MAX_HEIGHT_DP = intPreferencesKey("droidbar_max_height_dp")
        val DROIDBAR_Y_OFFSET_DP = intPreferencesKey("droidbar_y_offset_dp")
        val DROIDBAR_HAPTICS_ENABLED = booleanPreferencesKey("droidbar_haptics_enabled")
        // Liquid & Extra JSON blob for extended properties
        val LIQUID_EXTENDED_JSON = stringPreferencesKey("liquid_extended_json")
    }

    private fun mapPreferences(preferences: Preferences): IslandSettings {
        val base = IslandSettings(
            isEnabled = preferences[PreferencesKeys.IS_ENABLED] ?: true,
            widthDp = preferences[PreferencesKeys.WIDTH_DP] ?: 124,
            heightDp = preferences[PreferencesKeys.HEIGHT_DP] ?: 38,
            cornerRadiusDp = preferences[PreferencesKeys.CORNER_RADIUS_DP] ?: 22,
            expandedCornerRadiusDp = preferences[PreferencesKeys.EXPANDED_CORNER_RADIUS_DP] ?: 38,
            topOffsetDp = preferences[PreferencesKeys.TOP_OFFSET_DP] ?: 12,
            verticalOffsetOpeningDp = preferences[PreferencesKeys.VERTICAL_OFFSET_OPENING_DP] ?: 0,
            horizontalOffsetDp = preferences[PreferencesKeys.HORIZONTAL_OFFSET_DP] ?: 0,
            isLeftCutout = preferences[PreferencesKeys.IS_LEFT_CUTOUT] ?: false,
            isTeardropCutout = preferences[PreferencesKeys.IS_TEARDROP_CUTOUT] ?: false,
            showOutline = preferences[PreferencesKeys.SHOW_OUTLINE] ?: true,
            outlineColorArgb = preferences[PreferencesKeys.OUTLINE_COLOR_ARGB] ?: 0x33FFFFFF,
            outlineThicknessDp = preferences[PreferencesKeys.OUTLINE_THICKNESS_DP] ?: 1.5f,
            expandedScale = preferences[PreferencesKeys.EXPANDED_SCALE] ?: 1.1f,
            hapticsEnabled = preferences[PreferencesKeys.HAPTICS_ENABLED] ?: true,
            showCalls = preferences[PreferencesKeys.SHOW_CALLS] ?: true,
            showVoiceMemo = preferences[PreferencesKeys.SHOW_VOICE_MEMO] ?: true,
            showMedia = preferences[PreferencesKeys.SHOW_MEDIA] ?: true,
            showNotifications = preferences[PreferencesKeys.SHOW_NOTIFICATIONS] ?: true,
            showLiveProgress = preferences[PreferencesKeys.SHOW_LIVE_PROGRESS] ?: true,
            showScreenRecording = preferences[PreferencesKeys.SHOW_SCREEN_RECORDING] ?: true,
            showFlashlight = preferences[PreferencesKeys.SHOW_FLASHLIGHT] ?: true,
            showBluetooth = preferences[PreferencesKeys.SHOW_BLUETOOTH] ?: true,
            showCharging = preferences[PreferencesKeys.SHOW_CHARGING] ?: true,
            showRingerMode = preferences[PreferencesKeys.SHOW_RINGER_MODE] ?: true,
            showAirplaneMode = preferences[PreferencesKeys.SHOW_AIRPLANE_MODE] ?: true,
            showInLandscape = preferences[PreferencesKeys.SHOW_IN_LANDSCAPE] ?: false,
            animationSpeed = (preferences[PreferencesKeys.ANIMATION_SPEED_MULTIPLIER] ?: 0.85f).coerceIn(0.5f, 2.0f),
            blockedPackages = preferences[PreferencesKeys.BLOCKED_PACKAGES] ?: emptySet(),
            restartOnBoot = preferences[PreferencesKeys.RESTART_ON_BOOT] ?: true,
            bubbleEffectEnabled = preferences[PreferencesKeys.BUBBLE_EFFECT_ENABLED] ?: true,
            dismissMediaOnPause = preferences[PreferencesKeys.DISMISS_MEDIA_ON_PAUSE] ?: false,
            alwaysShowPill = preferences[PreferencesKeys.ALWAYS_SHOW_PILL] ?: true,
            showDualActivityInCompact = preferences[PreferencesKeys.SHOW_DUAL_ACTIVITY_IN_COMPACT] ?: true,
            showFps = preferences[PreferencesKeys.SHOW_FPS] ?: false,
            showWeather = preferences[PreferencesKeys.SHOW_WEATHER] ?: false,
            showNetworkSpeed = preferences[PreferencesKeys.SHOW_NETWORK_SPEED] ?: false,
            showAlarm = preferences[PreferencesKeys.SHOW_ALARM] ?: true,
            autoDismissNotifications = preferences[PreferencesKeys.AUTO_DISMISS_NOTIFICATIONS] ?: false,
            notificationAutoDismissSeconds = preferences[PreferencesKeys.NOTIFICATION_AUTO_DISMISS_SECONDS] ?: 3,
            persistentCharging = preferences[PreferencesKeys.PERSISTENT_CHARGING] ?: false,
            notificationMaxLines = (preferences[PreferencesKeys.NOTIFICATION_MAX_LINES] ?: 2).coerceIn(1, 6),
            weatherCity = preferences[PreferencesKeys.WEATHER_CITY] ?: "",
            fontKey = preferences[PreferencesKeys.FONT_KEY] ?: "DEFAULT",
            swapActivitiesOnSwipe = preferences[PreferencesKeys.SWAP_ACTIVITIES_ON_SWIPE] ?: true,
            blurredAlbumArtBackground = preferences[PreferencesKeys.BLURRED_ALBUM_ART_BACKGROUND] ?: false,
            designLanguage = preferences[PreferencesKeys.DESIGN_LANGUAGE]?.let {
                runCatching { DesignLanguage.valueOf(it) }.getOrNull()
            } ?: if (preferences[PreferencesKeys.GLASS_ENABLED] == true) DesignLanguage.GLASS else DesignLanguage.MATERIAL3,
            glassEnabled = preferences[PreferencesKeys.GLASS_ENABLED] ?: false,
            glassOpacity = (preferences[PreferencesKeys.GLASS_OPACITY] ?: 25).coerceIn(0, 100),
            mediaWaveAnimation = preferences[PreferencesKeys.MEDIA_WAVE_ANIMATION] ?: true,
            spinningAlbumArt = preferences[PreferencesKeys.SPINNING_ALBUM_ART] ?: false,
            wavyProgressBar = preferences[PreferencesKeys.WAVY_PROGRESS_BAR] ?: true,
            hideOnLockScreen = preferences[PreferencesKeys.HIDE_ON_LOCK_SCREEN] ?: false,
            expandDamping = (preferences[PreferencesKeys.EXPAND_DAMPING] ?: 0.74f).coerceIn(0.25f, 1.0f),
            expandStiffness = (preferences[PreferencesKeys.EXPAND_STIFFNESS] ?: 380f).coerceIn(100f, 1500f),
            appearDamping = (preferences[PreferencesKeys.APPEAR_DAMPING] ?: 0.6f).coerceIn(0.25f, 1.0f),
            appearStiffness = (preferences[PreferencesKeys.APPEAR_STIFFNESS] ?: 280f).coerceIn(100f, 1500f),
            splitDamping = (preferences[PreferencesKeys.SPLIT_DAMPING] ?: 0.63f).coerceIn(0.25f, 1.0f),
            splitStiffness = (preferences[PreferencesKeys.SPLIT_STIFFNESS] ?: 320f).coerceIn(100f, 1500f),
            featurePriorities = preferences[PreferencesKeys.FEATURE_PRIORITIES]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?.let { saved ->
                    val missing = DEFAULT_FEATURE_PRIORITIES.filter { it !in saved }
                    saved + missing
                } ?: DEFAULT_FEATURE_PRIORITIES,
            mediaControlAlignment = preferences[PreferencesKeys.MEDIA_CONTROL_ALIGNMENT] ?: "CENTER",
            mediaLayersOrder = preferences[PreferencesKeys.MEDIA_LAYERS_ORDER]
                ?.split(",")
                ?.filter { it in DEFAULT_MEDIA_LAYERS }
                ?.let { saved ->
                    val missing = DEFAULT_MEDIA_LAYERS.filter { it !in saved }
                    (saved + missing).distinct()
                }
                ?.takeIf { it.size == DEFAULT_MEDIA_LAYERS.size }
                ?: DEFAULT_MEDIA_LAYERS,
            droidbarEnabled = preferences[PreferencesKeys.DROIDBAR_ENABLED] ?: false,
            droidbarShowFlashlight = preferences[PreferencesKeys.DROIDBAR_SHOW_FLASHLIGHT] ?: true,
            droidbarShowRingMode = preferences[PreferencesKeys.DROIDBAR_SHOW_RING_MODE] ?: true,
            droidbarShowScreenshot = preferences[PreferencesKeys.DROIDBAR_SHOW_SCREENSHOT] ?: true,
            droidbarShowWifi = preferences[PreferencesKeys.DROIDBAR_SHOW_WIFI] ?: true,
            droidbarShowMobileData = preferences[PreferencesKeys.DROIDBAR_SHOW_MOBILE_DATA] ?: true,
            droidbarUseMostUsedApps = preferences[PreferencesKeys.DROIDBAR_USE_MOST_USED_APPS] ?: true,
            droidbarCustomApps = preferences[PreferencesKeys.DROIDBAR_CUSTOM_APPS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?: emptyList(),
            droidbarBackgroundOpacity = (preferences[PreferencesKeys.DROIDBAR_BACKGROUND_OPACITY] ?: 1.0f).coerceIn(0f, 1f),
            droidbarMaxHeightDp = preferences[PreferencesKeys.DROIDBAR_MAX_HEIGHT_DP] ?: 600,
            droidbarYOffsetDp = preferences[PreferencesKeys.DROIDBAR_Y_OFFSET_DP] ?: 0,
            droidbarHapticsEnabled = preferences[PreferencesKeys.DROIDBAR_HAPTICS_ENABLED] ?: true
        )

        val extJsonStr = preferences[PreferencesKeys.LIQUID_EXTENDED_JSON]
        if (extJsonStr.isNullOrBlank()) return base
        return runCatching {
            val j = JSONObject(extJsonStr)
            base.copy(
                appLanguage = j.optString("appLanguage", base.appLanguage),
                appThemeMode = j.optString("appThemeMode", base.appThemeMode),
                accentColorKey = j.optString("accentColorKey", base.accentColorKey),
                islandRefreshRateHz = j.optInt("islandRefreshRateHz", base.islandRefreshRateHz),
                multitaskingMode = j.optString("multitaskingMode", base.multitaskingMode),
                multitaskingBubbleShape = j.optString("multitaskingBubbleShape", base.multitaskingBubbleShape),
                multitaskingBubbleWidthDp = j.optInt("multitaskingBubbleWidthDp", base.multitaskingBubbleWidthDp),
                multitaskingCornerRadiusDp = j.optInt("multitaskingCornerRadiusDp", base.multitaskingCornerRadiusDp),
                centeringShift = j.optBoolean("centeringShift", base.centeringShift),
                leftMultitaskingCircle = j.optBoolean("leftMultitaskingCircle", base.leftMultitaskingCircle),
                showSliderValues = j.optBoolean("showSliderValues", base.showSliderValues),
                crashlyticsEnabled = j.optBoolean("crashlyticsEnabled", base.crashlyticsEnabled),
                devPanelUnlocked = j.optBoolean("devPanelUnlocked", base.devPanelUnlocked),
                liquidRefraction = j.optDouble("liquidRefraction", base.liquidRefraction.toDouble()).toFloat(),
                liquidSurfaceTension = j.optDouble("liquidSurfaceTension", base.liquidSurfaceTension.toDouble()).toFloat(),
                tapToExpand = j.optBoolean("tapToExpand", base.tapToExpand),
                disableExpandInLandscape = j.optBoolean("disableExpandInLandscape", base.disableExpandInLandscape),
                closeOnBackgroundTap = j.optBoolean("closeOnBackgroundTap", base.closeOnBackgroundTap),
                backgroundDimming = j.optBoolean("backgroundDimming", base.backgroundDimming),
                privacyIndicators = j.optBoolean("privacyIndicators", base.privacyIndicators),
                privacyDotPositionPx = j.optInt("privacyDotPositionPx", base.privacyDotPositionPx),
                motionBlur = j.optBoolean("motionBlur", base.motionBlur),
                islandOpacity = j.optDouble("islandOpacity", base.islandOpacity.toDouble()).toFloat(),
                islandColorArgb = j.optLong("islandColorArgb", base.islandColorArgb),
                dynamicLightGlow = j.optBoolean("dynamicLightGlow", base.dynamicLightGlow),
                lockRotation = j.optBoolean("lockRotation", base.lockRotation),
                hideWhenShadeOpen = j.optBoolean("hideWhenShadeOpen", base.hideWhenShadeOpen),
                islandGesturesEnabled = j.optBoolean("islandGesturesEnabled", base.islandGesturesEnabled),
                gesturesOnlyWhenEmpty = j.optBoolean("gesturesOnlyWhenEmpty", base.gesturesOnlyWhenEmpty),
                adaptiveGesturesEnabled = j.optBoolean("adaptiveGesturesEnabled", base.adaptiveGesturesEnabled),
                swipeLeftAction = j.optString("swipeLeftAction", base.swipeLeftAction),
                swipeRightAction = j.optString("swipeRightAction", base.swipeRightAction),
                swipeUpAction = j.optString("swipeUpAction", base.swipeUpAction),
                swipeDownAction = j.optString("swipeDownAction", base.swipeDownAction),
                doubleTapAction = j.optString("doubleTapAction", base.doubleTapAction),
                longPressAction = j.optString("longPressAction", base.longPressAction),
                eqShowEqualizer = j.optBoolean("eqShowEqualizer", base.eqShowEqualizer),
                eqRealTimeVisuals = j.optBoolean("eqRealTimeVisuals", base.eqRealTimeVisuals),
                eqPreset = j.optString("eqPreset", base.eqPreset),
                eqBand60Hz = j.optDouble("eqBand60Hz", base.eqBand60Hz.toDouble()).toFloat(),
                eqBand230Hz = j.optDouble("eqBand230Hz", base.eqBand230Hz.toDouble()).toFloat(),
                eqBand910Hz = j.optDouble("eqBand910Hz", base.eqBand910Hz.toDouble()).toFloat(),
                eqBand3kHz = j.optDouble("eqBand3kHz", base.eqBand3kHz.toDouble()).toFloat(),
                eqBand14kHz = j.optDouble("eqBand14kHz", base.eqBand14kHz.toDouble()).toFloat(),
                powerSavingProfile = j.optString("powerSavingProfile", base.powerSavingProfile),
                ultraPowerSaverEnabled = j.optBoolean("ultraPowerSaverEnabled", base.ultraPowerSaverEnabled),
                ultraModeTrigger = j.optString("ultraModeTrigger", base.ultraModeTrigger),
                ultraTriggerThresholdPct = j.optInt("ultraTriggerThresholdPct", base.ultraTriggerThresholdPct),
                ultraTrueBlackOled = j.optBoolean("ultraTrueBlackOled", base.ultraTrueBlackOled),
                ultraStaticIndicators = j.optBoolean("ultraStaticIndicators", base.ultraStaticIndicators),
                ultraLimit30Fps = j.optBoolean("ultraLimit30Fps", base.ultraLimit30Fps),
                ultraDisableHaptics = j.optBoolean("ultraDisableHaptics", base.ultraDisableHaptics),
                ultraEssentialOnly = j.optBoolean("ultraEssentialOnly", base.ultraEssentialOnly),
                customAnimationsEnabled = j.optBoolean("customAnimationsEnabled", base.customAnimationsEnabled),
                animationPreset = j.optString("animationPreset", base.animationPreset),
                animExpandSpeed = j.optInt("animExpandSpeed", base.animExpandSpeed),
                animExpandBounce = j.optDouble("animExpandBounce", base.animExpandBounce.toDouble()).toFloat(),
                animExpandOvershoot = j.optDouble("animExpandOvershoot", base.animExpandOvershoot.toDouble()).toFloat(),
                animCollapseSpeed = j.optInt("animCollapseSpeed", base.animCollapseSpeed),
                animCollapseBounce = j.optDouble("animCollapseBounce", base.animCollapseBounce.toDouble()).toFloat(),
                animCollapseShrink = j.optDouble("animCollapseShrink", base.animCollapseShrink.toDouble()).toFloat(),
                animCornerSpeed = j.optInt("animCornerSpeed", base.animCornerSpeed),
                animSquashDurationMs = j.optInt("animSquashDurationMs", base.animSquashDurationMs),
                animPressScale = j.optDouble("animPressScale", base.animPressScale.toDouble()).toFloat(),
                animBubbleSpeed = j.optInt("animBubbleSpeed", base.animBubbleSpeed),
                animBubbleGapDp = j.optInt("animBubbleGapDp", base.animBubbleGapDp),
                animFadeSpeedMs = j.optInt("animFadeSpeedMs", base.animFadeSpeedMs),
                activityExpandedWidthDp = j.optInt("activityExpandedWidthDp", base.activityExpandedWidthDp),
                activityExpandedHeightDp = j.optInt("activityExpandedHeightDp", base.activityExpandedHeightDp),
                activityCompactWidthDp = j.optInt("activityCompactWidthDp", base.activityCompactWidthDp),
                activityCompactHeightDp = j.optInt("activityCompactHeightDp", base.activityCompactHeightDp)
            )
        }.getOrDefault(base)
    }

    private fun buildExtendedJson(s: IslandSettings): String {
        return JSONObject().apply {
            put("appLanguage", s.appLanguage)
            put("appThemeMode", s.appThemeMode)
            put("accentColorKey", s.accentColorKey)
            put("islandRefreshRateHz", s.islandRefreshRateHz)
            put("multitaskingMode", s.multitaskingMode)
            put("multitaskingBubbleShape", s.multitaskingBubbleShape)
            put("multitaskingBubbleWidthDp", s.multitaskingBubbleWidthDp)
            put("multitaskingCornerRadiusDp", s.multitaskingCornerRadiusDp)
            put("centeringShift", s.centeringShift)
            put("leftMultitaskingCircle", s.leftMultitaskingCircle)
            put("showSliderValues", s.showSliderValues)
            put("crashlyticsEnabled", s.crashlyticsEnabled)
            put("devPanelUnlocked", s.devPanelUnlocked)
            put("liquidRefraction", s.liquidRefraction.toDouble())
            put("liquidSurfaceTension", s.liquidSurfaceTension.toDouble())
            put("tapToExpand", s.tapToExpand)
            put("disableExpandInLandscape", s.disableExpandInLandscape)
            put("closeOnBackgroundTap", s.closeOnBackgroundTap)
            put("backgroundDimming", s.backgroundDimming)
            put("privacyIndicators", s.privacyIndicators)
            put("privacyDotPositionPx", s.privacyDotPositionPx)
            put("motionBlur", s.motionBlur)
            put("islandOpacity", s.islandOpacity.toDouble())
            put("islandColorArgb", s.islandColorArgb)
            put("dynamicLightGlow", s.dynamicLightGlow)
            put("lockRotation", s.lockRotation)
            put("hideWhenShadeOpen", s.hideWhenShadeOpen)
            put("islandGesturesEnabled", s.islandGesturesEnabled)
            put("gesturesOnlyWhenEmpty", s.gesturesOnlyWhenEmpty)
            put("adaptiveGesturesEnabled", s.adaptiveGesturesEnabled)
            put("swipeLeftAction", s.swipeLeftAction)
            put("swipeRightAction", s.swipeRightAction)
            put("swipeUpAction", s.swipeUpAction)
            put("swipeDownAction", s.swipeDownAction)
            put("doubleTapAction", s.doubleTapAction)
            put("longPressAction", s.longPressAction)
            put("eqShowEqualizer", s.eqShowEqualizer)
            put("eqRealTimeVisuals", s.eqRealTimeVisuals)
            put("eqPreset", s.eqPreset)
            put("eqBand60Hz", s.eqBand60Hz.toDouble())
            put("eqBand230Hz", s.eqBand230Hz.toDouble())
            put("eqBand910Hz", s.eqBand910Hz.toDouble())
            put("eqBand3kHz", s.eqBand3kHz.toDouble())
            put("eqBand14kHz", s.eqBand14kHz.toDouble())
            put("powerSavingProfile", s.powerSavingProfile)
            put("ultraPowerSaverEnabled", s.ultraPowerSaverEnabled)
            put("ultraModeTrigger", s.ultraModeTrigger)
            put("ultraTriggerThresholdPct", s.ultraTriggerThresholdPct)
            put("ultraTrueBlackOled", s.ultraTrueBlackOled)
            put("ultraStaticIndicators", s.ultraStaticIndicators)
            put("ultraLimit30Fps", s.ultraLimit30Fps)
            put("ultraDisableHaptics", s.ultraDisableHaptics)
            put("ultraEssentialOnly", s.ultraEssentialOnly)
            put("customAnimationsEnabled", s.customAnimationsEnabled)
            put("animationPreset", s.animationPreset)
            put("animExpandSpeed", s.animExpandSpeed)
            put("animExpandBounce", s.animExpandBounce.toDouble())
            put("animExpandOvershoot", s.animExpandOvershoot.toDouble())
            put("animCollapseSpeed", s.animCollapseSpeed)
            put("animCollapseBounce", s.animCollapseBounce.toDouble())
            put("animCollapseShrink", s.animCollapseShrink.toDouble())
            put("animCornerSpeed", s.animCornerSpeed)
            put("animSquashDurationMs", s.animSquashDurationMs)
            put("animPressScale", s.animPressScale.toDouble())
            put("animBubbleSpeed", s.animBubbleSpeed)
            put("animBubbleGapDp", s.animBubbleGapDp)
            put("animFadeSpeedMs", s.animFadeSpeedMs)
            put("activityExpandedWidthDp", s.activityExpandedWidthDp)
            put("activityExpandedHeightDp", s.activityExpandedHeightDp)
            put("activityCompactWidthDp", s.activityCompactWidthDp)
            put("activityCompactHeightDp", s.activityCompactHeightDp)
            put("widthDp", s.widthDp)
            put("heightDp", s.heightDp)
            put("topOffsetDp", s.topOffsetDp)
            put("horizontalOffsetDp", s.horizontalOffsetDp)
            put("cornerRadiusDp", s.cornerRadiusDp)
            put("expandedCornerRadiusDp", s.expandedCornerRadiusDp)
            put("alwaysShowPill", s.alwaysShowPill)
        }.toString()
    }

    val settingsFlow: Flow<IslandSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences -> mapPreferences(preferences) }

    suspend fun updateSettings(transform: (IslandSettings) -> IslandSettings) {
        context.dataStore.edit { preferences ->
            val currentSettings = mapPreferences(preferences)
            val newSettings = transform(currentSettings)

            preferences[PreferencesKeys.IS_ENABLED] = newSettings.isEnabled
            preferences[PreferencesKeys.WIDTH_DP] = newSettings.widthDp
            preferences[PreferencesKeys.HEIGHT_DP] = newSettings.heightDp
            preferences[PreferencesKeys.CORNER_RADIUS_DP] = newSettings.cornerRadiusDp
            preferences[PreferencesKeys.EXPANDED_CORNER_RADIUS_DP] = newSettings.expandedCornerRadiusDp
            preferences[PreferencesKeys.TOP_OFFSET_DP] = newSettings.topOffsetDp
            preferences[PreferencesKeys.VERTICAL_OFFSET_OPENING_DP] = newSettings.verticalOffsetOpeningDp
            preferences[PreferencesKeys.HORIZONTAL_OFFSET_DP] = newSettings.horizontalOffsetDp
            preferences[PreferencesKeys.IS_LEFT_CUTOUT] = newSettings.isLeftCutout
            preferences[PreferencesKeys.IS_TEARDROP_CUTOUT] = newSettings.isTeardropCutout
            preferences[PreferencesKeys.SHOW_OUTLINE] = newSettings.showOutline
            preferences[PreferencesKeys.OUTLINE_COLOR_ARGB] = newSettings.outlineColorArgb
            preferences[PreferencesKeys.OUTLINE_THICKNESS_DP] = newSettings.outlineThicknessDp
            preferences[PreferencesKeys.EXPANDED_SCALE] = newSettings.expandedScale
            preferences[PreferencesKeys.HAPTICS_ENABLED] = newSettings.hapticsEnabled
            preferences[PreferencesKeys.SHOW_CALLS] = newSettings.showCalls
            preferences[PreferencesKeys.SHOW_VOICE_MEMO] = newSettings.showVoiceMemo
            preferences[PreferencesKeys.SHOW_MEDIA] = newSettings.showMedia
            preferences[PreferencesKeys.SHOW_NOTIFICATIONS] = newSettings.showNotifications
            preferences[PreferencesKeys.SHOW_LIVE_PROGRESS] = newSettings.showLiveProgress
            preferences[PreferencesKeys.SHOW_SCREEN_RECORDING] = newSettings.showScreenRecording
            preferences[PreferencesKeys.SHOW_FLASHLIGHT] = newSettings.showFlashlight
            preferences[PreferencesKeys.SHOW_BLUETOOTH] = newSettings.showBluetooth
            preferences[PreferencesKeys.SHOW_CHARGING] = newSettings.showCharging
            preferences[PreferencesKeys.SHOW_RINGER_MODE] = newSettings.showRingerMode
            preferences[PreferencesKeys.SHOW_AIRPLANE_MODE] = newSettings.showAirplaneMode
            preferences[PreferencesKeys.SHOW_IN_LANDSCAPE] = newSettings.showInLandscape
            preferences[PreferencesKeys.ANIMATION_SPEED_MULTIPLIER] = newSettings.animationSpeed
            preferences[PreferencesKeys.BLOCKED_PACKAGES] = newSettings.blockedPackages
            preferences[PreferencesKeys.RESTART_ON_BOOT] = newSettings.restartOnBoot
            preferences[PreferencesKeys.BUBBLE_EFFECT_ENABLED] = newSettings.bubbleEffectEnabled
            preferences[PreferencesKeys.DISMISS_MEDIA_ON_PAUSE] = newSettings.dismissMediaOnPause
            preferences[PreferencesKeys.ALWAYS_SHOW_PILL] = newSettings.alwaysShowPill
            preferences[PreferencesKeys.SHOW_DUAL_ACTIVITY_IN_COMPACT] = newSettings.showDualActivityInCompact
            preferences[PreferencesKeys.SHOW_FPS] = newSettings.showFps
            preferences[PreferencesKeys.SHOW_WEATHER] = newSettings.showWeather
            preferences[PreferencesKeys.SHOW_NETWORK_SPEED] = newSettings.showNetworkSpeed
            preferences[PreferencesKeys.SHOW_ALARM] = newSettings.showAlarm
            preferences[PreferencesKeys.AUTO_DISMISS_NOTIFICATIONS] = newSettings.autoDismissNotifications
            preferences[PreferencesKeys.NOTIFICATION_AUTO_DISMISS_SECONDS] = newSettings.notificationAutoDismissSeconds
            preferences[PreferencesKeys.PERSISTENT_CHARGING] = newSettings.persistentCharging
            preferences[PreferencesKeys.NOTIFICATION_MAX_LINES] = newSettings.notificationMaxLines
            preferences[PreferencesKeys.WEATHER_CITY] = newSettings.weatherCity
            preferences[PreferencesKeys.FONT_KEY] = newSettings.fontKey
            preferences[PreferencesKeys.SWAP_ACTIVITIES_ON_SWIPE] = newSettings.swapActivitiesOnSwipe
            preferences[PreferencesKeys.BLURRED_ALBUM_ART_BACKGROUND] = newSettings.blurredAlbumArtBackground
            preferences[PreferencesKeys.DESIGN_LANGUAGE] = newSettings.designLanguage.name
            preferences[PreferencesKeys.GLASS_ENABLED] = (newSettings.designLanguage == DesignLanguage.GLASS)
            preferences[PreferencesKeys.GLASS_OPACITY] = newSettings.glassOpacity
            preferences[PreferencesKeys.MEDIA_WAVE_ANIMATION] = newSettings.mediaWaveAnimation
            preferences[PreferencesKeys.SPINNING_ALBUM_ART] = newSettings.spinningAlbumArt
            preferences[PreferencesKeys.WAVY_PROGRESS_BAR] = newSettings.wavyProgressBar
            preferences[PreferencesKeys.HIDE_ON_LOCK_SCREEN] = newSettings.hideOnLockScreen
            preferences[PreferencesKeys.EXPAND_DAMPING] = newSettings.expandDamping
            preferences[PreferencesKeys.EXPAND_STIFFNESS] = newSettings.expandStiffness
            preferences[PreferencesKeys.APPEAR_DAMPING] = newSettings.appearDamping
            preferences[PreferencesKeys.APPEAR_STIFFNESS] = newSettings.appearStiffness
            preferences[PreferencesKeys.SPLIT_DAMPING] = newSettings.splitDamping
            preferences[PreferencesKeys.SPLIT_STIFFNESS] = newSettings.splitStiffness
            preferences[PreferencesKeys.FEATURE_PRIORITIES] = newSettings.featurePriorities.joinToString(",")
            preferences[PreferencesKeys.MEDIA_CONTROL_ALIGNMENT] = newSettings.mediaControlAlignment
            preferences[PreferencesKeys.MEDIA_LAYERS_ORDER] = newSettings.mediaLayersOrder.joinToString(",")
            preferences[PreferencesKeys.DROIDBAR_ENABLED] = newSettings.droidbarEnabled
            preferences[PreferencesKeys.DROIDBAR_SHOW_FLASHLIGHT] = newSettings.droidbarShowFlashlight
            preferences[PreferencesKeys.DROIDBAR_SHOW_RING_MODE] = newSettings.droidbarShowRingMode
            preferences[PreferencesKeys.DROIDBAR_SHOW_SCREENSHOT] = newSettings.droidbarShowScreenshot
            preferences[PreferencesKeys.DROIDBAR_SHOW_WIFI] = newSettings.droidbarShowWifi
            preferences[PreferencesKeys.DROIDBAR_SHOW_MOBILE_DATA] = newSettings.droidbarShowMobileData
            preferences[PreferencesKeys.DROIDBAR_USE_MOST_USED_APPS] = newSettings.droidbarUseMostUsedApps
            preferences[PreferencesKeys.DROIDBAR_CUSTOM_APPS] = newSettings.droidbarCustomApps.joinToString(",")
            preferences[PreferencesKeys.DROIDBAR_BACKGROUND_OPACITY] = newSettings.droidbarBackgroundOpacity
            preferences[PreferencesKeys.DROIDBAR_MAX_HEIGHT_DP] = newSettings.droidbarMaxHeightDp
            preferences[PreferencesKeys.DROIDBAR_Y_OFFSET_DP] = newSettings.droidbarYOffsetDp
            preferences[PreferencesKeys.DROIDBAR_HAPTICS_ENABLED] = newSettings.droidbarHapticsEnabled
            preferences[PreferencesKeys.LIQUID_EXTENDED_JSON] = buildExtendedJson(newSettings)
        }
    }

    suspend fun exportConfigJson(): String {
        val current = settingsFlow.first()
        return buildExtendedJson(current)
    }

    suspend fun importConfigJson(jsonStr: String): Boolean {
        return try {
            val j = JSONObject(jsonStr)
            updateSettings { base ->
                base.copy(
                    widthDp = j.optInt("widthDp", base.widthDp),
                    heightDp = j.optInt("heightDp", base.heightDp),
                    topOffsetDp = j.optInt("topOffsetDp", base.topOffsetDp),
                    horizontalOffsetDp = j.optInt("horizontalOffsetDp", base.horizontalOffsetDp),
                    cornerRadiusDp = j.optInt("cornerRadiusDp", base.cornerRadiusDp),
                    expandedCornerRadiusDp = j.optInt("expandedCornerRadiusDp", base.expandedCornerRadiusDp),
                    alwaysShowPill = j.optBoolean("alwaysShowPill", base.alwaysShowPill),
                    appLanguage = j.optString("appLanguage", base.appLanguage),
                    appThemeMode = j.optString("appThemeMode", base.appThemeMode),
                    accentColorKey = j.optString("accentColorKey", base.accentColorKey),
                    islandRefreshRateHz = j.optInt("islandRefreshRateHz", base.islandRefreshRateHz),
                    multitaskingMode = j.optString("multitaskingMode", base.multitaskingMode),
                    multitaskingBubbleShape = j.optString("multitaskingBubbleShape", base.multitaskingBubbleShape),
                    liquidRefraction = j.optDouble("liquidRefraction", base.liquidRefraction.toDouble()).toFloat(),
                    liquidSurfaceTension = j.optDouble("liquidSurfaceTension", base.liquidSurfaceTension.toDouble()).toFloat(),
                    animationPreset = j.optString("animationPreset", base.animationPreset),
                    powerSavingProfile = j.optString("powerSavingProfile", base.powerSavingProfile)
                )
            }
            true
        } catch (_: Exception) {
            false
        }
    }
}
