package com.cosmos.launcher

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class CosmosPhysics {
    // Physics constants
    private val SPRING_STRENGTH = 0.018f      // orbital snap speed
    private val GRAVITY_STRENGTH = 0.0006f    // pull toward cluster center
    private val REPULSION_STRENGTH = 0.35f    // node-to-node repulsion
    private val DAMPING = 0.86f               // velocity decay
    private val MAX_VELOCITY = 12f            // max speed limit
    private val MIN_DISTANCE = 25f            // minimum separation distance
    private val CENTER_MASS_STRENGTH = 0.0003f // keep apps on-screen

    private val clusterCenters = mapOf(
        "work" to Pair(0f, 0f),
        "social" to Pair(0f, 0f),
        "creative" to Pair(0f, 0f),
        "utility" to Pair(0f, 0f),
        "finance" to Pair(0f, 0f)
    )

    fun tick(nodes: List<AppNode>, centerX: Float, centerY: Float, width: Float, height: Float, gravityMultiplier: Float = 1f, speedMultiplier: Float = 1f) {
        // Clear forces
        nodes.forEach {
            it.fx = 0f
            it.fy = 0f
        }

        // Apply forces only to non-dragging nodes
        for (node in nodes) {
            if (node.isDragging) continue

            // 1. Orbital spring force
            applyOrbitalSpring(node, speedMultiplier)

            // 2. Cluster gravity
            applyClusterGravity(node, gravityMultiplier)

            // 3. Repulsion between nodes
            for (other in nodes) {
                if (node.packageName != other.packageName) {
                    applyRepulsion(node, other)
                }
            }

            // 4. Center mass force (keep on-screen)
            applyScreenBounds(node, centerX, centerY, width, height)
        }

        // Update positions and velocities
        for (node in nodes) {
            if (node.isDragging) continue

            // Apply acceleration
            val mass = 1f + (node.importance * 2f) // heavier nodes move less
            node.vx += (node.fx / mass) * 0.016f // 60fps timestep
            node.vy += (node.fy / mass) * 0.016f

            // Damping
            node.vx *= DAMPING
            node.vy *= DAMPING

            // Velocity limits
            val speed = sqrt(node.vx * node.vx + node.vy * node.vy)
            if (speed > MAX_VELOCITY) {
                node.vx = (node.vx / speed) * MAX_VELOCITY
                node.vy = (node.vy / speed) * MAX_VELOCITY
            }

            // Update position
            node.x += node.vx
            node.y += node.vy

            // Update orbital angle
            node.orbitAngle += node.orbitSpeed * speedMultiplier
            if (node.orbitAngle > 2 * PI.toFloat()) {
                node.orbitAngle -= 2 * PI.toFloat()
            }
        }
    }

    private fun applyOrbitalSpring(node: AppNode, speedMultiplier: Float) {
        // Calculate target orbit position
        val targetX = node.targetOrbitRadius * cos(node.targetOrbitAngle)
        val targetY = node.targetOrbitRadius * sin(node.targetOrbitAngle)

        // Spring force toward target
        val dx = targetX - node.x
        val dy = targetY - node.y
        val distance = sqrt(dx * dx + dy * dy)

        if (distance > 0.1f) {
            val force = SPRING_STRENGTH * speedMultiplier
            node.fx += (dx / distance) * force * distance * 0.5f
            node.fy += (dy / distance) * force * distance * 0.5f
        }
    }

    private fun applyClusterGravity(node: AppNode, gravityMultiplier: Float) {
        // Gentle pull toward cluster center (origin)
        val clusterX = 0f
        val clusterY = 0f
        val dx = clusterX - node.x
        val dy = clusterY - node.y
        val distance = sqrt(dx * dx + dy * dy)

        if (distance > 1f) {
            val force = GRAVITY_STRENGTH * gravityMultiplier * node.importance
            node.fx += (dx / distance) * force
            node.fy += (dy / distance) * force
        }
    }

    private fun applyRepulsion(node: AppNode, other: AppNode) {
        val dx = node.x - other.x
        val dy = node.y - other.y
        val distance = sqrt(dx * dx + dy * dy)
        val minDist = node.radius + other.radius + 10f

        if (distance < minDist && distance > 0.1f) {
            val overlap = minDist - distance
            val force = REPULSION_STRENGTH * overlap
            node.fx += (dx / distance) * force
            node.fy += (dy / distance) * force
        }
    }

    private fun applyScreenBounds(node: AppNode, centerX: Float, centerY: Float, width: Float, height: Float) {
        // Soft bounds to keep nodes roughly on-screen
        val padding = 200f
        val minX = centerX - width / 2 - padding
        val maxX = centerX + width / 2 + padding
        val minY = centerY - height / 2 - padding
        val maxY = centerY + height / 2 + padding

        if (node.x < minX) {
            node.fx += (minX - node.x) * CENTER_MASS_STRENGTH
        } else if (node.x > maxX) {
            node.fx += (maxX - node.x) * CENTER_MASS_STRENGTH
        }

        if (node.y < minY) {
            node.fy += (minY - node.y) * CENTER_MASS_STRENGTH
        } else if (node.y > maxY) {
            node.fy += (maxY - node.y) * CENTER_MASS_STRENGTH
        }
    }

    fun assignOrbits(nodes: List<AppNode>, centerX: Float, centerY: Float) {
        val clusters = nodes.groupBy { it.cluster }

        clusters.forEach { (cluster, clusterNodes) ->
            val baseRadius = 80f + (cluster.hashCode() % 5) * 40f
            clusterNodes.forEachIndexed { index, node ->
                val angle = (index * 2 * PI.toFloat() / clusterNodes.size)
                node.targetOrbitRadius = baseRadius
                node.targetOrbitAngle = angle
                node.orbitAngle = angle
                node.x = centerX + baseRadius * cos(angle)
                node.y = centerY + baseRadius * sin(angle)
            }
        }
    }
}
