package com.cosmos.launcher

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class CosmosRenderer {
    private val starPaint = Paint().apply {
        color = Color.WHITE
        alpha = 100
    }

    private val orbitPaint = Paint().apply {
        color = Color.parseColor("#7b8fff")
        alpha = 40
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val clusterPaint = Paint().apply {
        color = Color.parseColor("#3a3f8c")
        style = Paint.Style.FILL
    }

    private val tetherPaint = Paint().apply {
        color = Color.parseColor("#7b8fff")
        style = Paint.Style.STROKE
        strokeWidth = 1f
    }

    private val labelPaint = Paint().apply {
        color = Color.parseColor("#e8eaf6")
        textSize = 10f
        textAlign = Paint.Align.CENTER
    }

    private val glowPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val orbitLinePaint = Paint().apply {
        color = Color.parseColor("#447b8fff")
        alpha = 30
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
    }

    fun render(canvas: Canvas, nodes: List<AppNode>, centerX: Float, centerY: Float, touchX: Float = -1f, touchY: Float = -1f) {
        // Draw background
        canvas.drawColor(Color.parseColor("#04060f"))

        // Draw stars
        drawStars(canvas, centerX, centerY, canvas.width, canvas.height)

        // Draw orbit paths
        drawOrbits(canvas, nodes, centerX, centerY)

        // Draw tethers (connections between nodes)
        drawTethers(canvas, nodes)

        // Draw nodes (apps)
        nodes.sortedBy { it.importance }.forEach { node ->
            drawNode(canvas, node)
        }

        // Draw touch indicator
        if (touchX > 0 && touchY > 0) {
            drawTouchIndicator(canvas, touchX, touchY)
        }
    }

    private fun drawStars(canvas: Canvas, centerX: Float, centerY: Float, width: Int, height: Int) {
        val starCount = 100
        val seed = 12345L
        val random = kotlin.random.Random(seed)

        for (i in 0 until starCount) {
            val x = random.nextInt(width).toFloat()
            val y = random.nextInt(height).toFloat()
            val size = 0.5f + random.nextFloat() * 1.5f
            canvas.drawCircle(x, y, size, starPaint)
        }
    }

    private fun drawOrbits(canvas: Canvas, nodes: List<AppNode>, centerX: Float, centerY: Float) {
        val orbits = mutableSetOf<Float>()
        nodes.forEach { orbits.add(it.targetOrbitRadius) }

        orbits.forEach { radius ->
            canvas.drawCircle(centerX, centerY, radius, orbitPaint)
        }

        // Draw individual orbit lines for each node
        nodes.forEach { node ->
            val segments = 32
            for (i in 0 until segments) {
                val angle1 = (i * 2 * PI.toFloat() / segments)
                val angle2 = ((i + 1) * 2 * PI.toFloat() / segments)

                val x1 = centerX + node.targetOrbitRadius * cos(angle1)
                val y1 = centerY + node.targetOrbitRadius * sin(angle1)
                val x2 = centerX + node.targetOrbitRadius * cos(angle2)
                val y2 = centerY + node.targetOrbitRadius * sin(angle2)

                canvas.drawLine(x1, y1, x2, y2, orbitLinePaint)
            }
        }
    }

    private fun drawTethers(canvas: Canvas, nodes: List<AppNode>) {
        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val node1 = nodes[i]
                val node2 = nodes[j]

                // Only draw tethers between nodes of same cluster or high importance
                if (node1.cluster == node2.cluster || (node1.importance > 0.7f && node2.importance > 0.7f)) {
                    val distance = node1.distanceTo(node2)
                    if (distance < 300f) {
                        tetherPaint.alpha = ((1f - distance / 300f) * 100).toInt()
                        canvas.drawLine(node1.x, node1.y, node2.x, node2.y, tetherPaint)
                    }
                }
            }
        }
    }

    private fun drawNode(canvas: Canvas, node: AppNode) {
        if (!node.isVisible) return

        // Draw glow
        glowPaint.color = getClusterColor(node.cluster)
        glowPaint.alpha = (node.glowIntensity * 150).toInt()
        canvas.drawCircle(node.x, node.y, node.radius * 1.3f, glowPaint)

        // Draw core
        clusterPaint.color = getClusterColor(node.cluster)
        canvas.drawCircle(node.x, node.y, node.radius, clusterPaint)

        // Draw icon (if available)
        if (node.icon != null) {
            val iconSize = (node.radius * 1.5f).toInt()
            val bounds = RectF(
                node.x - iconSize / 2,
                node.y - iconSize / 2,
                node.x + iconSize / 2,
                node.y + iconSize / 2
            )
            node.icon!!.bounds = android.graphics.Rect(
                bounds.left.toInt(),
                bounds.top.toInt(),
                bounds.right.toInt(),
                bounds.bottom.toInt()
            )
            node.icon!!.draw(canvas)
        }

        // Draw label if important enough
        if (node.importance > 0.5f) {
            labelPaint.alpha = (node.importance * 255).toInt()
            canvas.drawText(node.label, node.x, node.y + node.radius + 20f, labelPaint)
        }
    }

    private fun drawTouchIndicator(canvas: Canvas, touchX: Float, touchY: Float) {
        val touchPaint = Paint().apply {
            color = Color.parseColor("#7b8fff")
            style = Paint.Style.STROKE
            strokeWidth = 2f
            alpha = 150
        }
        canvas.drawCircle(touchX, touchY, 40f, touchPaint)
        canvas.drawCircle(touchX, touchY, 30f, touchPaint)
    }

    private fun getClusterColor(cluster: String): Int {
        return when (cluster) {
            "work" -> Color.parseColor("#FF6B6B")      // Red
            "social" -> Color.parseColor("#51CF66")    // Green
            "creative" -> Color.parseColor("#A78BFA")  // Purple
            "utility" -> Color.parseColor("#FBBF24")   // Amber
            "finance" -> Color.parseColor("#06B6D4")   // Teal
            else -> Color.parseColor("#7b8fff")        // Default blue
        }
    }
}
