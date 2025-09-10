package com.sap.cdc.bitsnbytes.ui.viewmodel.factory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.view.screens.AboutMeViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.AuthMethodsViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.ConfigurationViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.AccountViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.CustomIDSignInViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.EmailRegistrationViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.EmailSignInViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.LinkAccountViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.LoginOptionsViewModel
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
                requireNotNull(authenticationFlowDelegate) {
                    "AuthenticationFlowDelegate is required for PendingRegistrationViewModel"
                }
                SignInViewModel(context, authenticationFlowDelegate) as T
            }

            modelClass.isAssignableFrom(SocialSignInViewModel::class.java) -> {
                requireNotNull(authenticationFlowDelegate) {
                    "AuthenticationFlowDelegate is required for SocialSignInViewModel"
                }
                SocialSignInViewModel(context, authenticationFlowDelegate) as T
            }

            modelClass.isAssignableFrom(ScreenSetViewModel::class.java) -> {
                requireNotNull(authenticationFlowDelegate) {
                    "AuthenticationFlowDelegate is required for ScreenSetViewModel"
                }
                ScreenSetViewModel(context, authenticationFlowDelegate) as T
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
                requireNotNull(authenticationFlowDelegate) {
                    "AuthenticationFlowDelegate is required for LoginOptionsViewModel"
                }
                LoginOptionsViewModel(context, authenticationFlowDelegate) as T
            }

            modelClass.isAssignableFrom(LinkAccountViewModel::class.java) -> {
                requireNotNull(authenticationFlowDelegate) {
                    "AuthenticationFlowDelegate is required for LinkAccountViewModel"
                }
                LinkAccountViewModel(context, authenticationFlowDelegate) as T
            }

            modelClass.isAssignableFrom(RegisterViewModel::class.java) -> {
                requireNotNull(authenticationFlowDelegate) {
                    "AuthenticationFlowDelegate is required for RegisterViewModel"
                }
                RegisterViewModel(context, authenticationFlowDelegate) as T
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

            modelClass.isAssignableFrom(EmailRegistrationViewModel::class.java) -> {
                requireNotNull(authenticationFlowDelegate) {
                    "AuthenticationFlowDelegate is required for EmailRegisterViewModel"
                }
                EmailRegistrationViewModel(context, authenticationFlowDelegate) as T
            }

            modelClass.isAssignableFrom(ConfigurationViewModel::class.java) -> {
                requireNotNull(authenticationFlowDelegate) {
                    "AuthenticationFlowDelegate is required for ConfigurationViewModel"
                }
                ConfigurationViewModel(context, authenticationFlowDelegate) as T
            }

            modelClass.isAssignableFrom(AboutMeViewModel::class.java) -> {
                requireNotNull(authenticationFlowDelegate) {
                    "AuthenticationFlowDelegate is required for AboutMeViewModel"
                }
                AboutMeViewModel(context, authenticationFlowDelegate) as T
            }

            modelClass.isAssignableFrom(AuthMethodsViewModel::class.java) -> {
                requireNotNull(authenticationFlowDelegate) {
                    "AuthenticationFlowDelegate is required for AuthMethodsViewModel"
                }
                AuthMethodsViewModel(context) as T
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
