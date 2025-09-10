package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.viewmodel.ISocialSignInViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.SocialSignInViewModel

interface IRegisterViewModel : ISocialSignInViewModel {

}

// Mock preview class for the RegisterViewModel
class RegisterViewModelPreview : IRegisterViewModel

class RegisterViewModel(context: Context, flowDelegate: AuthenticationFlowDelegate) :
    SocialSignInViewModel(context, flowDelegate),
    IRegisterViewModel