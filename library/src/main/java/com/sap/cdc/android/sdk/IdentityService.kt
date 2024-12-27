package com.sap.cdc.android.sdk

import com.sap.cdc.android.sdk.auth.AuthenticationService
import com.sap.cdc.android.sdk.auth.IAuthApis
import com.sap.cdc.android.sdk.auth.IAuthApisGet
import com.sap.cdc.android.sdk.auth.IAuthApisSet
import com.sap.cdc.android.sdk.auth.IAuthResolvers
import com.sap.cdc.android.sdk.auth.IAuthSession
import com.sap.cdc.android.sdk.core.SiteConfig

interface IdentityServiceDelegate {
    fun initialize(siteConfig: SiteConfig)
    fun getSiteConfig(): SiteConfig
    fun getAuthenticationService(): AuthenticationService
    fun authenticate(): IAuthApis
    fun session(): IAuthSession
    fun get(): IAuthApisGet
    fun set(): IAuthApisSet
    fun resolve(): IAuthResolvers
}

object IdentityService : IdentityServiceDelegate {

    private lateinit var siteConfig: SiteConfig
    private lateinit var authenticationService: AuthenticationService

    override fun initialize(siteConfig: SiteConfig) {
        this.siteConfig = siteConfig
        if (!::authenticationService.isInitialized) {
            authenticationService = AuthenticationService(siteConfig)
        } else {
            authenticationService.session().resetWithConfig(siteConfig)
        }
    }

    override fun getSiteConfig(): SiteConfig = siteConfig

    override fun getAuthenticationService(): AuthenticationService = authenticationService

    override fun authenticate(): IAuthApis = authenticationService.authenticate()

    override fun session(): IAuthSession = authenticationService.session()

    override fun get(): IAuthApisGet = authenticationService.get()

    override fun set(): IAuthApisSet = authenticationService.set()

    override fun resolve(): IAuthResolvers = authenticationService.resolve()

}