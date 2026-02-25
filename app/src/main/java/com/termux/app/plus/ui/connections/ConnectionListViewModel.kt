package com.termux.app.plus.ui.connections

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.termux.app.plus.data.db.TermuxPlusDatabase
import com.termux.app.plus.data.model.ConnectionProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class ConnectionListViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = TermuxPlusDatabase.getInstance(application).connectionProfileDao()

    val connections: Flow<List<ConnectionProfile>> = dao.getAll()

    fun updateLastUsed(profileId: Long) {
        viewModelScope.launch {
            dao.updateLastUsed(profileId, System.currentTimeMillis())
        }
    }

    fun deleteProfile(profile: ConnectionProfile) {
        viewModelScope.launch {
            dao.delete(profile)
        }
    }
}
