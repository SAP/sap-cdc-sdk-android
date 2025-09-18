package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.feature.AuthCallbacks
import com.sap.cdc.android.sdk.feature.provider.passkey.PasskeyCredential
import com.sap.cdc.android.sdk.feature.provider.passkey.PasskeyCredentials
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel
import kotlinx.coroutines.launch

interface IPasskeysCredentialsViewModel {

    val passkeyCredentials: PasskeyCredentials?
    val isLoadingPasskeys: Boolean
    val error: String?

    fun loadPasskeys()

    fun revokePasskey(
        keyId: String,
        activity: ComponentActivity,
        authCallbacks: AuthCallbacks.() -> Unit
    )

    fun clearError()
}

class PasskeysCredentialsViewModel(
    context: Context,
    val authenticationFlowDelegate: AuthenticationFlowDelegate
) : BaseViewModel(context), IPasskeysCredentialsViewModel {

    // PasskeyCredentials state management
    private var _passkeyCredentials by mutableStateOf<PasskeyCredentials?>(null)
    override val passkeyCredentials: PasskeyCredentials? get() = _passkeyCredentials

    // Loading state for passkeys
    private var _isLoadingPasskeys by mutableStateOf(false)
    override val isLoadingPasskeys: Boolean get() = _isLoadingPasskeys

    // Error state
    private var _error by mutableStateOf<String?>(null)
    override val error: String? get() = _error

    init {
        // Load passkeys when ViewModel is created
        loadPasskeys()
    }

    /**
     * Load passkeys using the AuthenticationFlowDelegate getPasskeys method.
     * This method parses the response and updates the passkeyCredentials state.
     */
    override fun loadPasskeys() {
        _isLoadingPasskeys = true
        _error = null

        viewModelScope.launch {
            authenticationFlowDelegate.getPasskeys {
                doOnSuccess { authSuccess ->
                    try {
                        // Parse the response and update the passkey credentials state
                        val credentials = json.decodeFromString<PasskeyCredentials>(authSuccess.jsonData)
                        _passkeyCredentials = credentials
                    } catch (e: Exception) {
                        _error = "Failed to parse passkeys data"
                    } finally {
                        _isLoadingPasskeys = false
                    }
                }

                doOnError { authError ->
                    _error = authError.message ?: "Failed to load passkeys"
                    _isLoadingPasskeys = false
                }
            }
        }
    }

    /**
     * Revoke a specific passkey by its ID.
     * After successful revocation, reload the passkeys list.
     */
    override fun revokePasskey(
        keyId: String,
        activity: ComponentActivity,
        authCallbacks: AuthCallbacks.() -> Unit
    ) {
        viewModelScope.launch {
            authenticationFlowDelegate.revokePasskey(keyId) {
                // Register original callbacks first
                authCallbacks()

                doOnSuccess {
                    // Refresh the passkeys list after successful revocation
                    loadPasskeys()
                }

                doOnError { authError ->
                    _error = authError.message
                }
            }
        }
    }

    /**
     * Clear any error state
     */
    override fun clearError() {
        _error = null
    }
}

// Preview class for PasskeysCredentialsViewModel with mock data (populated state)
class PasskeysCredentialsViewModelPreview : IPasskeysCredentialsViewModel {
    override val passkeyCredentials: PasskeyCredentials? = PasskeyCredentials(
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
    override val isLoadingPasskeys: Boolean = false
    override val error: String? = null

    override fun loadPasskeys() {}
    override fun revokePasskey(keyId: String, activity: ComponentActivity, authCallbacks: AuthCallbacks.() -> Unit) {}
    override fun clearError() {}
}

