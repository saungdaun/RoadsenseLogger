package com.roadsense.logger.core.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [
        Index(value = ["projectName"], name = "idx_project_name", unique = true),
        Index(value = ["createdAt"], name = "idx_project_created_at")
    ]
)
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val projectName: String,
    val projectCode: String,
    val clientName: String = "",
    val location: String = "",
    val startDate: Long = 0,
    val endDate: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val description: String = ""
)