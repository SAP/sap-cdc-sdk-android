package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context

interface ISignInViewModel: ISocialSignInViewModel {

}

// Mock preview class for the SignInViewModel
class SignInViewModelPreview: ISignInViewModel

class SignInViewModel(context: Context) : SocialSignInViewModel(context), ISignInViewModel