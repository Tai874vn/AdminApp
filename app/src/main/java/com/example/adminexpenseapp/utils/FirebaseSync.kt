package com.example.adminexpenseapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.adminexpenseapp.database.DatabaseHelper
import com.example.adminexpenseapp.models.Expense
import com.example.adminexpenseapp.models.Project
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

object FirebaseSync {

    private const val COLLECTION_PROJECTS = "projects"
    private const val COLLECTION_EXPENSES = "expenses"

    interface SyncCallback {
        fun onSuccess(projectCount: Int, expenseCount: Int)
        fun onFailure(errorMessage: String)
    }

    interface FetchCallback {
        fun onFetchComplete(newProjects: Int, updatedProjects: Int, deletedLocally: Int)
        fun onFailure(errorMessage: String)
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun uploadProjects(context: Context, projects: List<Project>, callback: SyncCallback) {
        if (!isNetworkAvailable(context)) {
            callback.onFailure("No network connection.")
            return
        }

        val db = FirebaseFirestore.getInstance()
        val dbHelper = DatabaseHelper.getInstance(context)
        val batch = db.batch()
        var expenseCount = 0
        val syncTime = System.currentTimeMillis()

        for (project in projects) {
            project.lastSyncAt = syncTime
            val projectRef = db.collection(COLLECTION_PROJECTS).document(project.id)
            batch.set(projectRef, project.toMap())

            val expenses = dbHelper.getExpensesByProjectId(project.id)
            for (expense in expenses) {
                expense.lastSyncAt = syncTime
                val expenseRef = projectRef.collection(COLLECTION_EXPENSES).document(expense.id)
                batch.set(expenseRef, expense.toMap())
                expenseCount++
            }
        }

        batch.commit()
            .addOnSuccessListener {
                for (p in projects) {
                    dbHelper.updateSyncTimestamp(p.id, syncTime)
                    dbHelper.getExpensesByProjectId(p.id).forEach {
                        dbHelper.updateExpenseSyncTimestamp(it.id, syncTime)
                    }
                }
                callback.onSuccess(projects.size, expenseCount)
            }
            .addOnFailureListener { e -> callback.onFailure("Upload failed: ${e.message}") }
    }

    fun uploadAll(context: Context, callback: SyncCallback) {
        val dbHelper = DatabaseHelper.getInstance(context)
        uploadProjects(context, dbHelper.getAllProjects(), callback)
    }

    fun deleteProjectsFromCloud(context: Context, projectIds: List<String>, callback: SyncCallback) {
        if (!isNetworkAvailable(context)) {
            callback.onFailure("No network connection.")
            return
        }
        val db = FirebaseFirestore.getInstance()
        
        var deletedProjects = 0
        val totalToDelete = projectIds.size

        if (projectIds.isEmpty()) {
            callback.onSuccess(0, 0)
            return
        }

        for (id in projectIds) {
            val projectRef = db.collection(COLLECTION_PROJECTS).document(id)
            
            projectRef.collection(COLLECTION_EXPENSES).get().addOnSuccessListener { expenses ->
                val batch = db.batch()
                
                for (expDoc in expenses) {
                    batch.delete(expDoc.reference)
                }
                
                batch.delete(projectRef)
                
                batch.commit().addOnSuccessListener {
                    deletedProjects++
                    if (deletedProjects == totalToDelete) {
                        callback.onSuccess(deletedProjects, 0)
                    }
                }
            }.addOnFailureListener {
                projectRef.delete().addOnCompleteListener {
                    deletedProjects++
                    if (deletedProjects == totalToDelete) {
                        callback.onSuccess(deletedProjects, 0)
                    }
                }
            }
        }
    }

    fun fetchAll(context: Context, callback: FetchCallback) {
        if (!isNetworkAvailable(context)) {
            callback.onFailure("No network connection.")
            return
        }

        val db = FirebaseFirestore.getInstance()
        val dbHelper = DatabaseHelper.getInstance(context)

        dbHelper.setForeignKeysEnabled(false)

        db.collection(COLLECTION_PROJECTS).get(Source.SERVER)
            .addOnSuccessListener { projectSnapshots ->
                syncProjects(context, projectSnapshots, dbHelper, callback)
            }
            .addOnFailureListener { e ->
                dbHelper.setForeignKeysEnabled(true)
                callback.onFailure("Fetch failed: ${e.message}")
            }
    }

    private fun syncProjects(
        context: Context,
        projectSnapshots: com.google.firebase.firestore.QuerySnapshot,
        dbHelper: DatabaseHelper,
        callback: FetchCallback
    ) {
        val deletedCount = deleteRemovedProjects(projectSnapshots, dbHelper)

        if (projectSnapshots.isEmpty) {
            dbHelper.setForeignKeysEnabled(true)
            callback.onFetchComplete(0, 0, deletedCount)
            return
        }

        val syncTime = System.currentTimeMillis()
        val syncStats = SyncStats(deletedCount = deletedCount)
        val totalProjects = projectSnapshots.size()

        for (doc in projectSnapshots) {
            val cloudProject = mapToProject(doc.id, doc.data)

            val wasInserted = syncProjectToLocal(cloudProject, syncTime, dbHelper)
            if (wasInserted) syncStats.newCount++ else syncStats.updateCount++

            syncExpensesForProject(
                doc.reference,
                cloudProject.id,
                syncTime,
                dbHelper,
                syncStats,
                totalProjects,
                callback
            )
        }
    }

    private fun deleteRemovedProjects(
        projectSnapshots: com.google.firebase.firestore.QuerySnapshot,
        dbHelper: DatabaseHelper
    ): Int {
        val cloudIds = projectSnapshots.documents.map { it.id }.toSet()
        val localProjects = dbHelper.getAllProjects()
        var deletedCount = 0

        for (local in localProjects) {
            if (local.id !in cloudIds && local.lastSyncAt > 0) {
                dbHelper.deleteProject(local.id)
                deletedCount++
            }
        }
        return deletedCount
    }

    private fun syncProjectToLocal(
        cloudProject: Project,
        syncTime: Long,
        dbHelper: DatabaseHelper
    ): Boolean {
        val localProject = dbHelper.getProjectById(cloudProject.id)
        cloudProject.lastSyncAt = syncTime

        return if (localProject == null) {
            dbHelper.insertProject(cloudProject)
            true
        } else if (cloudProject.updatedAt > localProject.updatedAt) {
            dbHelper.updateProject(cloudProject)
            false
        } else {
            dbHelper.updateSyncTimestamp(cloudProject.id, syncTime)
            false
        }
    }

    private fun syncExpensesForProject(
        projectRef: com.google.firebase.firestore.DocumentReference,
        projectId: String,
        syncTime: Long,
        dbHelper: DatabaseHelper,
        syncStats: SyncStats,
        totalProjects: Int,
        callback: FetchCallback
    ) {
        projectRef.collection(COLLECTION_EXPENSES).get(Source.SERVER)
            .addOnSuccessListener { expenseSnapshots ->
                deleteRemovedExpenses(expenseSnapshots, projectId, dbHelper)

                for (expDoc in expenseSnapshots) {
                    val cloudExp = mapToExpense(expDoc.id, expDoc.data)
                    cloudExp.projectId = projectId
                    cloudExp.lastSyncAt = syncTime
                    dbHelper.insertExpense(cloudExp)
                }

                completeIfDone(syncStats, totalProjects, dbHelper, callback)
            }
            .addOnFailureListener {
                completeIfDone(syncStats, totalProjects, dbHelper, callback)
            }
    }

    private fun deleteRemovedExpenses(
        expenseSnapshots: com.google.firebase.firestore.QuerySnapshot,
        projectId: String,
        dbHelper: DatabaseHelper
    ) {
        val cloudExpIds = expenseSnapshots.documents.map { it.id }.toSet()
        val localExpenses = dbHelper.getExpensesByProjectId(projectId)

        for (localExp in localExpenses) {
            if (localExp.id !in cloudExpIds && localExp.lastSyncAt > 0) {
                dbHelper.deleteExpense(localExp.id)
            }
        }
    }

    private fun completeIfDone(
        syncStats: SyncStats,
        totalProjects: Int,
        dbHelper: DatabaseHelper,
        callback: FetchCallback
    ) {
        syncStats.completedProjects++
        if (syncStats.completedProjects == totalProjects) {
            dbHelper.setForeignKeysEnabled(true)
            callback.onFetchComplete(
                syncStats.newCount,
                syncStats.updateCount,
                syncStats.deletedCount
            )
        }
    }

    private class SyncStats(
        var newCount: Int = 0,
        var updateCount: Int = 0,
        var deletedCount: Int = 0,
        var completedProjects: Int = 0
    )

    private fun mapToProject(id: String, data: Map<String, Any>): Project {
        return Project(
            id = id,
            projectCode = data["project_code"] as? String ?: "",
            projectName = data["project_name"] as? String ?: "",
            projectDescription = data["project_description"] as? String ?: "",
            startDate = data["start_date"] as? String ?: "",
            endDate = data["end_date"] as? String ?: "",
            projectManager = data["project_manager"] as? String ?: "",
            projectStatus = data["project_status"] as? String ?: "",
            projectBudget = (data["project_budget"] as? Number)?.toDouble() ?: 0.0,
            currency = data["currency"] as? String ?: "USD",
            specialRequirements = data["special_requirements"] as? String ?: "",
            clientDepartment = data["client_department"] as? String ?: "",
            priority = data["priority"] as? String ?: "",
            notes = data["notes"] as? String ?: "",
            lastSyncAt = (data["last_sync_at"] as? Number)?.toLong() ?: 0,
            createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }

    private fun mapToExpense(id: String, data: Map<String, Any>): Expense {
        return Expense(
            id = id,
            projectId = data["project_id"] as? String ?: "",
            expenseId = data["expense_id"] as? String ?: "",
            dateOfExpense = data["date_of_expense"] as? String ?: "",
            amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
            currency = data["currency"] as? String ?: "USD",
            expenseType = data["expense_type"] as? String ?: "",
            paymentMethod = data["payment_method"] as? String ?: "",
            claimant = data["claimant"] as? String ?: "",
            paymentStatus = data["payment_status"] as? String ?: "",
            description = data["description"] as? String ?: "",
            location = data["location"] as? String ?: "",
            lastSyncAt = (data["last_sync_at"] as? Number)?.toLong() ?: 0,
            createdAt = (data["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            updatedAt = (data["updated_at"] as? Number)?.toLong() ?: System.currentTimeMillis()
        )
    }
}
