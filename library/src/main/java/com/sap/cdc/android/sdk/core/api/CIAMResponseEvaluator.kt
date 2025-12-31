package com.sap.cdc.android.sdk.core.api

/**
 * Interface for evaluating CIAM API responses for specific conditions or error patterns.
 * 
 * Response evaluators provide a strategy pattern for detecting and handling specific
 * types of errors or response conditions. Implementations can check for particular
 * error codes, messages, or flags that require special handling or recovery logic.
 * 
 * Example use cases:
 * - Detecting invalid session/GMID errors that require re-authentication
 * - Identifying network-related errors that should trigger retry logic
 * - Recognizing validation errors that need user input correction
 * 
 * @see com.sap.cdc.android.sdk.core.api.CIAMResponse
 * @see com.sap.cdc.android.sdk.core.api.InvalidGMIDResponseEvaluator
 */
interface CIAMResponseEvaluator {

    /**
     * Evaluates a CDC response for a specific condition or error pattern.
     * 
     * @param response The CIAMResponse to evaluate
     * @return true if the response matches the evaluated condition, false otherwise
     */
    fun evaluate(response: CIAMResponse): Boolean
}

/**
 * Evaluator that detects invalid GMID errors in CDC responses.
 * 
 * This evaluator identifies responses indicating that the GMID or session is invalid,
 * missing, or expired. Such errors typically require the application to obtain a new
 * GMID or re-authenticate the user session.
 * 
 * The evaluator checks for specific error patterns:
 * - Error details indicating missing cookies or device IDs
 * - Error code 400006 (invalid parameter) combined with "missingKey" flag
 * - Session validation failures
 * 
 * When this evaluator returns true, the SDK typically needs to:
 * 1. Clear the current GMID/session
 * 2. Obtain a new GMID from CDC
 * 3. Retry the original request with the new GMID
 * 
 * @see com.sap.cdc.android.sdk.core.api.CIAMResponseEvaluator
 * @see com.sap.cdc.android.sdk.core.api.CIAMResponse
 */
class InvalidGMIDResponseEvaluator : CIAMResponseEvaluator {

    companion object {
        /**
         * Error detail indicating a missing cookie error.
         * This typically means the GMID cookie is not present or has expired.
         */
        const val DETAILS_CAUSE_MISSING_COOKIE = "missing cookie"
        
        /**
         * Error detail indicating an invalid session due to missing device ID.
         * This occurs when the session cannot be validated without a proper device identifier.
         */
        const val DETAILS_SESSION_IS_INVALID = "Session is invalid (Missing DeviceId)"
        
        /**
         * Error detail indicating missing GCID (Gigya Cookie ID) or UCID (User Cookie ID).
         * These cookies are required for proper session tracking and management.
         */
        const val DETAILS_MISSING_GCID_OR_UCID = "Missing required parameter: gcid or ucid cookie"

        /**
         * Error flag indicating a missing required key.
         * This flag is set when a required parameter is absent from the request.
         */
        const val FLAGS_MISSING_KEY = "missingKey"

        /**
         * Error code 400006 indicates an invalid parameter value.
         * When combined with the MISSING_KEY flag, this typically indicates a GMID issue.
         */
        const val ERROR_INVALID_PARAMETER_VALUE = 400006
    }

    /**
     * Evaluates whether the response indicates an invalid GMID error.
     * 
     * This method checks for various indicators of GMID-related errors:
     * 1. Error details matching known cookie/session error messages
     * 2. Error code 400006 combined with "missingKey" flag
     * 
     * The evaluation only applies to error responses (non-zero error codes).
     * 
     * @param response The CIAMResponse to evaluate
     * @return true if the response indicates an invalid GMID condition, false otherwise
     * 
     * @see CIAMResponse.isError
     * @see CIAMResponse.errorCode
     * @see CIAMResponse.errorDetails
     * @see CIAMResponse.errorFlags
     */
    override fun evaluate(response: CIAMResponse): Boolean {
        if (!response.isError()) return false

        val errorCode = response.errorCode()
        val errorDetails = response.errorDetails()
        val errorFlags = response.errorFlags()

        // Check detail clauses
        if (errorDetails in listOf(
                DETAILS_CAUSE_MISSING_COOKIE,
                DETAILS_SESSION_IS_INVALID,
                DETAILS_MISSING_GCID_OR_UCID
            )
        ) {
            return true
        }

        // Check error code/flag pair
        return errorCode == ERROR_INVALID_PARAMETER_VALUE && errorFlags == FLAGS_MISSING_KEY
    }
}
