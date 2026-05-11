package com.cosmos.launcher

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider

class SettingsActivity : AppCompatActivity() {
    private lateinit var sliderGravity: Slider
    private lateinit var sliderOrbitSpeed: Slider
    private lateinit var tvGravityValue: TextView
    private lateinit var tvOrbitValue: TextView
    private lateinit var btnReset: MaterialButton
    private lateinit var btnBack: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        sliderGravity = findViewById(R.id.sliderGravity)
        sliderOrbitSpeed = findViewById(R.id.sliderOrbitSpeed)
        tvGravityValue = findViewById(R.id.tvGravityValue)
        tvOrbitValue = findViewById(R.id.tvOrbitValue)
        btnReset = findViewById(R.id.btnReset)
        btnBack = findViewById(R.id.btnBack)

        // Load saved preferences
        val prefs = getSharedPreferences("cosmos_settings", MODE_PRIVATE)
        sliderGravity.value = prefs.getFloat("gravity", 100f)
        sliderOrbitSpeed.value = prefs.getFloat("speed", 100f)

        // Gravity slider
        sliderGravity.addOnChangeListener { _, value, _ ->
            tvGravityValue.text = "${value.toInt()}%"
            prefs.edit().putFloat("gravity", value).apply()
        }

        // Orbit speed slider
        sliderOrbitSpeed.addOnChangeListener { _, value, _ ->
            tvOrbitValue.text = "${value.toInt()}%"
            prefs.edit().putFloat("speed", value).apply()
        }

        // Reset button
        btnReset.setOnClickListener {
            prefs.edit().clear().apply()
            sliderGravity.value = 100f
            sliderOrbitSpeed.value = 100f
            tvGravityValue.text = "100%"
            tvOrbitValue.text = "100%"
        }

        // Back button
        btnBack.setOnClickListener {
            finish()
        }
    }
}
