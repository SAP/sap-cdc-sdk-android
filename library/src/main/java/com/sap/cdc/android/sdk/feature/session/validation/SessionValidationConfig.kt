package com.sap.cdc.android.sdk.feature.session.validation

import com.sap.cdc.android.sdk.BuildConfig

/**
 * Configuration data class for session validation service.
 *
 * @param intervalMinutes The interval in minutes between validation checks (default: 15 minutes)
 * @param enabled Whether session validation is enabled (default: true)
 *
 * Created by Tal Mirmelshtein on 18/09/2024
 * Copyright: SAP LTD.
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
