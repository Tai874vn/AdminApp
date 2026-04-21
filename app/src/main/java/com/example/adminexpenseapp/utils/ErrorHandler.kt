package com.example.adminexpenseapp.utils

import android.content.Context
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import android.view.View

/**
 * Centralized error handling and user messaging utility.
 * Ensures consistent error messages and user feedback across the app.
 */
object ErrorHandler {

    /**
     * Shows a short error toast message.
     */
    fun showError(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Shows a long error toast message.
     */
    fun showErrorLong(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Shows a success toast message.
     */
    fun showSuccess(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Shows an error with a Snackbar (allows action button).
     */
    fun showErrorWithAction(
        view: View,
        message: String,
        actionText: String? = null,
        action: (() -> Unit)? = null
    ) {
        val snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        if (actionText != null && action != null) {
            snackbar.setAction(actionText) { action() }
        }
        snackbar.show()
    }

    /**
     * Shows a success message with Snackbar.
     */
    fun showSuccessSnackbar(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }

    // Validation error messages
    object Validation {
        const val EMPTY_PROJECT_CODE = "Please enter the project code"
        const val EMPTY_PROJECT_NAME = "Please enter the project name"
        const val EMPTY_PROJECT_DESC = "Please enter the project description"
        const val EMPTY_START_DATE = "Please select the start date"
        const val EMPTY_END_DATE = "Please select the end date"
        const val EMPTY_PROJECT_MANAGER = "Please enter the project manager"
        const val EMPTY_PROJECT_STATUS = "Please select the project status"
        const val EMPTY_BUDGET = "Please enter the project budget"
        const val INVALID_BUDGET = "Please enter a valid number"
        const val NEGATIVE_BUDGET = "Budget must be positive"
        const val EMPTY_CURRENCY = "Please select a currency"
        const val REQUIRED_FIELDS = "Please fill in all required fields"

        const val EMPTY_EXPENSE_ID = "Please enter the expense ID"
        const val EMPTY_EXPENSE_DATE = "Please select the expense date"
        const val EMPTY_AMOUNT = "Please enter the amount"
        const val INVALID_AMOUNT = "Please enter a valid amount"
        const val NEGATIVE_AMOUNT = "Amount must be positive"
        const val EMPTY_EXPENSE_TYPE = "Please select the expense type"
        const val EMPTY_PAYMENT_METHOD = "Please select the payment method"
        const val EMPTY_CLAIMANT = "Please enter the claimant name"
        const val EMPTY_PAYMENT_STATUS = "Please select the payment status"
    }

    // Network error messages
    object Network {
        const val NO_CONNECTION = "No network connection"
        const val FETCH_FAILED = "Failed to fetch data from cloud"
        const val UPLOAD_FAILED = "Failed to upload data to cloud"
        const val SYNC_FAILED = "Synchronization failed"
        const val DELETE_FAILED = "Failed to delete from cloud"
    }

    // Success messages
    object Success {
        const val PROJECT_SAVED = "Project saved successfully"
        const val PROJECT_UPDATED = "Project updated successfully"
        const val PROJECT_DELETED = "Project deleted successfully"
        const val EXPENSE_SAVED = "Expense saved successfully"
        const val EXPENSE_UPDATED = "Expense updated successfully"
        const val EXPENSE_DELETED = "Expense deleted successfully"
        const val UPLOAD_SUCCESS = "Upload to database success!"
        const val SYNC_COMPLETE = "Sync complete"
        const val DATABASE_RESET = "Database reset successfully"

        fun syncComplete(newProjects: Int, updatedProjects: Int, deletedLocally: Int): String {
            return if (deletedLocally > 0) {
                "Sync complete: $newProjects new, $updatedProjects updated, $deletedLocally deleted locally"
            } else {
                "Sync complete: $newProjects new, $updatedProjects updated"
            }
        }

        fun deletedFromCloud(count: Int): String {
            return "Deleted $count project${if (count != 1) "s" else ""} from cloud"
        }
    }

    // Database error messages
    object Database {
        const val SAVE_FAILED = "Failed to save to database"
        const val UPDATE_FAILED = "Failed to update database"
        const val DELETE_FAILED = "Failed to delete from database"
        const val LOAD_FAILED = "Failed to load data"
        const val NOT_FOUND = "Record not found"
    }
}
