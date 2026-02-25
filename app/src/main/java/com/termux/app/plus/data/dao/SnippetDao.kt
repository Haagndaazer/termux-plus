package com.termux.app.plus.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.termux.app.plus.data.model.Snippet
import kotlinx.coroutines.flow.Flow

@Dao
interface SnippetDao {

    @Query("SELECT * FROM snippets ORDER BY sortOrder ASC, name ASC")
    fun getAll(): Flow<List<Snippet>>

    @Query("SELECT * FROM snippets WHERE id = :id")
    suspend fun getById(id: Long): Snippet?

    @Insert
    suspend fun insert(snippet: Snippet): Long

    @Update
    suspend fun update(snippet: Snippet)

    @Delete
    suspend fun delete(snippet: Snippet)

    @Query("DELETE FROM snippets WHERE id = :id")
    suspend fun deleteById(id: Long)
}
