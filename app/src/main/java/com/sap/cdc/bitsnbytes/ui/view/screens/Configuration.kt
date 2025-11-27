@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import com.sap.cdc.bitsnbytes.ui.view.composables.ActionOutlineInverseButton
import com.sap.cdc.bitsnbytes.ui.view.composables.CustomColoredSizeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.LargeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.MediumVerticalSpacer

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Application configuration view.
 * View allows reconfiguration of the CDC SDK & transition from native UI flows to screen-sets.
 */

@Composable
fun ConfigurationView(viewModel : IConfigurationViewModel) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .background(Color.LightGray)
            .fillMaxWidth()
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // UI elements.
        MediumVerticalSpacer()
        Column(
            modifier = Modifier
                .background(Color.White)
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            ConfigurationCardEdit(
                title = "Api Key",
                value = state.apiKey,
                onValueChange = { viewModel.onApiKeyChanged(it) }
            )
            Spacer(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth()
                    .background(color = Color.LightGray)
            )
            ConfigurationCardExposed(
                title = "Domain",
                value = state.domain,
                onValueChange = { viewModel.onDomainChanged(it) }
            )
            Spacer(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth()
                    .background(color = Color.LightGray)
            )
            ConfigurationCardEdit(
                title = "cname".uppercase(),
                value = state.cname,
                onValueChange = { viewModel.onCnameChanged(it) }
            )
            CustomColoredSizeVerticalSpacer(
                color = Color.LightGray,
                size = 2.dp
            )
            Box(modifier = Modifier.height(54.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Use Web View (default: native view)",
                        style = AppTheme.typography.body,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = state.useWebView,
                        onCheckedChange = { viewModel.onWebViewToggled(it) },
                        thumbContent = if (state.useWebView) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }
            CustomColoredSizeVerticalSpacer(
                color = Color.LightGray,
                size = 2.dp
            )
            Box(modifier = Modifier.height(54.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Debug Navigation Logging",
                        style = AppTheme.typography.body,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = state.debugNavigationLogging,
                        onCheckedChange = { viewModel.onDebugNavigationLoggingToggled(it) },
                        thumbContent = if (state.debugNavigationLogging) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        }
                    )
                }
            }
        }
        LargeVerticalSpacer()

        ActionOutlineInverseButton(
            modifier = Modifier.size(width = 260.dp, height = 48.dp),
            text = "Save Changes",
            onClick = { viewModel.onSaveChanges() }
        )
    }
}

@Preview
@Composable
fun ConfigurationViewPreview() {
    AppTheme {
        ConfigurationView(ConfigurationViewModelPreview())
    }
}

@Composable
fun ConfigurationCardEdit(title: String, value: String, onValueChange: (String) -> Unit) {
    MediumVerticalSpacer()
    Column(
    ) {
        Text(
            title, style = AppTheme.typography.labelNormal,
            modifier = Modifier.padding(start = 16.dp),)
        TextField(
            value,
            textStyle = AppTheme.typography.body,
            onValueChange = onValueChange,
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                disabledTextColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent
            )
        )
    }
}

@Composable
fun ConfigurationCardExposed(title: String, value: String, onValueChange: (String) -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }

    Column {
        MediumVerticalSpacer()
        Text(
            title, style = AppTheme.typography.labelNormal,
            modifier = Modifier.padding(start = 16.dp),
        )
        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { isExpanded = it })
        {
            TextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                textStyle = AppTheme.typography.body,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    .fillMaxWidth()
            )

            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
                modifier = Modifier
                    .background(Color.White)
                    .exposedDropdownSize()
            )
            {
                DropdownMenuItem(
                    modifier = Modifier.fillMaxWidth(),
                    text = {
                        Text(text = "us1.gigya.com")
                    },
                    onClick = {
                        onValueChange("us1.gigya.com")
                        isExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(text = "eu1.gigya.com")
                    },
                    onClick = {
                        onValueChange("eu1.gigya.com")
                        isExpanded = false
                    },
                )

            }
        }
    }
}
