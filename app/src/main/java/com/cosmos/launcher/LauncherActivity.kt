package com.cosmos.launcher

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

class LauncherActivity : AppCompatActivity() {
    private lateinit var cosmosView: CosmosView
    private lateinit var tvTime: TextView
    private lateinit var tvDate: TextView
    private lateinit var loadingView: FrameLayout
    private lateinit var repository: AppRepository
    private val USAGE_STATS_PERMISSION = "android.permission.PACKAGE_USAGE_STATS"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        // Hide system UI
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )

        cosmosView = findViewById(R.id.cosmosView)
        tvTime = findViewById(R.id.tvTime)
        tvDate = findViewById(R.id.tvDate)
        loadingView = findViewById(R.id.loadingView)
        repository = AppRepository(this)

        // Request permissions if needed
        if (ContextCompat.checkSelfPermission(this, USAGE_STATS_PERMISSION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(USAGE_STATS_PERMISSION), 1001)
        }

        // Setup tap handler
        cosmosView.setOnAppTappedListener { appNode ->
            launchApp(appNode.packageName)
        }

        // Load apps
        Thread {
            val apps = repository.loadInstalledApps()
            runOnUiThread {
                cosmosView.setAppNodes(apps)
                loadingView.alpha = 0f
                updateTime()
                startTimeUpdates()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun updateTime() {
        val now = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEE · d MMM yyyy", Locale.getDefault()).apply {
            timeZone = TimeZone.getDefault()
        }

        tvTime.text = timeFormat.format(now.time).uppercase()
        tvDate.text = dateFormat.format(now.time).uppercase()
    }

    private fun startTimeUpdates() {
        Thread {
            while (true) {
                Thread.sleep(1000)
                runOnUiThread { updateTime() }
            }
        }.start()
    }

    private fun launchApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
