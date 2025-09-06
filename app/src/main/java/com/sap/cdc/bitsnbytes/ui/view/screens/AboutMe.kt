package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import com.sap.cdc.bitsnbytes.ui.view.composables.ActionOutlineInverseButton
import com.sap.cdc.bitsnbytes.ui.view.composables.IndeterminateLinearIndicator
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages
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
            "${viewModel.accountInfo()?.profile?.firstName ?: ""} ${viewModel.accountInfo()?.profile?.lastName ?: ""}".trim()
        )
    }

    Column(
        modifier = Modifier
            .background(Color(0xFFF5F5F5))
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        // About me section header with gray background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFE0E0E0))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = "About me",
                style = AppTheme.typography.labelLarge,
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Name section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 1.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Name",
                    style = AppTheme.typography.body,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = AppTheme.typography.labelNormal.copy(
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(Color.Black),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    singleLine = true
                )
            }
        }

        // Divider
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 1.dp),
            thickness = 1.dp,
            color = Color.LightGray
        )

        // Email section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 1.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Email",
                    style = AppTheme.typography.body,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = viewModel.accountInfo()?.profile?.email ?: "",
                    style = AppTheme.typography.labelNormal,
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Error message
        if (setError.isNotEmpty()) {
            SimpleErrorMessages(setError)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Save Changes button
        ActionOutlineInverseButton(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = "Save Changes",
            onClick = {
                loading = true
                viewModel.updateAccountInfoWith(
                    name = name,
                    success = {
                        loading = false
                    },
                    onFailed = { error ->
                        loading = false
                        setError = error.errorDetails ?: "An error occurred"
                    }
                )
            }
        )

        // Loading indicator
        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                IndeterminateLinearIndicator(loading)
            }
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
