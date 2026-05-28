package com.app.mobilldu.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PhotoRecordEntity): Long

    @Query("UPDATE photo_records SET status = :status, errorMessage = :errorMessage WHERE id = :id")
    suspend fun updateStatus(id: Long, status: String, errorMessage: String? = null)

    @Query("SELECT * FROM photo_records ORDER BY createdAt DESC")
    fun getAllRecords(): Flow<List<PhotoRecordEntity>>

    @Query("DELETE FROM photo_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM photo_records ORDER BY createdAt DESC")
    suspend fun getAllRecordsOnce(): List<PhotoRecordEntity>
}
