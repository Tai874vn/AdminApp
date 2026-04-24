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
    SQLiteOpenHelper(context.applicationContext, DB_FILE_NAME, null, DB_VERSION_NUMBER) {

    companion object {
        private const val DB_FILE_NAME = "expense_tracker.db"
        private const val DB_VERSION_NUMBER = 5

        private const val TBL_PROJECTS = "projects"
        private const val FLD_ID = "id"
        private const val FLD_CODE = "project_code"
        private const val FLD_NAME = "project_name"
        private const val FLD_DESC = "project_description"
        private const val FLD_START = "start_date"
        private const val FLD_END = "end_date"
        private const val FLD_MANAGER = "project_manager"
        private const val FLD_STATUS = "project_status"
        private const val FLD_BUDGET = "project_budget"
        private const val FLD_CURRENCY = "currency"
        private const val FLD_SPECIAL = "special_requirements"
        private const val FLD_CLIENT = "client_department"
        private const val FLD_PRIORITY = "priority"
        private const val FLD_NOTES = "notes"
        private const val FLD_SYNC = "last_sync_at"
        private const val FLD_CREATED = "created_at"
        private const val FLD_UPDATED = "updated_at"

        private const val TBL_EXPENSES = "expenses"
        private const val FLD_EXP_ID = "id"
        private const val FLD_EXP_PROJECT = "project_id"
        private const val FLD_EXP_CODE = "expense_id"
        private const val FLD_EXP_DATE = "date_of_expense"
        private const val FLD_EXP_AMOUNT = "amount"
        private const val FLD_EXP_CURRENCY = "currency"
        private const val FLD_EXP_TYPE = "expense_type"
        private const val FLD_EXP_METHOD = "payment_method"
        private const val FLD_EXP_CLAIMANT = "claimant"
        private const val FLD_EXP_STATUS = "payment_status"
        private const val FLD_EXP_DESC = "description"
        private const val FLD_EXP_LOCATION = "location"
        private const val FLD_EXP_SYNC = "last_sync_at"
        private const val FLD_EXP_CREATED = "created_at"
        private const val FLD_EXP_UPDATED = "updated_at"

        @Volatile
        private var dbProvider: DatabaseHelper? = null

        fun getInstance(ctx: Context): DatabaseHelper =
            dbProvider ?: synchronized(this) {
                dbProvider ?: DatabaseHelper(ctx).also { dbProvider = it }
            }
    }

    override fun onCreate(sqlDb: SQLiteDatabase) {
        sqlDb.execSQL("""
            CREATE TABLE $TBL_PROJECTS (
                $FLD_ID TEXT PRIMARY KEY,
                $FLD_CODE TEXT NOT NULL,
                $FLD_NAME TEXT NOT NULL,
                $FLD_DESC TEXT NOT NULL,
                $FLD_START TEXT NOT NULL,
                $FLD_END TEXT NOT NULL,
                $FLD_MANAGER TEXT NOT NULL,
                $FLD_STATUS TEXT NOT NULL,
                $FLD_BUDGET REAL NOT NULL,
                $FLD_CURRENCY TEXT NOT NULL DEFAULT 'USD',
                $FLD_SPECIAL TEXT,
                $FLD_CLIENT TEXT,
                $FLD_PRIORITY TEXT,
                $FLD_NOTES TEXT,
                $FLD_SYNC INTEGER DEFAULT 0,
                $FLD_CREATED INTEGER,
                $FLD_UPDATED INTEGER
            )
        """)

        sqlDb.execSQL("""
            CREATE TABLE $TBL_EXPENSES (
                $FLD_EXP_ID TEXT PRIMARY KEY,
                $FLD_EXP_PROJECT TEXT NOT NULL,
                $FLD_EXP_CODE TEXT NOT NULL,
                $FLD_EXP_DATE TEXT NOT NULL,
                $FLD_EXP_AMOUNT REAL NOT NULL,
                $FLD_EXP_CURRENCY TEXT NOT NULL,
                $FLD_EXP_TYPE TEXT NOT NULL,
                $FLD_EXP_METHOD TEXT NOT NULL,
                $FLD_EXP_CLAIMANT TEXT NOT NULL,
                $FLD_EXP_STATUS TEXT NOT NULL,
                $FLD_EXP_DESC TEXT,
                $FLD_EXP_LOCATION TEXT,
                $FLD_EXP_SYNC INTEGER DEFAULT 0,
                $FLD_EXP_CREATED INTEGER,
                $FLD_EXP_UPDATED INTEGER,
                FOREIGN KEY($FLD_EXP_PROJECT) REFERENCES $TBL_PROJECTS($FLD_ID) ON DELETE CASCADE
            )
        """)
    }

    override fun onUpgrade(sqlDb: SQLiteDatabase, oldVer: Int, newVer: Int) {
        sqlDb.execSQL("DROP TABLE IF EXISTS $TBL_EXPENSES")
        sqlDb.execSQL("DROP TABLE IF EXISTS $TBL_PROJECTS")
        onCreate(sqlDb)
    }

    override fun onOpen(sqlDb: SQLiteDatabase) {
        super.onOpen(sqlDb)
        sqlDb.execSQL("PRAGMA foreign_keys=ON;")
    }

    /** Helper to temporarily disable FK checks during sync to avoid crashes */
    fun setForeignKeysEnabled(isAllowed: Boolean) {
        writableDatabase.execSQL("PRAGMA foreign_keys = ${if (isAllowed) "ON" else "OFF"};")
    }

    fun insertProject(proj: Project): String {
        val db = writableDatabase
        val content = ContentValues().apply {
            put(FLD_ID, proj.id)
            put(FLD_CODE, proj.projectCode)
            put(FLD_NAME, proj.projectName)
            put(FLD_DESC, proj.projectDescription)
            put(FLD_START, proj.startDate)
            put(FLD_END, proj.endDate)
            put(FLD_MANAGER, proj.projectManager)
            put(FLD_STATUS, proj.projectStatus)
            put(FLD_BUDGET, proj.projectBudget)
            put(FLD_CURRENCY, proj.currency)
            put(FLD_SPECIAL, proj.specialRequirements)
            put(FLD_CLIENT, proj.clientDepartment)
            put(FLD_PRIORITY, proj.priority)
            put(FLD_NOTES, proj.notes)
            put(FLD_SYNC, proj.lastSyncAt)
            put(FLD_CREATED, proj.createdAt)
            put(FLD_UPDATED, proj.updatedAt)
        }
        db.insertWithOnConflict(TBL_PROJECTS, null, content, SQLiteDatabase.CONFLICT_REPLACE)
        return proj.id
    }

    fun updateProject(proj: Project): Int {
        val db = writableDatabase
        val content = ContentValues().apply {
            put(FLD_CODE, proj.projectCode)
            put(FLD_NAME, proj.projectName)
            put(FLD_DESC, proj.projectDescription)
            put(FLD_START, proj.startDate)
            put(FLD_END, proj.endDate)
            put(FLD_MANAGER, proj.projectManager)
            put(FLD_STATUS, proj.projectStatus)
            put(FLD_BUDGET, proj.projectBudget)
            put(FLD_CURRENCY, proj.currency)
            put(FLD_SPECIAL, proj.specialRequirements)
            put(FLD_CLIENT, proj.clientDepartment)
            put(FLD_PRIORITY, proj.priority)
            put(FLD_NOTES, proj.notes)
            put(FLD_SYNC, proj.lastSyncAt)
            put(FLD_UPDATED, System.currentTimeMillis())
        }
        return db.update(TBL_PROJECTS, content, "$FLD_ID = ?", arrayOf(proj.id))
    }

    fun deleteProject(projId: String): Boolean {
        val db = writableDatabase
        return db.delete(TBL_PROJECTS, "$FLD_ID = ?", arrayOf(projId)) > 0
    }

    fun getProjectById(projId: String): Project? {
        val db = readableDatabase
        val res = db.query(TBL_PROJECTS, null, "$FLD_ID = ?", arrayOf(projId), null, null, null)
        return res.use { if (it.moveToFirst()) mapToProject(it) else null }
    }

    fun getAllProjects(): List<Project> {
        val list = mutableListOf<Project>()
        val db = readableDatabase
        val res = db.query(TBL_PROJECTS, null, null, null, null, null, "$FLD_UPDATED DESC")
        res.use { while (it.moveToNext()) list.add(mapToProject(it)) }
        return list
    }

    fun searchProjects(term: String): List<Project> {
        val list = mutableListOf<Project>()
        val db = readableDatabase
        val res = db.query(TBL_PROJECTS, null, "$FLD_NAME LIKE ? OR $FLD_DESC LIKE ?", arrayOf("%$term%", "%$term%"), null, null, "$FLD_UPDATED DESC")
        res.use { while (it.moveToNext()) list.add(mapToProject(it)) }
        return list
    }

    fun advancedSearchProjects(date: String?, status: String?, manager: String?): List<Project> {
        val list = mutableListOf<Project>()
        val db = readableDatabase
        val query = StringBuilder("1=1")
        val params = mutableListOf<String>()
        if (!date.isNullOrBlank()) { query.append(" AND ($FLD_START LIKE ? OR $FLD_END LIKE ?)"); params.add("%$date%"); params.add("%$date%") }
        if (!status.isNullOrBlank()) { query.append(" AND $FLD_STATUS = ?"); params.add(status) }
        if (!manager.isNullOrBlank()) { query.append(" AND $FLD_MANAGER LIKE ?"); params.add("%$manager%") }
        val res = db.query(TBL_PROJECTS, null, query.toString(), params.toTypedArray(), null, null, "$FLD_UPDATED DESC")
        res.use { while (it.moveToNext()) list.add(mapToProject(it)) }
        return list
    }

    fun updateSyncTimestamp(projId: String, time: Long) {
        val db = writableDatabase
        val content = ContentValues().apply { put(FLD_SYNC, time) }
        db.update(TBL_PROJECTS, content, "$FLD_ID = ?", arrayOf(projId))
    }

    fun resetDatabase() {
        val db = writableDatabase
        db.execSQL("DELETE FROM $TBL_EXPENSES")
        db.execSQL("DELETE FROM $TBL_PROJECTS")
    }

    private fun mapToProject(row: Cursor): Project = Project(
        id = row.getString(row.getColumnIndexOrThrow(FLD_ID)),
        projectCode = row.getString(row.getColumnIndexOrThrow(FLD_CODE)),
        projectName = row.getString(row.getColumnIndexOrThrow(FLD_NAME)),
        projectDescription = row.getString(row.getColumnIndexOrThrow(FLD_DESC)),
        startDate = row.getString(row.getColumnIndexOrThrow(FLD_START)),
        endDate = row.getString(row.getColumnIndexOrThrow(FLD_END)),
        projectManager = row.getString(row.getColumnIndexOrThrow(FLD_MANAGER)),
        projectStatus = row.getString(row.getColumnIndexOrThrow(FLD_STATUS)),
        projectBudget = row.getDouble(row.getColumnIndexOrThrow(FLD_BUDGET)),
        currency = row.getString(row.getColumnIndexOrThrow(FLD_CURRENCY)),
        specialRequirements = row.getString(row.getColumnIndexOrThrow(FLD_SPECIAL)) ?: "",
        clientDepartment = row.getString(row.getColumnIndexOrThrow(FLD_CLIENT)) ?: "",
        priority = row.getString(row.getColumnIndexOrThrow(FLD_PRIORITY)) ?: "",
        notes = row.getString(row.getColumnIndexOrThrow(FLD_NOTES)) ?: "",
        lastSyncAt = row.getLong(row.getColumnIndexOrThrow(FLD_SYNC)),
        createdAt = row.getLong(row.getColumnIndexOrThrow(FLD_CREATED)),
        updatedAt = row.getLong(row.getColumnIndexOrThrow(FLD_UPDATED))
    )

    fun insertExpense(exp: Expense): String {
        val db = writableDatabase
        val content = ContentValues().apply {
            put(FLD_EXP_ID, exp.id)
            put(FLD_EXP_PROJECT, exp.projectId)
            put(FLD_EXP_CODE, exp.expenseId)
            put(FLD_EXP_DATE, exp.dateOfExpense)
            put(FLD_EXP_AMOUNT, exp.amount)
            put(FLD_EXP_CURRENCY, exp.currency)
            put(FLD_EXP_TYPE, exp.expenseType)
            put(FLD_EXP_METHOD, exp.paymentMethod)
            put(FLD_EXP_CLAIMANT, exp.claimant)
            put(FLD_EXP_STATUS, exp.paymentStatus)
            put(FLD_EXP_DESC, exp.description)
            put(FLD_EXP_LOCATION, exp.location)
            put(FLD_EXP_SYNC, exp.lastSyncAt)
            put(FLD_EXP_CREATED, exp.createdAt)
            put(FLD_EXP_UPDATED, exp.updatedAt)
        }
        db.insertWithOnConflict(TBL_EXPENSES, null, content, SQLiteDatabase.CONFLICT_REPLACE)
        return exp.id
    }

    fun updateExpense(exp: Expense): Int {
        val db = writableDatabase
        val content = ContentValues().apply {
            put(FLD_EXP_CODE, exp.expenseId)
            put(FLD_EXP_DATE, exp.dateOfExpense)
            put(FLD_EXP_AMOUNT, exp.amount)
            put(FLD_EXP_CURRENCY, exp.currency)
            put(FLD_EXP_TYPE, exp.expenseType)
            put(FLD_EXP_METHOD, exp.paymentMethod)
            put(FLD_EXP_CLAIMANT, exp.claimant)
            put(FLD_EXP_STATUS, exp.paymentStatus)
            put(FLD_EXP_DESC, exp.description)
            put(FLD_EXP_LOCATION, exp.location)
            put(FLD_EXP_SYNC, exp.lastSyncAt)
            put(FLD_EXP_UPDATED, System.currentTimeMillis())
        }
        return db.update(TBL_EXPENSES, content, "$FLD_EXP_ID = ?", arrayOf(exp.id))
    }

    fun updateExpenseSyncTimestamp(expId: String, time: Long) {
        val db = writableDatabase
        val content = ContentValues().apply { put(FLD_EXP_SYNC, time) }
        db.update(TBL_EXPENSES, content, "$FLD_EXP_ID = ?", arrayOf(expId))
    }

    fun deleteExpense(expId: String): Boolean {
        val db = writableDatabase
        return db.delete(TBL_EXPENSES, "$FLD_EXP_ID = ?", arrayOf(expId)) > 0
    }

    fun getExpensesByProjectId(projId: String): List<Expense> {
        val list = mutableListOf<Expense>()
        val db = readableDatabase
        val res = db.query(TBL_EXPENSES, null, "$FLD_EXP_PROJECT = ?", arrayOf(projId), null, null, "$FLD_EXP_UPDATED DESC")
        res.use { while (it.moveToNext()) list.add(mapToExpense(it)) }
        return list
    }

    fun getExpenseById(expId: String): Expense? {
        val db = readableDatabase
        val res = db.query(TBL_EXPENSES, null, "$FLD_EXP_ID = ?", arrayOf(expId), null, null, null)
        return res.use { if (it.moveToFirst()) mapToExpense(it) else null }
    }

    fun getTotalExpenses(projId: String): Double {
        val project = getProjectById(projId) ?: return 0.0
        val expenses = getExpensesByProjectId(projId)
        var sum = 0.0
        for (item in expenses) {
            val converted = CurrencyConverter.convert(item.amount, item.currency, project.currency)
            sum += converted
        }
        return sum
    }

    private fun mapToExpense(row: Cursor): Expense = Expense(
        id = row.getString(row.getColumnIndexOrThrow(FLD_EXP_ID)),
        projectId = row.getString(row.getColumnIndexOrThrow(FLD_EXP_PROJECT)),
        expenseId = row.getString(row.getColumnIndexOrThrow(FLD_EXP_CODE)),
        dateOfExpense = row.getString(row.getColumnIndexOrThrow(FLD_EXP_DATE)),
        amount = row.getDouble(row.getColumnIndexOrThrow(FLD_EXP_AMOUNT)),
        currency = row.getString(row.getColumnIndexOrThrow(FLD_EXP_CURRENCY)),
        expenseType = row.getString(row.getColumnIndexOrThrow(FLD_EXP_TYPE)),
        paymentMethod = row.getString(row.getColumnIndexOrThrow(FLD_EXP_METHOD)),
        claimant = row.getString(row.getColumnIndexOrThrow(FLD_EXP_CLAIMANT)),
        paymentStatus = row.getString(row.getColumnIndexOrThrow(FLD_EXP_STATUS)),
        description = row.getString(row.getColumnIndexOrThrow(FLD_EXP_DESC)) ?: "",
        location = row.getString(row.getColumnIndexOrThrow(FLD_EXP_LOCATION)) ?: "",
        lastSyncAt = row.getLong(row.getColumnIndexOrThrow(FLD_EXP_SYNC)),
        createdAt = row.getLong(row.getColumnIndexOrThrow(FLD_EXP_CREATED)),
        updatedAt = row.getLong(row.getColumnIndexOrThrow(FLD_EXP_UPDATED))
    )
}
