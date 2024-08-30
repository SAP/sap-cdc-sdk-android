package com.sap.cdc.android.sdk.example.ui.view

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sap.cdc.android.sdk.example.ui.viewmodel.IViewModelAuthentication
import com.sap.cdc.android.sdk.example.ui.viewmodel.ViewModelAuthenticationPreview

@Composable
fun ResolvePendingRegistrationWithMissingFields(
    viewModel: IViewModelAuthentication,
    missingFields: List<String>
) {
    var loading by remember { mutableStateOf(false) }

    // UI elements.
    IndeterminateLinearIndicator(loading)

    val focusManager = LocalFocusManager.current

    var registerError by remember { mutableStateOf("") }

    val values = remember {
        mutableStateMapOf(*missingFields.map { it to "" }.toTypedArray())
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.size(80.dp))
        Text("Account Pending Registration", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            "Missing required fields for registration",
            fontSize = 16.sp,
            fontWeight = FontWeight.Light
        )
        Spacer(modifier = Modifier.size(24.dp))

        Column(
            modifier = Modifier
                .padding(start = 48.dp, end = 48.dp)
                .verticalScroll(rememberScrollState())
        ) {
            missingFields.forEach { field ->
                Text(
                    "${field.replaceFirstChar { it.uppercase() }}: *",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light,
                )
                TextField(
                    value = values[field].toString(),
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            "${field.replaceFirstChar { it.uppercase() }} Placeholder",
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
                        values[field] = it
                    },
                    keyboardActions = KeyboardActions {
                        focusManager.moveFocus(FocusDirection.Next)
                    },
                )
                Spacer(modifier = Modifier.size(12.dp))
            }

            Spacer(modifier = Modifier.size(24.dp))
            OutlinedButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp),
                shape = RoundedCornerShape(6.dp),
                onClick = {
                    registerError = ""
                    loading = true
                    // Resolve pending registration.
                    viewModel.resolvePendingRegistrationWithMissingProfileFields(values, onLogin = {
                        Log.d("", "")
                    }, onFailedWith = { error ->
                        if (error != null) {
                            // Need to display error information.
                            registerError = error.errorDetails!!
                        }
                    })
                }) {
                Text("Resolve")
            }
        }
    }
}

@Composable
@Preview
fun ResolvePendingRegistrationWithMissingFieldsPreview() {
    ResolvePendingRegistrationWithMissingFields(
        viewModel = ViewModelAuthenticationPreview(),
        missingFields = listOf("email")
    )
}


