package com.example.adminexpenseapp.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adminexpenseapp.R
import com.example.adminexpenseapp.database.DatabaseHelper
import com.example.adminexpenseapp.models.Project
import com.example.adminexpenseapp.utils.FirebaseSync
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditProjectActivity : AppCompatActivity() {

    private lateinit var etProjectCode: TextInputEditText
    private lateinit var etProjectName: TextInputEditText
    private lateinit var etProjectDesc: TextInputEditText
    private lateinit var etStartDate: TextInputEditText
    private lateinit var etEndDate: TextInputEditText
    private lateinit var etProjectManager: TextInputEditText
    private lateinit var etBudget: TextInputEditText
    private lateinit var etSpecialReq: TextInputEditText
    private lateinit var etClientDept: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var spinnerStatus: AutoCompleteTextView
    private lateinit var spinnerPriority: AutoCompleteTextView
    private lateinit var spinnerCurrency: AutoCompleteTextView
    private lateinit var tilProjectCode: TextInputLayout
    private lateinit var tilProjectName: TextInputLayout
    private lateinit var tilProjectDesc: TextInputLayout
    private lateinit var tilStartDate: TextInputLayout
    private lateinit var tilEndDate: TextInputLayout
    private lateinit var tilProjectManager: TextInputLayout
    private lateinit var tilStatus: TextInputLayout
    private lateinit var tilBudget: TextInputLayout
    private lateinit var tilCurrency: TextInputLayout

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var project: Project
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.UK)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_project) // Reuse same layout

        setSupportActionBar(findViewById(R.id.toolbar))
        title = "Edit Project"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dbHelper = DatabaseHelper.getInstance(this)
        val projectId = intent.getStringExtra("project_id") ?: ""
        
        val foundProject = dbHelper.getProjectById(projectId)
        if (foundProject == null) {
            Toast.makeText(this, "Project not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        project = foundProject

        initViews()
        setupSpinners()
        setupDatePickers()
        populateFields()

        val btnNext = findViewById<MaterialButton>(R.id.btnNext)
        btnNext.text = "Save Changes"
        btnNext.setOnClickListener { validateAndFetchBeforeSave() }
    }

    private fun initViews() {
        etProjectCode = findViewById(R.id.etProjectCode)
        etProjectName = findViewById(R.id.etProjectName)
        etProjectDesc = findViewById(R.id.etProjectDesc)
        etStartDate = findViewById(R.id.etStartDate)
        etEndDate = findViewById(R.id.etEndDate)
        etProjectManager = findViewById(R.id.etProjectManager)
        etBudget = findViewById(R.id.etBudget)
        etSpecialReq = findViewById(R.id.etSpecialReq)
        etClientDept = findViewById(R.id.etClientDept)
        etNotes = findViewById(R.id.etNotes)
        spinnerStatus = findViewById(R.id.spinnerStatus)
        spinnerPriority = findViewById(R.id.spinnerPriority)
        spinnerCurrency = findViewById(R.id.spinnerCurrency)
        tilProjectCode = findViewById(R.id.tilProjectCode)
        tilProjectName = findViewById(R.id.tilProjectName)
        tilProjectDesc = findViewById(R.id.tilProjectDesc)
        tilStartDate = findViewById(R.id.tilStartDate)
        tilEndDate = findViewById(R.id.tilEndDate)
        tilProjectManager = findViewById(R.id.tilProjectManager)
        tilStatus = findViewById(R.id.tilStatus)
        tilBudget = findViewById(R.id.tilBudget)
        tilCurrency = findViewById(R.id.tilCurrency)
    }

    private fun setupSpinners() {
        spinnerStatus.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
            arrayOf("Active", "Completed", "On Hold")))
        spinnerPriority.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
            arrayOf("Low", "Medium", "High", "Critical")))
        spinnerCurrency.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
            arrayOf("USD", "GBP", "EUR", "VND", "JPY")))
    }

    private fun setupDatePickers() {
        etStartDate.isFocusable = false
        etEndDate.isFocusable = false
        etStartDate.setOnClickListener { showDatePicker(etStartDate) }
        etEndDate.setOnClickListener { showDatePicker(etEndDate) }
    }

    private fun showDatePicker(target: TextInputEditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, y, m, d ->
            cal.set(y, m, d)
            target.setText(dateFormat.format(cal.time))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun populateFields() {
        etProjectCode.setText(project.projectCode)
        etProjectName.setText(project.projectName)
        etProjectDesc.setText(project.projectDescription)
        etStartDate.setText(project.startDate)
        etEndDate.setText(project.endDate)
        etProjectManager.setText(project.projectManager)
        etBudget.setText(project.projectBudget.toString())
        etSpecialReq.setText(project.specialRequirements)
        etClientDept.setText(project.clientDepartment)
        etNotes.setText(project.notes)
        spinnerStatus.setText(project.projectStatus, false)
        spinnerCurrency.setText(project.currency, false)
        if (project.priority.isNotBlank()) spinnerPriority.setText(project.priority, false)
    }

    private fun validateAndFetchBeforeSave() {
        listOf(tilProjectCode, tilProjectName, tilProjectDesc, tilStartDate,
            tilEndDate, tilProjectManager, tilStatus, tilBudget, tilCurrency).forEach { it.error = null }

        var isValid = true
        val code = etProjectCode.text.toString().trim()
        val name = etProjectName.text.toString().trim()
        val desc = etProjectDesc.text.toString().trim()
        val start = etStartDate.text.toString().trim()
        val end = etEndDate.text.toString().trim()
        val manager = etProjectManager.text.toString().trim()
        val status = spinnerStatus.text.toString().trim()
        val budgetStr = etBudget.text.toString().trim()
        val currency = spinnerCurrency.text.toString().trim()

        if (code.isEmpty()) { tilProjectCode.error = "Required"; isValid = false }
        if (name.isEmpty()) { tilProjectName.error = "Required"; isValid = false }
        if (desc.isEmpty()) { tilProjectDesc.error = "Required"; isValid = false }
        if (start.isEmpty()) { tilStartDate.error = "Required"; isValid = false }
        if (end.isEmpty()) { tilEndDate.error = "Required"; isValid = false }
        if (manager.isEmpty()) { tilProjectManager.error = "Required"; isValid = false }
        if (status.isEmpty()) { tilStatus.error = "Required"; isValid = false }
        if (currency.isEmpty()) { tilCurrency.error = "Required"; isValid = false }

        val budget = if (budgetStr.isEmpty()) {
            tilBudget.error = "Required"; isValid = false; 0.0
        } else {
            budgetStr.toDoubleOrNull() ?: run { tilBudget.error = "Invalid number"; isValid = false; 0.0 }
        }

        if (!isValid) return

        project = project.copy(
            projectCode = code, projectName = name, projectDescription = desc,
            startDate = start, endDate = end, projectManager = manager,
            projectStatus = status, projectBudget = budget,
            currency = currency,
            specialRequirements = etSpecialReq.text.toString().trim(),
            clientDepartment = etClientDept.text.toString().trim(),
            priority = spinnerPriority.text.toString().trim(),
            notes = etNotes.text.toString().trim(),
            updatedAt = System.currentTimeMillis()
        )


        FirebaseSync.fetchAll(this, object : FirebaseSync.FetchCallback {
            override fun onFetchComplete(newProjects: Int, updatedProjects: Int, deletedLocally: Int) {
                saveUpdate()
            }
            override fun onFailure(err: String) {
                saveUpdate()
            }
        })
    }

    private fun saveUpdate() {
        dbHelper.updateProject(project)
        Toast.makeText(this, "Project updated!", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
