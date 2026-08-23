package com.example.otadashboard.security

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanLogDao {
    @Query("SELECT * FROM scan_logs ORDER BY id DESC")
    fun getAllLogs(): Flow<List<ScanLogEntity>>

    @Insert
    suspend fun insertLog(log: ScanLogEntity)

    @Query("DELETE FROM scan_logs")
    suspend fun clearLogs()
}
