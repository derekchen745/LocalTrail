package com.example.localtrail.utils

import android.content.Context
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SyncState(
    val isOnline: Boolean = false,
    val isSyncing: Boolean = false,
    val unsyncedCount: Int = 0,
    val lastSyncTime: Long? = null,
    val syncError: String? = null
)

class SyncStatusManager private constructor() {
    
    private val _syncStatus = MutableStateFlow(SyncState())
    val syncStatus: StateFlow<SyncState> = _syncStatus.asStateFlow()
    
    fun updateNetworkStatus(isOnline: Boolean) {
        _syncStatus.value = _syncStatus.value.copy(isOnline = isOnline)
    }
    
    fun updateSyncingStatus(isSyncing: Boolean) {
        _syncStatus.value = _syncStatus.value.copy(isSyncing = isSyncing)
    }
    
    fun updateUnsyncedCount(count: Int) {
        _syncStatus.value = _syncStatus.value.copy(unsyncedCount = count)
    }
    
    fun updateLastSyncTime(time: Long) {
        _syncStatus.value = _syncStatus.value.copy(lastSyncTime = time, syncError = null)
    }
    
    fun updateSyncError(error: String?) {
        _syncStatus.value = _syncStatus.value.copy(syncError = error)
    }
    
    fun showSyncToast(context: Context) {
        val status = _syncStatus.value
        val message = when {
            !status.isOnline -> "Offline - trails will sync when connected"
            status.isSyncing -> "Syncing trails..."
            status.unsyncedCount > 0 -> "${status.unsyncedCount} trails pending sync"
            else -> "All trails synced!"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    companion object {
        @Volatile
        private var INSTANCE: SyncStatusManager? = null
        
        fun getInstance(): SyncStatusManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SyncStatusManager().also { INSTANCE = it }
            }
        }
    }
}
