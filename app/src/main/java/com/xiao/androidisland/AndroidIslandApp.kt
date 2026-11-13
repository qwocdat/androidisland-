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
        shizukuBridge = ShizukuBridge(this).also { it.initialize() }
        systemStateManager = SystemStateManager(this, shizukuBridge)
    }

    override fun onTerminate() {
        if (::systemStateManager.isInitialized) {
            systemStateManager.dispose()
        }
        if (::shizukuBridge.isInitialized) {
            shizukuBridge.dispose()
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
