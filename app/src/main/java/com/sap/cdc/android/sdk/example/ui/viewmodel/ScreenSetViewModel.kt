package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import com.sap.cdc.android.sdk.screensets.WebBridgeJS


/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 */
interface IViewModelScreenSet {

    fun newWebBridgeJS(): WebBridgeJS? = null
}

/**
 * Preview mock view model
 */
class ViewModelScreenSetPreview() : IViewModelScreenSet

class ScreenSetViewModel(context: Context) : BaseViewModel(context), IViewModelScreenSet {

    override fun newWebBridgeJS(): WebBridgeJS = identityService.getWebBridge()
}

