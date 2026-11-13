package com.xiao.androidisland.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class IslandBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            AndroidIslandOverlayService.startOrUpdate(context)
        }
    }
}
