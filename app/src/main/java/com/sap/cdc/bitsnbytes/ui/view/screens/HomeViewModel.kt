package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel

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
