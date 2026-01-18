package com.roadsense.logger.core.data.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "survey_data",
    indices = [
        Index(value = ["segmentId"], name = "idx_survey_segment_id"),
        Index(value = ["timestamp"], name = "idx_survey_timestamp"),
        Index(value = ["chainage"], name = "idx_survey_chainage")
    ],
    foreignKeys = [
        ForeignKey(
            entity = RoadSegmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["segmentId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class SurveyDataEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val segmentId: String,
    val timestamp: Long,
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
    val syncStatus: Int = 0  // 0 = not synced, 1 = syncing, 2 = synced
)