package com.sap.cdc.android.sdk.example.ui.view.custom

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.example.ui.theme.AppTheme

/**
 * Created by Tal Mirmelshtein on 14/12/2024
 * Copyright: SAP LTD.
 */

@Composable
fun TitledText(title: String, value: String) {
    Spacer(modifier = Modifier.height(14.dp))
    Box() {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                title, style = AppTheme.typography.body
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value, style = AppTheme.typography.labelNormal
            )
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

@Preview
@Composable
fun TitledTextPreview() {
    TitledText("email", "johndoe@gmail.com")
}

@Composable
fun UpdatableEditBox(title: String, initialValue: String, onValueChanged: (String) -> Unit) {

    var value by remember {
        mutableStateOf("")
    }

    value = initialValue
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
                    value = it
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
    UpdatableEditBox(title = "title", initialValue = "value", onValueChanged = {})
}
