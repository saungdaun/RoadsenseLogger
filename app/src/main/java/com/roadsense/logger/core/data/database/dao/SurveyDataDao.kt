package com.roadsense.logger.core.data.database.dao

import androidx.room.*
import com.roadsense.logger.core.data.database.entities.SurveyDataEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SurveyDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(surveyData: SurveyDataEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(surveyDataList: List<SurveyDataEntity>)

    @Update
    suspend fun update(surveyData: SurveyDataEntity)

    @Delete
    suspend fun delete(surveyData: SurveyDataEntity)

    // Query yang BENAR - semua kolom sesuai dengan entity
    @Query("SELECT * FROM survey_data WHERE segmentId = :segmentId ORDER BY timestamp ASC")
    fun getSurveyDataBySegment(segmentId: String): Flow<List<SurveyDataEntity>>

    @Query("SELECT * FROM survey_data WHERE segmentId = :segmentId AND chainage BETWEEN :startChainage AND :endChainage ORDER BY chainage ASC")
    fun getSurveyDataByChainageRange(segmentId: String, startChainage: Float, endChainage: Float): Flow<List<SurveyDataEntity>>

    @Query("SELECT * FROM survey_data WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp ASC")
    fun getSurveyDataByTimeRange(startTime: Long, endTime: Long): Flow<List<SurveyDataEntity>>

    @Query("SELECT * FROM survey_data WHERE syncStatus = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getUnsyncedData(limit: Int = 100): List<SurveyDataEntity>

    @Query("SELECT AVG(speed) as avgSpeed, MAX(speed) as maxSpeed, MIN(speed) as minSpeed FROM survey_data WHERE segmentId = :segmentId")
    suspend fun getSegmentStatistics(segmentId: String): SegmentStatistics?

    @Query("DELETE FROM survey_data WHERE segmentId = :segmentId")
    suspend fun deleteBySegment(segmentId: String)

    @Query("DELETE FROM survey_data WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM survey_data WHERE segmentId = :segmentId")
    suspend fun getSurveyCount(segmentId: String): Int

    @Query("SELECT * FROM survey_data ORDER BY timestamp DESC LIMIT :limit")
    fun getLatestSurveyData(limit: Int = 50): Flow<List<SurveyDataEntity>>
}

// Data class untuk statistics - HARUS PUBLIC
data class SegmentStatistics(
    val avgSpeed: Float? = null,
    val maxSpeed: Float? = null,
    val minSpeed: Float? = null
)