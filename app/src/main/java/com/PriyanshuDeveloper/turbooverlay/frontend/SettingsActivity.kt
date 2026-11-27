package com.PriyanshuDeveloper.turbooverlay.frontend

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.PriyanshuDeveloper.turbooverlay.databinding.ActivitySettingsBinding
import com.PriyanshuDeveloper.turbooverlay.phoenixengine.GameLauncherManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSettingsBack.setOnClickListener {
            finish()
        }

        binding.btnUserManualSection.setOnClickListener {
            startActivity(Intent(this, UserManualActivity::class.java))
        }

        binding.switchLagProtection.apply {
            isChecked = GameLauncherManager.isLagProtectionEnabled(this@SettingsActivity)

            val states = arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            )
            val thumbColors = intArrayOf(
                0xFF4CAF50.toInt(),
                0xFFAAAAAA.toInt()
            )
            val trackColors = intArrayOf(
                0x804CAF50.toInt(),
                0x44777777
            )
            thumbTintList = ColorStateList(states, thumbColors)
            trackTintList = ColorStateList(states, trackColors)

            setOnCheckedChangeListener { _, isChecked ->
                GameLauncherManager.setLagProtectionEnabled(this@SettingsActivity, isChecked)
            }
        }
    }
}
