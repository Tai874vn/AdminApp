package com.example.adminexpenseapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.adminexpenseapp.activities.AddProjectActivity
import com.example.adminexpenseapp.activities.ProjectDetailActivity
import com.example.adminexpenseapp.activities.SearchActivity
import com.example.adminexpenseapp.adapters.ProjectAdapter
import com.example.adminexpenseapp.database.DatabaseHelper
import com.example.adminexpenseapp.models.Project
import com.example.adminexpenseapp.utils.FirebaseSync
import com.example.adminexpenseapp.utils.ErrorHandler
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var rvProjects: RecyclerView
    private lateinit var tvEmpty: TextView
    private lateinit var adapter: ProjectAdapter
    private lateinit var dbHelper: DatabaseHelper
    private var actionMode: ActionMode? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(findViewById(R.id.toolbar))
        title = "Expense Tracker - Projects"

        dbHelper = DatabaseHelper.getInstance(this)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        rvProjects = findViewById(R.id.rvProjects)
        tvEmpty = findViewById(R.id.tvEmpty)

        rvProjects.layoutManager = LinearLayoutManager(this)
        adapter = ProjectAdapter(
            onProjectClick = { project ->
                val intent = Intent(this, ProjectDetailActivity::class.java)
                intent.putExtra("project_id", project.id)
                startActivity(intent)
            },
            onProjectLongClick = { _ ->
                toggleActionMode()
            }
        )
        rvProjects.adapter = adapter

        swipeRefresh.setOnRefreshListener {
            fetchFromCloud(showToast = true)
        }

        findViewById<FloatingActionButton>(R.id.fabAddProject).setOnClickListener {
            startActivity(Intent(this, AddProjectActivity::class.java))
        }

        if (savedInstanceState == null) {
            com.example.adminexpenseapp.utils.CurrencyConverter.fetchRatesFromCloud(this)
            fetchFromCloud(showToast = false)
        }
    }

    override fun onResume() {
        super.onResume()
        loadProjects()
    }

    private fun loadProjects() {
        val projects = dbHelper.getAllProjects()
        adapter.setProjects(projects)
        tvEmpty.visibility = if (projects.isEmpty()) View.VISIBLE else View.GONE
        rvProjects.visibility = if (projects.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun toggleActionMode() {
        if (actionMode == null) {
            actionMode = startSupportActionMode(actionModeCallback)
        } else {
            val count = adapter.getSelectedCount()
            if (count == 0) {
                actionMode?.finish()
            } else {
                actionMode?.title = "$count Selected"
            }
        }
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_selection, menu)
            return true
        }
        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = when (item.itemId) {
            R.id.action_sync_selected -> {
                val selected = adapter.getSelectedProjects().toList()
                uploadProjectsList(selected)
                mode.finish()
                true
            }
            R.id.action_delete_local_selected -> {
                // Capture IDs before finishing mode to avoid empty selection
                val selectedIds = adapter.getSelectedProjects().map { it.id }
                if (selectedIds.isNotEmpty()) {
                    showDeleteSelectionDialog(selectedIds)
                }
                mode.finish()
                true
            }
            else -> false
        }
        override fun onDestroyActionMode(mode: ActionMode) {
            adapter.clearSelection()
            actionMode = null
        }
    }

    private fun showDeleteSelectionDialog(ids: List<String>) {
        val options = arrayOf("Delete from Phone (Locally)", "Delete from Cloud (Firebase)")
        AlertDialog.Builder(this)
            .setTitle("Delete ${ids.size} Projects")
            .setItems(options) { _, which ->
                if (which == 0) {
                    ids.forEach { dbHelper.deleteProject(it) }
                    loadProjects()
                    ErrorHandler.showSuccess(this, ErrorHandler.Success.PROJECT_DELETED)
                } else {
                    deleteFromCloud(ids)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun uploadProjectsList(projects: List<Project>) {
        swipeRefresh.isRefreshing = true
        FirebaseSync.uploadProjects(this, projects, object : FirebaseSync.SyncCallback {
            override fun onSuccess(pCount: Int, eCount: Int) {
                swipeRefresh.isRefreshing = false
                ErrorHandler.showSuccess(this@MainActivity, ErrorHandler.Success.UPLOAD_SUCCESS)
                loadProjects()
            }
            override fun onFailure(err: String) {
                swipeRefresh.isRefreshing = false
                ErrorHandler.showError(this@MainActivity, err)
            }
        })
    }

    private fun deleteFromCloud(ids: List<String>) {
        swipeRefresh.isRefreshing = true
        FirebaseSync.deleteProjectsFromCloud(this, ids, object : FirebaseSync.SyncCallback {
            override fun onSuccess(pCount: Int, eCount: Int) {
                fetchFromCloud(false)
                ErrorHandler.showSuccess(this@MainActivity, ErrorHandler.Success.deletedFromCloud(pCount))
            }
            override fun onFailure(err: String) {
                swipeRefresh.isRefreshing = false
                ErrorHandler.showError(this@MainActivity, err)
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_search -> { startActivity(Intent(this, SearchActivity::class.java)); true }
        R.id.action_sync -> { fetchFromCloud(true); true }
        R.id.action_upload -> { showUploadConfirmation(); true }
        R.id.action_reset -> { showResetConfirmation(); true }
        else -> super.onOptionsItemSelected(item)
    }

    private fun fetchFromCloud(showToast: Boolean) {
        swipeRefresh.isRefreshing = true
        FirebaseSync.fetchAll(this, object : FirebaseSync.FetchCallback {
            override fun onFetchComplete(n: Int, u: Int, d: Int) {
                swipeRefresh.isRefreshing = false
                loadProjects()
                if (showToast) {
                    val msg = ErrorHandler.Success.syncComplete(n, u, d)
                    ErrorHandler.showSuccess(this@MainActivity, msg)
                }
            }
            override fun onFailure(err: String) {
                swipeRefresh.isRefreshing = false
                if (showToast) ErrorHandler.showError(this@MainActivity, err)
            }
        })
    }

    private fun showUploadConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Upload All to Cloud")
            .setMessage("Sync ALL local data to Firebase? This will overwrite Cloud data.")
            .setPositiveButton("Sync All") { _, _ ->
                swipeRefresh.isRefreshing = true
                FirebaseSync.uploadAll(this, object : FirebaseSync.SyncCallback {
                    override fun onSuccess(pCount: Int, eCount: Int) {
                        swipeRefresh.isRefreshing = false
                        ErrorHandler.showSuccess(this@MainActivity, ErrorHandler.Success.UPLOAD_SUCCESS)
                        loadProjects()
                    }
                    override fun onFailure(err: String) {
                        swipeRefresh.isRefreshing = false
                        ErrorHandler.showError(this@MainActivity, err)
                    }
                })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showResetConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Reset Local Database")
            .setMessage("Delete ALL projects locally? (Cloud data remains).")
            .setPositiveButton("Reset") { _, _ ->
                dbHelper.resetDatabase()
                loadProjects()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
