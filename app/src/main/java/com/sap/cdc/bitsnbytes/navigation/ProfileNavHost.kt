package com.sap.cdc.bitsnbytes.navigation

import android.util.Base64
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
import com.sap.cdc.android.sdk.feature.ResolvableContext
import com.sap.cdc.android.sdk.feature.TwoFactorContext
import com.sap.cdc.bitsnbytes.ui.view.composables.AuthenticationTabView
import com.sap.cdc.bitsnbytes.ui.view.screens.AboutMeView
import com.sap.cdc.bitsnbytes.ui.view.screens.AboutMeViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.AuthMethodsView
import com.sap.cdc.bitsnbytes.ui.view.screens.AuthMethodsViewModel
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
import com.sap.cdc.bitsnbytes.ui.view.screens.OtpVerifyView
import com.sap.cdc.bitsnbytes.ui.view.screens.PendingRegistrationView
import com.sap.cdc.bitsnbytes.ui.view.screens.PhoneSelectionView
import com.sap.cdc.bitsnbytes.ui.view.screens.PhoneVerificationView
import com.sap.cdc.bitsnbytes.ui.view.screens.RegisterView
import com.sap.cdc.bitsnbytes.ui.view.screens.ScreenSetView
import com.sap.cdc.bitsnbytes.ui.view.screens.SignInView
import com.sap.cdc.bitsnbytes.ui.view.screens.TOTPVerificationView
import com.sap.cdc.bitsnbytes.ui.view.screens.WelcomeView
import com.sap.cdc.bitsnbytes.ui.view.screens.OtpSignInViewModel
import com.sap.cdc.bitsnbytes.ui.view.screens.OtpVerifyViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.PendingRegistrationViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.RegisterViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.ScreenSetViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.SignInViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.TFAAuthenticationViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.WelcomeViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.factory.CustomViewModelFactory
import com.sap.cdc.bitsnbytes.ui.viewmodel.factory.ViewModelScopeProvider
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
fun OptimizedProfileNavHost(appStateManager: AppStateManager) {
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

    // ✅ Create the shared AuthenticationFlowDelegate ONCE at the top level
    val authDelegate = ViewModelScopeProvider.activityScopedAuthenticationDelegate(context)

    NavHost(
        profileNavController, startDestination =
            when (authDelegate.hasValidSession()) {
                true -> ProfileScreenRoute.MyProfile.route
                false -> ProfileScreenRoute.Welcome.route
            }
    ) {
        composable(ProfileScreenRoute.Welcome.route) {
            // ✅ OPTIMIZED: Use activity-scoped ViewModel to retain state across navigation
            val viewModel: WelcomeViewModel = ViewModelScopeProvider.activityScopedViewModel(
                factory = CustomViewModelFactory(context)
            )
            WelcomeView(viewModel)
        }

        composable(ProfileScreenRoute.SignIn.route) {
            // ✅ OPTIMIZED: Activity-scoped ViewModel retains login form state
            val viewModel: SignInViewModel = ViewModelScopeProvider.activityScopedViewModel(
                factory = CustomViewModelFactory(context, authDelegate)
            )
            SignInView(viewModel)
        }

        composable(ProfileScreenRoute.Register.route) {
            // ✅ OPTIMIZED: Activity-scoped ViewModel retains registration form state
            val viewModel: RegisterViewModel = ViewModelScopeProvider.activityScopedViewModel(
                factory = CustomViewModelFactory(context)
            )
            RegisterView(viewModel)
        }

        composable("${ProfileScreenRoute.AuthTabView.route}/{selected}") { backStackEntry ->
            val selected = backStackEntry.arguments?.getString("selected")
            AuthenticationTabView(selected = selected!!.toInt())
        }

        composable(ProfileScreenRoute.EmailSignIn.route) {
            // ✅ OPTIMIZED: Retains email and form state during navigation
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
            // ✅ OPTIMIZED: Retains registration form data
            val viewModel: EmailRegistrationViewModel = ViewModelScopeProvider.activityScopedViewModel(
                factory = CustomViewModelFactory(context, authDelegate)
            )
            EmailRegisterView(viewModel)
        }

        composable("${ProfileScreenRoute.ResolvePendingRegistration.route}/{RegistrationContext}") { backStackEntry ->
            val resolvableJson = backStackEntry.arguments?.getString("RegistrationContext")
            val registrationContext = Json.decodeFromString<RegistrationContext>(resolvableJson!!)
            // Screen-scoped for temporary resolution flows
            val viewModel: PendingRegistrationViewModel = ViewModelScopeProvider.screenScopedViewModel(
                factory = CustomViewModelFactory(context, authDelegate)
            )
            PendingRegistrationView(viewModel, registrationContext)
        }

        composable("${ProfileScreenRoute.ResolveLinkAccount.route}/{linkingContext}") { backStackEntry ->
            val resolvableJson = backStackEntry.arguments?.getString("linkingContext")
            val linkingContext = Json.decodeFromString<LinkingContext>(resolvableJson!!)
            // Screen-scoped for temporary resolution flows
            val viewModel: LinkAccountViewModel = ViewModelScopeProvider.screenScopedViewModel(
                factory = CustomViewModelFactory(context)
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
                factory = CustomViewModelFactory(context)
            )
            ScreenSetView(
                viewModel,
                "Default-RegistrationLogin",
                "gigya-login-screen"
            )
        }

        composable(ScreenSetsRoute.ScreenSetRegistrationLoginRegister.route) {
            val viewModel: ScreenSetViewModel = ViewModelScopeProvider.screenScopedViewModel(
                factory = CustomViewModelFactory(context)
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
            val otpContextJson = backStackEntry.arguments?.getString("otpContext")
            val input = backStackEntry.arguments?.getString("inputField")
            val type = backStackEntry.arguments?.getString("type")
            val otpType = OTPType.getByValue(type!!.toInt())
            val otpContext: OTPContext = Json.decodeFromString<OTPContext>(otpContextJson!!)
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
            val twoFactorJson = backStackEntry.arguments?.getString("twoFactorContext")
            val resolvable = Json.decodeFromString<TwoFactorContext>(twoFactorJson!!)
            val viewModel: AuthMethodsViewModel = ViewModelScopeProvider.screenScopedViewModel(
                factory = CustomViewModelFactory(context)
            )
            AuthMethodsView(viewModel, resolvable)
        }

        composable("${ProfileScreenRoute.PhoneSelection.route}/{resolvableContext}") { backStackEntry ->
            val resolvableJson = backStackEntry.arguments?.getString("resolvableContext")
            val resolvable = Json.decodeFromString<ResolvableContext>(resolvableJson!!)
            val viewModel: TFAAuthenticationViewModel = ViewModelScopeProvider.screenScopedViewModel(
                factory = CustomViewModelFactory(context)
            )
            PhoneSelectionView(viewModel)
        }

        composable("${ProfileScreenRoute.PhoneVerification.route}/{resolvableContext}") { backStackEntry ->
            val resolvableJsonEncoded = backStackEntry.arguments?.getString("resolvableContext")
            val resolvableJson =
                String(Base64.decode(resolvableJsonEncoded, Base64.DEFAULT), Charsets.UTF_8)
            val resolvable = Json.decodeFromString<ResolvableContext>(resolvableJson)
            val viewModel: TFAAuthenticationViewModel = ViewModelScopeProvider.screenScopedViewModel(
                factory = CustomViewModelFactory(context)
            )
            PhoneVerificationView(viewModel)
        }

        composable("${ProfileScreenRoute.TOTPVerification.route}/{resolvableContext}") { backStackEntry ->
            val resolvableJsonEncoded = backStackEntry.arguments?.getString("resolvableContext")
            val resolvableJson =
                String(Base64.decode(resolvableJsonEncoded, Base64.DEFAULT), Charsets.UTF_8)
            val resolvable = Json.decodeFromString<ResolvableContext>(resolvableJson)
            val viewModel: TFAAuthenticationViewModel = ViewModelScopeProvider.screenScopedViewModel(
                factory = CustomViewModelFactory(context)
            )
            TOTPVerificationView(viewModel)
        }
    }
}
