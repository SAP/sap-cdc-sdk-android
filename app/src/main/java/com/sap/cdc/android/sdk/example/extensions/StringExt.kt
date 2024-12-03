package com.sap.cdc.android.sdk.example.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.sap.cdc.android.sdk.example.R


/**
 * Helper extension method to split first/last name.
 */
fun String.splitFullName(): Pair<String?, String?> {
    val names = this.trim().split(Regex("\\s+"))
    return names.firstOrNull() to names.lastOrNull()
}

/**
 * Helper extension method for composable view to provide the correct painter resource for
 * String social provider name.
 */
@Composable
fun String.providerIcon(): Painter {
    when (this) {
        "line" -> {
            return painterResource(id = R.drawable.ic_line)
        }

        "google" -> {
            return painterResource(id = R.drawable.google_v)
        }

        "facebook" -> {
            return painterResource(id = R.drawable.facebook_v)
        }

        "apple" -> {
            return painterResource(id = R.drawable.apple_v)
        }

        else -> {
            return painterResource(id = R.drawable.ic_logo)
        }
    }
}