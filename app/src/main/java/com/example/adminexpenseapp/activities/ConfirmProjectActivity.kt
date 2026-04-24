package com.example.adminexpenseapp.activities

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adminexpenseapp.R
import com.example.adminexpenseapp.database.DatabaseHelper
import com.example.adminexpenseapp.models.Project
import com.example.adminexpenseapp.utils.FirebaseSync
import com.google.android.material.button.MaterialButton

class ConfirmProjectActivity : AppCompatActivity() {

    private var project: Project? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_project)

        setSupportActionBar(findViewById(R.id.toolbar))
        title = "Confirm Project Details"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        @Suppress("DEPRECATION")
        project = intent.getSerializableExtra("project") as? Project
        if (project == null) {
            Toast.makeText(this, "Error: No project data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        displayDetails()

        findViewById<MaterialButton>(R.id.btnConfirm).setOnClickListener { 
            // 1. Fetch Cloud Data first to ensure no conflicts before saving
            fetchAndSave()
        }
        findViewById<MaterialButton>(R.id.btnEdit).setOnClickListener { finish() }
    }

    private fun displayDetails() {
        val p = project ?: return
        findViewById<TextView>(R.id.tvConfirmCode).text = p.projectCode
        findViewById<TextView>(R.id.tvConfirmName).text = p.projectName
        findViewById<TextView>(R.id.tvConfirmDesc).text = p.projectDescription
        findViewById<TextView>(R.id.tvConfirmStartDate).text = p.startDate
        findViewById<TextView>(R.id.tvConfirmEndDate).text = p.endDate
        findViewById<TextView>(R.id.tvConfirmManager).text = p.projectManager
        findViewById<TextView>(R.id.tvConfirmStatus).text = p.projectStatus
        findViewById<TextView>(R.id.tvConfirmBudget).text = String.format("%.2f", p.projectBudget)
        findViewById<TextView>(R.id.tvConfirmCurrency).text = p.currency
        findViewById<TextView>(R.id.tvConfirmSpecialReq).text =
            p.specialRequirements.ifBlank { "None" }
        findViewById<TextView>(R.id.tvConfirmClientDept).text =
            p.clientDepartment.ifBlank { "None" }
        findViewById<TextView>(R.id.tvConfirmPriority).text =
            p.priority.ifBlank { "Not set" }
        findViewById<TextView>(R.id.tvConfirmNotes).text =
            p.notes.ifBlank { "None" }
    }

    private fun fetchAndSave() {
        // Reduced risk: Refresh local state from Cloud before inserting new record
        FirebaseSync.fetchAll(this, object : FirebaseSync.FetchCallback {
            override fun onFetchComplete(newProjects: Int, updatedProjects: Int, deletedLocally: Int) {
                saveProject()
            }
            override fun onFailure(errorMessage: String) {
                saveProject()
            }
        })
    }

    private fun saveProject() {
        val p = project ?: return
        val dbHelper = DatabaseHelper.getInstance(this)
        val id = dbHelper.insertProject(p)

        if (id.isNotEmpty()) {
            Toast.makeText(this, "Project saved successfully!", Toast.LENGTH_SHORT).show()
            finishAffinity()
            startActivity(packageManager.getLaunchIntentForPackage(packageName))
        } else {
            Toast.makeText(this, "Error saving project", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
