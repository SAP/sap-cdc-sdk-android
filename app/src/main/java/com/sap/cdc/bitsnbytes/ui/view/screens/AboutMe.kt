package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sap.cdc.bitsnbytes.ui.theme.AppTheme
import com.sap.cdc.bitsnbytes.ui.view.composables.ActionOutlineInverseButton
import com.sap.cdc.bitsnbytes.ui.view.composables.IndeterminateLinearIndicator
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages
import com.sap.cdc.bitsnbytes.ui.view.composables.TitledText
import com.sap.cdc.bitsnbytes.ui.view.composables.UpdatableEditBox
import com.sap.cdc.bitsnbytes.ui.viewmodel.AccountViewModelPreview
import com.sap.cdc.bitsnbytes.ui.viewmodel.IAccountViewModel


/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 *
 * Account information view.
 * Allow user to update the name of the account using setAccountInfo API.
 */

@Composable
fun AboutMeView(viewModel: IAccountViewModel) {
    var loading by remember { mutableStateOf(false) }
    var setError by remember { mutableStateOf("") }

    var name by remember {
        mutableStateOf(
            "${viewModel.accountInfo()?.profile?.firstName ?: ""} " +
                    (viewModel.accountInfo()?.profile?.lastName ?: "")
        )
    }

    // UI elements

    Column(
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        // Title box
        Box(
            modifier = Modifier
                .height(height = 36.dp)
                .fillMaxWidth()
                .background(Color.LightGray)
        ) {
            Text(
                "About Me", style = AppTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
            )
        }

        // Name (dynamic edit box) - can be updated.
        UpdatableEditBox(
            title = "Name: ",
            initialValue = name
        ) {
            name = it
        }
        HorizontalDivider(
            modifier = Modifier
                .height(3.dp)
                .background(Color.LightGray)
        )

        // Email (static)
        TitledText(
            title = "Email",
            value = viewModel.accountInfo()?.profile?.email ?: ""
        )

        HorizontalDivider(
            modifier = Modifier
                .height(3.dp)
                .background(Color.LightGray)
        )
        Spacer(modifier = Modifier.height(40.dp))

        // Error message

        if (setError.isNotEmpty()) {
            SimpleErrorMessages(setError)
        }
        
        // Save Changes button
        ActionOutlineInverseButton(
            modifier = Modifier.padding(start = 40.dp, end = 40.dp),
            text = "Save changes",
            onClick = {
                loading = true
                viewModel.updateAccountInfoWith(
                    name = name,
                    success = {
                        loading = false
                    },
                    onFailed = { error ->
                        loading = false
                        setError = error.errorDetails!!
                    }
                )
            }
        )

        // Loading indicator on top of all views.
        Box(Modifier.fillMaxWidth()) {
            IndeterminateLinearIndicator(loading)
        }
    }
}

@Preview
@Composable
fun AboutMeViewPreview() {
    AppTheme {
        AboutMeView(AccountViewModelPreview())
    }
}

