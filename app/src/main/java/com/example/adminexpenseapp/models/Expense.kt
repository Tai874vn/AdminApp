package com.example.adminexpenseapp.models

import java.io.Serializable
import java.util.UUID

data class Expense(
    var id: String = UUID.randomUUID().toString(),
    var projectId: String = "",
    var expenseId: String = "",        // User-defined expense code
    var dateOfExpense: String = "",
    var amount: Double = 0.0,
    var currency: String = "",
    var expenseType: String = "",
    var paymentMethod: String = "",
    var claimant: String = "",
    var paymentStatus: String = "",     // Paid, Pending, Reimbursed
    var description: String = "",       // Optional
    var location: String = "",          // Optional
    var lastSyncAt: Long = 0,           // 0 means never synced
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
) : Serializable {

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "project_id" to projectId,
        "expense_id" to expenseId,
        "date_of_expense" to dateOfExpense,
        "amount" to amount,
        "currency" to currency,
        "expense_type" to expenseType,
        "payment_method" to paymentMethod,
        "claimant" to claimant,
        "payment_status" to paymentStatus,
        "description" to description,
        "location" to location,
        "last_sync_at" to lastSyncAt,
        "created_at" to createdAt,
        "updated_at" to updatedAt
    )

    companion object {
        val EXPENSE_TYPES = arrayOf(
            "Travel", "Equipment", "Materials", "Services",
            "Software/Licenses", "Labour costs", "Utilities", "Miscellaneous"
        )

        val PAYMENT_METHODS = arrayOf(
            "Cash", "Credit Card", "Bank Transfer", "Cheque"
        )

        val PAYMENT_STATUSES = arrayOf(
            "Paid", "Pending", "Reimbursed"
        )

        val CURRENCIES = arrayOf(
            "USD", "GBP", "EUR", "VND", "JPY", "AUD", "CAD", "SGD"
        )
    }
}
