package com.PriyanshuDeveloper.turbooverlay.frontend

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.ViewGroup
import android.view.Window
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.PriyanshuDeveloper.turbooverlay.R
import com.PriyanshuDeveloper.turbooverlay.databinding.DialogInfoCapsuleBinding
import rikka.shizuku.Shizuku

class InfoCapsuleDialog(
    context: Context,
    private val onPermissionStateChanged: (() -> Unit)? = null
) : Dialog(context) {

    private lateinit var binding: DialogInfoCapsuleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogInfoCapsuleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.btnCloseInfo.setOnClickListener { dismiss() }

        binding.btnShizukuPerm.setOnClickListener {
            val shizukuPkg = "moe.shizuku.privileged.api"
            if (!isPackageInstalled(shizukuPkg)) {
                Toast.makeText(context, context.getString(R.string.error_shizuku_not_found), Toast.LENGTH_SHORT).show()
            } else {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(shizukuPkg)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                } else {
                    Toast.makeText(context, context.getString(R.string.error_shizuku_not_found), Toast.LENGTH_SHORT).show()
                }
                if (Shizuku.pingBinder() && !hasShizukuPermission()) {
                    try { Shizuku.requestPermission(0) } catch (_: Exception) {}
                }
            }
        }

        binding.btnBatteryOptPerm.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri())
                context.startActivity(intent)
            } catch (_: Exception) {}
        }

        binding.btnOverlayPerm.setOnClickListener {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())
                context.startActivity(intent)
            } catch (_: Exception) {}
        }

        refreshPermissionStates()
    }

    override fun onStart() {
        super.onStart()
        refreshPermissionStates()
    }

    fun refreshPermissionStates() {
        val isShizukuOk = Shizuku.pingBinder() && hasShizukuPermission()
        val isOverlayOk = Settings.canDrawOverlays(context)
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isBatteryOptOk = pm?.isIgnoringBatteryOptimizations(context.packageName) == true

        val greenTint = ColorStateList.valueOf(0xFF4CAF50.toInt())
        val redTint = ColorStateList.valueOf(0xFFF44336.toInt())

        binding.btnShizukuPerm.backgroundTintList = if (isShizukuOk) greenTint else redTint
        binding.btnBatteryOptPerm.backgroundTintList = if (isBatteryOptOk) greenTint else redTint
        binding.btnOverlayPerm.backgroundTintList = if (isOverlayOk) greenTint else redTint

        onPermissionStateChanged?.invoke()
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun hasShizukuPermission(): Boolean {
        return try {
            if (Shizuku.isPreV11()) {
                ContextCompat.checkSelfPermission(context, "moe.shizuku.manager.permission.API_V23") == PackageManager.PERMISSION_GRANTED
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (_: Exception) {
            false
        }
    }
}
