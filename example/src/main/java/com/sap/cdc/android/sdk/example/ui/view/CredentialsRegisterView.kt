package com.sap.cdc.android.sdk.example.ui.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.example.ui.route.NavigationCoordinator
import com.sap.cdc.android.sdk.example.ui.route.ProfileScreenRoute
import com.sap.cdc.android.sdk.example.ui.viewmodel.ACredentialsRegistrationViewModel
import com.sap.cdc.android.sdk.example.ui.viewmodel.CredentialsRegistrationViewModel
import com.sap.cdc.android.sdk.example.ui.viewmodel.CredentialsRegistrationViewModelPreview

/**
 * Created by Tal Mirmelshtein on 10/06/2024
 * Copyright: SAP LTD.
 */

@Composable
fun CredentialsRegistrationView(viewModel: ACredentialsRegistrationViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        val context = LocalContext.current
        // Editable variables.
        var name by remember {
            mutableStateOf("")
        }
        var email by remember {
            mutableStateOf("")
        }
        var password by remember {
            mutableStateOf("")
        }
        var confirmPassword by remember {
            mutableStateOf("")
        }
        // State modifiers.
        var passwordVisible: Boolean by remember { mutableStateOf(false) }
        val isNotMatching = remember {
            derivedStateOf {
                password != confirmPassword
            }
        }

        var registerError by remember { mutableStateOf("") }

        val focusManager = LocalFocusManager.current

        var loading by remember { mutableStateOf(false) }
        // UI elements.
        IndeterminateLinearIndicator(loading)

        Spacer(modifier = Modifier.size(30.dp))
        Text("Create your account", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.size(12.dp))
        Text("Please fill out the listed inputs", fontSize = 16.sp, fontWeight = FontWeight.Light)
        Spacer(modifier = Modifier.size(12.dp))
        Column(
            modifier = Modifier
                .padding(start = 48.dp, end = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Name Input.
            Text(
                "Name: *",
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
            )
            TextField(
                name,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Name Placeholder",
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
                    name = it
                },
                keyboardActions = KeyboardActions {
                    focusManager.moveFocus(FocusDirection.Next)
                },
            )
            // Email input.
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                "Email: *",
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
            )
            TextField(
                email,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Email Placeholder",
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
                    email = it
                },
                keyboardActions = KeyboardActions {
                    focusManager.moveFocus(FocusDirection.Next)
                },
            )
            // Password input.
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                "Password: *",
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
            )
            TextField(
                password,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                ),
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                onValueChange = {
                    password = it
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                keyboardActions = KeyboardActions {
                    focusManager.moveFocus(FocusDirection.Next)
                },
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    // Please provide localized description for accessibility services
                    val description = if (passwordVisible) "Hide password" else "Show password"

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, description)
                    }
                }
            )
            // Confirm password input.
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                "Confirm password: *",
                fontSize = 16.sp,
                fontWeight = FontWeight.Light,
            )
            TextField(
                confirmPassword,
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                ),
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                onValueChange = {
                    confirmPassword = it
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                keyboardActions = KeyboardActions {
                    focusManager.clearFocus()
                },
                trailingIcon = {
                    val image = if (passwordVisible)
                        Icons.Filled.Visibility
                    else Icons.Filled.VisibilityOff

                    // Please provide localized description for accessibility services
                    val description = if (passwordVisible) "Hide password" else "Show password"

                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, description)
                    }
                }
            )
            if (isNotMatching.value) {
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
                        text = "Your passwords do not match!",
                        color = Color.Red,
                    )
                }
            }
            if (registerError.isNotEmpty()) {
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
                        text = registerError,
                        color = Color.Red,
                    )
                }
            }
            Spacer(modifier = Modifier.size(14.dp))
            Text(
                "By clicking on \"register\", you conform that you have read and agree to the privacy policy and terms of use.",
            )
            Spacer(modifier = Modifier.size(24.dp))
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp),
                shape = RoundedCornerShape(6.dp),
                onClick = {
                    registerError = ""
                    loading = true
                    // Credentials registration.
                    viewModel.register(
                        email, password,
                        onLogin = {
                            loading = false
                            NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                        },
                        onFailed = { error ->
                            if (error != null) {
                                // Need to display error information.
                                registerError = error.errorDetails!!
                            }
                        }
                    )
                }) {
                Text("Register")
            }
        }
    }
}


@Preview
@Composable
fun CreateYourAccountPreview() {
    CredentialsRegistrationView(CredentialsRegistrationViewModelPreview(LocalContext.current))
}