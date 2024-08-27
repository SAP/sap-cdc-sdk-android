package com.sap.cdc.android.sdk.example.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.example.ui.viewmodel.IViewModelProfile
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelProfilePreview


/**
 * Created by Tal Mirmelshtein on 20/06/2024
 * Copyright: SAP LTD.
 */

@Composable
fun ViewAboutMe(viewModel: IViewModelProfile) {
    var loading by remember { mutableStateOf(false) }

    var setError by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        Box(
            modifier = Modifier
                .height(height = 36.dp)
                .fillMaxWidth()
                .background(Color.LightGray)
        ) {
            Text(
                "About Me", fontSize = 14.sp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 10.dp)
            )
        }
        UpdatableEditBox(title = "Name: ", value = "") {

        }
        HorizontalDivider(
            modifier = Modifier
                .height(3.dp)
                .background(Color.LightGray)
        )
        UpdatableEditBox(title = "Email", value = "") {

        }
        HorizontalDivider(
            modifier = Modifier
                .height(3.dp)
                .background(Color.LightGray)
        )
        Spacer(modifier = Modifier.height(40.dp))
        if (setError.isNotEmpty()) {
            Spacer(modifier = Modifier.size(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Cancel,
                    contentDescription = "",
                    tint = Color.Red
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = setError,
                    color = Color.Red,
                )
            }
        }
        OutlinedButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(6.dp),
            onClick = {
                loading = true
                viewModel.setAccountInfo(
                    success = {
                        loading = false
                    },
                    onFailed = { error ->
                        loading = false
                        setError = error.errorDetails!!
                    }
                )
            }) {
            Text("Save changes")
        }
    }
}

@Preview
@Composable
fun ViewAboutMePreview() {
    ViewAboutMe(viewModel = ViewModelProfilePreview())
}

@Composable
fun UpdatableEditBox(title: String, value: String, onValueChanged: (String) -> Unit) {
    Spacer(modifier = Modifier.height(14.dp))
    Box() {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
            )
            TextField(
                value,
                modifier = Modifier
                    .fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                placeholder = {
                    Text(
                        "Update name",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                    )
                },
                textStyle = TextStyle(
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                ),
                onValueChange = {
                    onValueChanged(it)
                },
                keyboardActions = KeyboardActions {

                },
            )
        }
    }
    Spacer(modifier = Modifier.height(14.dp))
}

@Preview
@Composable
fun UpdatableEditBoxPreview() {
    UpdatableEditBox(title = "title", value = "value", onValueChanged = {})
}