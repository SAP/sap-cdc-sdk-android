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
import androidx.compose.runtime.MutableState
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
    val apiKey = remember { mutableStateOf(viewModel.currentApiKey()) }
    val domain = remember { mutableStateOf(viewModel.currentApiDomain()) }
    val cname = remember { mutableStateOf(viewModel.currentCname()) }
    var checked by remember { mutableStateOf(viewModel.webViewUse()) }

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
            ConfigurationCardEdit(title = "Api Key", valueState = apiKey)
            Spacer(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth()
                    .background(color = Color.LightGray)
            )
            ConfigurationCardExposed(title = "Domain", valueState = domain)
            Spacer(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth()
                    .background(color = Color.LightGray)
            )
            ConfigurationCardEdit(
                title = "cname".uppercase(), valueState = cname
            )
            CustomColoredSizeVerticalSpacer(
                color = Color.LightGray,
                size = 2.dp
            )
            Box(modifier = Modifier.height(54.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text("Use Web View (default: native view)", style = AppTheme.typography.body)
                    Switch(
                        checked = viewModel.webViewUse(),
                        onCheckedChange = {
                            checked = it
                            viewModel.updateWebViewUse(checked)
                        },
                        thumbContent = if (checked) {
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
            onClick = {
                viewModel.updateWithNewConfig(
                    SiteConfig(
                        context,
                        apiKey = apiKey.value,
                        domain = domain.value,
                        cname = cname.value
                    )
                )
            }
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
fun ConfigurationCardEdit(title: String, valueState: MutableState<String>) {
    MediumVerticalSpacer()
    Column(
    ) {
        Text(
            title, style = AppTheme.typography.labelNormal,
            modifier = Modifier.padding(start = 16.dp),)
        TextField(
            valueState.value,
            textStyle = AppTheme.typography.body,
            onValueChange = {
                valueState.value = it
            },
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
fun ConfigurationCardExposed(title: String, valueState: MutableState<String>) {
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
                value = valueState.value,
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
                        valueState.value = "us1.gigya.com"
                        isExpanded = false
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(text = "eu1.gigya.com")
                    },
                    onClick = {
                        valueState.value = "eu1.gigya.com"
                        isExpanded = false
                    },
                )

            }
        }
    }
}
