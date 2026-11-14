package com.droidbubble.app.model

val DEFAULT_FEATURE_PRIORITIES = listOf(
    "CALL",
    "VOICE_MEMO",
    "MEDIA",
    "PROGRESS",
    "RECORDING",
    "NOTIFICATION",
    "FLASHLIGHT",
    "PERSISTENT_CHARGING",
    "ALARM",
    "FPS",
    "WEATHER",
    "NETWORK_SPEED"
)

val DEFAULT_MEDIA_LAYERS = listOf(
    "COVER_AND_TITLE",
    "SLIDER",
    "CONTROLS"
)

enum class DesignLanguage(val id: String, val displayName: String) {
    DROIDBUBBLE("DROIDBUBBLE", "Androidisland"),
    GLASS("GLASS", "Kính trong suốt"),
    MATERIAL3("MATERIAL3", "Material 3")
}

data class IslandSettings(
    val isEnabled: Boolean = true,
    val widthDp: Int = 124,
    val heightDp: Int = 38,
    val cornerRadiusDp: Int = 22,
    val expandedCornerRadiusDp: Int = 38,
    val topOffsetDp: Int = 12,
    val verticalOffsetOpeningDp: Int = 0,
    val horizontalOffsetDp: Int = 0,
    val isLeftCutout: Boolean = false,
    val isTeardropCutout: Boolean = false,
    val showOutline: Boolean = true,
    val outlineColorArgb: Long = 0x33FFFFFF,
    val outlineThicknessDp: Float = 1.5f,
    val expandedScale: Float = 1.1f,
    val hapticsEnabled: Boolean = true,
    val showCalls: Boolean = true,
    val showVoiceMemo: Boolean = true,
    val showMedia: Boolean = true,
    val showNotifications: Boolean = true,
    val showLiveProgress: Boolean = true,
    val showScreenRecording: Boolean = true,
    val showFlashlight: Boolean = true,
    val showBluetooth: Boolean = true,
    val showCharging: Boolean = true,
    val showRingerMode: Boolean = true,
    val showAirplaneMode: Boolean = true,
    val showInLandscape: Boolean = false,
    val animationSpeed: Float = 0.85f,
    val blockedPackages: Set<String> = emptySet(),
    val restartOnBoot: Boolean = true,
    val bubbleEffectEnabled: Boolean = true,
    val dismissMediaOnPause: Boolean = false,
    val alwaysShowPill: Boolean = true,
    val showDualActivityInCompact: Boolean = true,
    val showFps: Boolean = false,
    val showWeather: Boolean = false,
    val showNetworkSpeed: Boolean = false,
    val showAlarm: Boolean = false,
    val autoDismissNotifications: Boolean = false,
    val notificationAutoDismissSeconds: Int = 3,
    val persistentCharging: Boolean = false,
    val notificationMaxLines: Int = 2,
    val weatherCity: String = "",
    val fontKey: String = "DEFAULT",
    val swapActivitiesOnSwipe: Boolean = true,
    val blurredAlbumArtBackground: Boolean = false,
    val designLanguage: DesignLanguage = DesignLanguage.MATERIAL3,
    val glassEnabled: Boolean = false,
    val glassOpacity: Int = 25,
    val mediaWaveAnimation: Boolean = true,
    val spinningAlbumArt: Boolean = false,
    val wavyProgressBar: Boolean = true,
    val hideOnLockScreen: Boolean = false,
    val expandDamping: Float = 0.74f,
    val expandStiffness: Float = 380f,
    val appearDamping: Float = 0.6f,
    val appearStiffness: Float = 280f,
    val splitDamping: Float = 0.63f,
    val splitStiffness: Float = 320f,
    val featurePriorities: List<String> = DEFAULT_FEATURE_PRIORITIES,
    val mediaControlAlignment: String = "CENTER",
    val mediaLayersOrder: List<String> = DEFAULT_MEDIA_LAYERS,
    val droidbarEnabled: Boolean = false,
    val droidbarShowFlashlight: Boolean = true,
    val droidbarShowRingMode: Boolean = true,
    val droidbarShowScreenshot: Boolean = true,
    val droidbarShowWifi: Boolean = true,
    val droidbarShowMobileData: Boolean = true,
    val droidbarUseMostUsedApps: Boolean = true,
    val droidbarCustomApps: List<String> = emptyList(),
    val droidbarBackgroundOpacity: Float = 1.0f,
    val droidbarMaxHeightDp: Int = 600,
    val droidbarYOffsetDp: Int = 0,
    val droidbarHapticsEnabled: Boolean = true,
    // --- New Liquid Island & Video Features ---
    val appLanguage: String = "vi", // Default language: Vietnamese (Tiếng Việt)
    val appThemeMode: String = "LIGHT", // SYSTEM, DARK, OLED, LIGHT
    val accentColorKey: String = "BLUE", // BLUE, PURPLE, PINK, RED, ORANGE, GREEN, CYAN
    val islandRefreshRateHz: Int = 165, // 60, 90, 120, 165
    val multitaskingMode: String = "FLUID_MERGE", // CARDS, CIRCLE, FLUID_MERGE, APP_LAUNCH
    val multitaskingBubbleShape: String = "CIRCLE", // CIRCLE, SQUIRCLE, ADAPTIVE, CUSTOM
    val multitaskingBubbleWidthDp: Int = 46,
    val multitaskingCornerRadiusDp: Int = 16,
    val centeringShift: Boolean = false,
    val leftMultitaskingCircle: Boolean = false,
    val showSliderValues: Boolean = true,
    val crashlyticsEnabled: Boolean = true,
    val devPanelUnlocked: Boolean = false,
    // Liquid Material
    val liquidRefraction: Float = 1.14f,
    val liquidSurfaceTension: Float = 4.00f,
    // Interaction & Customization
    val tapToExpand: Boolean = true,
    val disableExpandInLandscape: Boolean = true,
    val closeOnBackgroundTap: Boolean = true,
    val backgroundDimming: Boolean = false,
    val privacyIndicators: Boolean = true,
    val privacyDotPositionPx: Int = 3,
    val motionBlur: Boolean = false,
    val islandOpacity: Float = 1.0f,
    val islandColorArgb: Long = 0xFF000000,
    val dynamicLightGlow: Boolean = true,
    val lockRotation: Boolean = false,
    val hideWhenShadeOpen: Boolean = false,
    // Gestures & Shortcuts
    val islandGesturesEnabled: Boolean = true,
    val gesturesOnlyWhenEmpty: Boolean = false,
    val adaptiveGesturesEnabled: Boolean = true,
    val swipeLeftAction: String = "VOLUME_DOWN",
    val swipeRightAction: String = "VOLUME_UP",
    val swipeUpAction: String = "DISMISS",
    val swipeDownAction: String = "NOTIFICATION_SHADE",
    val doubleTapAction: String = "RECENT_APPS",
    val longPressAction: String = "FLASHLIGHT",
    // Audio Equalizer (5-band hardware EQ)
    val eqShowEqualizer: Boolean = true,
    val eqRealTimeVisuals: Boolean = true,
    val eqPreset: String = "CUSTOM",
    val eqBand60Hz: Float = 0.0f,
    val eqBand230Hz: Float = 0.0f,
    val eqBand910Hz: Float = 0.0f,
    val eqBand3kHz: Float = 0.0f,
    val eqBand14kHz: Float = 0.0f,
    // Power Saver
    val powerSavingProfile: String = "STANDARD", // STANDARD, ECO, ULTRA
    val ultraPowerSaverEnabled: Boolean = false,
    val ultraModeTrigger: String = "SYSTEM_OR_LOW", // SYSTEM_OR_LOW, SYSTEM_SAVER, LOW_BATTERY
    val ultraTriggerThresholdPct: Int = 15,
    val ultraTrueBlackOled: Boolean = true,
    val ultraStaticIndicators: Boolean = true,
    val ultraLimit30Fps: Boolean = true,
    val ultraDisableHaptics: Boolean = true,
    val ultraEssentialOnly: Boolean = false,
    // Custom Animation Settings Sub-Screen
    val customAnimationsEnabled: Boolean = true,
    val animationPreset: String = "SMOOTH", // DEFAULT, BOUNCY, SMOOTH, SNAPPY, ELASTIC, GENTLE
    val animExpandSpeed: Int = 140,
    val animExpandBounce: Float = 0.85f,
    val animExpandOvershoot: Float = 1.03f,
    val animCollapseSpeed: Int = 202,
    val animCollapseBounce: Float = 0.90f,
    val animCollapseShrink: Float = 0.98f,
    val animCornerSpeed: Int = 170,
    val animSquashDurationMs: Int = 800,
    val animPressScale: Float = 0.91f,
    val animBubbleSpeed: Int = 396,
    val animBubbleGapDp: Int = 6,
    val animFadeSpeedMs: Int = 500,
    // Per-Activity Geometry Sizes
    val activityExpandedWidthDp: Int = 300,
    val activityExpandedHeightDp: Int = 80,
    val activityCompactWidthDp: Int = 170,
    val activityCompactHeightDp: Int = 38,
    // Hide Native OS Island (HyperIsland / Fluid Cloud / Atomic Island / Magic Capsule)
    val hideNativeOsIsland: Boolean = true,
    val nativeIslandMaskBoost: Boolean = true,
    val hideStatusBar: Boolean = false,
    val lockScreenAuthMode: String = "FACE_ID"
) {
    val isGlass: Boolean get() = designLanguage == DesignLanguage.GLASS || glassEnabled
    val isMaterial3: Boolean get() = designLanguage == DesignLanguage.MATERIAL3
}
