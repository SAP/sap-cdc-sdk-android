package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import com.sap.cdc.android.sdk.feature.screensets.WebBridgeJS


/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 */
interface IViewModelScreenSet {

    fun newWebBridgeJS(): WebBridgeJS? = null
}

// Mocked preview class for ScreenSetViewModel
class ScreenSetViewModelPreview: IViewModelScreenSet

class ScreenSetViewModel(context: Context) : BaseViewModel(context), IViewModelScreenSet {

    override fun newWebBridgeJS(): WebBridgeJS = identityService.getWebBridge()
}

