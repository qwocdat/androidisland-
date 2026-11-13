package com.xiao.androidisland.core.permission

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

enum class ShizukuReadyState {
    NOT_INSTALLED,
    STOPPED,
    PERMISSION_REQUIRED,
    READY,
}

class ShizukuBridge {

    private val _state = MutableStateFlow(ShizukuReadyState.NOT_INSTALLED)
    val state: StateFlow<ShizukuReadyState> = _state.asStateFlow()

    private val binderReceived = Shizuku.OnBinderReceivedListener { refreshStatus() }
    private val binderDead = Shizuku.OnBinderDeadListener { _state.value = ShizukuReadyState.STOPPED }
    private val permListener = Shizuku.OnRequestPermissionResultListener { code, res ->
        if (code == REQUEST_CODE) {
            _state.value = if (res == PackageManager.PERMISSION_GRANTED) {
                ShizukuReadyState.READY
            } else {
                ShizukuReadyState.PERMISSION_REQUIRED
            }
        }
    }

    fun register() {
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceived)
            Shizuku.addBinderDeadListener(binderDead)
            Shizuku.addRequestPermissionResultListener(permListener)
            refreshStatus()
        } catch (t: Throwable) {
            Log.w(TAG, "Shizuku init skipped: ${t.message}")
            _state.value = ShizukuReadyState.NOT_INSTALLED
        }
    }

    fun unregister() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceived)
            Shizuku.removeBinderDeadListener(binderDead)
            Shizuku.removeRequestPermissionResultListener(permListener)
        } catch (_: Throwable) {}
    }

    fun refreshStatus(): ShizukuReadyState {
        val st = try {
            if (!Shizuku.pingBinder()) {
                ShizukuReadyState.STOPPED
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                ShizukuReadyState.READY
            } else {
                ShizukuReadyState.PERMISSION_REQUIRED
            }
        } catch (_: Throwable) {
            ShizukuReadyState.NOT_INSTALLED
        }
        _state.value = st
        return st
    }

    fun requestPermission(): Boolean {
        return try {
            if (!Shizuku.pingBinder()) return false
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                _state.value = ShizukuReadyState.READY
                true
            } else {
                Shizuku.requestPermission(REQUEST_CODE)
                false
            }
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun setSystemStatusBarHidden(hidden: Boolean): Boolean {
        val cmd = if (hidden) {
            "settings put global policy_control immersive.status=*"
        } else {
            "settings put global policy_control null"
        }
        return executeCommand(cmd)
    }

    suspend fun collapseStockStatusBar(): Boolean {
        return executeCommand("cmd statusbar collapse")
    }

    suspend fun captureSingleScreenFrame(): Bitmap? = withContext(Dispatchers.IO) {
        if (refreshStatus() != ShizukuReadyState.READY) return@withContext null
        return@withContext try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java,
            )
            method.isAccessible = true
            val proc = method.invoke(null, arrayOf("sh", "-c", "screencap -p"), null, null) as? Process
                ?: return@withContext null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = 2
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bmp = proc.inputStream.buffered().use { BitmapFactory.decodeStream(it, null, opts) }
            proc.waitFor()
            bmp
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun executeCommand(cmd: String): Boolean = withContext(Dispatchers.IO) {
        if (refreshStatus() != ShizukuReadyState.READY) return@withContext false
        return@withContext try {
            val method = Shizuku::class.java.getDeclaredMethod(
                "newProcess",
                Array<String>::class.java,
                Array<String>::class.java,
                String::class.java,
            )
            method.isAccessible = true
            val proc = method.invoke(null, arrayOf("sh", "-c", cmd), null, null) as? Process
                ?: return@withContext false
            proc.waitFor() == 0
        } catch (_: Throwable) {
            false
        }
    }

    companion object {
        private const val TAG = "ShizukuBridge"
        private const val REQUEST_CODE = 2027
    }
}
