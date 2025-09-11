package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJS
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.view.viewmodel.BaseViewModel


/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 */
interface IViewModelScreenSet {

    fun newWebBridgeJS(): WebBridgeJS? = null
}

// Mocked preview class for ScreenSetViewModel
class ScreenSetViewModelPreview : IViewModelScreenSet

class ScreenSetViewModel(context: Context, val flowDelegate: AuthenticationFlowDelegate) : BaseViewModel(context),
    IViewModelScreenSet {

    override fun newWebBridgeJS(): WebBridgeJS = flowDelegate.getWebBridge()
}

