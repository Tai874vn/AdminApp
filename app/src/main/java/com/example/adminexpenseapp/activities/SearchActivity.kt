package com.example.adminexpenseapp.activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.adminexpenseapp.R
import com.example.adminexpenseapp.adapters.ProjectAdapter
import com.example.adminexpenseapp.database.DatabaseHelper
import com.example.adminexpenseapp.models.Project
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SearchActivity : AppCompatActivity() {

    private lateinit var etSearch: TextInputEditText
    private lateinit var etSearchDate: TextInputEditText
    private lateinit var etSearchOwner: TextInputEditText
    private lateinit var spinnerSearchStatus: AutoCompleteTextView
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var tvNoResults: TextView
    private lateinit var tvResultCount: TextView
    private lateinit var layoutAdvanced: LinearLayout
    private lateinit var btnToggleAdvanced: MaterialButton
    private lateinit var btnAdvancedSearch: MaterialButton
    private lateinit var adapter: ProjectAdapter
    private lateinit var dbHelper: DatabaseHelper

    private var advancedVisible = false
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.UK)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        setSupportActionBar(findViewById(R.id.toolbar))
        title = "Search Projects"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dbHelper = DatabaseHelper.getInstance(this)

        initViews()
        setupRecyclerView()
        setupBasicSearch()
        setupAdvancedSearch()
    }

    private fun initViews() {
        etSearch = findViewById(R.id.etSearch)
        etSearchDate = findViewById(R.id.etSearchDate)
        etSearchOwner = findViewById(R.id.etSearchOwner)
        spinnerSearchStatus = findViewById(R.id.spinnerSearchStatus)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        tvNoResults = findViewById(R.id.tvNoResults)
        tvResultCount = findViewById(R.id.tvResultCount)
        layoutAdvanced = findViewById(R.id.layoutAdvanced)
        btnToggleAdvanced = findViewById(R.id.btnToggleAdvanced)
        btnAdvancedSearch = findViewById(R.id.btnAdvancedSearch)
    }

    private fun setupRecyclerView() {
        rvSearchResults.layoutManager = LinearLayoutManager(this)
        adapter = ProjectAdapter(
            onProjectClick = { project ->
                val intent = Intent(this, ProjectDetailActivity::class.java)
                intent.putExtra("project_id", project.id)
                startActivity(intent)
            },
            onProjectLongClick = { _ ->
            }
        )
        rvSearchResults.adapter = adapter
    }

    private fun setupBasicSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                if (query.isNotEmpty()) {
                    displayResults(dbHelper.searchProjects(query))
                } else {
                    adapter.setProjects(emptyList())
                    tvNoResults.visibility = View.GONE
                    tvResultCount.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupAdvancedSearch() {
        btnToggleAdvanced.setOnClickListener {
            advancedVisible = !advancedVisible
            layoutAdvanced.visibility = if (advancedVisible) View.VISIBLE else View.GONE
            btnToggleAdvanced.text = if (advancedVisible) "Hide Advanced Search" else "Advanced Search"
        }

        val statuses = arrayOf("", "Active", "Completed", "On Hold")
        spinnerSearchStatus.setAdapter(ArrayAdapter(this,
            android.R.layout.simple_dropdown_item_1line, statuses))

        etSearchDate.isFocusable = false
        etSearchDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(this, { _, y, m, d ->
                cal.set(y, m, d)
                etSearchDate.setText(dateFormat.format(cal.time))
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        btnAdvancedSearch.setOnClickListener {
            val date = etSearchDate.text.toString().trim()
            val status = spinnerSearchStatus.text.toString().trim()
            val owner = etSearchOwner.text.toString().trim()
            displayResults(dbHelper.advancedSearchProjects(date, status, owner))
        }
    }

    private fun displayResults(results: List<Project>) {
        adapter.setProjects(results)
        if (results.isEmpty()) {
            tvNoResults.visibility = View.VISIBLE
            rvSearchResults.visibility = View.GONE
            tvResultCount.visibility = View.GONE
        } else {
            tvNoResults.visibility = View.GONE
            rvSearchResults.visibility = View.VISIBLE
            tvResultCount.visibility = View.VISIBLE
            tvResultCount.text = getString(R.string.results_found, results.size)
        }
    }

    override fun onSupportNavigateUp(): Boolean { finish(); return true }
}
