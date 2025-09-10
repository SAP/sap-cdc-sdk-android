package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate

interface IRegisterViewModel : ISocialSignInViewModel {

}

// Mock preview class for the RegisterViewModel
class RegisterViewModelPreview : IRegisterViewModel

class RegisterViewModel(context: Context, flowDelegate: AuthenticationFlowDelegate) :
    SocialSignInViewModel(context, flowDelegate),
    IRegisterViewModel