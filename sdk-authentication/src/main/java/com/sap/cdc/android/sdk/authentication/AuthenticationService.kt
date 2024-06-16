package com.sap.cdc.android.sdk.authentication

import com.sap.cdc.android.sdk.session.SessionService

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
class AuthenticationService(val sessionService: SessionService) {

    fun apis(): IAuthApis = AuthApis(sessionService)

    fun resolvers(): IAuthResolvers = AuthResolvers(sessionService)

}