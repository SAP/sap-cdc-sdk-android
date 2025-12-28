package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.provider.passkey.PasskeyCredential
import com.sap.cdc.android.sdk.feature.provider.passkey.PasskeyCredentials
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.state.PasskeysCredentialsState
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface IPasskeysCredentialsViewModel {
    val state: StateFlow<PasskeysCredentialsState>

    fun loadPasskeys()
    fun onRevokePasskey(keyId: String, activity: ComponentActivity)
}

class PasskeysCredentialsViewModel(
    context: Context,
    val authenticationFlowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IPasskeysCredentialsViewModel {

    private val _state = MutableStateFlow(PasskeysCredentialsState())
    override val state: StateFlow<PasskeysCredentialsState> = _state.asStateFlow()

    init {
        // Load passkeys when ViewModel is created
        loadPasskeys()
    }

    override fun loadPasskeys() {
        _state.update { it.copy(isLoadingPasskeys = true, error = null) }

        viewModelScope.launch {
            authenticationFlowDelegate.getPasskeys {
                doOnSuccess { authSuccess ->
                    try {
                        val credentials = json.decodeFromString<PasskeyCredentials>(authSuccess.jsonData)
                        _state.update { it.copy(isLoadingPasskeys = false, passkeyCredentials = credentials) }
                    } catch (e: Exception) {
                        _state.update { it.copy(isLoadingPasskeys = false, error = "Failed to parse passkeys data") }
                    }
                }

                doOnError { authError ->
                    _state.update { it.copy(isLoadingPasskeys = false, error = authError.message ?: "Failed to load passkeys") }
                }
            }
        }
    }

    override fun onRevokePasskey(keyId: String, activity: ComponentActivity) {
        _state.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            authenticationFlowDelegate.revokePasskey(keyId) {
                onSuccess = {
                    _state.update { it.copy(isLoading = false) }
                    loadPasskeys()
                }

                onError = { authError ->
                    _state.update { it.copy(isLoading = false, error = authError.message) }
                }
            }
        }
    }
}

// Preview class for PasskeysCredentialsViewModel with mock data (populated state)
class PasskeysCredentialsViewModelPreview : IPasskeysCredentialsViewModel {
    override val state: StateFlow<PasskeysCredentialsState> = MutableStateFlow(
        PasskeysCredentialsState(
            passkeyCredentials = PasskeyCredentials(
                credentials = listOf(
                    PasskeyCredential(
                        id = "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA",
                        deviceName = "iPhone 15 Pro",
                        registrationDate = "2024-01-15T10:30:00Z",
                        ipAddress = "192.168.1.100",
                        city = "Tel Aviv",
                        state = "Tel Aviv District",
                        country = "Israel",
                        platform = "iOS",
                        browser = "Safari",
                        isMobile = true,
                        lastLogin = "2024-03-10T14:22:00Z"
                    ),
                    PasskeyCredential(
                        id = "ISMiMzQlJic4KTArLi0uLzAxMjM0NTY3ODk6OzwrPj9A",
                        deviceName = "MacBook Pro",
                        registrationDate = "2024-02-20T16:45:00Z",
                        ipAddress = "192.168.1.101",
                        city = "Tel Aviv",
                        state = "Tel Aviv District",
                        country = "Israel",
                        platform = "macOS",
                        browser = "Chrome",
                        isMobile = false,
                        lastLogin = "2024-03-08T09:15:00Z"
                    ),
                    PasskeyCredential(
                        id = "QUJDREVGRwhJSktMTU5PUFFSU1RVVldYWVpbXF1eX2A",
                        deviceName = "Samsung Galaxy S24",
                        registrationDate = "2024-03-01T12:20:00Z",
                        ipAddress = "10.0.0.50",
                        city = "Haifa",
                        state = "Haifa District",
                        country = "Israel",
                        platform = "Android",
                        browser = "Chrome Mobile",
                        isMobile = true,
                        lastLogin = "2024-03-12T18:30:00Z"
                    )
                )
            )
        )
    ).asStateFlow()

    override fun loadPasskeys() {}
    override fun onRevokePasskey(keyId: String, activity: ComponentActivity) {}
}
