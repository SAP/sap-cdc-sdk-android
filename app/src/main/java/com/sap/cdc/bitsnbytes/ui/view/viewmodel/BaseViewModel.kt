package com.sap.cdc.bitsnbytes.ui.view.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

/**
 * Created by Tal Mirmelshtein on 18/06/2024
 * Copyright: SAP LTD.
 *
 * Base class for application view models.
 */
open class BaseViewModel(context: Context): ViewModel() {

    /**
     * Available Json interface.
     */
    @OptIn(ExperimentalSerializationApi::class)
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = true
    }

    open fun cancelAllTimers() {
        // Stub.
    }

}