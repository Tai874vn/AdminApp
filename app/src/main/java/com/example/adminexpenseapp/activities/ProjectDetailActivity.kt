package com.example.adminexpenseapp.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adminexpenseapp.MainActivity
import com.example.adminexpenseapp.R
import com.example.adminexpenseapp.adapters.ExpenseAdapter
import com.example.adminexpenseapp.database.DatabaseHelper
import com.example.adminexpenseapp.models.Project
import com.example.adminexpenseapp.utils.FirebaseSync
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProjectDetailActivity : AppCompatActivity() {

    private var projectId: String = ""
    private var project: Project? = null
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var rvExpenses: RecyclerView
    private lateinit var tvEmptyExpenses: TextView
    private lateinit var tvTotalExpenses: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_project_detail)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dbHelper = DatabaseHelper.getInstance(this)
        projectId = intent.getStringExtra("project_id") ?: ""

        if (projectId.isEmpty()) {
            Toast.makeText(this, "Error: Project not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        rvExpenses = findViewById(R.id.rvExpenses)
        tvEmptyExpenses = findViewById(R.id.tvEmptyExpenses)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)

        rvExpenses.layoutManager = LinearLayoutManager(this)
        expenseAdapter = ExpenseAdapter(
            onEditClick = { expense ->
                val intent = Intent(this, EditExpenseActivity::class.java)
                intent.putExtra("expense_id", expense.id)
                intent.putExtra("project_id", projectId)
                startActivity(intent)
            },
            onDeleteClick = { expense ->
                AlertDialog.Builder(this)
                    .setTitle("Delete Expense")
                    .setMessage("Delete expense '${expense.expenseId}' locally?")
                    .setPositiveButton("Delete") { _, _ ->
                        dbHelper.deleteExpense(expense.id)
                        loadExpenses()
                        Toast.makeText(this, "Expense deleted locally", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        )
        rvExpenses.adapter = expenseAdapter

        findViewById<FloatingActionButton>(R.id.fabAddExpense).setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            intent.putExtra("project_id", projectId)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadProjectDetails()
        loadExpenses()
    }

    private fun loadProjectDetails() {
        project = dbHelper.getProjectById(projectId)
        val p = project ?: run {
            if (!isFinishing) finish()
            return
        }

        title = p.projectName
        findViewById<TextView>(R.id.tvDetailCode).text = p.projectCode
        findViewById<TextView>(R.id.tvDetailName).text = p.projectName
        findViewById<TextView>(R.id.tvDetailDesc).text = p.projectDescription
        findViewById<TextView>(R.id.tvDetailDates).text = "${p.startDate} – ${p.endDate}"
        findViewById<TextView>(R.id.tvDetailManager).text = p.projectManager
        findViewById<TextView>(R.id.tvDetailStatus).text = p.projectStatus
        findViewById<TextView>(R.id.tvDetailBudget).text = String.format("%.2f %s", p.projectBudget, p.currency)

        val tvSpecial = findViewById<TextView>(R.id.tvDetailSpecialReq)
        val labelSpecial = findViewById<TextView>(R.id.labelSpecialReq)
        if (p.specialRequirements.isNotBlank()) {
            tvSpecial.text = p.specialRequirements
            tvSpecial.visibility = View.VISIBLE
            labelSpecial.visibility = View.VISIBLE
        } else {
            tvSpecial.visibility = View.GONE
            labelSpecial.visibility = View.GONE
        }

        val tvClient = findViewById<TextView>(R.id.tvDetailClientDept)
        val labelClient = findViewById<TextView>(R.id.labelClientDept)
        if (p.clientDepartment.isNotBlank()) {
            tvClient.text = p.clientDepartment
            tvClient.visibility = View.VISIBLE
            labelClient.visibility = View.VISIBLE
        } else {
            tvClient.visibility = View.GONE
            labelClient.visibility = View.GONE
        }
    }

    private fun loadExpenses() {
        val expenses = dbHelper.getExpensesByProjectId(projectId)
        expenseAdapter.setExpenses(expenses)

        val total = dbHelper.getTotalExpenses(projectId)
        val currency = project?.currency ?: ""
        tvTotalExpenses.text = String.format("Total Expenses: %.2f %s", total, currency)

        if (expenses.isEmpty()) {
            tvEmptyExpenses.visibility = View.VISIBLE
            rvExpenses.visibility = View.GONE
        } else {
            tvEmptyExpenses.visibility = View.GONE
            rvExpenses.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_project_detail, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_edit_project -> {
            val intent = Intent(this, EditProjectActivity::class.java)
            intent.putExtra("project_id", projectId)
            startActivity(intent)
            true
        }
        R.id.action_delete_project -> {
            showDeleteSelectionDialog()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun showDeleteSelectionDialog() {
        val options = arrayOf("Delete from Phone (Locally)", "Delete from Cloud (Firebase)")
        AlertDialog.Builder(this)
            .setTitle("Delete Project")
            .setItems(options) { _, which ->
                if (which == 0) deleteLocally() else deleteFromCloud()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteLocally() {
        dbHelper.deleteProject(projectId)
        Toast.makeText(this, "Project deleted locally", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun deleteFromCloud() {
        FirebaseSync.deleteProjectsFromCloud(this, listOf(projectId), object : FirebaseSync.SyncCallback {
            override fun onSuccess(projectCount: Int, expenseCount: Int) {
                Toast.makeText(this@ProjectDetailActivity, "Project deleted from cloud", Toast.LENGTH_SHORT).show()
                finish()
            }
            override fun onFailure(err: String) {
                Toast.makeText(this@ProjectDetailActivity, err, Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
