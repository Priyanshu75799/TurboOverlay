package com.PriyanshuDeveloper.turbooverlay.frontend

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.PriyanshuDeveloper.turbooverlay.databinding.ItemAppSelectionBinding

data class AppInfoModel(
    val appName: String,
    val packageName: String,
    val icon: Drawable,
    var isSelected: Boolean = false
)

class AppSelectionAdapter(
    private val appList: List<AppInfoModel>
) : RecyclerView.Adapter<AppSelectionAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemAppSelectionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppSelectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = appList[position]
        holder.binding.tvSelectAppName.text = item.appName
        holder.binding.tvSelectAppPackage.text = item.packageName
        holder.binding.imgSelectAppIcon.setImageDrawable(item.icon)
        holder.binding.chkAppSelect.isChecked = item.isSelected

        holder.itemView.setOnClickListener {
            item.isSelected = !item.isSelected
            holder.binding.chkAppSelect.isChecked = item.isSelected
        }
    }

    override fun getItemCount(): Int = appList.size

    fun getSelectedPackages(): Set<String> {
        return appList.filter { it.isSelected }.map { it.packageName }.toSet()
    }
}
