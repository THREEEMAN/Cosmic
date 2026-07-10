package com.cosmos.launcher

import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process

class AppRepository(private val context: Context) {
    private val packageManager = context.packageManager
    private val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

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
                    val cluster = ClusterClassifier.classify(appInfo.packageName)

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

}
