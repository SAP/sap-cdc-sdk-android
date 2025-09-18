package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel

interface IAuthMethodsViewModel {

    // Stub
}

class AuthMethodsViewModel(context: Context, val flowDelegate: AuthenticationFlowDelegate) : BaseViewModel(context),
    IAuthMethodsViewModel {

    // Stub
}

// Mocked preview class for AuthMethodsViewModel
class AuthMethodsViewModelPreview : IAuthMethodsViewModel