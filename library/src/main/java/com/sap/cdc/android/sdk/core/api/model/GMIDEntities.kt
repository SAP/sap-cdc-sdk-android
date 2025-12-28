package com.sap.cdc.android.sdk.core.api.model

import kotlinx.serialization.Serializable

/**
 * GMID (Global Member ID) entity for SAP CDC session management.
 * 
 * Represents the unique identifier and refresh timestamp for a CDC session.
 * GMID is used to track user sessions across multiple devices and applications.
 * 
 * @property gmid The global member identifier string
 * @property refreshTime Timestamp when the GMID should be refreshed (in milliseconds)
 * 
 * @author Tal Mirmelshtein
 * @since 10/06/2024
 * 
 * Copyright: SAP LTD.
 */
@Serializable
data class GMIDEntity(
    val gmid: String?,
    val refreshTime: Long? = 0L
)
