package com.example.adminexpenseapp.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.adminexpenseapp.database.DatabaseHelper
import com.example.adminexpenseapp.models.Expense
import com.example.adminexpenseapp.models.Project
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

/**
 * Handles syncing local SQLite data with Firebase Firestore.
 */
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

    /** Optimized Delete: Fetches and deletes subcollections before the project itself */
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
            
            // 1. Find all expenses for this project in the cloud
            projectRef.collection(COLLECTION_EXPENSES).get().addOnSuccessListener { expenses ->
                val batch = db.batch()
                
                // 2. Delete every expense document
                for (expDoc in expenses) {
                    batch.delete(expDoc.reference)
                }
                
                // 3. Delete the parent project document
                batch.delete(projectRef)
                
                batch.commit().addOnSuccessListener {
                    deletedProjects++
                    if (deletedProjects == totalToDelete) {
                        callback.onSuccess(deletedProjects, 0)
                    }
                }
            }.addOnFailureListener {
                // Fail-safe: Try to delete the project even if expense fetch fails
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
                val cloudIds = projectSnapshots.documents.map { it.id }.toSet()
                val localProjects = dbHelper.getAllProjects()
                var deletedCount = 0

                for (local in localProjects) {
                    if (local.id !in cloudIds && local.lastSyncAt > 0) {
                        dbHelper.deleteProject(local.id)
                        deletedCount++
                    }
                }

                if (projectSnapshots.isEmpty) {
                    dbHelper.setForeignKeysEnabled(true)
                    callback.onFetchComplete(0, 0, deletedCount)
                    return@addOnSuccessListener
                }

                var newCount = 0
                var updateCount = 0
                val syncTime = System.currentTimeMillis()
                val totalProjects = projectSnapshots.size()
                var completedProjects = 0

                for (doc in projectSnapshots) {
                    val cloudProject = mapToProject(doc.id, doc.data)
                    val localProject = dbHelper.getProjectById(cloudProject.id)

                    cloudProject.lastSyncAt = syncTime
                    if (localProject == null) {
                        dbHelper.insertProject(cloudProject)
                        newCount++
                    } else if (cloudProject.updatedAt > localProject.updatedAt) {
                        dbHelper.updateProject(cloudProject)
                        updateCount++
                    } else {
                        dbHelper.updateSyncTimestamp(cloudProject.id, syncTime)
                    }

                    doc.reference.collection(COLLECTION_EXPENSES).get(Source.SERVER)
                        .addOnSuccessListener { expenseSnapshots ->
                            val cloudExpIds = expenseSnapshots.documents.map { it.id }.toSet()
                            val localExpenses = dbHelper.getExpensesByProjectId(cloudProject.id)
                            
                            for (localExp in localExpenses) {
                                if (localExp.id !in cloudExpIds && localExp.lastSyncAt > 0) {
                                    dbHelper.deleteExpense(localExp.id)
                                }
                            }

                            for (expDoc in expenseSnapshots) {
                                val cloudExp = mapToExpense(expDoc.id, expDoc.data)
                                cloudExp.projectId = cloudProject.id
                                cloudExp.lastSyncAt = syncTime
                                // DatabaseHelper.insertExpense now uses CONFLICT_REPLACE (upsert)
                                dbHelper.insertExpense(cloudExp)
                            }
                            
                            completedProjects++
                            if (completedProjects == totalProjects) {
                                dbHelper.setForeignKeysEnabled(true)
                                callback.onFetchComplete(newCount, updateCount, deletedCount)
                            }
                        }
                        .addOnFailureListener {
                            completedProjects++
                            if (completedProjects == totalProjects) {
                                dbHelper.setForeignKeysEnabled(true)
                                callback.onFetchComplete(newCount, updateCount, deletedCount)
                            }
                        }
                }
            }
            .addOnFailureListener { e -> 
                dbHelper.setForeignKeysEnabled(true)
                callback.onFailure("Fetch failed: ${e.message}") 
            }
    }

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
