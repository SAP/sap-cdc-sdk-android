package com.sap.cdc.bitsnbytes.ui.view.custom

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sap.cdc.bitsnbytes.ui.theme.AppTheme


@Composable
fun SimpleErrorMessages(
    text: String,
) {
    SmallVerticalSpacer()
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
            style = AppTheme.typography.labelLarge,
            text = text,
            color = Color.Red,
        )
    }
}

@Preview
@Composable
fun SimpleErrorMessagePreview() {
    AppTheme {
        SimpleErrorMessages(
            text = "Some error message"
        )
    }
}

@Composable
fun PasswordNotMatchingError() {
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
            style = AppTheme.typography.labelLarge,
            text = "Your passwords do not match!",
            color = Color.Red,
        )
    }
}

@Preview
@Composable
fun PasswordNotMatchingErrorPreview() {
    AppTheme {
        PasswordNotMatchingError()
    }

}