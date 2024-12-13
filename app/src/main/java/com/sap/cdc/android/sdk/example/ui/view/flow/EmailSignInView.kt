package com.sap.cdc.android.sdk.example.ui.view.flow

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.example.ui.route.NavigationCoordinator
import com.sap.cdc.android.sdk.example.ui.route.ProfileScreenRoute
import com.sap.cdc.android.sdk.example.ui.view.custom.IndeterminateLinearIndicator
import com.sap.cdc.android.sdk.example.ui.viewmodel.IViewModelAuthentication
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelAuthenticationPreview


/**
 * Created by Tal Mirmelshtein on 19/06/2024
 * Copyright: SAP LTD.
 *
 * Credentials sign in basic view.
 */

@Composable
fun EmailSignInView(viewModel: IViewModelAuthentication) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        var loading by remember { mutableStateOf(false) }

        var email by remember {
            mutableStateOf("")
        }
        var password by remember {
            mutableStateOf("")
        }

        var signInError by remember { mutableStateOf("") }

        var passwordVisible: Boolean by remember { mutableStateOf(false) }

        val focusManager = LocalFocusManager.current

        // UI elements.
        IndeterminateLinearIndicator(loading)
        Spacer(modifier = Modifier.size(30.dp))
        Text("Sign In with Email", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            "Please enter your email and password",
            fontSize = 16.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(
            modifier = Modifier
                .padding(start = 48.dp, end = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
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
                placeholder = {
                    Text(
                        "Password Placeholder",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                    )
                },
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                onValueChange = {
                    password = it
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
            Spacer(modifier = Modifier.size(12.dp))
            Box(modifier = Modifier.align(Alignment.End)) {
                Surface(onClick = {
                    // Forgot password route.
                }) {
                    Text("Forgot password?", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.size(6.dp))
            if (signInError.isNotEmpty()) {
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
                        text = signInError,
                        color = Color.Red,
                    )
                }
            }
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                onClick = {
                    loading = true
                    viewModel.login(
                        email = email, password = password,
                        onLogin = {
                            signInError = ""
                            loading = false
                            NavigationCoordinator.INSTANCE.navigate(ProfileScreenRoute.MyProfile.route)
                        },
                        onFailedWith = { error ->
                            loading = false
                            signInError = error?.errorDescription!!
                        },
                        onLoginIdentifierExists = {
                            loading = false
                        }
                    )
                }) {
                Text("Login")
            }
        }
    }
}


@Preview
@Composable
fun EmailSignInViewPreview() {
    EmailSignInView(ViewModelAuthenticationPreview())
}