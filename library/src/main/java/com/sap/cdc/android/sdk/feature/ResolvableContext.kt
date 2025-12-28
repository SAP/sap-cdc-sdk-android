package com.sap.cdc.android.sdk.feature

import kotlinx.serialization.Serializable

/**
 * Resolvable error context for interrupted authentication flows.
 * 
 * Defines error codes and descriptions for authentication flows that require
 * additional user action (registration completion, 2FA, CAPTCHA, etc.).
 * 
 * @author Tal Mirmelshtein
 * @since 10/06/2024
 * 
 * Copyright: SAP LTD.
 */
@Serializable
class ResolvableContext(
) {

    companion object {

        const val ERR_NONE = 0
        const val ERR_ACCOUNT_PENDING_REGISTRATION = 206001
        const val ERR_ACCOUNT_PENDING_VERIFICATION = 206002
        const val ERR_LOGIN_IDENTIFIER_EXISTS = 403043
        const val ERR_ENTITY_EXIST_CONFLICT = 409003
        const val ERR_PENDING_TWO_FACTOR_REGISTRATION = 403102
        const val ERR_PENDING_TWO_FACTOR_VERIFICATION = 403101
        const val ERR_CAPTCHA_REQUIRED = 401020

        val resolvables = mapOf(
            ERR_ACCOUNT_PENDING_REGISTRATION to "Account pending registration",
            ERR_ENTITY_EXIST_CONFLICT to "Entity exist conflict",
            ERR_PENDING_TWO_FACTOR_REGISTRATION to "Pending two factor registration",
            ERR_PENDING_TWO_FACTOR_VERIFICATION to "Pending two factor verification",
            ERR_CAPTCHA_REQUIRED to "Captcha required",
            ERR_LOGIN_IDENTIFIER_EXISTS to "Login identifier exists"
        )
    }
}
