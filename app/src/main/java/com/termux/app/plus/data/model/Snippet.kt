package com.termux.app.plus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snippets")
data class Snippet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val command: String,
    val autoExecute: Boolean = true,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
