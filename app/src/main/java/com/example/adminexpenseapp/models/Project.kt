package com.example.adminexpenseapp.models

import java.io.Serializable
import java.util.UUID

data class Project(
    var id: String = UUID.randomUUID().toString(),
    var projectCode: String = "",
    var projectName: String = "",
    var projectDescription: String = "",
    var startDate: String = "",
    var endDate: String = "",
    var projectManager: String = "",
    var projectStatus: String = "",
    var projectBudget: Double = 0.0,
    var currency: String = "USD",
    var specialRequirements: String = "",
    var clientDepartment: String = "",
    var priority: String = "",
    var notes: String = "",
    var lastSyncAt: Long = 0,
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
) : Serializable {

    fun toMap(): Map<String, Any> = mapOf(
        "id" to id,
        "project_code" to projectCode,
        "project_name" to projectName,
        "project_description" to projectDescription,
        "start_date" to startDate,
        "end_date" to endDate,
        "project_manager" to projectManager,
        "project_status" to projectStatus,
        "project_budget" to projectBudget,
        "currency" to currency,
        "special_requirements" to specialRequirements,
        "client_department" to clientDepartment,
        "priority" to priority,
        "notes" to notes,
        "last_sync_at" to lastSyncAt,
        "created_at" to createdAt,
        "updated_at" to updatedAt
    )
}
