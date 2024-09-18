package com.sap.cdc.android.sdk.auth

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
class AuthEndpoints {

    companion object {

        const val EP_ACCOUNTS_INIT_REGISTRATION = "accounts.initRegistration"
        const val EP_ACCOUNTS_REGISTER = "accounts.register"
        const val EP_ACCOUNTS_FINALIZE_REGISTRATION = "accounts.finalizeRegistration"
        const val EP_ACCOUNTS_LOGIN = "accounts.login"
        const val EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN = "accounts.notifySocialLogin"
        const val EP_ACCOUNTS_LOGOUT = "accounts.logout"
        const val EP_ACCOUNTS_GET_ACCOUNT_INFO = "accounts.getAccountInfo"
        const val EP_ACCOUNTS_SET_ACCOUNT_INFO = "accounts.setAccountInfo"
        const val EP_ACCOUNTS_GET_CONFLICTING_ACCOUNTS = "accounts.getConflictingAccount"

        const val EP_SOCIALIZE_GET_IDS = "socialize.getIDs"
        const val EP_SOCIALIZE_LOGIN = "socialize.login"
        const val EP_SOCIALIZE_LOGOUT = "socialize.logout"
        const val EP_SOCIALIZE_REMOVE_CONNECTION = "socialize.removeConnection"

        // May be redundant cause connection can be done using notifySocialLogin with
        // loginMode = connect.
        const val EP_SOCIALIZE_ADD_CONNECTION = "socialize.addConnection"
    }
}