package com.PriyanshuDeveloper.turbooverlay.frontend

import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import com.PriyanshuDeveloper.turbooverlay.databinding.ActivityMainBinding
import com.PriyanshuDeveloper.turbooverlay.phoenixengine.GameLauncherManager
import com.PriyanshuDeveloper.turbooverlay.phoenixengine.MetricsReader
import com.PriyanshuDeveloper.turbooverlay.phoenixengine.ShizukuShellManager
import com.google.android.material.button.MaterialButton
import rikka.shizuku.Shizuku
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val metricsReader = MetricsReader()
    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    private var hasPromptedShizukuPermission = false
    private var infoDialog: InfoCapsuleDialog? = null

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

        binding.rvGameLauncher.layoutManager = GridLayoutManager(this, 3)

        binding.btnInfo.setOnClickListener {
            showInfoDialog()
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnAddGame.setOnClickListener {
            AppSelectionDialog(this) {
                loadGameLauncher()
            }.show()
        }

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
        loadGameLauncher()
    }

    private fun showInfoDialog() {
        if (infoDialog?.isShowing == true) return
        infoDialog = InfoCapsuleDialog(this) {
            updateInfoButtonColor()
        }
        infoDialog?.show()
    }

    private fun loadGameLauncher() {
        executor.execute {
            val savedPackages = GameLauncherManager.getSavedGamePackages(this)
            val pm = packageManager

            val list = savedPackages.mapNotNull { pkg ->
                try {
                    val appInfo = pm.getApplicationInfo(pkg, 0)
                    val label = pm.getApplicationLabel(appInfo).toString()
                    val icon = pm.getApplicationIcon(appInfo)
                    LauncherAppModel(label, pkg, icon)
                } catch (_: Exception) {
                    null
                }
            }.sortedBy { it.appName.lowercase() }

            handler.post {
                if (list.isEmpty()) {
                    binding.tvEmptyGamesHint.visibility = View.VISIBLE
                    binding.rvGameLauncher.visibility = View.GONE
                } else {
                    binding.tvEmptyGamesHint.visibility = View.GONE
                    binding.rvGameLauncher.visibility = View.VISIBLE
                    binding.rvGameLauncher.adapter = GameLauncherAdapter(
                        appList = list,
                        onItemClick = { item -> launchGame(item) }
                    )
                }
            }
        }
    }

    private fun launchGame(item: LauncherAppModel) {
        val launchIntent = packageManager.getLaunchIntentForPackage(item.packageName)
        if (launchIntent != null) {
            startActivity(launchIntent)

            val isShizukuConnected = try {
                Shizuku.pingBinder() && hasShizukuPermission()
            } catch (_: Exception) { false }

            if (isShizukuConnected) {
                if (checkOverlayPermission()) {
                    val serviceIntent = Intent(this, OverlayService::class.java).apply {
                        putExtra(OverlayService.EXTRA_AUTO_BOOST, true)
                        putExtra(OverlayService.EXTRA_LAUNCHED_APP_NAME, item.appName)
                    }
                    startService(serviceIntent)
                }
            }
        } else {
            Toast.makeText(this, "Could not launch ${item.appName}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupButtonAnimations() {
        val touchListener = { view: View, event: MotionEvent ->
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
        loadGameLauncher()
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
                setButtonsEnabled(true)
            } else {
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
            setButtonsEnabled(false)
        }

        updateInfoButtonColor()
        infoDialog?.refreshPermissionStates()
    }

    private fun updateInfoButtonColor() {
        val isShizukuConnected = try { Shizuku.pingBinder() && hasShizukuPermission() } catch (_: Exception) { false }
        val isOverlayGranted = checkOverlayPermission()
        val pm = getSystemService(POWER_SERVICE) as? PowerManager
        val isBatteryOptDisabled = pm?.isIgnoringBatteryOptimizations(packageName) == true

        val colorInt = when {
            isShizukuConnected -> 0xFF4CAF50.toInt() // GREEN
            isOverlayGranted && isBatteryOptDisabled -> 0xFF2196F3.toInt() // BLUE
            else -> 0xFFF44336.toInt() // RED
        }

        binding.btnInfo.imageTintList = ColorStateList.valueOf(colorInt)
        binding.btnInfo.setColorFilter(colorInt)
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        val color = 0xFF4CAF50.toInt()
        val tint = ColorStateList.valueOf(color)

        binding.btnOverlay.isEnabled = true
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
        handler.removeCallbacksAndMessages(null)
        executor.shutdown()
        try {
            Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        } catch (_: Exception) {}
    }
}
