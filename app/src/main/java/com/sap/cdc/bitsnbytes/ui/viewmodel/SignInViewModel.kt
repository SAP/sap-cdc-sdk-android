package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context

interface ISignInViewModel: ISocialSignInViewModel {

    fun passkeySignIn(
        hostActivity: ComponentActivity,
        onLogin: () -> Unit,
        onFailedWith: (CDCError?) -> Unit
    )
}

/**
 * Preview mock view model.
 */
class SignInViewModelPreview : ISignInViewModel {}

class SignInViewModel(context: Context) : SocialSignInViewModel(context), ISignInViewModel {

}