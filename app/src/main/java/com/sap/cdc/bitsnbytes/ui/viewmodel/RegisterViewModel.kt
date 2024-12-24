package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context

interface IRegisterViewModel : ISocialSignInViewModel {

}

/**
 * Preview mock view model.
 */
class RegisterViewModelPreview : IRegisterViewModel {}

class RegisterViewModel(context: Context) : SocialSignInViewModel(context), IRegisterViewModel {

}