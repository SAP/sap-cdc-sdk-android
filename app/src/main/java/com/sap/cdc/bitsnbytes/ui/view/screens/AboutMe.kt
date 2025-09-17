package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.sap.cdc.bitsnbytes.ui.view.composables.LoadingStateColumn
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages
import com.sap.cdc.bitsnbytes.ui.view.composables.SuccessBanner
import kotlinx.coroutines.delay


/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 *
 * Account information view.
 * Allow user to update the name of the account using setAccountInfo API.
 */

@Composable
fun AboutMeView(viewModel: IAboutMeViewModel) {
    var loading by remember { mutableStateOf(false) }
    var setError by remember { mutableStateOf("") }
    var showBanner by remember { mutableStateOf(false) }

    val accountInfo by viewModel.flowDelegate?.userAccount?.collectAsState() ?: remember { mutableStateOf(null) }

    var name by remember {
        mutableStateOf(
            "${accountInfo?.profile?.firstName ?: ""} ${accountInfo?.profile?.lastName ?: ""}".trim()
        )
    }

    var nickname by remember {
        mutableStateOf(
            accountInfo?.profile?.nickname ?: ""
        )
    }

    var alias by remember {
        mutableStateOf(
            accountInfo?.customIdentifiers?.alias ?: ""
        )
    }

    if (showBanner) {
        SuccessBanner(
            message = "Account updated successfully",
            onDismiss = { showBanner = false }
        )
    }

    LoadingStateColumn(
        modifier = Modifier
            .background(Color(0xFFF5F5F5))
            .fillMaxWidth()
            .fillMaxHeight(),
        loading = loading
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

        // Alias section
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
                    text = "National ID",
                    style = AppTheme.typography.body,
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                BasicTextField(
                    value = alias,
                    onValueChange = { alias = it },
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
                .padding(top = 1.dp, bottom = 1.dp),
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
                    text = accountInfo?.profile?.email ?: "",
                    style = AppTheme.typography.labelNormal,
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Divider
        HorizontalDivider(
            modifier = Modifier.padding(vertical = 1.dp),
            thickness = 1.dp,
            color = Color.LightGray
        )

        // Nickname section
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
                    text = "Nickname",
                    style = AppTheme.typography.body,
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = nickname,
                    style = AppTheme.typography.labelNormal,
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }


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
                viewModel.setAccountInfo(
                    newName = name,
                    alias = alias,
                    authCallbacks = {
                        onSuccess = {
                            loading = false
                            showBanner = true
                        }
                        onError = { error ->
                            loading = false
                            setError = error.message
                        }
                    }
                )
            }
        )

        AnimatedVisibility(
            visible = showBanner,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            SuccessBanner(
                message = "Account updated successfully",
                onDismiss = { showBanner = false }
            )
        }

        // Auto-hide after 2 seconds
        if (showBanner) {
            LaunchedEffect(Unit) {
                delay(2000)
                showBanner = false
            }
        }
    }
}

@Preview
@Composable
fun AboutMeViewPreview() {
    AppTheme {
        AboutMeView(AboutMeViewModelPreview())
    }
}
