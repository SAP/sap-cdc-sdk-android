package com.sap.cdc.android.sdk.auth

class AuthResolvable {

    companion object {

        const val ERR_ACCOUNT_PENDING_REGISTRATION = 206001

        val resolvables = mapOf(
            ERR_ACCOUNT_PENDING_REGISTRATION to "Account pending registration"
        )
    }

}