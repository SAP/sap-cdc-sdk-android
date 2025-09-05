package com.sap.cdc.bitsnbytes.ui.view.composables

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.bitsnbytes.apptheme.AppTheme

/**
 * Created by Tal Mirmelshtein on 14/12/2024
 * Copyright: SAP LTD.
 */

@Composable
fun OutlineTitleAndEditTextField(
    modifier: Modifier,
    titleText: String,
    inputText: String,
    placeholderText: String,
    onValueChange: (String) -> Unit,
    focusManager: FocusManager,
) {
    Text(titleText, style = AppTheme.typography.labelNormal)
    SmallVerticalSpacer()
    OutlinedTextField(
        inputText,
        singleLine = false,
        modifier = modifier.fillMaxWidth().moveFocusOnTab(),
        shape = RoundedCornerShape(6.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color.Gray,
            unfocusedBorderColor = Color.Gray
        ),
        placeholder = {
            Text(
                placeholderText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
            )
        },
        textStyle = AppTheme.typography.body,
        onValueChange = {
            onValueChange(it)
        },
        keyboardActions = KeyboardActions(
            onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            }
        ) {
            focusManager.moveFocus(FocusDirection.Next)
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next
        )
    )
}

@Preview
@Composable
fun OutlineTitleAndEditTextFieldPreview() {
    AppTheme {
        OutlineTitleAndEditTextField(
            modifier = Modifier,
            titleText = "Email *",
            inputText = "",
            placeholderText = "Email placeholder",
            onValueChange = {

            },
            focusManager = LocalFocusManager.current
        )
    }
}

@Composable
fun OutlineTitleAndEditPasswordTextField(
    titleText: String,
    inputText: String,
    placeholderText: String,
    passwordVisible: Boolean,
    onValueChange: (String) -> Unit,
    onEyeClick: (Boolean) -> Unit,
    focusManager: FocusManager,
) {
    Text(titleText, style = AppTheme.typography.labelNormal)
    SmallVerticalSpacer()
    OutlinedTextField(
        inputText,
        singleLine = false,
        modifier = Modifier.fillMaxWidth().moveFocusOnTab(),
        shape = RoundedCornerShape(6.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color.Gray,
            unfocusedBorderColor = Color.Gray
        ),
        placeholder = {
            Text(
                placeholderText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
            )
        },
        textStyle = AppTheme.typography.body,
        onValueChange = {
            onValueChange(it)
        },
        keyboardActions = KeyboardActions(
            onNext = {
                focusManager.moveFocus(FocusDirection.Down)
            }
        ) {
            focusManager.moveFocus(FocusDirection.Next)
        },
        keyboardOptions = KeyboardOptions(
            imeAction = ImeAction.Next
        ),
        visualTransformation = if (passwordVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            val image = if (passwordVisible)
                Icons.Filled.Visibility
            else Icons.Filled.VisibilityOff

            // Please provide localized description for accessibility services
            val description = if (passwordVisible) "Hide password" else "Show password"

            IconButton(
                onClick = { onEyeClick(!passwordVisible) }) {
                Icon(imageVector = image, description)
            }
        }
    )
}

@Preview
@Composable
fun OutlineTitleAndEditPasswordTextFieldPreview() {
    AppTheme {
        OutlineTitleAndEditPasswordTextField(
            titleText = "Password *",
            inputText = "",
            placeholderText = "Password placeholder",
            passwordVisible = true,
            onValueChange = {

            },
            onEyeClick = {

            },
            focusManager = LocalFocusManager.current
        )
    }
}