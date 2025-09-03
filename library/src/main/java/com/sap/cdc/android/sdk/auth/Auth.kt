package com.sap.cdc.android.sdk.auth

import com.sap.cdc.android.sdk.core.api.CDCResponse
import com.sap.cdc.android.sdk.core.api.model.CDCError
import kotlinx.serialization.json.JsonObject

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */


/**
 * Authentication result state enum.
 * ERROR - Indicates an unresolvable error in the the API response.
 * SUCCESS - API success or end of flow.
 * INTERRUPTED - Indicates an resolvable error occurred in the the API response. Flow can continue according to the error.
 */
enum class AuthState {
    ERROR, SUCCESS, INTERRUPTED
}

/**
 * Authentication response main class interface.
 */
interface IAuthResponse {

    fun cdcResponse(): CDCResponse
    fun asJsonString(): String?
    fun asJsonObject(): JsonObject?
    fun toDisplayError(): CDCError?
    fun state(): AuthState
    fun resolvable(): ResolvableContext?
}

/**
 * Authentication flow main response class.
 */
class AuthResponse(private val cdcResponse: CDCResponse) : IAuthResponse {

    var resolvableContext: ResolvableContext? = null

    override fun cdcResponse(): CDCResponse = cdcResponse

    override fun asJsonString(): String? = this.cdcResponse.asJson()

    override fun asJsonObject(): JsonObject? = this.cdcResponse.jsonObject

    internal fun isError(): Boolean = cdcResponse.isError()

    override fun toDisplayError(): CDCError = this.cdcResponse.toCDCError()

    fun isResolvable(): Boolean =
        ResolvableContext.resolvables.containsKey(cdcResponse.errorCode()) || cdcResponse.containsKey(
            "vToken"
        )

    /**
     * Defines flow state.
     * Success - marks the end of the flow.
     * Error - indicates an unresolvable error in the the API response.
     * Interrupted - indicates a continuation of the flow is available providing additional data/steps/
     */
    override fun state(): AuthState {
        if (isResolvable()) {
            return AuthState.INTERRUPTED
        }
        if (isError()) {
            return AuthState.ERROR
        }
        return AuthState.SUCCESS
    }

    /**
     * Get reference to resolvable data entity.
     * This data is required to complete interrupted flows.
     */
    override fun resolvable(): ResolvableContext? = resolvableContext
}
