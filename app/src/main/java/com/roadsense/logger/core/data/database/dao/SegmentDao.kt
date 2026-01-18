package com.roadsense.logger.core.data.database.dao

import androidx.room.*
import com.roadsense.logger.core.data.database.entities.RoadSegmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SegmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(segment: RoadSegmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(segments: List<RoadSegmentEntity>)

    @Update
    suspend fun update(segment: RoadSegmentEntity)

    @Delete
    suspend fun delete(segment: RoadSegmentEntity)

    // Query yang BENAR - sesuaikan dengan kolom di RoadSegmentEntity
    @Query("SELECT * FROM road_segments ORDER BY segmentName ASC")
    fun getAllSegments(): Flow<List<RoadSegmentEntity>>

    @Query("SELECT * FROM road_segments WHERE projectId = :projectId ORDER BY startChainage ASC")
    fun getSegmentsByProject(projectId: String): Flow<List<RoadSegmentEntity>>

    @Query("SELECT * FROM road_segments WHERE id = :segmentId")
    suspend fun getSegmentById(segmentId: String): RoadSegmentEntity?

    @Query("SELECT * FROM road_segments WHERE segmentName LIKE '%' || :search || '%'")
    fun searchSegments(search: String): Flow<List<RoadSegmentEntity>>

    @Query("DELETE FROM road_segments WHERE projectId = :projectId")
    suspend fun deleteSegmentsByProject(projectId: String)

    @Query("SELECT COUNT(*) FROM road_segments WHERE projectId = :projectId")
    suspend fun getSegmentCount(projectId: String): Int
}