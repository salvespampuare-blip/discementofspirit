package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SpiritualLogDao {
    @Query("SELECT * FROM spiritual_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<SpiritualLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: SpiritualLog)

    @Delete
    suspend fun deleteLog(log: SpiritualLog)

    @Query("DELETE FROM spiritual_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)

    @Query("DELETE FROM spiritual_logs")
    suspend fun clearAllLogs()
}
