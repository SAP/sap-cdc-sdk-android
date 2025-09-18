package com.sap.cdc.android.sdk.feature

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
        const val EP_ACCOUNTS_VERIFY_LOGIN = "accounts.verifyLogin"

        const val EP_ACCOUNTS_ID_TOKEN_EXCHANGE = "accounts.identity.token.exchange"
        const val EP_ACCOUNTS_ID_CREATE_TOKEN = "accounts.identifiers.createToken"

        const val EP_ACCOUNT_AUTH_DEVICE_REGISTER = "accounts.devices.register"
        const val EP_ACCOUNT_AUTH_DEVICE_UNREGISTER = "accounts.devices.unregister"
        const val EP_ACCOUNT_AUTH_PUSH_VERIFY = "accounts.auth.push.verify"

        const val EP_SOCIALIZE_GET_IDS = "socialize.getIDs"
        const val EP_SOCIALIZE_LOGIN = "socialize.login"
        const val EP_SOCIALIZE_LOGOUT = "socialize.logout"
        const val EP_SOCIALIZE_REMOVE_CONNECTION = "socialize.removeConnection"

        const val EP_OTP_SEND_CODE = "accounts.otp.sendCode"
        const val EP_OTP_LOGIN = "accounts.otp.login"
        const val EP_OTP_UPDATE = "accounts.otp.update"

        const val EP_OAUTH_AUTHORIZE = "oauth.authorize"
        const val EP_OAUTH_CONNECT = "oauth.connect"
        const val EP_OAUTH_DISCONNECT = "oauth.disconnect"
        const val EP_OAUTH_TOKEN = "oauth.token"

        const val EP_PASSKEYS_DELETE = "accounts.auth.fido.removeCredential"
        const val EP_PASSKEYS_GET_ASSERTION_OPTIONS = "accounts.auth.fido.getAssertionOptions"
        const val EP_PASSKEYS_INIT = "accounts.auth.fido.initRegisterCredentials"
        const val EP_PASSKEYS_REGISTER = "accounts.auth.fido.registerCredentials"
        const val EP_PASSKEYS_VERIFY_ASSERTION = "accounts.auth.fido.verifyAssertion"
        const val EP_PASSKEYS_GET_CREDENTIALS = "accounts.auth.fido.getCredentials"

        const val EP_TFA_INIT = "accounts.tfa.initTFA"
        const val EP_TFA_FINALIZE = "accounts.tfa.finalizeTFA"
        const val EP_TFA_GET_PROVIDERS = "accounts.tfa.getProviders"
        const val EP_TFA_PUSH_OPT_IN = "accounts.tfa.push.optin"
        const val EP_TFA_PUSH_VERIFY = "accounts.tfa.push.verify"
        const val EP_TFA_EMAIL_GET = "accounts.tfa.email.getEmails"
        const val EP_TFA_EMAILS_SEND_CODE = "accounts.tfa.email.sendVerificationCode"
        const val EP_TFA_EMAILS_COMPLETE_VERIFICATION = "accounts.tfa.email.completeVerification"
        const val EP_TFA_PHONE_GET = "accounts.tfa.phone.getRegisteredPhoneNumbers"
        const val EP_TFA_PHONE_SEND_CODE = "accounts.tfa.phone.sendVerificationCode"
        const val EP_TFA_PHONE_COMPLETE_VERIFICATION = "accounts.tfa.phone.completeVerification"
        const val EP_TFA_TOTP_REGISTER = "accounts.tfa.totp.register"
        const val EP_TFA_TOTP_VERIFY = "accounts.tfa.totp.verify"

        const val EP_RISK_SAPTCHA_GET_CHALLENGE = "accounts.risk.saptcha.getChallenge"
        const val EP_RISK_SAPTCHA_VERIFY = "accounts.risk.saptcha.verify"

        // May be redundant cause connection can be done using notifySocialLogin with
        // loginMode = connect.
        const val EP_SOCIALIZE_ADD_CONNECTION = "socialize.addConnection"
    }
}