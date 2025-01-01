package com.sap.cdc.bitsnbytes.ui.viewmodel

import android.content.Context
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.bitsnbytes.ApplicationConfig

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

interface IConfigurationViewModel {

    fun currentApiKey(): String = ""
    fun currentApiDomain(): String = ""
    fun currentCname(): String = ""
    fun updateWithNewConfig(siteConfig: SiteConfig) {}
    fun webViewUse(): Boolean = ApplicationConfig.useWebViews
    fun updateWebViewUse(use: Boolean) {}
}

// Mocked preview class for ConfigurationViewModel
class ConfigurationViewModelPreview : IConfigurationViewModel

class ConfigurationViewModel(context: Context) : BaseViewModel(context), IConfigurationViewModel {

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
