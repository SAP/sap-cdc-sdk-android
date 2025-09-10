package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

interface IViewModelHome {

    fun validSession(): Boolean = false
}

class HomeViewModel(context: Context, val flowDelegate: AuthenticationFlowDelegate) :
    BaseViewModel(context), IViewModelHome {

    /**
     * Check Identity session state.
     */
    override
    fun validSession(): Boolean = flowDelegate.isAuthenticated.value
}
