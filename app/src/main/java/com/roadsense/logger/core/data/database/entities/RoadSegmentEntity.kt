package com.roadsense.logger.core.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "road_segments",
    indices = [
        Index(value = ["projectId"], name = "idx_road_segment_project_id"),
        Index(value = ["segmentName"], name = "idx_road_segment_name")
    ],
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class RoadSegmentEntity(
    @PrimaryKey
    val id: String,
    val projectId: String,
    val segmentName: String,
    val startChainage: Float,
    val endChainage: Float,
    val createdAt: Long,
    val updatedAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val notes: String = ""
)