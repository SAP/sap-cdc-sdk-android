package com.sap.cdc.android.sdk.example.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
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
                modifier = Modifier.align(Alignment.CenterStart)
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