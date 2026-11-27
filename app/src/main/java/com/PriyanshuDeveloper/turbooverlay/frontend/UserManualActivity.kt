package com.PriyanshuDeveloper.turbooverlay.frontend

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.PriyanshuDeveloper.turbooverlay.databinding.ActivityUserManualBinding

class UserManualActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserManualBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManualBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnManualBack.setOnClickListener {
            finish()
        }
    }
}
