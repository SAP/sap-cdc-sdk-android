package com.sap.cdc.bitsnbytes.ui.view.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color


@Composable
fun LoadingStateColumn(
    loading: Boolean,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier.background(Color.White).imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier!!
                .background(backgroundColor)
                .fillMaxWidth()
                .fillMaxHeight()
        ) {
            content()
        }
        Box(Modifier.fillMaxWidth()) {
            IndeterminateLinearIndicator(loading)
        }
    }
}
