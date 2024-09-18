package com.sap.cdc.android.sdk.example.extensions

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.sap.cdc.android.sdk.example.R


/**
 * Helper extension method to split first/last name.
 */
fun String.splitFullName(name: String): Pair<String, String> {
    val index = name.lastIndexOf("")
    val firstName = index.let { it1 -> name.substring(0, it1) }
    val lastName = index.plus(1).let { it1 -> name.substring(it1) }
    return Pair(firstName, lastName)
}

/**
 * Helper extension method to parse String value for required missing fields list.
 */
fun String.parseRequiredMissingFieldsForRegistration(): List<String> {
    val fields = this.substringAfterLast(": ")
    return fields.split(", ")
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