package com.cosmos.launcher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.sqrt

class CosmosView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    private var renderThread: RenderThread? = null
    private var nodes = mutableListOf<AppNode>()
    private val physics = CosmosPhysics()
    private val renderer = CosmosRenderer()
    private var isRunning = false
    private var onAppTapped: ((AppNode) -> Unit)? = null

    // Physics parameters (adjustable via settings)
    var gravityMultiplier = 1f
    var speedMultiplier = 1f

    // Touch handling
    private var selectedNode: AppNode? = null
    private var touchX = -1f
    private var touchY = -1f

    init {
        holder.addCallback(this)
        setZOrderOnTop(false)
    }

    fun setAppNodes(appNodes: List<AppNode>) {
        nodes.clear()
        nodes.addAll(appNodes)
        physics.assignOrbits(nodes, width / 2f, height / 2f)
    }

    fun setOnAppTappedListener(listener: (AppNode) -> Unit) {
        onAppTapped = listener
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isRunning = true
        renderThread = RenderThread(holder).also { it.start() }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRunning = false
        renderThread?.join()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        physics.assignOrbits(nodes, width / 2f, height / 2f)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchX = event.x
                touchY = event.y
                selectedNode = nodes.firstOrNull { it.distanceTo(touchX, touchY) < it.radius + 20f }
                selectedNode?.let {
                    it.isDragging = true
                    it.dragOffsetX = it.x - touchX
                    it.dragOffsetY = it.y - touchY
                }
            }
            MotionEvent.ACTION_MOVE -> {
                touchX = event.x
                touchY = event.y
                selectedNode?.let {
                    it.x = touchX + it.dragOffsetX
                    it.y = touchY + it.dragOffsetY
                }
            }
            MotionEvent.ACTION_UP -> {
                selectedNode?.let {
                    it.isDragging = false
                    // Check if tap (small movement)
                    val dx = event.x - touchX
                    val dy = event.y - touchY
                    val distance = sqrt(dx * dx + dy * dy)
                    if (distance < 30f) {
                        onAppTapped?.invoke(it)
                    }
                }
                selectedNode = null
                touchX = -1f
                touchY = -1f
            }
        }
        return true
    }

    private inner class RenderThread(private val holder: SurfaceHolder) : Thread() {
        private var fpsCounter = 0
        private var lastFpsTime = System.currentTimeMillis()

        override fun run() {
            while (isRunning) {
                val startTime = System.currentTimeMillis()

                // Update physics
                physics.tick(
                    nodes,
                    width / 2f,
                    height / 2f,
                    width.toFloat(),
                    height.toFloat(),
                    gravityMultiplier,
                    speedMultiplier
                )

                // Render
                try {
                    val canvas = holder.lockCanvas()
                    if (canvas != null) {
                        renderer.render(canvas, nodes, width / 2f, height / 2f, touchX, touchY)
                        holder.unlockCanvasAndPost(canvas)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                // Frame rate limiting (60 FPS target)
                val elapsed = System.currentTimeMillis() - startTime
                val sleepTime = (16 - elapsed).coerceAtLeast(1L)
                Thread.sleep(sleepTime)

                // FPS logging (every second)
                fpsCounter++
                val now = System.currentTimeMillis()
                if (now - lastFpsTime >= 1000) {
                    android.util.Log.d("CosmosView", "FPS: $fpsCounter")
                    fpsCounter = 0
                    lastFpsTime = now
                }
            }
        }
    }
}
