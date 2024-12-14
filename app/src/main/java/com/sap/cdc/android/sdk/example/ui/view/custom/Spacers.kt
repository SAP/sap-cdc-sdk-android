package com.sap.cdc.android.sdk.example.ui.view.custom

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.sap.cdc.android.sdk.example.ui.theme.AppTheme

/**
 * Created by Tal Mirmelshtein on 14/12/2024
 * Copyright: SAP LTD.
 */

@Composable
fun MediumSpacer() {
    Spacer(modifier = Modifier.size(AppTheme.size.spacerMedium))
}

@Composable
fun SmallSpacer() {
    Spacer(modifier = Modifier.size(AppTheme.size.spacerSmall))
}

@Composable
fun LargeSpacer() {
    Spacer(modifier = Modifier.size(AppTheme.size.spacerLarge))
}

@Composable
fun CustomSizeSpacer(
    size: Dp
) {
    Spacer(modifier = Modifier.size(size))
}