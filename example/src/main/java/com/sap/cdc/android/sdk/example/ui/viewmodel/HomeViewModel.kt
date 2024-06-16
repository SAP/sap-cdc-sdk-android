package com.sap.cdc.android.sdk.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.sap.cdc.android.sdk.example.cdc.IdentityServiceRepository

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

interface IHomeViewModel {

    fun validSession(): Boolean
}

class HomeViewModel(context: Context) : ViewModel(), IHomeViewModel {

    private val identityService: IdentityServiceRepository =
        IdentityServiceRepository.getInstance(context)

    /**
     * Check Identity session state.
     */
    override
    fun validSession(): Boolean = identityService.getSession() != null
}


class HomeViewModelMock() : IHomeViewModel {

    override fun validSession(): Boolean {
        return true
    }

}
