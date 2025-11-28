package com.sap.cdc.android.sdk.feature.session

import com.sap.cdc.android.sdk.core.SiteConfig

interface IAuthSession {

    fun availableSession(): Boolean

    fun getSession(): Session?

    fun clearSession()

    fun invalidateSession()

    fun setSession(session: Session)

    fun resetWithConfig(siteConfig: SiteConfig)

    fun sessionSecurityLevel(): SessionSecureLevel

}

internal class AuthSession(private val sessionService: SessionService) : IAuthSession {

    override fun availableSession(): Boolean = sessionService.availableSession()

    override fun getSession(): Session? = sessionService.getSession()

    override fun clearSession() = sessionService.clearSession()

    override fun invalidateSession() = sessionService.invalidateSession()

    override fun setSession(session: Session) = sessionService.setSession(session)

    override fun resetWithConfig(siteConfig: SiteConfig) {
        sessionService.reloadWithSiteConfig(siteConfig)
    }

    override fun sessionSecurityLevel(): SessionSecureLevel = sessionService.sessionSecureLevel()
}