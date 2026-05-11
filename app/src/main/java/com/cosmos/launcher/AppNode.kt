package com.cosmos.launcher

import android.graphics.drawable.Drawable
import kotlin.math.sqrt

data class AppNode(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val cluster: String = "utility"
) {
    // Physics state
    var x: Float = 0f
    var y: Float = 0f
    var vx: Float = 0f
    var vy: Float = 0f
    var fx: Float = 0f
    var fy: Float = 0f
    var isDragging: Boolean = false
    var dragOffsetX: Float = 0f
    var dragOffsetY: Float = 0f

    // Importance scoring
    var launchCount: Int = 0
    var usageTimeMs: Long = 0L
    var lastUsedMs: Long = System.currentTimeMillis()
    var importance: Float = 0.1f

    // Visual properties
    var radius: Float = 20f
    var glowIntensity: Float = 0.5f
    var tetherOpacity: Float = 0.3f
    var isVisible: Boolean = true

    // Orbital properties
    var orbitRadius: Float = 100f
    var orbitAngle: Float = 0f
    var orbitSpeed: Float = 0.02f
    var targetOrbitRadius: Float = 100f
    var targetOrbitAngle: Float = 0f

    fun refreshImportance() {
        val now = System.currentTimeMillis()
        val dayMs = 24 * 60 * 60 * 1000L
        val thirtyDaysMs = 30 * dayMs

        // Launch score: normalized by typical max launches
        val launchScore = (launchCount / 100f).coerceIn(0f, 1f)

        // Usage score: normalized by 2 hours
        val usageScore = (usageTimeMs / (2 * 60 * 60 * 1000f)).coerceIn(0f, 1f)

        // Recency score: exponential decay
        val daysSinceUsed = (now - lastUsedMs).toFloat() / dayMs
        val recencyScore = (1f / (1f + daysSinceUsed * 0.5f))

        // Weighted importance
        importance = (launchScore * 0.45f + usageScore * 0.35f + recencyScore * 0.20f)
            .coerceIn(0.05f, 1f)

        // Update visual properties based on importance
        radius = 15f + (importance * 25f)
        glowIntensity = 0.3f + (importance * 0.7f)
        tetherOpacity = 0.2f + (importance * 0.8f)
        orbitSpeed = 0.005f + (importance * 0.025f)
    }

    fun distanceTo(other: AppNode): Float {
        val dx = x - other.x
        val dy = y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    fun distanceTo(px: Float, py: Float): Float {
        val dx = x - px
        val dy = y - py
        return sqrt(dx * dx + dy * dy)
    }
}
