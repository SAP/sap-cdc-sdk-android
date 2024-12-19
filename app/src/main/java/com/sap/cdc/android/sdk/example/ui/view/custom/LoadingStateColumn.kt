package com.sap.cdc.android.sdk.example.ui.view.custom

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color


@Composable
fun LoadingStateColumn(
    loading: Boolean,
    modifier: Modifier? = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier!!
                .background(Color.White)
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
