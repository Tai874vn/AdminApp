package com.example.adminexpenseapp.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.adminexpenseapp.models.Expense
import com.example.adminexpenseapp.models.Project
import com.example.adminexpenseapp.utils.CurrencyConverter

class DatabaseHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "expense_tracker.db"
        private const val DATABASE_VERSION = 5

        private const val TABLE_PROJECTS = "projects"
        private const val COL_ID = "id"
        private const val COL_CODE = "project_code"
        private const val COL_NAME = "project_name"
        private const val COL_DESC = "project_description"
        private const val COL_START = "start_date"
        private const val COL_END = "end_date"
        private const val COL_MANAGER = "project_manager"
        private const val COL_STATUS = "project_status"
        private const val COL_BUDGET = "project_budget"
        private const val COL_CURRENCY = "currency"
        private const val COL_SPECIAL = "special_requirements"
        private const val COL_CLIENT = "client_department"
        private const val COL_PRIORITY = "priority"
        private const val COL_NOTES = "notes"
        private const val COL_SYNC = "last_sync_at"
        private const val COL_CREATED = "created_at"
        private const val COL_UPDATED = "updated_at"

        private const val TABLE_EXPENSES = "expenses"
        private const val COL_EXP_ID = "id"
        private const val COL_EXP_PROJECT = "project_id"
        private const val COL_EXP_CODE = "expense_id"
        private const val COL_EXP_DATE = "date_of_expense"
        private const val COL_EXP_AMOUNT = "amount"
        private const val COL_EXP_CURRENCY = "currency"
        private const val COL_EXP_TYPE = "expense_type"
        private const val COL_EXP_METHOD = "payment_method"
        private const val COL_EXP_CLAIMANT = "claimant"
        private const val COL_EXP_STATUS = "payment_status"
        private const val COL_EXP_DESC = "description"
        private const val COL_EXP_LOCATION = "location"
        private const val COL_EXP_SYNC = "last_sync_at"
        private const val COL_EXP_CREATED = "created_at"
        private const val COL_EXP_UPDATED = "updated_at"

        @Volatile
        private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper =
            instance ?: synchronized(this) {
                instance ?: DatabaseHelper(context).also { instance = it }
            }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_PROJECTS (
                $COL_ID TEXT PRIMARY KEY,
                $COL_CODE TEXT NOT NULL,
                $COL_NAME TEXT NOT NULL,
                $COL_DESC TEXT NOT NULL,
                $COL_START TEXT NOT NULL,
                $COL_END TEXT NOT NULL,
                $COL_MANAGER TEXT NOT NULL,
                $COL_STATUS TEXT NOT NULL,
                $COL_BUDGET REAL NOT NULL,
                $COL_CURRENCY TEXT NOT NULL DEFAULT 'USD',
                $COL_SPECIAL TEXT,
                $COL_CLIENT TEXT,
                $COL_PRIORITY TEXT,
                $COL_NOTES TEXT,
                $COL_SYNC INTEGER DEFAULT 0,
                $COL_CREATED INTEGER,
                $COL_UPDATED INTEGER
            )
        """)

        db.execSQL("""
            CREATE TABLE $TABLE_EXPENSES (
                $COL_EXP_ID TEXT PRIMARY KEY,
                $COL_EXP_PROJECT TEXT NOT NULL,
                $COL_EXP_CODE TEXT NOT NULL,
                $COL_EXP_DATE TEXT NOT NULL,
                $COL_EXP_AMOUNT REAL NOT NULL,
                $COL_EXP_CURRENCY TEXT NOT NULL,
                $COL_EXP_TYPE TEXT NOT NULL,
                $COL_EXP_METHOD TEXT NOT NULL,
                $COL_EXP_CLAIMANT TEXT NOT NULL,
                $COL_EXP_STATUS TEXT NOT NULL,
                $COL_EXP_DESC TEXT,
                $COL_EXP_LOCATION TEXT,
                $COL_EXP_SYNC INTEGER DEFAULT 0,
                $COL_EXP_CREATED INTEGER,
                $COL_EXP_UPDATED INTEGER,
                FOREIGN KEY($COL_EXP_PROJECT) REFERENCES $TABLE_PROJECTS($COL_ID) ON DELETE CASCADE
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_EXPENSES")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PROJECTS")
        onCreate(db)
    }

    override fun onOpen(db: SQLiteDatabase) {
        super.onOpen(db)
        db.execSQL("PRAGMA foreign_keys=ON;")
    }

    /** Helper to temporarily disable FK checks during sync to avoid crashes */
    fun setForeignKeysEnabled(enabled: Boolean) {
        writableDatabase.execSQL("PRAGMA foreign_keys = ${if (enabled) "ON" else "OFF"};")
    }

    fun insertProject(project: Project): String {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_ID, project.id)
            put(COL_CODE, project.projectCode)
            put(COL_NAME, project.projectName)
            put(COL_DESC, project.projectDescription)
            put(COL_START, project.startDate)
            put(COL_END, project.endDate)
            put(COL_MANAGER, project.projectManager)
            put(COL_STATUS, project.projectStatus)
            put(COL_BUDGET, project.projectBudget)
            put(COL_CURRENCY, project.currency)
            put(COL_SPECIAL, project.specialRequirements)
            put(COL_CLIENT, project.clientDepartment)
            put(COL_PRIORITY, project.priority)
            put(COL_NOTES, project.notes)
            put(COL_SYNC, project.lastSyncAt)
            put(COL_CREATED, project.createdAt)
            put(COL_UPDATED, project.updatedAt)
        }
        db.insertWithOnConflict(TABLE_PROJECTS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return project.id
    }

    fun updateProject(project: Project): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_CODE, project.projectCode)
            put(COL_NAME, project.projectName)
            put(COL_DESC, project.projectDescription)
            put(COL_START, project.startDate)
            put(COL_END, project.endDate)
            put(COL_MANAGER, project.projectManager)
            put(COL_STATUS, project.projectStatus)
            put(COL_BUDGET, project.projectBudget)
            put(COL_CURRENCY, project.currency)
            put(COL_SPECIAL, project.specialRequirements)
            put(COL_CLIENT, project.clientDepartment)
            put(COL_PRIORITY, project.priority)
            put(COL_NOTES, project.notes)
            put(COL_SYNC, project.lastSyncAt)
            put(COL_UPDATED, System.currentTimeMillis())
        }
        return db.update(TABLE_PROJECTS, values, "$COL_ID = ?", arrayOf(project.id))
    }

    fun deleteProject(projectId: String): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_PROJECTS, "$COL_ID = ?", arrayOf(projectId)) > 0
    }

    fun getProjectById(id: String): Project? {
        val db = readableDatabase
        val cursor = db.query(TABLE_PROJECTS, null, "$COL_ID = ?", arrayOf(id), null, null, null)
        return cursor.use { if (it.moveToFirst()) cursorToProject(it) else null }
    }

    fun getAllProjects(): List<Project> {
        val projects = mutableListOf<Project>()
        val db = readableDatabase
        val cursor = db.query(TABLE_PROJECTS, null, null, null, null, null, "$COL_UPDATED DESC")
        cursor.use { while (it.moveToNext()) projects.add(cursorToProject(it)) }
        return projects
    }

    fun searchProjects(query: String): List<Project> {
        val projects = mutableListOf<Project>()
        val db = readableDatabase
        val cursor = db.query(TABLE_PROJECTS, null, "$COL_NAME LIKE ? OR $COL_DESC LIKE ?", arrayOf("%$query%", "%$query%"), null, null, "$COL_UPDATED DESC")
        cursor.use { while (it.moveToNext()) projects.add(cursorToProject(it)) }
        return projects
    }

    fun advancedSearchProjects(date: String?, status: String?, owner: String?): List<Project> {
        val projects = mutableListOf<Project>()
        val db = readableDatabase
        val selection = StringBuilder("1=1")
        val args = mutableListOf<String>()
        if (!date.isNullOrBlank()) { selection.append(" AND ($COL_START LIKE ? OR $COL_END LIKE ?)"); args.add("%$date%"); args.add("%$date%") }
        if (!status.isNullOrBlank()) { selection.append(" AND $COL_STATUS = ?"); args.add(status) }
        if (!owner.isNullOrBlank()) { selection.append(" AND $COL_MANAGER LIKE ?"); args.add("%$owner%") }
        val cursor = db.query(TABLE_PROJECTS, null, selection.toString(), args.toTypedArray(), null, null, "$COL_UPDATED DESC")
        cursor.use { while (it.moveToNext()) projects.add(cursorToProject(it)) }
        return projects
    }

    fun updateSyncTimestamp(projectId: String, timestamp: Long) {
        val db = writableDatabase
        val values = ContentValues().apply { put(COL_SYNC, timestamp) }
        db.update(TABLE_PROJECTS, values, "$COL_ID = ?", arrayOf(projectId))
    }

    fun resetDatabase() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TABLE_EXPENSES")
        db.execSQL("DELETE FROM $TABLE_PROJECTS")
    }

    private fun cursorToProject(cursor: Cursor): Project = Project(
        id = cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)),
        projectCode = cursor.getString(cursor.getColumnIndexOrThrow(COL_CODE)),
        projectName = cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
        projectDescription = cursor.getString(cursor.getColumnIndexOrThrow(COL_DESC)),
        startDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_START)),
        endDate = cursor.getString(cursor.getColumnIndexOrThrow(COL_END)),
        projectManager = cursor.getString(cursor.getColumnIndexOrThrow(COL_MANAGER)),
        projectStatus = cursor.getString(cursor.getColumnIndexOrThrow(COL_STATUS)),
        projectBudget = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_BUDGET)),
        currency = cursor.getString(cursor.getColumnIndexOrThrow(COL_CURRENCY)),
        specialRequirements = cursor.getString(cursor.getColumnIndexOrThrow(COL_SPECIAL)) ?: "",
        clientDepartment = cursor.getString(cursor.getColumnIndexOrThrow(COL_CLIENT)) ?: "",
        priority = cursor.getString(cursor.getColumnIndexOrThrow(COL_PRIORITY)) ?: "",
        notes = cursor.getString(cursor.getColumnIndexOrThrow(COL_NOTES)) ?: "",
        lastSyncAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_SYNC)),
        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CREATED)),
        updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED))
    )

    fun insertExpense(expense: Expense): String {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_EXP_ID, expense.id)
            put(COL_EXP_PROJECT, expense.projectId)
            put(COL_EXP_CODE, expense.expenseId)
            put(COL_EXP_DATE, expense.dateOfExpense)
            put(COL_EXP_AMOUNT, expense.amount)
            put(COL_EXP_CURRENCY, expense.currency)
            put(COL_EXP_TYPE, expense.expenseType)
            put(COL_EXP_METHOD, expense.paymentMethod)
            put(COL_EXP_CLAIMANT, expense.claimant)
            put(COL_EXP_STATUS, expense.paymentStatus)
            put(COL_EXP_DESC, expense.description)
            put(COL_EXP_LOCATION, expense.location)
            put(COL_EXP_SYNC, expense.lastSyncAt)
            put(COL_EXP_CREATED, expense.createdAt)
            put(COL_EXP_UPDATED, expense.updatedAt)
        }
        db.insertWithOnConflict(TABLE_EXPENSES, null, values, SQLiteDatabase.CONFLICT_REPLACE)
        return expense.id
    }

    fun updateExpense(expense: Expense): Int {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_EXP_CODE, expense.expenseId)
            put(COL_EXP_DATE, expense.dateOfExpense)
            put(COL_EXP_AMOUNT, expense.amount)
            put(COL_EXP_CURRENCY, expense.currency)
            put(COL_EXP_TYPE, expense.expenseType)
            put(COL_EXP_METHOD, expense.paymentMethod)
            put(COL_EXP_CLAIMANT, expense.claimant)
            put(COL_EXP_STATUS, expense.paymentStatus)
            put(COL_EXP_DESC, expense.description)
            put(COL_EXP_LOCATION, expense.location)
            put(COL_EXP_SYNC, expense.lastSyncAt)
            put(COL_EXP_UPDATED, System.currentTimeMillis())
        }
        return db.update(TABLE_EXPENSES, values, "$COL_EXP_ID = ?", arrayOf(expense.id))
    }

    fun updateExpenseSyncTimestamp(expenseId: String, timestamp: Long) {
        val db = writableDatabase
        val values = ContentValues().apply { put(COL_EXP_SYNC, timestamp) }
        db.update(TABLE_EXPENSES, values, "$COL_EXP_ID = ?", arrayOf(expenseId))
    }

    fun deleteExpense(expenseId: String): Boolean {
        val db = writableDatabase
        return db.delete(TABLE_EXPENSES, "$COL_EXP_ID = ?", arrayOf(expenseId)) > 0
    }

    fun getExpensesByProjectId(projectId: String): List<Expense> {
        val expenses = mutableListOf<Expense>()
        val db = readableDatabase
        val cursor = db.query(TABLE_EXPENSES, null, "$COL_EXP_PROJECT = ?", arrayOf(projectId), null, null, "$COL_EXP_UPDATED DESC")
        cursor.use { while (it.moveToNext()) expenses.add(cursorToExpense(it)) }
        return expenses
    }

    fun getExpenseById(id: String): Expense? {
        val db = readableDatabase
        val cursor = db.query(TABLE_EXPENSES, null, "$COL_EXP_ID = ?", arrayOf(id), null, null, null)
        return cursor.use { if (it.moveToFirst()) cursorToExpense(it) else null }
    }

    fun getTotalExpenses(projectId: String): Double {
        val project = getProjectById(projectId) ?: return 0.0
        val expenses = getExpensesByProjectId(projectId)
        var total = 0.0
        for (expense in expenses) {
            val convertedAmount = CurrencyConverter.convert(expense.amount, expense.currency, project.currency)
            total += convertedAmount
        }
        return total
    }

    private fun cursorToExpense(cursor: Cursor): Expense = Expense(
        id = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXP_ID)),
        projectId = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXP_PROJECT)),
        expenseId = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXP_CODE)),
        dateOfExpense = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXP_DATE)),
        amount = cursor.getDouble(cursor.getColumnIndexOrThrow(COL_EXP_AMOUNT)),
        currency = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXP_CURRENCY)),
        expenseType = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXP_TYPE)),
        paymentMethod = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXP_METHOD)),
        claimant = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXP_CLAIMANT)),
        paymentStatus = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXP_STATUS)),
        description = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXP_DESC)) ?: "",
        location = cursor.getString(cursor.getColumnIndexOrThrow(COL_EXP_LOCATION)) ?: "",
        lastSyncAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EXP_SYNC)),
        createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EXP_CREATED)),
        updatedAt = cursor.getLong(cursor.getColumnIndexOrThrow(COL_EXP_UPDATED))
    )
}
