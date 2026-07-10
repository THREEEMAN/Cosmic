package com.cosmos.launcher

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CosmosPhysicsTest {

    private lateinit var physics: CosmosPhysics

    private val centerX = 500f
    private val centerY = 800f
    private val width = 1000f
    private val height = 1600f

    @Before
    fun setUp() {
        physics = CosmosPhysics()
    }

    private fun node(
        packageName: String,
        x: Float = 0f,
        y: Float = 0f,
        cluster: String = "utility"
    ) = AppNode(packageName = packageName, label = packageName, icon = null, cluster = cluster).apply {
        this.x = x
        this.y = y
        // Park the orbital spring target on the current position so individual
        // forces can be observed in isolation.
        targetOrbitRadius = sqrt(x * x + y * y)
        targetOrbitAngle = kotlin.math.atan2(y, x)
    }

    private fun speedOf(n: AppNode) = sqrt(n.vx * n.vx + n.vy * n.vy)

    private fun tick(nodes: List<AppNode>, times: Int = 1) {
        repeat(times) { physics.tick(nodes, centerX, centerY, width, height) }
    }

    @Test
    fun `velocity is clamped to the maximum speed limit`() {
        val n = node("a").apply { vx = 1000f; vy = -1000f }
        tick(listOf(n))
        assertTrue("speed ${speedOf(n)} should be clamped to 12", speedOf(n) <= 12.001f)
    }

    @Test
    fun `dragging node is not moved by the simulation`() {
        val n = node("a", x = 100f, y = 200f).apply {
            isDragging = true
            vx = 5f
            vy = -3f
        }
        val other = node("b", x = 105f, y = 200f) // overlapping, would normally repel
        tick(listOf(n, other), times = 10)

        assertEquals(100f, n.x, 1e-6f)
        assertEquals(200f, n.y, 1e-6f)
        assertEquals(5f, n.vx, 1e-6f)
        assertEquals(-3f, n.vy, 1e-6f)
        assertEquals(0f, n.fx, 1e-6f)
        assertEquals(0f, n.fy, 1e-6f)
    }

    @Test
    fun `overlapping nodes repel each other`() {
        val a = node("a", x = 10f, y = 0f)
        val b = node("b", x = -10f, y = 0f)
        val before = a.distanceTo(b)

        tick(listOf(a, b))

        assertTrue("overlapping nodes should separate", a.distanceTo(b) > before)
        assertTrue("a should be pushed right", a.x > 10f)
        assertTrue("b should be pushed left", b.x < -10f)
    }

    @Test
    fun `distant nodes do not repel`() {
        val a = node("a", x = 250f, y = 0f)
        val b = node("b", x = -250f, y = 0f)

        tick(listOf(a, b))

        // Only the (tiny) cluster gravity acts; velocities stay near zero.
        assertTrue("no significant force expected, got speed ${speedOf(a)}", speedOf(a) < 0.01f)
        assertTrue("no significant force expected, got speed ${speedOf(b)}", speedOf(b) < 0.01f)
    }

    @Test
    fun `off-screen node is pushed back toward the visible area`() {
        val n = node("a", x = centerX + width, y = centerY) // well past the right bound
        tick(listOf(n))
        assertTrue("node beyond the right edge should accelerate left", n.vx < 0f)
    }

    @Test
    fun `orbit angle advances and wraps at two pi`() {
        val n = node("a", x = 100f, y = 0f).apply {
            orbitAngle = 2 * PI.toFloat() - 0.001f
            orbitSpeed = 0.05f
        }
        tick(listOf(n))
        assertTrue("angle should wrap below 2*pi", n.orbitAngle < 2 * PI.toFloat())
        assertTrue("angle should stay positive after wrapping", n.orbitAngle >= 0f)
    }

    @Test
    fun `assignOrbits places cluster members evenly on a shared circle`() {
        val nodes = (0 until 4).map { node("app$it", cluster = "social") }

        physics.assignOrbits(nodes, centerX, centerY)

        val radius = nodes[0].targetOrbitRadius
        nodes.forEach { assertEquals(radius, it.targetOrbitRadius, 1e-6f) }

        nodes.forEachIndexed { index, n ->
            val expectedAngle = index * 2 * PI.toFloat() / nodes.size
            assertEquals(expectedAngle, n.targetOrbitAngle, 1e-4f)

            val distFromCenter = sqrt(
                (n.x - centerX) * (n.x - centerX) + (n.y - centerY) * (n.y - centerY)
            )
            assertEquals(radius, distFromCenter, 1e-2f)
        }
    }

    @Test
    fun `assignOrbits keeps every cluster radius in the intended 80 to 240 range`() {
        // Regression guard: String.hashCode() can be negative, and a plain
        // `%` used to produce zero or negative radii for some cluster names.
        val clusters = listOf("work", "social", "creative", "utility", "finance")
        val nodes = clusters.map { node(it, cluster = it) }

        physics.assignOrbits(nodes, centerX, centerY)

        nodes.forEach { n ->
            assertTrue(
                "cluster ${n.cluster} got radius ${n.targetOrbitRadius}, expected 80..240",
                n.targetOrbitRadius in 80f..240f
            )
        }
    }

    @Test
    fun `simulation stays stable and bounded over many ticks`() {
        val clusters = listOf("work", "social", "creative", "utility", "finance")
        val nodes = (0 until 20).map { i ->
            node("app$i", cluster = clusters[i % clusters.size]).apply { refreshImportance() }
        }
        physics.assignOrbits(nodes, centerX, centerY)

        tick(nodes, times = 300)

        nodes.forEach { n ->
            assertTrue("x must stay finite", n.x.isFinite())
            assertTrue("y must stay finite", n.y.isFinite())
            assertTrue("speed ${speedOf(n)} must respect the limit", speedOf(n) <= 12.001f)
            // Nodes must not fly arbitrarily far off-screen.
            assertTrue("node drifted too far: (${n.x}, ${n.y})", abs(n.x) < 10_000f && abs(n.y) < 10_000f)
        }
    }
}
