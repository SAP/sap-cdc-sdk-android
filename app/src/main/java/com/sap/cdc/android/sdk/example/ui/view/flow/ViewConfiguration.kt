@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package com.sap.cdc.android.sdk.example.ui.view.flow

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.core.SiteConfig
import com.sap.cdc.android.sdk.example.ui.viewmodel.IViewModelConfiguration
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelConfigurationPreview

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 *
 * Application configuration view.
 * View allows reconfiguration of the CDC SDK & transition from native UI flows to screen-sets.
 */

@Composable
fun ViewConfiguration(viewModel: IViewModelConfiguration) {
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
        Spacer(modifier = Modifier.height(14.dp))
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
            Spacer(
                modifier = Modifier
                    .height(2.dp)
                    .fillMaxWidth()
                    .background(color = Color.LightGray)
            )
            Box(modifier = Modifier.height(54.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Text("Use Web View (default: native view)")
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
        Spacer(modifier = Modifier.height(24.dp))
        ElevatedButton(
            modifier = Modifier.size(width = 260.dp, height = 48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(Color.Black),
            onClick = {
                viewModel.updateWithNewConfig(
                    SiteConfig(
                        context,
                        apiKey = apiKey.value,
                        domain = domain.value,
                        cname = cname.value
                    )
                )
            }) {
            Text("Save Changes")
        }
    }
}

@Preview
@Composable
fun ViewConfigurationPreview() {
    ViewConfiguration(ViewModelConfigurationPreview())
}

@Composable
fun ConfigurationCardEdit(title: String, valueState: MutableState<String>) {
    Column(
    ) {
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(10.dp)
        )
        TextField(
            valueState.value,
            textStyle = TextStyle(
                color = Color.Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            ),
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
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(10.dp),
        )
        ExposedDropdownMenuBox(
            expanded = isExpanded,
            onExpandedChange = { isExpanded = it })
        {
            TextField(
                value = valueState.value,
                onValueChange = {},
                readOnly = true,
                textStyle = TextStyle(
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                ),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier
                    .menuAnchor()
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
