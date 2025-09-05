package com.sap.cdc.bitsnbytes.navigation

import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sap.cdc.android.sdk.feature.auth.ResolvableContext
import com.sap.cdc.bitsnbytes.feature.auth.IdentityServiceRepository
import com.sap.cdc.bitsnbytes.ui.view.composables.AuthenticationTabView
import com.sap.cdc.bitsnbytes.ui.view.screens.AboutMeView
import com.sap.cdc.bitsnbytes.ui.view.screens.AuthMethodsView
import com.sap.cdc.bitsnbytes.ui.view.screens.EmailRegisterView
import com.sap.cdc.bitsnbytes.ui.view.screens.EmailSignInView
import com.sap.cdc.bitsnbytes.ui.view.screens.LinkAccountView
import com.sap.cdc.bitsnbytes.ui.view.screens.LoginOptionsView
import com.sap.cdc.bitsnbytes.ui.view.screens.MyProfileView
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
import com.sap.cdc.bitsnbytes.ui.viewmodel.AccountViewModel
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
import com.sap.cdc.bitsnbytes.ui.viewmodel.TFAAuthenticationViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.WelcomeViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.factory.CustomViewModelFactory
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

    val context = LocalContext.current.applicationContext
    val identityServiceRepository = IdentityServiceRepository.getInstance(context)

    NavHost(
        profileNavController, startDestination =
            when (identityServiceRepository.availableSession()) {
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
                factory = CustomViewModelFactory(context)
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
            val authDelegate = ViewModelScopeProvider.activityScopedAuthenticationDelegate(context)
            val viewModel: EmailSignInViewModel = ViewModelScopeProvider.activityScopedViewModel(
                factory = CustomViewModelFactory(context, authDelegate)
            )
            EmailSignInView(viewModel)
        }
        
        composable(ProfileScreenRoute.EmailRegister.route) {
            // ✅ OPTIMIZED: Retains registration form data
            val authDelegate = ViewModelScopeProvider.activityScopedAuthenticationDelegate(context)
            val viewModel: EmailRegisterViewModel = ViewModelScopeProvider.activityScopedViewModel(
                factory = CustomViewModelFactory(context, authDelegate)
            )
            EmailRegisterView(viewModel)
        }
        
        composable("${ProfileScreenRoute.ResolvePendingRegistration.route}/{resolvableContext}") { backStackEntry ->
            val resolvableJson = backStackEntry.arguments?.getString("resolvableContext")
            val resolvable = Json.decodeFromString<ResolvableContext>(resolvableJson!!)
            // Screen-scoped for temporary resolution flows
            val viewModel: PendingRegistrationViewModel = ViewModelScopeProvider.screenScopedViewModel(
                factory = CustomViewModelFactory(context)
            )
            PendingRegistrationView(viewModel, resolvable)
        }
        
        composable("${ProfileScreenRoute.ResolveLinkAccount.route}/{resolvableContext}") { backStackEntry ->
            val resolvableJson = backStackEntry.arguments?.getString("resolvableContext")
            val resolvable = Json.decodeFromString<ResolvableContext>(resolvableJson!!)
            // Screen-scoped for temporary resolution flows
            val viewModel: LinkAccountViewModel = ViewModelScopeProvider.screenScopedViewModel(
                factory = CustomViewModelFactory(context)
            )
            LinkAccountView(viewModel, resolvable)
        }
        
        composable(ProfileScreenRoute.MyProfile.route) {
            // ✅ OPTIMIZED: Profile data persists across navigation
            val viewModel: AccountViewModel = ViewModelScopeProvider.activityScopedViewModel(
                factory = CustomViewModelFactory(context)
            )
            MyProfileView(viewModel)
        }
        
        composable(ProfileScreenRoute.AboutMe.route) {
            // ✅ OPTIMIZED: Shares AccountViewModel with MyProfile for consistent data
            val viewModel: AccountViewModel = ViewModelScopeProvider.activityScopedViewModel(
                factory = CustomViewModelFactory(context)
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
                factory = CustomViewModelFactory(context)
            )
            OtpSignInView(viewModel, otpType = otpType!!)
        }
        
        composable("${ProfileScreenRoute.OTPVerify.route}/{resolvableContext}/{type}/{inputField}") { backStackEntry ->
            val resolvableJson = backStackEntry.arguments?.getString("resolvableContext")
            val input = backStackEntry.arguments?.getString("inputField")
            val type = backStackEntry.arguments?.getString("type")
            val otpType = OTPType.getByValue(type!!.toInt())
            val resolvable = Json.decodeFromString<ResolvableContext>(resolvableJson!!)
            val viewModel: OtpVerifyViewModel = ViewModelScopeProvider.screenScopedViewModel(
                factory = CustomViewModelFactory(context)
            )
            OtpVerifyView(
                viewModel,
                resolvable,
                otpType = otpType!!,
                inputField = input!!
            )
        }
        
        composable(ProfileScreenRoute.LoginOptions.route) {
            val viewModel: LoginOptionsViewModel = ViewModelScopeProvider.activityScopedViewModel(
                factory = CustomViewModelFactory(context)
            )
            LoginOptionsView(viewModel)
        }
        
        composable("${ProfileScreenRoute.AuthMethods.route}/{resolvableContext}") { backStackEntry ->
            val resolvableJson = backStackEntry.arguments?.getString("resolvableContext")
            val resolvable = Json.decodeFromString<ResolvableContext>(resolvableJson!!)
            val viewModel: TFAAuthenticationViewModel = ViewModelScopeProvider.screenScopedViewModel(
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
