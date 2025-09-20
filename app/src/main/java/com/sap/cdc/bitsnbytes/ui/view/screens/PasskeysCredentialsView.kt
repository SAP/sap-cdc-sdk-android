package com.sap.cdc.bitsnbytes.ui.view.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sap.cdc.android.sdk.feature.provider.passkey.PasskeyCredential
import com.sap.cdc.bitsnbytes.apptheme.AppTheme
import com.sap.cdc.bitsnbytes.ui.view.composables.ActionOutlineButton
import com.sap.cdc.bitsnbytes.ui.view.composables.LargeVerticalSpacer
import com.sap.cdc.bitsnbytes.ui.view.composables.LoadingStateColumn
import com.sap.cdc.bitsnbytes.ui.view.composables.SimpleErrorMessages
import com.sap.cdc.bitsnbytes.ui.view.composables.SmallVerticalSpacer

/**
 * PasskeysCredentialsView displays a list of user's registered passkeys.
 * Allows revoking individual passkeys with proper loading and error handling.
 **/
@Composable
fun PasskeysCredentialsView(viewModel: IPasskeysCredentialsViewModel) {
    val context = LocalContext.current

    var loading by remember { mutableStateOf(false) }
    var error: String? by remember { mutableStateOf(null) }

    // Clear any existing error when viewModel error changes
    if (viewModel.error != null && error != viewModel.error) {
        error = viewModel.error
    }

    LoadingStateColumn(
        loading = loading || viewModel.isLoadingPasskeys,
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        LargeVerticalSpacer()

        // Page title
        Text(
            text = "Passkeys Credentials",
            style = AppTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LargeVerticalSpacer()

        // Check if we have passkeys or if the list is empty
        val credentials = viewModel.passkeyCredentials?.credentials

        if (!viewModel.isLoadingPasskeys && (credentials == null || credentials.isEmpty())) {
            // Empty state - no passkeys registered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column {
                    Text(
                        text = "You have no passkeys registered",
                        style = AppTheme.typography.body,
                        textAlign = TextAlign.Center,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                    LargeVerticalSpacer()
                    Text(
                        text = "Register a passkey to enhance your account security.",
                        style = AppTheme.typography.body,
                        textAlign = TextAlign.Center,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                }
            }
        } else if (credentials != null && credentials.isNotEmpty()) {
            // Display the list of passkeys
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(credentials) { credential ->
                    PasskeyCredentialCard(
                        credential = credential,
                        onRevoke = { keyId ->
                            loading = true
                            error = null
                            viewModel.revokePasskey(
                                keyId = keyId,
                                activity = context as ComponentActivity
                            ) {
                                onSuccess = {
                                    loading = false
                                    // Success handled by ViewModel refreshing the list
                                }

                                onError = { authError ->
                                    loading = false
                                    error = authError.message
                                }
                            }
                        }
                    )
                }
            }
        }

        LargeVerticalSpacer()

        // Error message display
        val displayError = error ?: viewModel.error
        if (!displayError.isNullOrEmpty()) {
            SimpleErrorMessages(
                text = displayError
            )
            SmallVerticalSpacer()
        }

        // Bottom informational banner
        PasskeyInfoBanner()
    }
}

@Composable
private fun PasskeyCredentialCard(
    credential: PasskeyCredential,
    onRevoke: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.LightGray, RoundedCornerShape(0.dp)),
        colors = CardDefaults.cardColors(
            containerColor = Color.White,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Display credential name if available, otherwise show a generic name
                val credentialName = if (credential.id.isNotBlank()) {
                    "Passkey ${credential.id.take(8)}..."
                } else {
                    "Passkey"
                }

                Text(
                    text = credentialName,
                    style = AppTheme.typography.titleSmall
                )

                SmallVerticalSpacer()

                // Show credential ID (truncated for display)
                if (credential.id.isNotBlank()) {
                    Text(
                        text = "ID: ${credential.id.take(16)}...",
                        style = AppTheme.typography.labelSmall,
                        color = Color.Black.copy(alpha = 0.6f)
                    )
                }
            }

            // Revoke button
            ActionOutlineButton(
                text = "Revoke",
                onClick = { onRevoke(credential.id) },
                modifier = Modifier.padding(start = 16.dp),
                fillMaxWidth = false
            )
        }
    }
}

@Composable
private fun PasskeyInfoBanner() {
    Surface(
        color = Color(0xFFF5F5F5),
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF666666),
                modifier = Modifier.padding(end = 8.dp)
            )
            Text(
                text = "Note: Revoking a passkey removes it from this account but does not delete it from your device. You may need to manually remove it from your device's passkey manager.",
                color = Color(0xFF333333),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(name = "Passkey Info Banner")
@Composable
fun PasskeyInfoBannerPreview() {
    AppTheme {
        PasskeyInfoBanner()
    }
}

@Preview(name = "Passkeys List - Populated")
@Composable
fun PasskeysCredentialsViewPreview() {
    AppTheme {
        PasskeysCredentialsView(PasskeysCredentialsViewModelPreview())
    }
}
