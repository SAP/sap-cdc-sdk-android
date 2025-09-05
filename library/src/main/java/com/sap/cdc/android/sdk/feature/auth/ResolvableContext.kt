package com.sap.cdc.android.sdk.feature.auth

import com.sap.cdc.android.sdk.feature.auth.model.LinkEntities
import com.sap.cdc.android.sdk.feature.auth.model.TFAEmailEntity
import com.sap.cdc.android.sdk.feature.auth.model.TFAPhoneEntity
import com.sap.cdc.android.sdk.feature.auth.model.TFAProvidersEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

/**
 * Helper class containing required resolvable data for authentication interrupted flows.
 */
@Serializable
class ResolvableContext(
    var regToken: String? = null,
    var otp: ResolvableOtp? = null,
    var linking: ResolvableLinking? = null,
    var registration: ResolvableRegistration? = null,
    var tfa: ResolvableTFA? = null,
) {

    companion object {

        const val ERR_NONE = 0
        const val ERR_ACCOUNT_PENDING_REGISTRATION = 206001
        const val ERR_ACCOUNT_PENDING_VERIFICATION = 206002
        const val ERR_ENTITY_EXIST_CONFLICT = 403043
        const val ERR_PENDING_TWO_FACTOR_REGISTRATION = 403102
        const val ERR_PENDING_TWO_FACTOR_VERIFICATION = 403101
        const val ERR_CAPTCHA_REQUIRED = 401020

        val resolvables = mapOf(
            ERR_ACCOUNT_PENDING_REGISTRATION to "Account pending registration",
            ERR_ENTITY_EXIST_CONFLICT to "Entity exist conflict",
            ERR_PENDING_TWO_FACTOR_REGISTRATION to "Pending two factor registration",
            ERR_PENDING_TWO_FACTOR_VERIFICATION to "Pending two factor verification",
            ERR_CAPTCHA_REQUIRED to "Captcha required"
        )
    }

    /**
     * Encode class to Json helper method.
     */
    fun toJson(): String = Json.encodeToString(this)
}

@Serializable
data class ResolvableLinking(
    var provider: String? = null,
    var authToken: String? = null,
    var conflictingAccounts: LinkEntities? = null
)

@Serializable
data class ResolvableRegistration(
    var missingRequiredFields: List<String>? = null
)

@Serializable
data class ResolvableOtp(
    var vToken: String? = null
)

@Serializable
data class ResolvableTFA(
    var assertion: String? = null,
    var phvToken: String? = null,
    var tfaProviders: TFAProvidersEntity? = null,
    var emails: List<TFAEmailEntity>? = null,
    var phones: List<TFAPhoneEntity>? = null,
    var qrCode: String? = null,
    var sctToken: String? = null,
)
