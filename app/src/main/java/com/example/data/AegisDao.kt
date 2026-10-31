package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AegisDao {
    @Query("SELECT * FROM app_rules ORDER BY isBlockedInPartial DESC, appName ASC")
    fun observeAllAppRules(): Flow<List<AppRuleEntity>>

    @Query("SELECT * FROM app_rules")
    suspend fun getAllAppRulesOnce(): List<AppRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppRules(rules: List<AppRuleEntity>)

    @Update
    suspend fun updateAppRule(rule: AppRuleEntity)

    @Query("UPDATE app_rules SET blockedAttemptsCount = blockedAttemptsCount + 1 WHERE packageName = :packageName")
    suspend fun incrementAppBlockedCount(packageName: String)

    @Query("SELECT * FROM lock_sessions WHERE isActive = 1 ORDER BY id DESC LIMIT 1")
    fun observeActiveSession(): Flow<LockSessionEntity?>

    @Query("SELECT * FROM lock_sessions WHERE isActive = 1 ORDER BY id DESC LIMIT 1")
    suspend fun getActiveSessionOnce(): LockSessionEntity?

    @Query("SELECT * FROM lock_sessions ORDER BY startTimeEpochMs DESC LIMIT 30")
    fun observeSessionHistory(): Flow<List<LockSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLockSession(session: LockSessionEntity): Long

    @Update
    suspend fun updateLockSession(session: LockSessionEntity)

    @Query("UPDATE lock_sessions SET interceptionsCount = interceptionsCount + 1 WHERE id = :sessionId")
    suspend fun incrementSessionInterceptions(sessionId: Long)

    @Query("SELECT * FROM interception_logs ORDER BY timestampEpochMs DESC LIMIT 50")
    fun observeInterceptionLogs(): Flow<List<InterceptionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInterceptionLog(log: InterceptionLogEntity)

    @Query("DELETE FROM interception_logs")
    suspend fun clearInterceptionLogs()
}
