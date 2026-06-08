package com.opendesign.data.db

import android.content.Context
import androidx.room.*
import com.opendesign.data.model.Artifact
import com.opendesign.data.model.Project
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>
    
    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProject(id: String): ProjectEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)
    
    @Delete
    suspend fun deleteProject(project: ProjectEntity)
    
    @Query("UPDATE projects SET updatedAt = :time WHERE id = :id")
    suspend fun updateTimestamp(id: String, time: Long)
}

@Dao
interface ArtifactDao {
    @Query("SELECT * FROM artifacts WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getArtifactsForProject(projectId: String): Flow<List<ArtifactEntity>>
    
    @Query("SELECT * FROM artifacts ORDER BY createdAt DESC")
    fun getAllArtifacts(): Flow<List<ArtifactEntity>>
    
    @Query("SELECT * FROM artifacts WHERE id = :id")
    suspend fun getArtifact(id: String): ArtifactEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtifact(artifact: ArtifactEntity)
    
    @Delete
    suspend fun deleteArtifact(artifact: ArtifactEntity)
}

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val designSystemId: String = ""
)

@Entity(tableName = "artifacts")
data class ArtifactEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val skill: String,
    val mode: String,
    val prompt: String,
    val htmlContent: String,
    val createdAt: Long,
    val designSystemName: String = ""
)

@Database(
    entities = [ProjectEntity::class, ArtifactEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun artifactDao(): ArtifactDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "open_design.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
