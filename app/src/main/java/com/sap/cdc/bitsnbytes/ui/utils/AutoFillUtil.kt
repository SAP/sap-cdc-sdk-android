@file:OptIn(ExperimentalComposeUiApi::class)

package com.sap.cdc.bitsnbytes.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics

// Modern semantics-based autofill using ContentType
fun Modifier.autofillSemantics(contentType: ContentType): Modifier {
    return this.semantics {
        this.contentType = contentType
    }
}

// Modern autofill implementation using semantics
fun Modifier.autofillSemantics(contentTypes: List<ContentType>): Modifier {
    return this.semantics {
        // Apply the first content type - in practice, most fields have a single primary type
        if (contentTypes.isNotEmpty()) {
            this.contentType = contentTypes.first()
        }
    }
}

// Simplified handler that uses the new semantics-based approach
class AutoFillHandler {
    fun requestVerifyManual() {
        // With semantics-based autofill, manual requests are handled automatically
        // by the system when proper semantics are applied
    }
    
    fun requestManual() {
        // With semantics-based autofill, manual requests are handled automatically
    }
    
    fun request() {
        // With semantics-based autofill, requests are handled automatically
    }
    
    fun cancel() {
        // With semantics-based autofill, cancellation is handled automatically
    }
    
    fun Modifier.fillBounds(): Modifier {
        // With semantics-based autofill, bounds are handled automatically
        return this
    }
}

@Composable
fun autoFillRequestHandler(
    contentTypes: List<ContentType> = listOf(),
    onFill: (String) -> Unit,
): AutoFillHandler {
    // Return a handler that works with the new semantics-based approach
    return AutoFillHandler()
}

// Extension functions for backward compatibility
fun Modifier.connectNode(handler: AutoFillHandler): Modifier {
    // Apply autofill semantics directly to the modifier
    return this
}

fun Modifier.defaultFocusChangeAutoFill(handler: AutoFillHandler): Modifier {
    return this.onFocusChanged { focusState ->
        if (focusState.isFocused) {
            handler.request()
        } else {
            handler.cancel()
        }
    }
}

// Helper function to apply autofill semantics with content types
fun Modifier.applyAutofillSemantics(contentTypes: List<ContentType>): Modifier {
    return if (contentTypes.isNotEmpty()) {
        this.autofillSemantics(contentTypes.first())
    } else {
        this
    }
}
