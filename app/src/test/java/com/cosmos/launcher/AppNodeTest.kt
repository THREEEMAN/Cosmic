package com.cosmos.launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppNodeTest {

    private val dayMs = 24 * 60 * 60 * 1000L
    private val now = 1_750_000_000_000L

    private fun node(
        launchCount: Int = 0,
        usageTimeMs: Long = 0L,
        lastUsedMs: Long = now
    ) = AppNode(packageName = "com.example.app", label = "Example", icon = null).apply {
        this.launchCount = launchCount
        this.usageTimeMs = usageTimeMs
        this.lastUsedMs = lastUsedMs
    }

    @Test
    fun `importance floors at 0_05 for an unused stale app`() {
        val n = node(launchCount = 0, usageTimeMs = 0L, lastUsedMs = now - 365 * dayMs)
        n.refreshImportance(now)
        assertEquals(0.05f, n.importance, 1e-6f)
    }

    @Test
    fun `importance caps at 1_0 for a heavily used recent app`() {
        val n = node(
            launchCount = 10_000,
            usageTimeMs = 100 * 60 * 60 * 1000L, // 100 hours, far past the 2h normalizer
            lastUsedMs = now
        )
        n.refreshImportance(now)
        assertEquals(1f, n.importance, 1e-6f)
    }

    @Test
    fun `importance combines weighted launch, usage and recency scores`() {
        // 50 launches -> launchScore 0.5; 1 hour -> usageScore 0.5; used just now -> recencyScore 1.0
        val n = node(launchCount = 50, usageTimeMs = 60 * 60 * 1000L, lastUsedMs = now)
        n.refreshImportance(now)
        val expected = 0.5f * 0.45f + 0.5f * 0.35f + 1f * 0.20f
        assertEquals(expected, n.importance, 1e-4f)
    }

    @Test
    fun `recency decays with days since last use`() {
        val fresh = node(launchCount = 10, lastUsedMs = now)
        val stale = node(launchCount = 10, lastUsedMs = now - 30 * dayMs)
        fresh.refreshImportance(now)
        stale.refreshImportance(now)
        assertTrue(
            "app used today should score higher than one used 30 days ago",
            fresh.importance > stale.importance
        )
    }

    @Test
    fun `visual properties scale linearly with importance`() {
        val n = node(launchCount = 10_000, usageTimeMs = 100 * 60 * 60 * 1000L, lastUsedMs = now)
        n.refreshImportance(now)
        // importance == 1.0
        assertEquals(40f, n.radius, 1e-4f)          // 15 + 1.0 * 25
        assertEquals(1f, n.glowIntensity, 1e-4f)    // 0.3 + 1.0 * 0.7
        assertEquals(1f, n.tetherOpacity, 1e-4f)    // 0.2 + 1.0 * 0.8
        assertEquals(0.03f, n.orbitSpeed, 1e-4f)    // 0.005 + 1.0 * 0.025

        val floor = node(lastUsedMs = now - 365 * dayMs)
        floor.refreshImportance(now)
        // importance == 0.05
        assertEquals(16.25f, floor.radius, 1e-4f)
        assertEquals(0.335f, floor.glowIntensity, 1e-4f)
    }

    @Test
    fun `distanceTo another node uses euclidean distance`() {
        val a = node().apply { x = 0f; y = 0f }
        val b = node().apply { x = 3f; y = 4f }
        assertEquals(5f, a.distanceTo(b), 1e-6f)
        assertEquals(5f, b.distanceTo(a), 1e-6f)
    }

    @Test
    fun `distanceTo a point uses euclidean distance`() {
        val a = node().apply { x = 1f; y = 1f }
        assertEquals(5f, a.distanceTo(4f, 5f), 1e-6f)
        assertEquals(0f, a.distanceTo(1f, 1f), 1e-6f)
    }
}
