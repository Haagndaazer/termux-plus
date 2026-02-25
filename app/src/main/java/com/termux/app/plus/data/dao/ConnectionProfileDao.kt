package com.termux.app.plus.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.termux.app.plus.data.model.ConnectionProfile
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionProfileDao {

    @Query("SELECT * FROM connection_profiles ORDER BY sortOrder ASC, lastUsedAt DESC")
    fun getAll(): Flow<List<ConnectionProfile>>

    @Query("SELECT * FROM connection_profiles WHERE id = :id")
    suspend fun getById(id: Long): ConnectionProfile?

    @Insert
    suspend fun insert(profile: ConnectionProfile): Long

    @Update
    suspend fun update(profile: ConnectionProfile)

    @Delete
    suspend fun delete(profile: ConnectionProfile)

    @Query("UPDATE connection_profiles SET lastUsedAt = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: Long, timestamp: Long)

    @Query("DELETE FROM connection_profiles WHERE id = :id")
    suspend fun deleteById(id: Long)
}
