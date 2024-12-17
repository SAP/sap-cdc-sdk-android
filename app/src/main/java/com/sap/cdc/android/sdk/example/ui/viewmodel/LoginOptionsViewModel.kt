package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sap.cdc.android.sdk.auth.session.SessionSecureLevel

interface ILoginOptionsViewModel {

    fun sessionSecurityLevel(): SessionSecureLevel {
        return SessionSecureLevel.STANDARD
    }

}

/**
 * Preview mock view model.
 */
class LoginOptionsViewModelPreview : ILoginOptionsViewModel {}

class LoginOptionsViewModel(context: Context) : BaseViewModel(context),
    ILoginOptionsViewModel {

    /**
     * Holding reference to session security level for changes.
     */
    private var sessionSecurityLevel by mutableStateOf(SessionSecureLevel.STANDARD)

    /**
     * Get session security level.
     */
    override fun sessionSecurityLevel(): SessionSecureLevel {
        sessionSecurityLevel = identityService.sessionSecurityLevel()
        return sessionSecurityLevel
    }
}

