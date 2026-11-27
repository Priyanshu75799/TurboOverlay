package com.PriyanshuDeveloper.turbooverlay.frontend

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.PriyanshuDeveloper.turbooverlay.databinding.ItemGameLauncherBinding

data class LauncherAppModel(
    val appName: String,
    val packageName: String,
    val icon: Drawable
)

class GameLauncherAdapter(
    private val appList: List<LauncherAppModel>,
    private val onItemClick: (LauncherAppModel) -> Unit
) : RecyclerView.Adapter<GameLauncherAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemGameLauncherBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGameLauncherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = appList[position]
        holder.binding.tvAppName.text = item.appName
        holder.binding.imgAppIcon.setImageDrawable(item.icon)

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = appList.size
}
