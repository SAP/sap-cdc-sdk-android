package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.sap.cdc.android.sdk.example.cdc.IdentityServiceRepository
import com.sap.cdc.android.sdk.session.SiteConfig

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

interface IConfigurationViewModel {

    fun currentApiKey(): String
    fun currentApiDomain(): String
    fun currentCname(): String
    fun updateWithNewConfig(siteConfig: SiteConfig)
}

class ConfigurationViewModel(context: Context) : ViewModel(), IConfigurationViewModel {

    private val identityService: IdentityServiceRepository =
        IdentityServiceRepository.getInstance(context)

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

}

class ConfigurationViewModelPreviewMock : IConfigurationViewModel {
    override fun currentApiKey(): String = "mockApiKey"

    override fun currentApiDomain(): String = "mockApiDomain"

    override fun currentCname(): String = "mockCname"

    override fun updateWithNewConfig(siteConfig: SiteConfig) {
        //Stub.
    }

}