package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.bitsnbytes.ApplicationConfig
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.ConfigurationState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

interface IConfigurationViewModel {
    val state: StateFlow<ConfigurationState>
    
    fun onApiKeyChanged(apiKey: String)
    fun onDomainChanged(domain: String)
    fun onCnameChanged(cname: String)
    fun onWebViewToggled(use: Boolean)
    fun onDebugNavigationLoggingToggled(enabled: Boolean)
    fun onSaveChanges()
    fun onDismissBanner()
    fun refreshState()
}

// Mocked preview class for ConfigurationViewModel
class ConfigurationViewModelPreview : IConfigurationViewModel {
    override val state: StateFlow<ConfigurationState> = MutableStateFlow(
        ConfigurationState(
            apiKey = "3_test_api_key",
            domain = "us1.gigya.com",
            cname = "",
            useWebView = false,
            debugNavigationLogging = false
        )
    ).asStateFlow()
    
    override fun onApiKeyChanged(apiKey: String) {}
    override fun onDomainChanged(domain: String) {}
    override fun onCnameChanged(cname: String) {}
    override fun onWebViewToggled(use: Boolean) {}
    override fun onDebugNavigationLoggingToggled(enabled: Boolean) {}
    override fun onSaveChanges() {}
    override fun onDismissBanner() {}
    override fun refreshState() {}
}

class ConfigurationViewModel(
    private val context: Context,
    val flowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IConfigurationViewModel {

    private val _state = MutableStateFlow(loadCurrentState())
    override val state: StateFlow<ConfigurationState> = _state.asStateFlow()

    /**
     * Helper method to load current configuration state from AuthenticationFlowDelegate.
     * This ensures the state always reflects the current configuration.
     */
    private fun loadCurrentState(): ConfigurationState {
        return ConfigurationState(
            apiKey = flowDelegate.siteConfig.apiKey,
            domain = flowDelegate.siteConfig.domain,
            cname = flowDelegate.siteConfig.cname ?: "",
            useWebView = ApplicationConfig.useWebViews,
            debugNavigationLogging = ApplicationConfig.debugNavigationLogging,
            showSuccessBanner = false
        )
    }

    override fun onApiKeyChanged(apiKey: String) {
        _state.update { it.copy(apiKey = apiKey) }
    }

    override fun onDomainChanged(domain: String) {
        _state.update { it.copy(domain = domain) }
    }

    override fun onCnameChanged(cname: String) {
        _state.update { it.copy(cname = cname) }
    }

    override fun onWebViewToggled(use: Boolean) {
        _state.update { it.copy(useWebView = use) }
        ApplicationConfig.useWebViews(use)
    }

    override fun onDebugNavigationLoggingToggled(enabled: Boolean) {
        _state.update { it.copy(debugNavigationLogging = enabled) }
        ApplicationConfig.setDebugNavigationLogging(enabled)
    }

    override fun onSaveChanges() {
        val currentState = _state.value
        val newSiteConfig = SiteConfig(
            context,
            apiKey = currentState.apiKey,
            domain = currentState.domain,
            cname = currentState.cname.ifBlank { null }
        )
        flowDelegate.reinitializeWithNewConfig(newSiteConfig)
        
        // Show success banner
        _state.update { it.copy(showSuccessBanner = true) }
    }

    override fun onDismissBanner() {
        _state.update { it.copy(showSuccessBanner = false) }
    }

    override fun refreshState() {
        _state.value = loadCurrentState()
    }
}
