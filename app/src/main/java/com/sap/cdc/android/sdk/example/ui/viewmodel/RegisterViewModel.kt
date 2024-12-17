package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context

interface IRegisterViewModel : ISocialSignInViewModel {

}

/**
 * Preview mock view model.
 */
class RegisterViewModelPreview : IRegisterViewModel {}

class RegisterViewModel(context: Context) : SocialSignInViewModel(context), IRegisterViewModel {

}