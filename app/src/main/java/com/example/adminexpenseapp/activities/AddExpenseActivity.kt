package com.example.adminexpenseapp.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.adminexpenseapp.R
import com.example.adminexpenseapp.database.DatabaseHelper
import com.example.adminexpenseapp.models.Expense
import com.example.adminexpenseapp.utils.FirebaseSync
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etExpenseId: TextInputEditText
    private lateinit var etDateOfExpense: TextInputEditText
    private lateinit var etAmount: TextInputEditText
    private lateinit var etClaimant: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var etLocation: TextInputEditText
    private lateinit var spinnerCurrency: AutoCompleteTextView
    private lateinit var spinnerExpenseType: AutoCompleteTextView
    private lateinit var spinnerPaymentMethod: AutoCompleteTextView
    private lateinit var spinnerPaymentStatus: AutoCompleteTextView

    private lateinit var tilExpenseId: TextInputLayout
    private lateinit var tilDateOfExpense: TextInputLayout
    private lateinit var tilAmount: TextInputLayout
    private lateinit var tilCurrency: TextInputLayout
    private lateinit var tilExpenseType: TextInputLayout
    private lateinit var tilPaymentMethod: TextInputLayout
    private lateinit var tilClaimant: TextInputLayout
    private lateinit var tilPaymentStatus: TextInputLayout

    private var projectId: String = ""
    private lateinit var dbHelper: DatabaseHelper
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.UK)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        setSupportActionBar(findViewById(R.id.toolbar))
        title = "Add Expense"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dbHelper = DatabaseHelper.getInstance(this)
        projectId = intent.getStringExtra("project_id") ?: ""

        if (projectId.isEmpty()) {
            Toast.makeText(this, "Error: No project specified", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupSpinners()
        setupDatePicker()
        prefillCurrency()

        findViewById<MaterialButton>(R.id.btnSaveExpense).setOnClickListener {
            validateAndFetchBeforeSave()
        }
    }

    private fun initViews() {
        etExpenseId = findViewById(R.id.etExpenseId)
        etDateOfExpense = findViewById(R.id.etDateOfExpense)
        etAmount = findViewById(R.id.etAmount)
        etClaimant = findViewById(R.id.etClaimant)
        etDescription = findViewById(R.id.etDescription)
        etLocation = findViewById(R.id.etLocation)
        spinnerCurrency = findViewById(R.id.spinnerCurrency)
        spinnerExpenseType = findViewById(R.id.spinnerExpenseType)
        spinnerPaymentMethod = findViewById(R.id.spinnerPaymentMethod)
        spinnerPaymentStatus = findViewById(R.id.spinnerPaymentStatus)

        tilExpenseId = findViewById(R.id.tilExpenseId)
        tilDateOfExpense = findViewById(R.id.tilDateOfExpense)
        tilAmount = findViewById(R.id.tilAmount)
        tilCurrency = findViewById(R.id.tilCurrency)
        tilExpenseType = findViewById(R.id.tilExpenseType)
        tilPaymentMethod = findViewById(R.id.tilPaymentMethod)
        tilClaimant = findViewById(R.id.tilClaimant)
        tilPaymentStatus = findViewById(R.id.tilPaymentStatus)
    }

    private fun setupSpinners() {
        spinnerCurrency.setAdapter(ArrayAdapter(this,
            android.R.layout.simple_dropdown_item_1line, Expense.CURRENCIES))
        spinnerExpenseType.setAdapter(ArrayAdapter(this,
            android.R.layout.simple_dropdown_item_1line, Expense.EXPENSE_TYPES))
        spinnerPaymentMethod.setAdapter(ArrayAdapter(this,
            android.R.layout.simple_dropdown_item_1line, Expense.PAYMENT_METHODS))
        spinnerPaymentStatus.setAdapter(ArrayAdapter(this,
            android.R.layout.simple_dropdown_item_1line, Expense.PAYMENT_STATUSES))
    }

    private fun prefillCurrency() {
        val project = dbHelper.getProjectById(projectId)
        if (project != null && project.currency.isNotBlank()) {
            spinnerCurrency.setText(project.currency, false)
        }
    }

    private fun setupDatePicker() {
        etDateOfExpense.isFocusable = false
        etDateOfExpense.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                etDateOfExpense.setText(dateFormat.format(cal.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun validateAndFetchBeforeSave() {
        val allTils = listOf(tilExpenseId, tilDateOfExpense, tilAmount, tilCurrency,
            tilExpenseType, tilPaymentMethod, tilClaimant, tilPaymentStatus)
        allTils.forEach { it.error = null }

        var isValid = true
        val expId = etExpenseId.text.toString().trim()
        val date = etDateOfExpense.text.toString().trim()
        val amountStr = etAmount.text.toString().trim()
        val currency = spinnerCurrency.text.toString().trim()
        val type = spinnerExpenseType.text.toString().trim()
        val method = spinnerPaymentMethod.text.toString().trim()
        val claimant = etClaimant.text.toString().trim()
        val status = spinnerPaymentStatus.text.toString().trim()

        if (expId.isEmpty()) { tilExpenseId.error = "Please enter the expense ID"; isValid = false }
        if (date.isEmpty()) { tilDateOfExpense.error = "Please select the date"; isValid = false }
        if (currency.isEmpty()) { tilCurrency.error = "Please select a currency"; isValid = false }
        if (type.isEmpty()) { tilExpenseType.error = "Please select the expense type"; isValid = false }
        if (method.isEmpty()) { tilPaymentMethod.error = "Please select a payment method"; isValid = false }
        if (claimant.isEmpty()) { tilClaimant.error = "Please enter the claimant name"; isValid = false }
        if (status.isEmpty()) { tilPaymentStatus.error = "Please select the payment status"; isValid = false }

        var amount = 0.0
        if (amountStr.isEmpty()) {
            tilAmount.error = "Please enter the amount"; isValid = false
        } else {
            amount = amountStr.toDoubleOrNull() ?: run {
                tilAmount.error = "Please enter a valid number"; isValid = false; 0.0
            }
            if (amount <= 0 && isValid) {
                tilAmount.error = "Amount must be greater than zero"; isValid = false
            }
        }

        if (!isValid) return

        val expense = Expense(
            projectId = projectId,
            expenseId = expId,
            dateOfExpense = date,
            amount = amount,
            currency = currency,
            expenseType = type,
            paymentMethod = method,
            claimant = claimant,
            paymentStatus = status,
            description = etDescription.text.toString().trim(),
            location = etLocation.text.toString().trim()
        )

        // Pre-action Fetch
        FirebaseSync.fetchAll(this, object : FirebaseSync.FetchCallback {
            override fun onFetchComplete(newProjects: Int, updatedProjects: Int, deletedLocally: Int) {
                saveExpense(expense)
            }
            override fun onFailure(errorMessage: String) {
                saveExpense(expense)
            }
        })
    }

    private fun saveExpense(expense: Expense) {
        val id = dbHelper.insertExpense(expense)
        if (id.isNotEmpty()) {
            Toast.makeText(this, "Expense added successfully!", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            Toast.makeText(this, "Error saving expense", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
