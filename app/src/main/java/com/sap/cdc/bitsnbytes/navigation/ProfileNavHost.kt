package com.sap.cdc.bitsnbytes.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sap.cdc.android.sdk.feature.LinkingContext
import com.sap.cdc.android.sdk.feature.OTPContext
import com.sap.cdc.android.sdk.feature.RegistrationContext
import com.sap.cdc.android.sdk.feature.TwoFactorContext
import com.sap.cdc.bitsnbytes.ui.view.composables.AuthenticationTabView
import com.sap.cdc.bitsnbytes.ui.view.screens.AboutMeView
import com.sap.cdc.bitsnbytes.ui.view.screens.AboutMeViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.AuthMethodsView
import com.sap.cdc.bitsnbytes.ui.view.screens.AuthMethodsViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.BiometricLockedView
import com.sap.cdc.bitsnbytes.ui.view.screens.BiometricLockedViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.CustomIDSignInView
import com.sap.cdc.bitsnbytes.ui.view.screens.CustomIDSignInViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.EmailRegisterView
import com.sap.cdc.bitsnbytes.ui.view.screens.EmailRegistrationViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.EmailSignInView
import com.sap.cdc.bitsnbytes.ui.view.screens.EmailSignInViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.LinkAccountView
import com.sap.cdc.bitsnbytes.ui.view.screens.LinkAccountViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.LoginOptionsView
import com.sap.cdc.bitsnbytes.ui.view.screens.LoginOptionsViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.MyProfileView
import com.sap.cdc.bitsnbytes.ui.view.screens.MyProfileViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.OTPType
import com.sap.cdc.bitsnbytes.ui.view.screens.OtpSignInView
import com.sap.cdc.bitsnbytes.ui.view.screens.OtpSignInViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.OtpVerifyView
import com.sap.cdc.bitsnbytes.ui.view.screens.OtpVerifyViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.PasskeysCredentialsView
import com.sap.cdc.bitsnbytes.ui.view.screens.PasskeysCredentialsViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.PendingRegistrationView
import com.sap.cdc.bitsnbytes.ui.view.screens.PendingRegistrationViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.PhoneSelectionView
import com.sap.cdc.bitsnbytes.ui.view.screens.PhoneSelectionViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.PhoneVerificationView
import com.sap.cdc.bitsnbytes.ui.view.screens.PhoneVerificationViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.RegisterView
import com.sap.cdc.bitsnbytes.ui.view.screens.RegisterViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.ScreenSetView
import com.sap.cdc.bitsnbytes.ui.view.screens.ScreenSetViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.SignInView
import com.sap.cdc.bitsnbytes.ui.view.screens.SignInViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.TOTPVerificationView
import com.sap.cdc.bitsnbytes.ui.view.screens.TOTPVerificationViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.WelcomeView
import com.sap.cdc.bitsnbytes.ui.view.screens.WelcomeViewModel
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.factory.CustomViewModelFactory
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.factory.ViewModelScopeProvider
import kotlinx.serialization.json.Json

/**
 * Optimized Profile Navigation Host with proper ViewModel scoping.
 * This demonstrates the solution to ViewModel state loss during navigation.
 *
 * Key improvements:
 * - ViewModels are properly scoped to retain state
 * - Uses AppStateManager for centralized state management
 * - Maintains existing functionality while fixing state issues
 */
@Composable
fun ProfileNavHost(appStateManager: AppStateManager) {
    val profileNavController = rememberNavController()

    // Update the app state manager to use our profile navigation controller
    appStateManager.setNavController(profileNavController)

    // Listen to navigation changes and update back navigation state
    val navBackStackEntry by profileNavController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        // Always show back navigation in profile section since user can go back to main home
        // Even when MyProfile is the start destination (logged in state), user should be able to go back
        val canGoBack = true // Always show back arrow in profile section
        appStateManager.setCanNavigateBack(canGoBack)

        // Track if we have profile navigation stack for proper back navigation handling
        val hasProfileBackStack = profileNavController.previousBackStackEntry != null
        appStateManager.setHasProfileBackStack(hasProfileBackStack)
    }

    val context = LocalContext.current.applicationContext

    // Get the single activity-scoped delegate (provided by MainActivity)
    val authDelegate = ViewModelScopeProvider.activityScopedAuthenticationDelegate(context)

    NavHost(
        profileNavController, startDestination =
                when (authDelegate.hasValidSession()) {
                    true -> {
                        if (authDelegate.isBiometricActive()) {
                            ProfileScreenRoute.BiometricLocked.route
                        } else {
                            ProfileScreenRoute.MyProfile.route
                        }
                    }

                    false -> ProfileScreenRoute.Welcome.route
                }
        ) {
            composable(ProfileScreenRoute.Welcome.route) {
                val viewModel: WelcomeViewModel = ViewModelScopeProvider.activityScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                WelcomeView(viewModel)
            }

            composable(ProfileScreenRoute.SignIn.route) {
                val viewModel: SignInViewModel = ViewModelScopeProvider.activityScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                SignInView(viewModel)
            }

            composable(ProfileScreenRoute.Register.route) {
                val viewModel: RegisterViewModel = ViewModelScopeProvider.activityScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                RegisterView(viewModel)
            }

            composable("${ProfileScreenRoute.AuthTabView.route}/{selected}") { backStackEntry ->
                val selected = backStackEntry.arguments?.getString("selected")
                AuthenticationTabView(selected = selected!!.toInt())
            }

            composable(ProfileScreenRoute.EmailSignIn.route) {
                val viewModel: EmailSignInViewModel = ViewModelScopeProvider.activityScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                EmailSignInView(viewModel)
            }

            composable(ProfileScreenRoute.CustomIdSignIn.route) {
                val viewModel: CustomIDSignInViewModel = ViewModelScopeProvider.activityScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                CustomIDSignInView(viewModel)
            }

            composable(ProfileScreenRoute.EmailRegister.route) {
                val viewModel: EmailRegistrationViewModel = ViewModelScopeProvider.activityScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                EmailRegisterView(viewModel)
            }

            composable("${ProfileScreenRoute.ResolvePendingRegistration.route}/{registrationContext}") { backStackEntry ->
                val encodedJson = backStackEntry.arguments?.getString("registrationContext")
                val decodedJson = Uri.decode(encodedJson!!)
                val registrationContext = Json.decodeFromString<RegistrationContext>(decodedJson)
                // Screen-scoped for temporary resolution flows
                val viewModel: PendingRegistrationViewModel = ViewModelScopeProvider.screenScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                PendingRegistrationView(viewModel, registrationContext)
            }

            composable("${ProfileScreenRoute.ResolveLinkAccount.route}/{linkingContext}") { backStackEntry ->
                val encodedJson = backStackEntry.arguments?.getString("linkingContext")
                val decodedJson = Uri.decode(encodedJson!!)
                val linkingContext = Json.decodeFromString<LinkingContext>(decodedJson)
                // Screen-scoped for temporary resolution flows
                val viewModel: LinkAccountViewModel = ViewModelScopeProvider.screenScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                LinkAccountView(viewModel, linkingContext)
            }

            composable(ProfileScreenRoute.MyProfile.route) {
                val viewModel: MyProfileViewModel = ViewModelScopeProvider.activityScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                MyProfileView(viewModel)
            }

            composable(ProfileScreenRoute.AboutMe.route) {
                val viewModel: AboutMeViewModel = ViewModelScopeProvider.activityScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                AboutMeView(viewModel)
            }

            // Continue with existing screens using optimized scoping...
            composable(ScreenSetsRoute.ScreenSetRegistrationLoginLogin.route) {
                val viewModel: ScreenSetViewModel = ViewModelScopeProvider.screenScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                ScreenSetView(
                    viewModel,
                    "Default-RegistrationLogin",
                    "gigya-login-screen"
                )
            }

            composable(ScreenSetsRoute.ScreenSetRegistrationLoginRegister.route) {
                val viewModel: ScreenSetViewModel = ViewModelScopeProvider.screenScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                ScreenSetView(
                    viewModel,
                    "Default-RegistrationLogin",
                    "gigya-register-screen"
                )
            }

            composable("${ProfileScreenRoute.OTPSignIn.route}/{type}") { backStackEntry ->
                val type = backStackEntry.arguments?.getString("type")
                val otpType = OTPType.getByValue(type!!.toInt())
                val viewModel: OtpSignInViewModel = ViewModelScopeProvider.screenScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                OtpSignInView(viewModel, otpType = otpType!!)
            }

            composable("${ProfileScreenRoute.OTPVerify.route}/{otpContext}/{type}/{inputField}") { backStackEntry ->
                val encodedOtpContextJson = backStackEntry.arguments?.getString("otpContext")
                val decodedOtpContextJson = Uri.decode(encodedOtpContextJson!!)
                val input = backStackEntry.arguments?.getString("inputField")
                val type = backStackEntry.arguments?.getString("type")
                val otpType = OTPType.getByValue(type!!.toInt())
                val otpContext: OTPContext = Json.decodeFromString<OTPContext>(decodedOtpContextJson)
                val viewModel: OtpVerifyViewModel = ViewModelScopeProvider.screenScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                OtpVerifyView(
                    viewModel,
                    otpContext,
                    otpType = otpType!!,
                    inputField = input!!
                )
            }

            composable(ProfileScreenRoute.LoginOptions.route) {
                val viewModel: LoginOptionsViewModel = ViewModelScopeProvider.activityScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                LoginOptionsView(viewModel)
            }

            composable("${ProfileScreenRoute.AuthMethods.route}/{twoFactorContext}") { backStackEntry ->
                val encodedTwoFactorJson = backStackEntry.arguments?.getString("twoFactorContext")
                val decodedTwoFactorJson = Uri.decode(encodedTwoFactorJson!!)
                val twoFactorContext = Json.decodeFromString<TwoFactorContext>(decodedTwoFactorJson)
                val viewModel: AuthMethodsViewModel = ViewModelScopeProvider.screenScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                viewModel.initializeWithContext(twoFactorContext)
                AuthMethodsView(viewModel)
            }

            composable("${ProfileScreenRoute.PhoneSelection.route}/{TwoFactorContext}") { backStackEntry ->
                val encodedTwoFactorJson = backStackEntry.arguments?.getString("TwoFactorContext")
                val decodedTwoFactorJson = Uri.decode(encodedTwoFactorJson!!)
                val twoFactorContext = Json.decodeFromString<TwoFactorContext>(decodedTwoFactorJson)
                val viewModel: PhoneSelectionViewModel = ViewModelScopeProvider.screenScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                viewModel.initializeWithContext(twoFactorContext)
                PhoneSelectionView(viewModel)
            }

            composable("${ProfileScreenRoute.PhoneVerification.route}/{twoFactorContext}") { backStackEntry ->
                val encodedTwoFactorJson = backStackEntry.arguments?.getString("twoFactorContext")
                val decodedTwoFactorJson = Uri.decode(encodedTwoFactorJson!!)
                val twoFactorContext = Json.decodeFromString<TwoFactorContext>(decodedTwoFactorJson)
                val viewModel: PhoneVerificationViewModel = ViewModelScopeProvider.screenScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                viewModel.initializeWithContext(twoFactorContext)
                PhoneVerificationView(viewModel)
            }

            composable("${ProfileScreenRoute.TOTPVerification.route}/{twoFactorContext}") { backStackEntry ->
                val encodedTwoFactorJson = backStackEntry.arguments?.getString("twoFactorContext")
                val decodedTwoFactorJson = Uri.decode(encodedTwoFactorJson!!)
                val twoFactorContext = Json.decodeFromString<TwoFactorContext>(decodedTwoFactorJson)
                val viewModel: TOTPVerificationViewModel = ViewModelScopeProvider.screenScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                viewModel.initializeWithContext(twoFactorContext)
                TOTPVerificationView(viewModel)
            }

            composable(ProfileScreenRoute.BiometricLocked.route) {
                val viewModel: BiometricLockedViewModel = ViewModelScopeProvider.screenScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                BiometricLockedView(viewModel)
            }

            composable(ProfileScreenRoute.PasskeysCredentials.route) {
                val viewModel: PasskeysCredentialsViewModel = ViewModelScopeProvider.activityScopedViewModel(
                    factory = CustomViewModelFactory(context, authDelegate)
                )
                PasskeysCredentialsView(viewModel)
            }
        }
}
