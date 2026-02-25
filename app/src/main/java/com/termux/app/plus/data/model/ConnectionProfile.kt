package com.termux.app.plus.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connection_profiles")
data class ConnectionProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nickname: String,
    val host: String,
    val port: Int = 22,
    val username: String,
    val authMethod: String,              // "PASSWORD", "KEY", "KEY_WITH_PASSPHRASE"
    val encryptedPassword: String? = null,
    val privateKeyPath: String? = null,
    val keepAliveEnabled: Boolean = true,
    val keepAliveInterval: Int = 60,     // seconds (ServerAliveInterval)
    val keepAliveCountMax: Int = 3,      // ServerAliveCountMax
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long? = null,
    val sortOrder: Int = 0
)
