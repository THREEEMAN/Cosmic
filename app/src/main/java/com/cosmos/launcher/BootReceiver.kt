package com.cosmos.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            // Launcher is already set as default, nothing extra needed on boot
            // But you could restore state or start services here if needed
            android.util.Log.d("BootReceiver", "Boot completed, Cosmos Launcher ready")
        }
    }
}
