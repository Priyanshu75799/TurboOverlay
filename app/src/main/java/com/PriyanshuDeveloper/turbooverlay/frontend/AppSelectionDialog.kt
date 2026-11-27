package com.PriyanshuDeveloper.turbooverlay.frontend

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import com.PriyanshuDeveloper.turbooverlay.databinding.DialogAppSelectionBinding
import com.PriyanshuDeveloper.turbooverlay.phoenixengine.GameLauncherManager
import java.util.concurrent.Executors

class AppSelectionDialog(
    context: Context,
    private val onSelectionDone: () -> Unit
) : Dialog(context) {

    private lateinit var binding: DialogAppSelectionBinding
    private var adapter: AppSelectionAdapter? = null
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        binding = DialogAppSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        binding.rvAppsSelection.layoutManager = LinearLayoutManager(context)

        binding.btnDoneSelection.setOnClickListener {
            val selected = adapter?.getSelectedPackages() ?: emptySet()
            GameLauncherManager.saveGamePackages(context, selected)
            onSelectionDone()
            dismiss()
        }

        loadApps()
    }

    private fun loadApps() {
        binding.progressAppsLoading.visibility = View.VISIBLE
        binding.rvAppsSelection.visibility = View.GONE

        executor.execute {
            val pm = context.packageManager
            val currentPkg = context.packageName

            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfos = try {
                pm.queryIntentActivities(mainIntent, 0)
            } catch (_: Exception) {
                emptyList()
            }

            val savedPackages = GameLauncherManager.getSavedGamePackages(context)

            val appList = resolveInfos
                .map { it.activityInfo }
                .filter { it.packageName != currentPkg }
                .distinctBy { it.packageName }
                .map { info ->
                    val name = info.loadLabel(pm).toString()
                    val icon = info.loadIcon(pm)
                    val isChecked = savedPackages.contains(info.packageName)
                    AppInfoModel(name, info.packageName, icon, isChecked)
                }
                .sortedBy { it.appName.lowercase() }

            handler.post {
                adapter = AppSelectionAdapter(appList)
                binding.rvAppsSelection.adapter = adapter
                binding.progressAppsLoading.visibility = View.GONE
                binding.rvAppsSelection.visibility = View.VISIBLE
            }
        }
    }

    override fun onStop() {
        super.onStop()
        executor.shutdown()
    }
}
