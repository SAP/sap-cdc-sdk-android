package com.sap.cdc.bitsnbytes.ui.view.screens

import android.content.Context
import com.sap.cdc.bitsnbytes.feature.auth.AuthenticationFlowDelegate
import com.sap.cdc.bitsnbytes.ui.viewmodel.BaseViewModel

interface ITOTPVerificationViewModel {
    
}

// Mock preview class for the TOTPVerificationViewModel
class TOTPVerificationViewModelPreview : ITOTPVerificationViewModel

class TOTPVerificationViewModel(context: Context, val flowDelegate: AuthenticationFlowDelegate) :
    BaseViewModel(context), ITOTPVerificationViewModel {
}