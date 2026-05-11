package com.cosmos.launcher

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process

class AppRepository(private val context: Context) {
    private val packageManager = context.packageManager
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    private val clusterMap = mapOf(
        // Work
        "com.google.android.gm" to "work",
        "com.google.android.calendar" to "work",
        "com.slack" to "work",
        "notion.id" to "work",
        "com.microsoft.office.word" to "work",
        "com.microsoft.office.excel" to "work",

        // Social
        "com.whatsapp" to "social",
        "com.instagram.android" to "social",
        "org.telegram.messenger" to "social",
        "com.facebook.katana" to "social",
        "com.twitter.android" to "social",

        // Creative
        "com.spotify.music" to "creative",
        "com.google.android.youtube" to "creative",
        "com.figma.android" to "creative",
        "com.adobe.lightroom" to "creative",
        "com.adobe.photoshop" to "creative",

        // Utility
        "com.google.android.maps" to "utility",
        "com.google.android.apps.maps" to "utility",
        "com.android.settings" to "utility",
        "com.google.android.apps.docs" to "utility",
        "com.google.android.apps.docs.editors.sheets" to "utility",
        "com.google.android.apps.docs.editors.slides" to "utility",

        // Finance
        "com.revolut.revolutmoney" to "finance",
        "com.paypal.android.p2pmobile" to "finance",
        "com.coinbase.android" to "finance",
        "com.andrognito.cryptonator" to "finance"
    )

    fun loadInstalledApps(): List<AppNode> {
        val apps = mutableListOf<AppNode>()
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        // Filter out system apps and Cosmos itself
        installedApps
            .filter { it.packageName != context.packageName }
            .filter { (it.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) == 0 }
            .forEach { appInfo ->
                try {
                    val label = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo)
                    val cluster = clusterMap[appInfo.packageName] ?: guessCluster(appInfo.packageName)

                    val node = AppNode(
                        packageName = appInfo.packageName,
                        label = label,
                        icon = icon,
                        cluster = cluster
                    )

                    // Load usage stats
                    updateUsageStats(node)
                    node.refreshImportance()

                    apps.add(node)
                } catch (e: Exception) {
                    // Ignore apps that can't be loaded
                }
            }

        return apps.sortedByDescending { it.importance }
    }

    private fun updateUsageStats(node: AppNode) {
        try {
            val now = System.currentTimeMillis()
            val thirtyDaysAgo = now - (30 * 24 * 60 * 60 * 1000L)

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                thirtyDaysAgo,
                now
            )

            val appStats = stats.find { it.packageName == node.packageName }
            if (appStats != null) {
                node.usageTimeMs = appStats.totalTimeInForeground
                node.lastUsedMs = appStats.lastTimeUsed
                node.launchCount = appStats.launchCount
            }
        } catch (e: Exception) {
            // Permission not granted or error reading stats
        }
    }

    private fun guessCluster(packageName: String): String {
        return when {
            packageName.contains("mail") || packageName.contains("email") || packageName.contains("calendar") || packageName.contains("slack") -> "work"
            packageName.contains("whatsapp") || packageName.contains("messenger") || packageName.contains("telegram") || packageName.contains("instagram") || packageName.contains("twitter") -> "social"
            packageName.contains("spotify") || packageName.contains("music") || packageName.contains("youtube") || packageName.contains("video") || packageName.contains("adobe") -> "creative"
            packageName.contains("bank") || packageName.contains("finance") || packageName.contains("paypal") || packageName.contains("revolut") || packageName.contains("crypto") -> "finance"
            else -> "utility"
        }
    }
}
