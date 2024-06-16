package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

class ViewModelCoordinator(context: Context) {

    companion object {

        private val mapInstances = mutableMapOf<ViewModels, Any>()

        private fun <T : Any> getInstance(key: ViewModels, create: () -> T): T {
            if (!mapInstances.containsKey(key)) {
                mapInstances[key] = create()
            }
            return mapInstances[key] as T
        }

        fun home(context: Context): IHomeViewModel {
            return getInstance(ViewModels.HOME) { HomeViewModel(context) }
        }

        fun configuration(context: Context): IConfigurationViewModel {
            return getInstance(ViewModels.CONFIGURATION) { ConfigurationViewModel(context) }
        }

        fun authentication(context: Context): IAuthenticationViewModel {
            return getInstance(ViewModels.AUTHENTICATION) { AuthenticationViewModel(context) }
        }
    }
}

enum class ViewModels {
    HOME, CONFIGURATION, AUTHENTICATION
}

