package com.sap.cdc.bitsnbytes.ui.view.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.sap.cdc.bitsnbytes.ui.theme.AppTheme

/**
 * Created by Tal Mirmelshtein on 14/12/2024
 * Copyright: SAP LTD.
 */

@Composable
fun MediumVerticalSpacer() {
    Spacer(modifier = Modifier.height(AppTheme.size.spacerMedium))
}

@Composable
fun MediumHorizontalSpacer() {
    Spacer(modifier = Modifier.width(AppTheme.size.spacerMedium))
}

@Composable
fun SmallVerticalSpacer() {
    Spacer(modifier = Modifier.height(AppTheme.size.spacerSmall))
}

@Composable
fun SmallHorizontalSpacer() {
    Spacer(modifier = Modifier.width(AppTheme.size.spacerSmall))
}

@Composable
fun LargeVerticalSpacer() {
    Spacer(modifier = Modifier.height(AppTheme.size.spacerLarge))
}

@Composable
fun LargeHorizontalSpacer() {
    Spacer(modifier = Modifier.width(AppTheme.size.spacerLarge))
}

@Composable
fun CustomSizeVerticalSpacer(
    size: Dp
) {
    Spacer(modifier = Modifier.height(size))
}

@Composable
fun CustomSizeHorizontalSpacer(
    size: Dp
) {
    Spacer(modifier = Modifier.width(size))
}

@Composable
fun CustomColoredSizeVerticalSpacer(
    size: Dp,
    color: Color
) {
    Spacer(modifier = Modifier
        .height(size)
        .fillMaxWidth()
        .background(color))
}


@Composable
fun CustomColoredSizeHorizontalSpacer(
    size: Dp,
    color: Color
) {
    Spacer(modifier = Modifier
        .width(size)
        .fillMaxWidth()
        .background(color))
}