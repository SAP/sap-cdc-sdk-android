package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlinx.serialization.json.Json


/**
 * Created by Tal Mirmelshtein on 18/06/2024
 * Copyright: SAP LTD.
 */
open class ViewModelBase(context: Context) : ViewModel() {

    val json = Json { ignoreUnknownKeys = true }

    // Loading state used for progress widget.
    var loading by mutableStateOf(false)
        internal set
}