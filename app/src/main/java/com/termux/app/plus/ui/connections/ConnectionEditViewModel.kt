package com.termux.app.plus.ui.connections

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.termux.app.plus.data.db.TermuxPlusDatabase
import com.termux.app.plus.data.model.ConnectionProfile
import com.termux.app.plus.security.CredentialManager
import kotlinx.coroutines.launch

class ConnectionEditViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = TermuxPlusDatabase.getInstance(application).connectionProfileDao()

    suspend fun getProfile(id: Long): ConnectionProfile? {
        return dao.getById(id)
    }

    fun saveProfile(
        existingId: Long?,
        nickname: String,
        host: String,
        port: Int,
        username: String,
        authMethod: String,
        password: String?,
        privateKeyPath: String?,
        keepAliveEnabled: Boolean,
        keepAliveInterval: Int,
        keepAliveCountMax: Int,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            val encryptedPassword = if (password != null && password.isNotEmpty()) {
                CredentialManager.encrypt(password, getApplication())
            } else {
                null
            }

            if (existingId != null) {
                val existing = dao.getById(existingId)
                if (existing != null) {
                    dao.update(
                        existing.copy(
                            nickname = nickname,
                            host = host,
                            port = port,
                            username = username,
                            authMethod = authMethod,
                            encryptedPassword = encryptedPassword ?: existing.encryptedPassword,
                            privateKeyPath = privateKeyPath,
                            keepAliveEnabled = keepAliveEnabled,
                            keepAliveInterval = keepAliveInterval,
                            keepAliveCountMax = keepAliveCountMax
                        )
                    )
                }
            } else {
                dao.insert(
                    ConnectionProfile(
                        nickname = nickname,
                        host = host,
                        port = port,
                        username = username,
                        authMethod = authMethod,
                        encryptedPassword = encryptedPassword,
                        privateKeyPath = privateKeyPath,
                        keepAliveEnabled = keepAliveEnabled,
                        keepAliveInterval = keepAliveInterval,
                        keepAliveCountMax = keepAliveCountMax
                    )
                )
            }
            onComplete()
        }
    }
}
