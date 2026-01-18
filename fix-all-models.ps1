# fix-all-models.ps1
Write-Host "FIXING ALL MODEL FILES" -ForegroundColor Cyan
Write-Host "=======================" -ForegroundColor Cyan

# Fungsi untuk logging dengan timestamp
function Write-Log {
    param(
        [string]$Message,
        [string]$Color = "White"
    )
    $timestamp = Get-Date -Format "HH:mm:ss"
    Write-Host "[$timestamp] $Message" -ForegroundColor $Color
}

# Fungsi untuk backup file dengan timestamp
function Backup-File {
    param([string]$FilePath)
    
    if (Test-Path $FilePath) {
        $backupName = "$FilePath.backup_$(Get-Date -Format 'yyyyMMdd_HHmmss')"
        try {
            Copy-Item $FilePath $backupName -Force
            Write-Log "Backup: $backupName" -Color Gray
            return $true
        } catch {
            Write-Log "Failed to backup: $_" -Color Red
            return $false
        }
    }
    return $true
}

# 1. Backup semua file yang akan diubah
Write-Log "1. Backing up files..." -Color Yellow

$filesToBackup = @(
    "app\src\main\java\com\roadsense\logger\core\data\models\ModelExtensions.kt",
    "app\src\main\java\com\roadsense\logger\core\data\repository\RoadsenseRepository.kt"
)

foreach ($file in $filesToBackup) {
    $backupResult = Backup-File $file
    if (-not $backupResult) {
        Write-Log "Failed to backup $file. Aborting..." -Color Red
        exit 1
    }
}

# 2. Update ModelExtensions.kt
Write-Log "2. Updating ModelExtensions.kt..." -Color Yellow
$modelExtensionsPath = "app\src\main\java\com\roadsense\logger\core\data\models\ModelExtensions.kt"
$modelExtensionsContent = @'
package com.roadsense.logger.core.data.models

import com.roadsense.logger.core.data.database.entities.ProjectEntity
import com.roadsense.logger.core.data.database.entities.RoadSegmentEntity
import com.roadsense.logger.core.data.database.entities.SurveyDataEntity
import java.util.*

// Data classes untuk UI/API
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

// Extension functions untuk konversi Entity -> Model
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

// Extension functions untuk konversi Model -> Entity
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

// Extension untuk List conversions
fun List<ProjectEntity>.toModelList(): List<Project> {
    return this.map { it.toModel() }
}

fun List<Project>.toEntityList(): List<ProjectEntity> {
    return this.map { it.toEntity() }
}

fun List<RoadSegmentEntity>.toModelList(): List<RoadSegment> {
    return this.map { it.toModel() }
}

fun List<RoadSegment>.toEntityList(): List<RoadSegmentEntity> {
    return this.map { it.toEntity() }
}

fun List<SurveyDataEntity>.toModelList(): List<SurveyData> {
    return this.map { it.toModel() }
}

fun List<SurveyData>.toEntityList(): List<SurveyDataEntity> {
    return this.map { it.toEntity() }
}
'@

try {
    # Buat direktori jika belum ada
    $modelDir = Split-Path $modelExtensionsPath -Parent
    if (!(Test-Path $modelDir)) {
        New-Item -ItemType Directory -Path $modelDir -Force | Out-Null
    }
    
    Set-Content -Path $modelExtensionsPath -Value $modelExtensionsContent -Encoding UTF8
    Write-Log "ModelExtensions.kt updated successfully" -Color Green
} catch {
    Write-Log "Failed to update ModelExtensions.kt: $_" -Color Red
    exit 1
}

# 3. Update RoadsenseRepository.kt
Write-Log "3. Updating RoadsenseRepository.kt..." -Color Yellow
$repositoryPath = "app\src\main\java\com\roadsense\logger\core\data\repository\RoadsenseRepository.kt"
$repositoryContent = @'
package com.roadsense.logger.core.data.repository

import com.roadsense.logger.core.data.database.dao.ProjectDao
import com.roadsense.logger.core.data.database.dao.SegmentDao
import com.roadsense.logger.core.data.database.dao.SurveyDataDao
import com.roadsense.logger.core.data.models.Project
import com.roadsense.logger.core.data.models.RoadSegment
import com.roadsense.logger.core.data.models.SurveyData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RoadsenseRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val segmentDao: SegmentDao,
    private val surveyDataDao: SurveyDataDao
) {
    
    // ============ PROJECT OPERATIONS ============
    
    suspend fun insertProject(project: Project): Long {
        return projectDao.insert(project.toEntity())
    }
    
    suspend fun updateProject(project: Project) {
        projectDao.update(project.toEntity())
    }
    
    suspend fun deleteProject(project: Project) {
        projectDao.delete(project.toEntity())
    }
    
    fun getAllProjects(): Flow<List<Project>> {
        return projectDao.getAllProjects().map { it.toModelList() }
    }
    
    suspend fun getProjectById(projectId: String): Project? {
        return projectDao.getProjectById(projectId)?.toModel()
    }
    
    fun getActiveProjects(): Flow<List<Project>> {
        return projectDao.getActiveProjects().map { it.toModelList() }
    }
    
    fun searchProjects(search: String): Flow<List<Project>> {
        return projectDao.searchProjects(search).map { it.toModelList() }
    }
    
    suspend fun deleteProjectById(projectId: String) {
        projectDao.deleteProjectById(projectId)
    }
    
    // ============ ROAD SEGMENT OPERATIONS ============
    
    suspend fun insertSegment(segment: RoadSegment): Long {
        return segmentDao.insert(segment.toEntity())
    }
    
    suspend fun updateSegment(segment: RoadSegment) {
        segmentDao.update(segment.toEntity())
    }
    
    suspend fun deleteSegment(segment: RoadSegment) {
        segmentDao.delete(segment.toEntity())
    }
    
    fun getAllSegments(): Flow<List<RoadSegment>> {
        return segmentDao.getAllSegments().map { it.toModelList() }
    }
    
    fun getSegmentsByProject(projectId: String): Flow<List<RoadSegment>> {
        return segmentDao.getSegmentsByProject(projectId).map { it.toModelList() }
    }
    
    suspend fun getSegmentById(segmentId: String): RoadSegment? {
        return segmentDao.getSegmentById(segmentId)?.toModel()
    }
    
    suspend fun deleteSegmentsByProject(projectId: String) {
        segmentDao.deleteSegmentsByProject(projectId)
    }
    
    suspend fun getSegmentCount(projectId: String): Int {
        return segmentDao.getSegmentCount(projectId)
    }
    
    // ============ SURVEY DATA OPERATIONS ============
    
    suspend fun insertSurveyData(surveyData: SurveyData): Long {
        return surveyDataDao.insert(surveyData.toEntity())
    }
    
    suspend fun insertSurveyDataBatch(surveyDataList: List<SurveyData>) {
        surveyDataDao.insertAll(surveyDataList.toEntityList())
    }
    
    suspend fun updateSurveyData(surveyData: SurveyData) {
        surveyDataDao.update(surveyData.toEntity())
    }
    
    suspend fun deleteSurveyData(surveyData: SurveyData) {
        surveyDataDao.delete(surveyData.toEntity())
    }
    
    fun getSurveyDataBySegment(segmentId: String): Flow<List<SurveyData>> {
        return surveyDataDao.getSurveyDataBySegment(segmentId).map { it.toModelList() }
    }
    
    fun getSurveyDataByChainageRange(
        segmentId: String, 
        startChainage: Float, 
        endChainage: Float
    ): Flow<List<SurveyData>> {
        return surveyDataDao.getSurveyDataByChainageRange(segmentId, startChainage, endChainage)
            .map { it.toModelList() }
    }
    
    fun getSurveyDataByTimeRange(startTime: Long, endTime: Long): Flow<List<SurveyData>> {
        return surveyDataDao.getSurveyDataByTimeRange(startTime, endTime)
            .map { it.toModelList() }
    }
    
    suspend fun getSegmentStatistics(segmentId: String): com.roadsense.logger.core.data.database.dao.SegmentStatistics? {
        return surveyDataDao.getSegmentStatistics(segmentId)
    }
    
    suspend fun getUnsyncedData(limit: Int = 100): List<SurveyData> {
        return surveyDataDao.getUnsyncedData(limit).toModelList()
    }
    
    suspend fun deleteSurveyDataBySegment(segmentId: String) {
        surveyDataDao.deleteBySegment(segmentId)
    }
    
    suspend fun getSurveyCount(segmentId: String): Int {
        return surveyDataDao.getSurveyCount(segmentId)
    }
    
    fun getLatestSurveyData(limit: Int = 50): Flow<List<SurveyData>> {
        return surveyDataDao.getLatestSurveyData(limit).map { it.toModelList() }
    }
}
'@

try {
    # Buat direktori jika belum ada
    $repoDir = Split-Path $repositoryPath -Parent
    if (!(Test-Path $repoDir)) {
        New-Item -ItemType Directory -Path $repoDir -Force | Out-Null
    }
    
    Set-Content -Path $repositoryPath -Value $repositoryContent -Encoding UTF8
    Write-Log "RoadsenseRepository.kt updated successfully" -Color Green
} catch {
    Write-Log "Failed to update RoadsenseRepository.kt: $_" -Color Red
    exit 1
}

# 4. Build project
Write-Log "4. Building project..." -Color Yellow

try {
    # Capture build output untuk diagnostics
    $buildOutput = & .\gradlew clean assembleDebug 2>&1
    
    if ($LASTEXITCODE -eq 0) {
        Write-Log "BUILD SUCCESS!" -Color Green
        
        # Cek APK
        $apk = "app\build\outputs\apk\debug\app-debug.apk"
        if (Test-Path $apk) {
            $size = [math]::Round((Get-Item $apk).Length / 1MB, 2)
            Write-Log "APK Size: ${size}MB" -Color Cyan
            
            # Show APK location
            $fullPath = Resolve-Path $apk
            Write-Log "APK Location: $fullPath" -Color Gray
        } else {
            Write-Log "Warning: APK not found at expected location" -Color Yellow
        }
    } else {
        Write-Log "BUILD FAILED" -Color Red
        Write-Log "Build output:" -Color Yellow
        $buildOutput | ForEach-Object { Write-Log "  $_" -Color Red }
        Write-Log "Check compilation errors above" -Color Yellow
        exit 1
    }
} catch {
    Write-Log "Failed to run gradle build: $_" -Color Red
    exit 1
}

Write-Log "" -Color White
Write-Log "All models and repository updated successfully!" -Color Cyan
Write-Log "Check the 'app-debug.apk' file for testing." -Color Green