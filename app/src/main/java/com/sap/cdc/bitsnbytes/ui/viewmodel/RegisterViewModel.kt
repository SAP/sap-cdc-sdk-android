package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context

interface IRegisterViewModel : ISocialSignInViewModel {

}

// Mock preview class for the RegisterViewModel
class RegisterViewModelPreview: IRegisterViewModel

class RegisterViewModel(context: Context) : SocialSignInViewModel(context), IRegisterViewModel