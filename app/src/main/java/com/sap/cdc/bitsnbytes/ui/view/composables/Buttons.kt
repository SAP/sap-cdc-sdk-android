package com.sap.cdc.bitsnbytes.ui.view.composables

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sap.cdc.bitsnbytes.R
import com.sap.cdc.bitsnbytes.apptheme.AppTheme

/**
 * Created by Tal Mirmelshtein on 14/12/2024
 * Copyright: SAP LTD.
 */

@Composable
fun IconAndTextOutlineButton(
    modifier: Modifier,
    text: String,
    iconResourceId: Int? = null,
    iconImageVector: ImageVector? = null,
    onClick: () -> Unit,
) {
    // Button will use pressed state in addition to ripple effect.
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val containerColor = if (isPressed) Color.Black else Color.Transparent
    val contentColor = if (isPressed) Color.White else Color.Black
    val borderColor = if (isPressed) Color.Black else Color.Black

    OutlinedButton(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, borderColor),
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        onClick = {
            onClick()
        }) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(2.dp))
            iconResourceId?.let {
                Icon(
                    painter = painterResource(id = iconResourceId),
                    contentDescription = "Localized description",
                    modifier = Modifier.size(width = 20.dp, height = 20.dp),
                    tint = Color.Unspecified
                )
            }
            iconImageVector?.let {
                Icon(
                    imageVector = iconImageVector,
                    contentDescription = "Localized description",
                    modifier = Modifier.size(width = 20.dp, height = 20.dp),
                    tint = Color.Unspecified
                )
            }
            MediumHorizontalSpacer()
            Text(text)
        }
    }
}

@Preview
@Composable
fun IconAndTextOutlineButtonPreview() {
    AppTheme {
        IconAndTextOutlineButton(
            modifier = Modifier,
            iconResourceId = R.drawable.ic_faceid,
            text = "Passwordless",
            onClick = {}
        )
    }
}

@Composable
fun ActionOutlineButton(
    modifier: Modifier,
    text: String,
    fillMaxWidth: Boolean? = true,
    onClick: () -> Unit,
) {
    // Button will use pressed state in addition to ripple effect.
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val containerColor = if (isPressed) Color.Black else Color.Transparent
    val contentColor = if (isPressed) Color.White else Color.Black
    val borderColor = if (isPressed) Color.Black else Color.Black

    OutlinedButton(
        modifier = when(fillMaxWidth) {
            true -> {
                modifier.fillMaxWidth()
            }
            false -> modifier
            null -> modifier.fillMaxWidth()
        },
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, borderColor),
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        onClick = {
            onClick()
        }) {
        Text(text)
    }
}

@Preview
@Composable
fun ActionOutlineButtonPreview() {
    AppTheme {
        ActionOutlineButton(
            modifier = Modifier,
            text = "Save changes",
            onClick = {}
        )
    }
}

@Composable
fun ActionOutlineInverseButton(
    modifier: Modifier,
    text: String,
    fillMaxWidth: Boolean? = true,
    onClick: () -> Unit,
) {
    // Button will use pressed state in addition to ripple effect.
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val containerColor = if (isPressed) Color.Transparent else Color.Black
    val contentColor = if (isPressed) Color.Black else Color.White
    val borderColor = if (isPressed) Color.Black else Color.Black

    OutlinedButton(
        modifier = when(fillMaxWidth) {
            true -> {
                modifier.fillMaxWidth()
            }
            false -> modifier
            null -> modifier.fillMaxWidth()
        },
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, borderColor),
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        onClick = {
            onClick()
        }) {
        Text(text)
    }
}

@Preview
@Composable
fun ActionOutlineInverseButtonPreview() {
    AppTheme {
        ActionOutlineInverseButton(
            modifier = Modifier,
            text = "Save changes",
            onClick = {}
        )
    }
}

@Composable
fun ActionTextButton(
    text: String,
    onClick: () -> Unit,
) {
    // Button will use ripple effect only.
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.Black
        )
    ) {
        Text(text, style = AppTheme.typography.titleNormal)
    }
}

@Preview
@Composable
fun ActionTextButtonPreview() {
    AppTheme {
        ActionTextButton("Sign in with SSO") { }
    }
}

@Composable
fun SmallActionTextButton(
    text: String,
    onClick: () -> Unit,
) {
    // Button will use ripple effect only.
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.Black
        )
    ) {
        Text(text, style = AppTheme.typography.titleSmall)
    }
}

@Preview
@Composable
fun SmallActionTextButtonPreview() {
    AppTheme {
        SmallActionTextButton("Sign in with SSO") { }
    }
}