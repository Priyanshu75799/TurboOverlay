package com.PriyanshuDeveloper.turbooverlay

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.PriyanshuDeveloper.turbooverlay.databinding.ActivityMainBinding
import com.google.android.material.button.MaterialButton
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val metricsReader = MetricsReader()
    private val handler = Handler(Looper.getMainLooper())

    private var hasPromptedShizukuPermission = false

    private val shizukuPermissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            updateShizukuStatus()
        } else {
            hasPromptedShizukuPermission = false
            updateShizukuStatus()
        }
    }

    private val statusUpdateRunnable = object : Runnable {
        override fun run() {
            updateShizukuStatus()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        try {
            Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        } catch (_: Exception) {}

        binding.btnOverlay.setOnClickListener {
            if (!checkOverlayPermission()) {
                requestOverlayPermission()
                return@setOnClickListener
            }
            if (Shizuku.pingBinder() && !hasShizukuPermission()) {
                try {
                    Shizuku.requestPermission(0)
                } catch (_: Exception) {}
                return@setOnClickListener
            }
            startService(Intent(this, OverlayService::class.java))
        }

        binding.btnStopApp.setOnClickListener {
            try {
                stopService(Intent(this, OverlayService::class.java))
            } catch (_: Exception) {}

            try {
                ShizukuShellManager.destroy()
            } catch (_: Exception) {}

            if (ShizukuShellManager.isReady()) {
                try {
                    metricsReader.executeShellCommand("am force-stop $packageName")
                } catch (_: Exception) {}
            }

            finishAffinity()
            finishAndRemoveTask()
            android.os.Process.killProcess(android.os.Process.myPid())
        }

        setupButtonAnimations()
    }

    private fun setupButtonAnimations() {
        val touchListener = { view: android.view.View, event: MotionEvent ->
            val button = view as? MaterialButton
            if (button != null) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        animateButtonPress(button)
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        animateButtonRelease(button)
                        button.performClick()
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        animateButtonRelease(button)
                        true
                    }
                    else -> false
                }
            } else false
        }

        binding.btnOverlay.setOnTouchListener(touchListener)
        binding.btnStopApp.setOnTouchListener(touchListener)
    }

    private fun animateButtonPress(button: MaterialButton) {
        button.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(50)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        button.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun animateButtonRelease(button: MaterialButton) {
        button.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(100)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(statusUpdateRunnable)
        handler.post(statusUpdateRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(statusUpdateRunnable)
    }

    private fun updateShizukuStatus() {
        val isRunning = try { Shizuku.pingBinder() } catch (_: Exception) { false }
        if (isRunning) {
            if (hasShizukuPermission()) {
                hasPromptedShizukuPermission = false
                ShizukuShellManager.init(this)
                binding.tvShizukuStatus.text = getString(R.string.shizuku_connected)
                binding.tvShizukuStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_light))
                setButtonsEnabled(true)
            } else {
                binding.tvShizukuStatus.text = getString(R.string.shizuku_permission_needed)
                binding.tvShizukuStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
                setButtonsEnabled(false)

                if (!hasPromptedShizukuPermission) {
                    hasPromptedShizukuPermission = true
                    try {
                        Shizuku.requestPermission(0)
                    } catch (_: Exception) {}
                }
            }
        } else {
            hasPromptedShizukuPermission = false
            binding.tvShizukuStatus.text = getString(R.string.shizuku_disconnected)
            binding.tvShizukuStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            setButtonsEnabled(false)
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        val color = if (enabled) 0xFF4CAF50.toInt() else 0xFF444444.toInt()
        val tint = ColorStateList.valueOf(color)

        binding.btnOverlay.isEnabled = enabled
        binding.btnOverlay.backgroundTintList = tint

        val stopTint = ColorStateList.valueOf(0xFFE53935.toInt())
        binding.btnStopApp.isEnabled = true
        binding.btnStopApp.backgroundTintList = stopTint
    }

    private fun hasShizukuPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) {
                ContextCompat.checkSelfPermission(this, "moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun checkOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(this)
    }

    private fun requestOverlayPermission() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
            startActivity(intent)
        } catch (_: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(statusUpdateRunnable)
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        } catch (_: Exception) {}
    }
}
