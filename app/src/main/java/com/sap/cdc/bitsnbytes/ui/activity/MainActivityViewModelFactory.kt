package com.sap.cdc.bitsnbytes.ui.activity

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate

/**
 * Factory for creating MainActivityViewModel with required dependencies.
 * 
 * Creates a single activity-scoped AuthenticationFlowDelegate instance that will be
 * shared across the entire application through the ViewModel lifecycle.
 * 
 * @property context Application context for initializing AuthenticationFlowDelegate
 */
class MainActivityViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainActivityViewModel::class.java)) {
            return MainActivityViewModel(
                authenticationFlowDelegate = AuthenticationFlowDelegate(context)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
