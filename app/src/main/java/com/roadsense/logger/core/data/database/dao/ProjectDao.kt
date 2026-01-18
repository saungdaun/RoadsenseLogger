package com.roadsense.logger.core.data.database.dao

import androidx.room.*
import com.roadsense.logger.core.data.database.entities.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)

    // Query yang BENAR - sesuaikan dengan kolom di ProjectEntity
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectById(projectId: String): ProjectEntity?

    @Query("SELECT * FROM projects WHERE isActive = 1 ORDER BY projectName ASC")
    fun getActiveProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE projectName LIKE '%' || :search || '%' OR clientName LIKE '%' || :search || '%'")
    fun searchProjects(search: String): Flow<List<ProjectEntity>>

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: String)

    @Query("UPDATE projects SET isActive = :isActive WHERE id = :projectId")
    suspend fun updateProjectStatus(projectId: String, isActive: Boolean)
}