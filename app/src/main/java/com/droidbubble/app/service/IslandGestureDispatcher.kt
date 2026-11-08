package com.droidbubble.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.SystemClock
import android.view.KeyEvent
import com.droidbubble.app.island.IslandStateManager
import com.droidbubble.app.model.IslandSettings

object IslandGestureDispatcher {

    fun executeGestureAction(context: Context, actionKey: String, settings: IslandSettings): Boolean {
        if (!settings.islandGesturesEnabled) return false
        if (settings.gesturesOnlyWhenEmpty && IslandStateManager.isOccupied) return false

        return when (actionKey) {
            "VOLUME_DOWN" -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                am?.adjustVolume(AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                true
            }
            "VOLUME_UP" -> {
                val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                am?.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                true
            }
            "DISMISS" -> {
                if (IslandStateManager.isNotificationActive) {
                    IslandStateManager.dismissCurrentNotification()
                } else if (IslandStateManager.isProgressActive) {
                    IslandStateManager.dismissProgress()
                } else if (IslandStateManager.isExpanded.value) {
                    IslandStateManager.collapse()
                }
                true
            }
            "NOTIFICATION_SHADE" -> {
                if (!IslandAccessibilityService.openNotificationShade()) {
                    IslandStateManager.expand()
                }
                true
            }
            "QUICK_SETTINGS" -> {
                IslandAccessibilityService.openQuickSettings()
                true
            }
            "RECENT_APPS" -> {
                val a11y = IslandAccessibilityService.instance
                if (a11y != null) {
                    a11y.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                } else {
                    IslandStateManager.toggleExpand()
                }
                true
            }
            "FLASHLIGHT" -> {
                val cm = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                runCatching {
                    val id = cm?.cameraIdList?.firstOrNull {
                        cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                    }
                    if (id != null) {
                        val currentOn = IslandStateManager.isFlashlightOn.value
                        cm.setTorchMode(id, !currentOn)
                    }
                }
                true
            }
            "SCREENSHOT" -> {
                val a11y = IslandAccessibilityService.instance
                a11y?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                true
            }
            "PLAY_PAUSE" -> {
                val cb = IslandStateManager.onPlayPause
                if (cb != null) {
                    cb.invoke()
                } else {
                    val am = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    val now = SystemClock.uptimeMillis()
                    am?.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0))
                    am?.dispatchMediaKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, 0))
                }
                true
            }
            "VOICE_MEMO" -> {
                VoiceMemoManager.toggleRecording(context)
                true
            }
            "EXPAND" -> {
                IslandStateManager.toggleExpand()
                true
            }
            else -> false
        }
    }
}
