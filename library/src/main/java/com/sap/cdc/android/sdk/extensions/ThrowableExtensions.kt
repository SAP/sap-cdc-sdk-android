package com.sap.cdc.android.sdk.extensions

import com.sap.cdc.android.sdk.CIAMDebuggable

/**
 * Extension functions for Throwable to enable conditional stack trace printing
 * based on CIAMDebuggable debug state.
 * 
 * These extensions ensure stack traces are only printed when the SDK is in debug mode,
 * preventing sensitive information leakage in production builds.
 * 
 * ## Problem Being Solved
 * 
 * Using printStackTrace() directly in try-catch blocks can leak sensitive information
 * in production builds. These extensions provide a safe alternative that respects the
 * SDK's debug mode setting.
 * 
 * ## Usage Examples
 * 
 * Simple usage (no tag):
 * ```
 * try {
 *     processData()
 * } catch (e: Exception) {
 *     e.printDebugStackTrace()
 * }
 * ```
 * 
 * With tag for better log organization:
 * ```
 * try {
 *     webBridge.load(webView, url)
 * } catch (e: Exception) {
 *     Log.e(LOG_TAG, "Error loading WebView", e)
 *     e.printDebugStackTrace("WebBridge")
 * }
 * ```
 */

/**
 * Prints the stack trace only if CIAMDebuggable debug logging is enabled.
 * 
 * This is a safe replacement for printStackTrace() that respects the SDK's debug state.
 * Use this when you don't need to log additional context about the exception.
 * 
 * When to Use:
 * - For quick debugging where you just need the stack trace
 * - When the exception type and message are self-explanatory
 * - In simple catch blocks without additional context
 * 
 * Behavior:
 * - Debug mode ON: Prints full stack trace to System.err
 * - Debug mode OFF: Does nothing (safe for production)
 */
fun Throwable.printDebugStackTrace() {
    if (CIAMDebuggable.isDebugEnabled()) {
        printStackTrace()
    }
}

/**
 * Prints the stack trace with a custom tag, only if debug logging is enabled.
 * Also logs the exception details using CIAMDebuggable.log() for consistent logging.
 * 
 * This method provides both structured logging (via CIAMDebuggable.log) and
 * detailed stack trace (via printStackTrace) when debug mode is enabled.
 * 
 * When to Use:
 * - When you want to identify which component threw the exception
 * - For better log filtering and organization
 * - When you need both log message AND stack trace
 * 
 * Behavior:
 * - Debug mode ON: 
 *   1. Logs "CDC_{tag}: Exception: {ExceptionType} - {message}"
 *   2. Prints full stack trace to System.err
 * - Debug mode OFF: 
 *   1. Does nothing (both log and stack trace are suppressed)
 * 
 * @param tag Custom tag to identify where the exception occurred (e.g., "WebBridge", "ScreenSet")
 */
fun Throwable.printDebugStackTrace(tag: String) {
    CIAMDebuggable.log(tag, "Exception: ${this.javaClass.simpleName} - ${this.message}")
    if (CIAMDebuggable.isDebugEnabled()) {
        printStackTrace()
    }
}
