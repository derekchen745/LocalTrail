package com.example.localtrail.utils

import android.content.Context
import androidx.lifecycle.lifecycleScope
import androidx.fragment.app.Fragment
import kotlinx.coroutines.launch

/**
 * Extension function to easily add sync status monitoring to any Fragment
 */
fun Fragment.observeSyncStatus() {
    val syncStatusManager = SyncStatusManager.getInstance()
    
    viewLifecycleOwner.lifecycleScope.launch {
        syncStatusManager.syncStatus.collect { status ->
            // You can customize this based on your UI needs
            when {
                status.isSyncing -> {
                    // Show syncing indicator
                }
                status.unsyncedCount > 0 && status.isOnline -> {
                    // Show unsynced count
                }
                !status.isOnline && status.unsyncedCount > 0 -> {
                    // Show offline indicator with pending count
                }
            }
        }
    }
}

/**
 * Extension function to show sync status toast
 */
fun Fragment.showSyncStatusToast() {
    SyncStatusManager.getInstance().showSyncToast(requireContext())
}
