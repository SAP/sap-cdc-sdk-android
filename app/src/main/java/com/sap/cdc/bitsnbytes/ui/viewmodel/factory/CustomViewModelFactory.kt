package com.sap.cdc.bitsnbytes.ui.viewmodel.factory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.viewmodel.AccountViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.ConfigurationViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.CustomIDSignInViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.EmailRegisterViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.EmailSignInViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.LinkAccountViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.LoginOptionsViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.OtpSignInViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.OtpVerifyViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.PendingRegistrationViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.RegisterViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.ScreenSetViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.SignInViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.SocialSignInViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.TFAAuthenticationViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.WelcomeViewModel

@Suppress("UNCHECKED_CAST")
class CustomViewModelFactory(
    private val context: Context,
    private val authenticationFlowDelegate: AuthenticationFlowDelegate? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(WelcomeViewModel::class.java) -> {
                WelcomeViewModel(context) as T
            }

            modelClass.isAssignableFrom(SignInViewModel::class.java) -> {
                SignInViewModel(context) as T
            }

            modelClass.isAssignableFrom(SocialSignInViewModel::class.java) -> {
                SocialSignInViewModel(context) as T
            }

            modelClass.isAssignableFrom(ScreenSetViewModel::class.java) -> {
                ScreenSetViewModel(context) as T
            }

            modelClass.isAssignableFrom(PendingRegistrationViewModel::class.java) -> {
                requireNotNull(authenticationFlowDelegate) {
                    "AuthenticationFlowDelegate is required for PendingRegistrationViewModel"
                }
                PendingRegistrationViewModel(context, authenticationFlowDelegate) as T
            }

            modelClass.isAssignableFrom(OtpSignInViewModel::class.java) -> {
                requireNotNull(authenticationFlowDelegate) {
                    "AuthenticationFlowDelegate is required for OtpSignInViewModel"
                }
                OtpSignInViewModel(context, authenticationFlowDelegate) as T
            }

            modelClass.isAssignableFrom(OtpVerifyViewModel::class.java) -> {
                requireNotNull(authenticationFlowDelegate) {
                    "AuthenticationFlowDelegate is required for OtpSignInViewModel"
                }
                OtpVerifyViewModel(context, authenticationFlowDelegate) as T
            }

            modelClass.isAssignableFrom(LoginOptionsViewModel::class.java) -> {
                LoginOptionsViewModel(context) as T
            }

            modelClass.isAssignableFrom(LinkAccountViewModel::class.java) -> {
                LinkAccountViewModel(context) as T
            }

            modelClass.isAssignableFrom(RegisterViewModel::class.java) -> {
                RegisterViewModel(context) as T
            }

            modelClass.isAssignableFrom(EmailSignInViewModel::class.java) -> {
                requireNotNull(authenticationFlowDelegate) {
                    "AuthenticationFlowDelegate is required for EmailSignInViewModel"
                }
                EmailSignInViewModel(context, authenticationFlowDelegate) as T
            }

            modelClass.isAssignableFrom(CustomIDSignInViewModel::class.java) -> {
                requireNotNull(authenticationFlowDelegate) {
                    "AuthenticationFlowDelegate is required for CustomIDSignInViewModel"
                }
                CustomIDSignInViewModel(context, authenticationFlowDelegate) as T
            }

            modelClass.isAssignableFrom(EmailRegisterViewModel::class.java) -> {
                requireNotNull(authenticationFlowDelegate) {
                    "AuthenticationFlowDelegate is required for EmailRegisterViewModel"
                }
                EmailRegisterViewModel(context, authenticationFlowDelegate) as T
            }

            modelClass.isAssignableFrom(ConfigurationViewModel::class.java) -> {
                ConfigurationViewModel(context) as T
            }

            modelClass.isAssignableFrom(AccountViewModel::class.java) -> {
                requireNotNull(authenticationFlowDelegate) {
                    "AuthenticationFlowDelegate is required for AccountViewModel"
                }
                AccountViewModel(context, authenticationFlowDelegate) as T
            }

            modelClass.isAssignableFrom(TFAAuthenticationViewModel::class.java) -> {
                TFAAuthenticationViewModel(context) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
