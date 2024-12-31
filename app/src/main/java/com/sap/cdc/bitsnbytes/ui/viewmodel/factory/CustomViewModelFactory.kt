package com.sap.cdc.bitsnbytes.ui.viewmodel.factory

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sap.cdc.bitsnbytes.ui.viewmodel.AccountViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.RegisterViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.SignInViewModel
import com.sap.cdc.bitsnbytes.ui.viewmodel.WelcomeViewModel

@Suppress("UNCHECKED_CAST")
class CustomViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(WelcomeViewModel::class.java) -> {
                WelcomeViewModel(context) as T
            }

            modelClass.isAssignableFrom(SignInViewModel::class.java) -> {
                SignInViewModel(context) as T
            }

            modelClass.isAssignableFrom(RegisterViewModel::class.java) -> {
                RegisterViewModel(context) as T
            }

            modelClass.isAssignableFrom(AccountViewModel::class.java) -> {
                AccountViewModel(context) as T
            }

            else -> throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}