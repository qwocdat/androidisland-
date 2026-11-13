package com.xiao.androidisland

import android.app.Application
import android.content.Context
import com.xiao.androidisland.core.permission.ShizukuBridge
import com.xiao.androidisland.core.system.SystemStateManager

class AndroidIslandApp : Application() {

    lateinit var shizukuBridge: ShizukuBridge
        private set

    lateinit var systemStateManager: SystemStateManager
        private set

    override fun onCreate() {
        super.onCreate()
        shizukuBridge = ShizukuBridge().also { it.register() }
        systemStateManager = SystemStateManager(this, shizukuBridge).also { it.startObserving() }
    }

    override fun onTerminate() {
        if (::systemStateManager.isInitialized) {
            systemStateManager.stopObserving()
        }
        if (::shizukuBridge.isInitialized) {
            shizukuBridge.unregister()
        }
        super.onTerminate()
    }

    companion object {
        fun system(context: Context): SystemStateManager {
            val app = context.applicationContext as AndroidIslandApp
            return app.systemStateManager
        }

        fun systemOrNull(context: Context): SystemStateManager? {
            return (context.applicationContext as? AndroidIslandApp)?.systemStateManager
        }
    }
}
