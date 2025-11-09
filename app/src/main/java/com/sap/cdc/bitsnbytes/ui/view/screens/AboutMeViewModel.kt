package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.bitsnbytes.extensions.splitFullName
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.AboutMeState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.encodeToJsonElement

interface IAboutMeViewModel {
    val state: StateFlow<AboutMeState>
    val flowDelegate: AuthenticationFlowDelegate?
        get() = null

    fun onNameChanged(name: String)
    fun onAliasChanged(alias: String)
    fun onSaveChanges()
    fun onDismissBanner()
}

class AboutMeViewModel(context: Context, override val flowDelegate: AuthenticationFlowDelegate) :
    BaseViewModel(context),
    IAboutMeViewModel {

    private val _state = MutableStateFlow(AboutMeState())
    override val state: StateFlow<AboutMeState> = _state.asStateFlow()

    init {
        // Initialize state from user account
        viewModelScope.launch {
            flowDelegate.userAccount.collect { account ->
                _state.update {
                    it.copy(
                        name = "${account?.profile?.firstName ?: ""} ${account?.profile?.lastName ?: ""}".trim(),
                        nickname = account?.profile?.nickname ?: "",
                        alias = account?.customIdentifiers?.alias ?: "",
                        email = account?.profile?.email ?: ""
                    )
                }
            }
        }
    }

    override fun onNameChanged(name: String) {
        _state.update { it.copy(name = name) }
    }

    override fun onAliasChanged(alias: String) {
        _state.update { it.copy(alias = alias) }
    }

    override fun onSaveChanges() {
        val currentState = _state.value
        val nameParts = currentState.name.splitFullName()
        
        val profileObject = json.encodeToJsonElement(
            mutableMapOf("firstName" to nameParts.first, "lastName" to nameParts.second)
        )
        val parameters = mutableMapOf("profile" to profileObject.toString())

        // Update alias (custom identifier) if provided
        if (currentState.alias.isNotEmpty()) {
            val customIdentifierObject = json.encodeToJsonElement(
                mutableMapOf("nationalId" to currentState.alias)
            )
            parameters["customIdentifiers"] = customIdentifierObject.toString()
        }

        _state.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            flowDelegate.setAccountInfo(parameters = parameters) {
                onSuccess = {
                    _state.update {
                        it.copy(isLoading = false, showSuccessBanner = true, error = null)
                    }
                }
                onError = { error ->
                    _state.update {
                        it.copy(isLoading = false, error = error.message)
                    }
                }
            }
        }
    }

    override fun onDismissBanner() {
        _state.update { it.copy(showSuccessBanner = false) }
    }
}

// Mocked preview class for AboutMeViewModel
class AboutMeViewModelPreview : IAboutMeViewModel {
    override val state: StateFlow<AboutMeState> = MutableStateFlow(
        AboutMeState(
            name = "John Doe",
            nickname = "Johnny",
            alias = "123456",
            email = "john.doe@example.com"
        )
    ).asStateFlow()
    
    override fun onNameChanged(name: String) {}
    override fun onAliasChanged(alias: String) {}
    override fun onSaveChanges() {}
    override fun onDismissBanner() {}
}
