// File: app/src/main/java/com/roadsense/logger/core/data/models/ModelExtensions.kt
package com.roadsense.logger.core.data.models

import com.roadsense.logger.core.data.database.entities.ProjectEntity
import com.roadsense.logger.core.data.database.entities.RoadSegmentEntity
import com.roadsense.logger.core.data.database.entities.SurveyDataEntity

// ============ SINGLE OBJECT CONVERSIONS ============

// Entity -> Model
fun ProjectEntity.toModel(): Project {
    return Project(
        id = this.id,
        projectName = this.projectName,
        projectCode = this.projectCode,
        clientName = this.clientName,
        location = this.location,
        startDate = this.startDate,
        endDate = this.endDate,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        isActive = this.isActive,
        description = this.description
    )
}

fun RoadSegmentEntity.toModel(): RoadSegment {
    return RoadSegment(
        id = this.id,
        projectId = this.projectId,
        segmentName = this.segmentName,
        startChainage = this.startChainage,
        endChainage = this.endChainage,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        isActive = this.isActive,
        notes = this.notes
    )
}

fun SurveyDataEntity.toModel(): SurveyData {
    return SurveyData(
        id = this.id,
        segmentId = this.segmentId,
        timestamp = this.timestamp,
        sta = this.sta,
        chainage = this.chainage,
        speed = this.speed,
        vibrationZ = this.vibrationZ,
        latitude = this.latitude,
        longitude = this.longitude,
        packetCount = this.packetCount,
        temperature = this.temperature,
        humidity = this.humidity,
        roadCondition = this.roadCondition,
        notes = this.notes,
        syncStatus = this.syncStatus
    )
}

// Model -> Entity
fun Project.toEntity(): ProjectEntity {
    return ProjectEntity(
        id = this.id,
        projectName = this.projectName,
        projectCode = this.projectCode,
        clientName = this.clientName,
        location = this.location,
        startDate = this.startDate,
        endDate = this.endDate,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        isActive = this.isActive,
        description = this.description
    )
}

fun RoadSegment.toEntity(): RoadSegmentEntity {
    return RoadSegmentEntity(
        id = this.id,
        projectId = this.projectId,
        segmentName = this.segmentName,
        startChainage = this.startChainage,
        endChainage = this.endChainage,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        isActive = this.isActive,
        notes = this.notes
    )
}

fun SurveyData.toEntity(): SurveyDataEntity {
    return SurveyDataEntity(
        id = this.id,
        segmentId = this.segmentId,
        timestamp = this.timestamp,
        sta = this.sta,
        chainage = this.chainage,
        speed = this.speed,
        vibrationZ = this.vibrationZ,
        latitude = this.latitude,
        longitude = this.longitude,
        packetCount = this.packetCount,
        temperature = this.temperature,
        humidity = this.humidity,
        roadCondition = this.roadCondition,
        notes = this.notes,
        syncStatus = this.syncStatus
    )
}

// ============ HAPUS SEMUA EXTENSION FUNCTIONS UNTUK LIST ============
// JANGAN PAKAI FUNCTIONS DI BAWAH INI:

// HAPUS INI:
// fun List<ProjectEntity>.toModelList(): List<Project> { ... }
// fun List<Project>.toEntityList(): List<ProjectEntity> { ... }
// fun List<RoadSegmentEntity>.toModelList(): List<RoadSegment> { ... }
// fun List<RoadSegment>.toEntityList(): List<RoadSegmentEntity> { ... }
// fun List<SurveyDataEntity>.toModelList(): List<SurveyData> { ... }
// fun List<SurveyData>.toEntityList(): List<SurveyDataEntity> { ... }