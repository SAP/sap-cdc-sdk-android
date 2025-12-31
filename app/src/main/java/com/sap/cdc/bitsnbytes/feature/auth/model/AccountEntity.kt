package com.sap.cdc.bitsnbytes.feature.auth.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

/**
 * Custom instance of the CIAM SDK account schema.
 * This class can be extended according to the site schema definition and will be serialized
 * accordingly.
 */
@Serializable
data class AccountEntity(
    @SerialName("UID") val uid: String,
    val profile: ProfileEntity,
    var customIdentifiers: CustomIdentifiers? = null,
)

/**
 * Custom instance of the CIAM SDK profile schema.
 */
@Serializable
data class ProfileEntity(
    val email: String,
    val firstName: String? = null,
    val lastName: String? = null,
    val nickname: String? = null,
)

/**
 * Custom instance of the CIAM SDK custom identifiers schema.
 */
@Serializable
data class CustomIdentifiers(
    val alias: String? = null,
)