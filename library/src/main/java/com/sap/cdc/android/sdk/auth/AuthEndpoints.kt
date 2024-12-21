package com.sap.cdc.android.sdk.auth

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */
class AuthEndpoints {

    companion object {

        // Accounts
        const val EP_ACCOUNTS_INIT_REGISTRATION = "accounts.initRegistration"
        const val EP_ACCOUNTS_REGISTER = "accounts.register"
        const val EP_ACCOUNTS_FINALIZE_REGISTRATION = "accounts.finalizeRegistration"
        const val EP_ACCOUNTS_LOGIN = "accounts.login"
        const val EP_ACCOUNTS_NOTIFY_SOCIAL_LOGIN = "accounts.notifySocialLogin"
        const val EP_ACCOUNTS_LOGOUT = "accounts.logout"
        const val EP_ACCOUNTS_GET_ACCOUNT_INFO = "accounts.getAccountInfo"
        const val EP_ACCOUNTS_SET_ACCOUNT_INFO = "accounts.setAccountInfo"
        const val EP_ACCOUNTS_GET_CONFLICTING_ACCOUNTS = "accounts.getConflictingAccount"
        const val EP_ACCOUNTS_ID_TOKEN_EXCHANGE = "accounts.identity.token.exchange"

        // ACCOUNTS OTP
        const val EP_OTP_SEND_CODE = "accounts.otp.sendCode"
        const val EP_OTP_LOGIN = "accounts.otp.login"
        const val EP_OTP_UPDATE = "accounts.otp.update"

        // ACCOUNTS FIDO/PASSKEYS
        const val EP_PASSKEYS_INIT = "accounts.auth.fido.initRegisterCredentials"
        const val EP_PASSKEYS_GET_CREDENTIALS = "accounts.auth.fido.getCredentials"
        const val EP_PASSKEYS_GET_ASSERTION_OPTIONS = "accounts.auth.fido.getAssertionOptions"
        const val EP_PASSKEYS_REGISTER = "accounts.auth.fido.registerCredentials"
        const val EP_PASSKEYS_VERIFY_ASSERTION = "accounts.auth.fido.verifyAssertion"
        const val EP_PASSKEYS_DELETE = "accounts.auth.fido.removeCredential"

        // Socialize
        const val EP_SOCIALIZE_GET_IDS = "socialize.getIDs"
        const val EP_SOCIALIZE_LOGIN = "socialize.login"
        const val EP_SOCIALIZE_LOGOUT = "socialize.logout"
        const val EP_SOCIALIZE_REMOVE_CONNECTION = "socialize.removeConnection"

        //Oauth
        const val EP_OAUTH_CONNECT = "oauth.connect"
        const val EP_OAUTH_AUTHORIZE = "oauth.authorize"
        const val EP_OAUTH_TOKEN = "oauth.token"
        const val EP_OAUTH_DISCONNECT = "oauth.disconnect"

        // May be redundant cause connection can be done using notifySocialLogin with
        // loginMode = connect.
        const val EP_SOCIALIZE_ADD_CONNECTION = "socialize.addConnection"
    }
}