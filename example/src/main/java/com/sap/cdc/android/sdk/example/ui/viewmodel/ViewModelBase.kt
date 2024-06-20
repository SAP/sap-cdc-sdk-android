package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.sap.cdc.android.sdk.example.cdc.IdentityServiceRepository
import kotlinx.serialization.json.Json


/**
 * Created by Tal Mirmelshtein on 18/06/2024
 * Copyright: SAP LTD.
 */
open class ViewModelBase(context: Context) : ViewModel() {

    val json = Json { ignoreUnknownKeys = true }

    val identityService: IdentityServiceRepository =
        IdentityServiceRepository.getInstance(context)
}