package com.sap.cdc.android.sdk.example.ui.view.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sap.cdc.android.sdk.auth.session.SessionSecureLevel
import com.sap.cdc.android.sdk.example.ui.theme.AppTheme
import com.sap.cdc.android.sdk.example.ui.view.custom.ActionOutlineButton
import com.sap.cdc.android.sdk.example.ui.view.custom.ActionOutlineInverseButton
import com.sap.cdc.android.sdk.example.ui.view.custom.LargeHorizontalSpacer
import com.sap.cdc.android.sdk.example.ui.view.custom.SmallVerticalSpacer
import com.sap.cdc.android.sdk.example.ui.viewmodel.ILoginOptionsViewModel
import com.sap.cdc.android.sdk.example.ui.viewmodel.LoginOptionsViewModelPreview


/**
 * Created by Tal Mirmelshtein on 17/12/2024
 * Copyright: SAP LTD.
 */

@Composable
fun LoginOptionsView(viewModel: ILoginOptionsViewModel) {

    val biometricLocked = remember { mutableStateOf(false) }

    // UI elements.

    Column(
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Option cards
        OptionCard(
            title = "Passwordless Login",
            status = "Activated",
            actionLabel = "Deactivate",
            onClick = { /* Handle deactivation */ },
            inverse = false
        )
        OptionCard(
            title = "Push 2-Factor Authentication",
            status = "Deactivated",
            actionLabel = "Activate",
            onClick = { /* Handle activation */ },
            inverse = false
        )
        OptionCard(
            title = "Biometrics",
            status = when (viewModel.sessionSecurityLevel()) {
                SessionSecureLevel.STANDARD -> "Deactivated"
                SessionSecureLevel.BIOMETRIC -> "Activated"
            },
            actionLabel = when (viewModel.sessionSecurityLevel()) {
                SessionSecureLevel.STANDARD -> "Activate"
                SessionSecureLevel.BIOMETRIC -> "Deactivate"
            },
            onClick = { /* Handle activation */ },
            inverse = viewModel.sessionSecurityLevel() == SessionSecureLevel.STANDARD
        )

        // Biometrics lock toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 24.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Lock biometrics:")
            LargeHorizontalSpacer()
            Switch(
                checked = biometricLocked.value,
                onCheckedChange = { biometricLocked.value = it }
            )
        }
    }
}

@Preview
@Composable
fun LoginOptionsViewPreview() {
    AppTheme {
        LoginOptionsView(LoginOptionsViewModelPreview())
    }
}

@Composable
private fun OptionCard(
    title: String,
    status: String,
    actionLabel: String,
    inverse: Boolean,
    onClick: () -> Unit
) {
    Card(
        //elevation = 4.dp,
        shape = RoundedCornerShape(0),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, style = AppTheme.typography.titleSmall)
                SmallVerticalSpacer()
                Text(text = status, style = AppTheme.typography.body)
            }
            if (inverse) {
                ActionOutlineInverseButton(
                    text = actionLabel,
                    onClick = onClick,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 120.dp),
                    fillMaxWidth = false
                )
            } else {
                ActionOutlineButton(
                    text = actionLabel,
                    onClick = onClick,
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp, minWidth = 120.dp),
                    fillMaxWidth = false
                )
            }
        }
    }
}