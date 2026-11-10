package com.droidbubble.app.tasks

enum class ToggleActionType {
    TOGGLE,
    TURN_ON,
    TURN_OFF
}

enum class RingerActionType {
    SET_RING,
    SET_VIBRATE,
    TOGGLE
}

sealed class TaskAction {
    data class OpenApp(val packageName: String, val appLabel: String) : TaskAction()
    data class Wifi(val type: ToggleActionType) : TaskAction()
    data class MobileData(val type: ToggleActionType) : TaskAction()
    data class Flashlight(val type: ToggleActionType) : TaskAction()
    data class LowPower(val type: ToggleActionType) : TaskAction()
    data class AirplaneMode(val type: ToggleActionType) : TaskAction()
    data class Bluetooth(val type: ToggleActionType) : TaskAction()
    data class RingerMode(val type: RingerActionType) : TaskAction()

    fun getDisplayName(): String {
        return when (this) {
            is OpenApp -> "Mở $appLabel"
            is Wifi -> when (type) {
                ToggleActionType.TOGGLE -> "Bật/Tắt Wi-Fi"
                ToggleActionType.TURN_ON -> "Bật Wi-Fi"
                ToggleActionType.TURN_OFF -> "Tắt Wi-Fi"
            }
            is MobileData -> when (type) {
                ToggleActionType.TOGGLE -> "Bật/Tắt Dữ liệu di động"
                ToggleActionType.TURN_ON -> "Bật Dữ liệu di động"
                ToggleActionType.TURN_OFF -> "Tắt Dữ liệu di động"
            }
            is Flashlight -> when (type) {
                ToggleActionType.TOGGLE -> "Bật/Tắt Đèn pin"
                ToggleActionType.TURN_ON -> "Bật Đèn pin"
                ToggleActionType.TURN_OFF -> "Tắt Đèn pin"
            }
            is LowPower -> when (type) {
                ToggleActionType.TOGGLE -> "Bật/Tắt Tiết kiệm pin"
                ToggleActionType.TURN_ON -> "Bật Tiết kiệm pin"
                ToggleActionType.TURN_OFF -> "Tắt Tiết kiệm pin"
            }
            is AirplaneMode -> when (type) {
                ToggleActionType.TOGGLE -> "Bật/Tắt Chế độ máy bay"
                ToggleActionType.TURN_ON -> "Bật Chế độ máy bay"
                ToggleActionType.TURN_OFF -> "Tắt Chế độ máy bay"
            }
            is Bluetooth -> when (type) {
                ToggleActionType.TOGGLE -> "Bật/Tắt Bluetooth"
                ToggleActionType.TURN_ON -> "Bật Bluetooth"
                ToggleActionType.TURN_OFF -> "Tắt Bluetooth"
            }
            is RingerMode -> when (type) {
                RingerActionType.SET_RING -> "Đặt chế độ Chuông"
                RingerActionType.SET_VIBRATE -> "Đặt chế độ Rung"
                RingerActionType.TOGGLE -> "Chuyển đổi Chuông / Rung"
            }
        }
    }
}

data class TaskShortcut(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val iconName: String = "Bolt",
    val showInBubble: Boolean = true,
    val actions: List<TaskAction> = emptyList()
)

