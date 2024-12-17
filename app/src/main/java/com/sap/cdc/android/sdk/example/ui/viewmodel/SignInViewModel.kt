package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context

interface ISignInViewModel: ISocialSignInViewModel {

}

/**
 * Preview mock view model.
 */
class SignInViewModelPreview : ISignInViewModel {}

class SignInViewModel(context: Context) : SocialSignInViewModel(context), ISignInViewModel {

}