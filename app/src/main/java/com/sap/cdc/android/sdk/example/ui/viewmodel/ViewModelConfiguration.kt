package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.example.ApplicationConfig

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

interface IViewModelConfiguration {

    fun currentApiKey(): String = ""
    fun currentApiDomain(): String = ""
    fun currentCname(): String = ""
    fun updateWithNewConfig(siteConfig: SiteConfig) {}
    fun webViewUse(): Boolean = ApplicationConfig.useWebViews
    fun updateWebViewUse(use: Boolean) {}
}

class ViewModelConfiguration(context: Context) : ViewModelBase(context), IViewModelConfiguration {

    override fun currentApiKey(): String = identityService.getConfig().apiKey

    override fun currentApiDomain(): String = identityService.getConfig().domain

    override fun currentCname(): String {
        var cName = identityService.getConfig().cname
        if (cName == null) {
            cName = ""
        }
        return cName
    }

    override fun updateWithNewConfig(siteConfig: SiteConfig) {
        identityService.reinitializeSessionService(siteConfig)
    }

    override fun updateWebViewUse(use: Boolean) {
        ApplicationConfig.useWebViews(use)
    }

}

class ViewModelConfigurationPreview : IViewModelConfiguration