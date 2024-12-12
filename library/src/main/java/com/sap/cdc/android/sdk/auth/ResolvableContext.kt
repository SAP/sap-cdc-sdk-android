package com.sap.cdc.android.sdk.auth

import com.sap.cdc.android.sdk.auth.model.ConflictingAccountsEntity
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
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
    var provider: String? = null,
    var authToken: String? = null,
    var conflictingAccounts: ConflictingAccountsEntity? = null,
    var missingRequiredFields: List<String>? = null,
    var vToken: String? = null,
) {

    companion object {

        const val ERR_NONE = 0
        const val ERR_ACCOUNT_PENDING_REGISTRATION = 206001
        const val ERR_ENTITY_EXIST_CONFLICT = 403043

        val resolvables = mapOf(
            ERR_ACCOUNT_PENDING_REGISTRATION to "Account pending registration",
            ERR_ENTITY_EXIST_CONFLICT to "Entity exist conflict"
        )
    }

    /**
     * Encode class to Json helper method.
     */
    fun toJson(): String = Json.encodeToString(this)
}
