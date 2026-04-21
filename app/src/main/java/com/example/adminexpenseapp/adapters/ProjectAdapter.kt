package com.example.adminexpenseapp.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.adminexpenseapp.R
import com.example.adminexpenseapp.models.Project
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip

class ProjectAdapter(
    private val onProjectClick: (Project) -> Unit,
    private val onProjectLongClick: (Project) -> Unit
) : RecyclerView.Adapter<ProjectAdapter.ViewHolder>() {

    private var projects: List<Project> = emptyList()
    private val selectedProjectIds = mutableSetOf<String>()
    private var isSelectionMode = false

    fun setProjects(list: List<Project>) {
        projects = list
        notifyDataSetChanged()
    }

    fun toggleSelection(projectId: String) {
        if (selectedProjectIds.contains(projectId)) {
            selectedProjectIds.remove(projectId)
        } else {
            selectedProjectIds.add(projectId)
        }
        isSelectionMode = selectedProjectIds.isNotEmpty()
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedProjectIds.clear()
        isSelectionMode = false
        notifyDataSetChanged()
    }

    fun getSelectedProjects(): List<Project> {
        return projects.filter { selectedProjectIds.contains(it.id) }
    }

    fun getSelectedCount(): Int = selectedProjectIds.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_project, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val project = projects[position]
        holder.tvProjectCode.text = project.projectCode
        holder.tvProjectName.text = project.projectName
        holder.tvProjectManager.text = "Manager: ${project.projectManager}"
        holder.tvDateRange.text = "${project.startDate} – ${project.endDate}"
        holder.tvBudget.text = String.format("%.2f %s", project.projectBudget, project.currency)
        holder.chipStatus.text = project.projectStatus

        // Visual selection indicator
        val isSelected = selectedProjectIds.contains(project.id)
        holder.cardView.isChecked = isSelected
        holder.cardView.strokeWidth = if (isSelected) 4 else 0
        holder.cardView.strokeColor = Color.parseColor("#1565C0")

        // Sync Status
        if (project.lastSyncAt == 0L) {
            holder.ivSyncStatus.setImageResource(R.drawable.ic_sync_cloud_off)
            holder.ivSyncStatus.setColorFilter(Color.GRAY)
        } else if (project.updatedAt > project.lastSyncAt) {
            holder.ivSyncStatus.setImageResource(R.drawable.ic_sync_cloud_on)
            holder.ivSyncStatus.setColorFilter(Color.parseColor("#FF8C00"))
        } else {
            holder.ivSyncStatus.setImageResource(R.drawable.ic_sync_cloud_on)
            holder.ivSyncStatus.setColorFilter(Color.parseColor("#4CAF50"))
        }

        holder.itemView.setOnClickListener {
            if (isSelectionMode) {
                toggleSelection(project.id)
                onProjectLongClick(project) // Re-use callback to update title/menu
            } else {
                onProjectClick(project)
            }
        }

        holder.itemView.setOnLongClickListener {
            toggleSelection(project.id)
            onProjectLongClick(project)
            true
        }
    }

    override fun getItemCount(): Int = projects.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardView: MaterialCardView = itemView as MaterialCardView
        val tvProjectCode: TextView = itemView.findViewById(R.id.tvProjectCode)
        val tvProjectName: TextView = itemView.findViewById(R.id.tvProjectName)
        val tvProjectManager: TextView = itemView.findViewById(R.id.tvProjectManager)
        val tvDateRange: TextView = itemView.findViewById(R.id.tvDateRange)
        val tvBudget: TextView = itemView.findViewById(R.id.tvBudget)
        val chipStatus: Chip = itemView.findViewById(R.id.chipStatus)
        val ivSyncStatus: ImageView = itemView.findViewById(R.id.ivSyncStatus)
    }
}
