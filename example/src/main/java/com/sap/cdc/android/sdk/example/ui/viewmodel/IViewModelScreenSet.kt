package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import com.sap.cdc.android.sdk.sceensets.WebBridgeJS


/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 */
interface IViewModelScreenSet {

    fun newWebBridgeJS(): WebBridgeJS? = null
}

class ViewModelScreenSet(context: Context) : ViewModelBase(context), IViewModelScreenSet {

    override fun newWebBridgeJS(): WebBridgeJS = identityService.getWebBridge()
}

class ViewModelScreenSetPreview() : IViewModelScreenSet