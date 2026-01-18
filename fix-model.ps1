# fix-model.ps1 - Fixed version without emoji issues
Write-Host "FIXING MODEL EXTENSIONS" -ForegroundColor Cyan
Write-Host "==========================" -ForegroundColor Cyan

# 1. Cek file ModelExtensions.kt
$modelFile = "app\src\main\java\com\roadsense\logger\core\data\models\ModelExtensions.kt"
Write-Host "1. Checking ModelExtensions.kt..." -NoNewline

if (Test-Path $modelFile) {
    $content = Get-Content $modelFile -Raw
    
    # Cek type mismatch errors
    $stringToLong = $content -match "String.*toLong"
    $longToString = $content -match "Long.*toString"
    
    if ($stringToLong -or $longToString) {
        Write-Host " NEEDS UPDATE" -ForegroundColor Yellow
        
        # Backup file lama
        Copy-Item $modelFile "$modelFile.backup" -Force
        Write-Host "   Backup created: $modelFile.backup" -ForegroundColor Cyan
        
        # Kode yang benar untuk ModelExtensions.kt
        $correctCode = @"
package com.roadsense.logger.core.data.models

import com.roadsense.logger.core.data.database.entities.ProjectEntity
import com.roadsense.logger.core.data.database.entities.RoadSegmentEntity
import com.roadsense.logger.core.data.database.entities.SurveyDataEntity

// Data classes untuk UI/API
data class Project(
    val id: String,
    val projectName: String,
    val projectCode: String,
    val clientName: String,
    val location: String,
    val startDate: Long,
    val endDate: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean,
    val description: String
)

data class RoadSegment(
    val id: String,
    val projectId: String,
    val segmentName: String,
    val startChainage: Float,
    val endChainage: Float,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean,
    val notes: String
)

data class SurveyData(
    val id: Long,
    val segmentId: String,
    val timestamp: Long,
    val sta: String,
    val chainage: Float,
    val speed: Float,
    val vibrationZ: Float,
    val latitude: Double,
    val longitude: Double,
    val packetCount: Int,
    val temperature: Float,
    val humidity: Float,
    val roadCondition: String,
    val notes: String,
    val syncStatus: Int
)

// Extension functions untuk konversi Entity <-> Model
fun ProjectEntity.toProject(): Project {
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

fun Project.toProjectEntity(): ProjectEntity {
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

fun RoadSegmentEntity.toRoadSegment(): RoadSegment {
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

fun RoadSegment.toRoadSegmentEntity(): RoadSegmentEntity {
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

fun SurveyDataEntity.toSurveyData(): SurveyData {
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

fun SurveyData.toSurveyDataEntity(): SurveyDataEntity {
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

// Extension untuk List conversions
fun List<ProjectEntity>.toProjectList(): List<Project> {
    return this.map { it.toProject() }
}

fun List<Project>.toProjectEntityList(): List<ProjectEntity> {
    return this.map { it.toProjectEntity() }
}

fun List<RoadSegmentEntity>.toRoadSegmentList(): List<RoadSegment> {
    return this.map { it.toRoadSegment() }
}

fun List<RoadSegment>.toRoadSegmentEntityList(): List<RoadSegmentEntity> {
    return this.map { it.toRoadSegmentEntity() }
}

fun List<SurveyDataEntity>.toSurveyDataList(): List<SurveyData> {
    return this.map { it.toSurveyData() }
}

fun List<SurveyData>.toSurveyDataEntityList(): List<SurveyDataEntity> {
    return this.map { it.toSurveyDataEntity() }
}
"@
        
        # Write kode yang benar
        Set-Content -Path $modelFile -Value $correctCode
        Write-Host "   File updated with correct types" -ForegroundColor Green
    } else {
        Write-Host " OK" -ForegroundColor Green
    }
} else {
    Write-Host " FILE NOT FOUND" -ForegroundColor Red
    Write-Host "   Creating new ModelExtensions.kt..." -ForegroundColor Yellow
    
    # Buat direktori jika belum ada
    $dir = Split-Path $modelFile -Parent
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force
    }
    
    # Kode yang sama seperti di atas
    $correctCode = @"
package com.roadsense.logger.core.data.models

import com.roadsense.logger.core.data.database.entities.ProjectEntity
import com.roadsense.logger.core.data.database.entities.RoadSegmentEntity
import com.roadsense.logger.core.data.database.entities.SurveyDataEntity

// Data classes untuk UI/API
data class Project(
    val id: String,
    val projectName: String,
    val projectCode: String,
    val clientName: String,
    val location: String,
    val startDate: Long,
    val endDate: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean,
    val description: String
)

data class RoadSegment(
    val id: String,
    val projectId: String,
    val segmentName: String,
    val startChainage: Float,
    val endChainage: Float,
    val createdAt: Long,
    val updatedAt: Long,
    val isActive: Boolean,
    val notes: String
)

data class SurveyData(
    val id: Long,
    val segmentId: String,
    val timestamp: Long,
    val sta: String,
    val chainage: Float,
    val speed: Float,
    val vibrationZ: Float,
    val latitude: Double,
    val longitude: Double,
    val packetCount: Int,
    val temperature: Float,
    val humidity: Float,
    val roadCondition: String,
    val notes: String,
    val syncStatus: Int
)

// Extension functions untuk konversi Entity <-> Model
fun ProjectEntity.toProject(): Project {
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

fun Project.toProjectEntity(): ProjectEntity {
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

fun RoadSegmentEntity.toRoadSegment(): RoadSegment {
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

fun RoadSegment.toRoadSegmentEntity(): RoadSegmentEntity {
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

fun SurveyDataEntity.toSurveyData(): SurveyData {
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

fun SurveyData.toSurveyDataEntity(): SurveyDataEntity {
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

// Extension untuk List conversions
fun List<ProjectEntity>.toProjectList(): List<Project> {
    return this.map { it.toProject() }
}

fun List<Project>.toProjectEntityList(): List<ProjectEntity> {
    return this.map { it.toProjectEntity() }
}

fun List<RoadSegmentEntity>.toRoadSegmentList(): List<RoadSegment> {
    return this.map { it.toRoadSegment() }
}

fun List<RoadSegment>.toRoadSegmentEntityList(): List<RoadSegmentEntity> {
    return this.map { it.toRoadSegmentEntity() }
}

fun List<SurveyDataEntity>.toSurveyDataList(): List<SurveyData> {
    return this.map { it.toSurveyData() }
}

fun List<SurveyData>.toSurveyDataEntityList(): List<SurveyDataEntity> {
    return this.map { it.toSurveyDataEntity() }
}
"@
    
    # Buat file baru
    Set-Content -Path $modelFile -Value $correctCode
    Write-Host "   New file created" -ForegroundColor Green
}

# 2. Clean and build
Write-Host ""
Write-Host "2. Building project..." -ForegroundColor Yellow
& .\gradlew clean assembleDebug 2>&1 | Out-Null

if ($LASTEXITCODE -eq 0) {
    Write-Host "   BUILD SUCCESS!" -ForegroundColor Green
} else {
    Write-Host "   BUILD FAILED" -ForegroundColor Red
    Write-Host "   Check ModelExtensions.kt for type errors" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "Model extensions fixed!" -ForegroundColor Cyan