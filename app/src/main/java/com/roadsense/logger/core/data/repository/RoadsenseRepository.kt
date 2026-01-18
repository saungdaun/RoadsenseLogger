package com.roadsense.logger.core.data.repository

import com.roadsense.logger.core.data.database.dao.ProjectDao
import com.roadsense.logger.core.data.models.*
import com.roadsense.logger.core.data.database.dao.SegmentDao
import com.roadsense.logger.core.data.database.dao.SurveyDataDao
import com.roadsense.logger.core.data.models.Project
import com.roadsense.logger.core.data.models.RoadSegment
import com.roadsense.logger.core.data.models.SurveyData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.roadsense.logger.core.data.models.toEntity

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
        return projectDao.getAllProjects().map { entities -> entities.map { it.toModel() } }
    }

    suspend fun getProjectById(projectId: String): Project? {
        return projectDao.getProjectById(projectId)?.toModel()
    }

    fun getActiveProjects(): Flow<List<Project>> {
        return projectDao.getActiveProjects().map { entities -> entities.map { it.toModel() } }
    }

    fun searchProjects(search: String): Flow<List<Project>> {
        return projectDao.searchProjects(search).map { entities -> entities.map { it.toModel() } }
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
        return segmentDao.getAllSegments().map { entities -> entities.map { it.toModel() } }
    }

    fun getSegmentsByProject(projectId: String): Flow<List<RoadSegment>> {
        return segmentDao.getSegmentsByProject(projectId).map { entities -> entities.map { it.toModel() } }
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
        surveyDataDao.insertAll(surveyDataList.map { it.toEntity() })
    }

    suspend fun updateSurveyData(surveyData: SurveyData) {
        surveyDataDao.update(surveyData.toEntity())
    }

    suspend fun deleteSurveyData(surveyData: SurveyData) {
        surveyDataDao.delete(surveyData.toEntity())
    }

    fun getSurveyDataBySegment(segmentId: String): Flow<List<SurveyData>> {
        return surveyDataDao.getSurveyDataBySegment(segmentId).map { entities -> entities.map { it.toModel() } }
    }

    fun getSurveyDataByChainageRange(
        segmentId: String,
        startChainage: Float,
        endChainage: Float
    ): Flow<List<SurveyData>> {
        return surveyDataDao.getSurveyDataByChainageRange(segmentId, startChainage, endChainage)
            .map { entities -> entities.map { it.toModel() } }
    }

    fun getSurveyDataByTimeRange(startTime: Long, endTime: Long): Flow<List<SurveyData>> {
        return surveyDataDao.getSurveyDataByTimeRange(startTime, endTime)
            .map { entities -> entities.map { it.toModel() } }
    }

    suspend fun getSegmentStatistics(segmentId: String): com.roadsense.logger.core.data.database.dao.SegmentStatistics? {
        return surveyDataDao.getSegmentStatistics(segmentId)
    }

    suspend fun getUnsyncedData(limit: Int = 100): List<SurveyData> {
        return surveyDataDao.getUnsyncedData(limit).map { it.toModel() }
    }

    suspend fun deleteSurveyDataBySegment(segmentId: String) {
        surveyDataDao.deleteBySegment(segmentId)
    }

    suspend fun getSurveyCount(segmentId: String): Int {
        return surveyDataDao.getSurveyCount(segmentId)
    }

    fun getLatestSurveyData(limit: Int = 50): Flow<List<SurveyData>> {
        return surveyDataDao.getLatestSurveyData(limit).map { entities -> entities.map { it.toModel() } }
    }
}
