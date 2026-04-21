package com.example.adminexpenseapp.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adminexpenseapp.R
import com.example.adminexpenseapp.models.Project
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddProjectActivity : AppCompatActivity() {

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

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.UK)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_project)

        setSupportActionBar(findViewById(R.id.toolbar))
        title = "Add New Project"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        initViews()
        setupSpinners()
        setupDatePickers()

        findViewById<MaterialButton>(R.id.btnNext).setOnClickListener {
            validateAndProceed()
        }
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
        val statuses = arrayOf("Active", "Completed", "On Hold")
        spinnerStatus.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, statuses))

        val priorities = arrayOf("Low", "Medium", "High", "Critical")
        spinnerPriority.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, priorities))

        val currencies = arrayOf("USD", "GBP", "EUR", "VND", "JPY")
        spinnerCurrency.setAdapter(ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, currencies))
        spinnerCurrency.setText("USD", false) // Default
    }

    private fun setupDatePickers() {
        etStartDate.isFocusable = false
        etEndDate.isFocusable = false
        etStartDate.setOnClickListener { showDatePicker(etStartDate) }
        etEndDate.setOnClickListener { showDatePicker(etEndDate) }
    }

    private fun showDatePicker(target: TextInputEditText) {
        val cal = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, day ->
            cal.set(year, month, day)
            target.setText(dateFormat.format(cal.time))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun validateAndProceed() {
        // Clear previous errors
        listOf(tilProjectCode, tilProjectName, tilProjectDesc, tilStartDate,
            tilEndDate, tilProjectManager, tilStatus, tilBudget, tilCurrency).forEach { it.error = null }

        var isValid = true
        val code = etProjectCode.text.toString().trim()
        val name = etProjectName.text.toString().trim()
        val desc = etProjectDesc.text.toString().trim()
        val startDate = etStartDate.text.toString().trim()
        val endDate = etEndDate.text.toString().trim()
        val manager = etProjectManager.text.toString().trim()
        val status = spinnerStatus.text.toString().trim()
        val budgetStr = etBudget.text.toString().trim()
        val currency = spinnerCurrency.text.toString().trim()

        if (code.isEmpty()) { tilProjectCode.error = "Please enter the project code"; isValid = false }
        if (name.isEmpty()) { tilProjectName.error = "Please enter the project name"; isValid = false }
        if (desc.isEmpty()) { tilProjectDesc.error = "Please enter the project description"; isValid = false }
        if (startDate.isEmpty()) { tilStartDate.error = "Please select the start date"; isValid = false }
        if (endDate.isEmpty()) { tilEndDate.error = "Please select the end date"; isValid = false }
        if (manager.isEmpty()) { tilProjectManager.error = "Please enter the project manager"; isValid = false }
        if (status.isEmpty()) { tilStatus.error = "Please select the project status"; isValid = false }
        if (currency.isEmpty()) { tilCurrency.error = "Please select a currency"; isValid = false }

        var budget = 0.0
        if (budgetStr.isEmpty()) {
            tilBudget.error = "Please enter the project budget"; isValid = false
        } else {
            budget = budgetStr.toDoubleOrNull() ?: run {
                tilBudget.error = "Please enter a valid number"; isValid = false; 0.0
            }
            if (budget < 0) { tilBudget.error = "Budget must be positive"; isValid = false }
        }

        if (!isValid) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val project = Project(
            projectCode = code,
            projectName = name,
            projectDescription = desc,
            startDate = startDate,
            endDate = endDate,
            projectManager = manager,
            projectStatus = status,
            projectBudget = budget,
            currency = currency,
            specialRequirements = etSpecialReq.text.toString().trim(),
            clientDepartment = etClientDept.text.toString().trim(),
            priority = spinnerPriority.text.toString().trim(),
            notes = etNotes.text.toString().trim()
        )

        val intent = Intent(this, ConfirmProjectActivity::class.java)
        intent.putExtra("project", project)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
