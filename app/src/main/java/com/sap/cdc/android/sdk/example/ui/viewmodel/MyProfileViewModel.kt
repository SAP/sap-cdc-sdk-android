package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.sap.cdc.android.sdk.auth.AuthState
import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.coroutines.launch

interface IMyProfileViewModel: IAccountViewModel {

    fun logOut(success: () -> Unit, onFailed: (CDCError) -> Unit) {
        //Stub
    }
}

class MyProfileViewModelPreview: IMyProfileViewModel{}

class MyProfileViewModel(context: Context): BaseViewModel(context), IMyProfileViewModel {

    /**
     * Log out of current session.
     */
    override fun logOut(success: () -> Unit, onFailed: (CDCError) -> Unit) {
        viewModelScope.launch {
            val authResponse = identityService.logout()
            when (authResponse.state()) {
                AuthState.SUCCESS -> {
                    success()
                }

                else -> {
                    onFailed(authResponse.toDisplayError()!!)
                }
            }
        }
    }
}