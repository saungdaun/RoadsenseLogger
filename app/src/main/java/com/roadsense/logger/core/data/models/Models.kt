// File: app/src/main/java/com/roadsense/logger/core/data/models/Models.kt
package com.roadsense.logger.core.data.models

import java.util.UUID

data class Project(
    val id: String = UUID.randomUUID().toString(),
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

data class RoadSegment(
    val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val segmentName: String,
    val startChainage: Float,
    val endChainage: Float,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val notes: String = ""
)

data class SurveyData(
    val id: Long = 0,
    val segmentId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val sta: String,
    val chainage: Float,
    val speed: Float,
    val vibrationZ: Float,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val packetCount: Int = 0,
    val temperature: Float = 0f,
    val humidity: Float = 0f,
    val roadCondition: String = "normal",
    val notes: String = "",
    val syncStatus: Int = 0
)