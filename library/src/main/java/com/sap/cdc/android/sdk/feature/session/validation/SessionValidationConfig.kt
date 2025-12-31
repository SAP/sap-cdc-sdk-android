package com.sap.cdc.android.sdk.feature.session.validation

import com.sap.ciam.android.sdk.BuildConfig

/**
 * Configuration for periodic session validation.
 * 
 * Controls the behavior of the SessionValidationService, including validation
 * frequency and enable/disable state. Enforces interval constraints in release builds.
 * 
 * @property intervalMinutes Validation interval in minutes (default: 15, range: 15-720)
 * @property enabled Whether session validation is enabled (default: true)
 * 
 * @author Tal Mirmelshtein
 * @since 18/09/2024
 * 
 * Copyright: SAP LTD.
 * 
 * @see SessionValidationService
 */
data class SessionValidationConfig(
    val intervalMinutes: Long = 15L,
    val enabled: Boolean = true
) {
    companion object {
        const val MIN_INTERVAL_MINUTES = 15L
        const val MAX_INTERVAL_MINUTES = 720L // 12 hours
    }

    init {
        // Enforce interval constraints only in release builds.
        if (!BuildConfig.DEBUG) {
            require(intervalMinutes in MIN_INTERVAL_MINUTES..MAX_INTERVAL_MINUTES) {
                "Validation interval must be between $MIN_INTERVAL_MINUTES and $MAX_INTERVAL_MINUTES minutes"
            }
        }
    }
}
