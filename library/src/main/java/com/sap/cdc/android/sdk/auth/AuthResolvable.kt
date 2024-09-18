package com.sap.cdc.android.sdk.auth

class AuthResolvable {

    companion object {

        const val ERR_ACCOUNT_PENDING_REGISTRATION = 206001
        const val ERR_LOGIN_IDENTIFIER_EXISTS = 403043

        val resolvables = mapOf(
            ERR_ACCOUNT_PENDING_REGISTRATION to "Account pending registration",
            ERR_LOGIN_IDENTIFIER_EXISTS to "Login identifier exists"
        )
    }

}