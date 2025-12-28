package com.sap.cdc.android.sdk.core

import android.content.Context
import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.api.Api
import com.sap.cdc.android.sdk.extensions.requiredStringResourceFromKey
import com.sap.cdc.android.sdk.extensions.stringResourceFromKey
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Configuration class for SAP CDC (Customer Data Cloud) SDK site settings.
 * 
 * This class manages the core configuration required to communicate with SAP CDC services,
 * including API credentials, domain settings, and server time synchronization.
 * 
 * The SiteConfig handles:
 * - API key and domain configuration for CDC service endpoints
 * - Optional CNAME configuration for custom domain routing
 * - Server time offset calculation and management for timestamp synchronization
 * 
 * ## Usage
 * ```kotlin
 * // Load from strings.xml resources
 * val config = SiteConfig(context)
 * 
 * // Or provide explicit configuration
 * val config = SiteConfig(
 *     applicationContext = context,
 *     apiKey = "your_api_key",
 *     domain = "us1.gigya.com",
 *     cname = "custom.domain.com" // Optional
 * )
 * 
 * // Use with AuthenticationService
 * val authService = AuthenticationService(config)
 * ```
 * 
 * ## Configuration in strings.xml
 * ```xml
 * <string name="com.sap.cxcdc.apikey">YOUR_API_KEY</string>
 * <string name="com.sap.cxcdc.domain">us1.gigya.com</string>
 * <string name="com.sap.cxcdc.cname">custom.domain.com</string> <!-- Optional -->
 * ```
 * 
 * @property applicationContext The Android application context used for resource access
 * @property apiKey The SAP CDC API key that identifies your CDC site
 * @property domain The CDC data center domain (e.g., "us1.gigya.com", "eu1.gigya.com")
 * @property cname Optional custom domain (CNAME) for CDC API requests. If provided, 
 *                 API requests will be routed through this domain instead of the default domain.
 * @property timeProvider Function that provides the current time in milliseconds. 
 *                        Defaults to System.currentTimeMillis(). Can be overridden for testing.
 * 
 * @constructor Creates a SiteConfig with explicit configuration parameters.
 * 
 * @see AuthenticationService
 * @author Tal Mirmelshtein
 * @since 10/06/2024
 * 
 * Copyright: SAP LTD.
 */
class SiteConfig(
    val applicationContext: Context,
    val apiKey: String,
    val domain: String,
    var cname: String? = null,
    private val timeProvider: () -> Long = { System.currentTimeMillis() }
) {
    // Failure to retrieve apiKey, domain will issue an IllegalArgumentException.
    /**
     * Creates a SiteConfig by loading configuration from application resources.
     * 
     * This constructor retrieves the CDC configuration from the application's string resources:
     * - "com.sap.cxcdc.apikey" (required) - The CDC API key
     * - "com.sap.cxcdc.domain" (required) - The CDC domain
     * - "com.sap.cxcdc.cname" (optional) - Custom CNAME if configured
     * 
     * @param context The Android context used to access application resources
     * @throws IllegalArgumentException if required resources (apiKey or domain) are not found
     */
    constructor(context: Context) : this(
        context,
        context.requiredStringResourceFromKey("com.sap.cxcdc.apikey"),
        context.requiredStringResourceFromKey("com.sap.cxcdc.domain"),
        context.stringResourceFromKey("com.sap.cxcdc.cname"),
    )

    /**
     * Creates a SiteConfig using a ResourceProvider abstraction.
     * 
     * This constructor enables better testability by accepting a ResourceProvider interface
     * instead of directly accessing Android Context resources. This allows for easy mocking
     * and testing without Android dependencies.
     * 
     * @param context The Android application context
     * @param resourceProvider The ResourceProvider implementation to retrieve configuration values
     * @throws IllegalArgumentException if required resources (apiKey or domain) are not found
     */
    constructor(
        context: Context, 
        resourceProvider: ResourceProvider
    ) : this(
        context,
        resourceProvider.getRequiredString("com.sap.cxcdc.apikey"),
        resourceProvider.getRequiredString("com.sap.cxcdc.domain"),
        resourceProvider.getString("com.sap.cxcdc.cname")
    )

    /**
     * The server time offset in seconds.
     * 
     * This offset represents the difference between the server time and local device time,
     * used to synchronize timestamps in API requests with the CDC server.
     */
    private var serverOffset: Long = 0

    companion object {
        /**
         * The date format used for parsing server date headers.
         * 
         * Format: "EEE, dd MMM yyyy HH:mm:ss zzz" (e.g., "Mon, 18 Nov 2024 19:21:00 GMT")
         * This follows the RFC 1123 date format commonly used in HTTP headers.
         */
        const val CDC_SERVER_OFFSET_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz"
    }

    /**
     * Calculates and returns the current server timestamp.
     * 
     * This method computes the synchronized server time by applying the stored server offset
     * to the current local time. The timestamp is returned in Unix epoch seconds format.
     * 
     * The calculation: (current_local_time_ms / 1000) + server_offset_seconds
     * 
     * @return The server timestamp as a string in Unix epoch seconds
     */
    fun getServerTimestamp(): String {
        val timestamp: String =
            ((timeProvider() / 1000) + serverOffset).toString()
        CDCDebuggable.log(Api.LOG_TAG, "serverOffset - get: $timestamp")
        return timestamp
    }

    /**
     * Sets the server time offset by parsing a server date string.
     * 
     * This method calculates the time difference between the server and local device
     * by parsing the provided date string (typically from HTTP response headers) and
     * comparing it with the current local time.
     * 
     * The offset is stored and used by [getServerTimestamp] to provide synchronized
     * server timestamps for API requests.
     * 
     * If the date string is null or cannot be parsed, the current offset is preserved
     * and no exception is thrown.
     * 
     * @param date The server date string in RFC 1123 format (e.g., "Mon, 18 Nov 2024 19:21:00 GMT").
     *             Can be null, in which case no offset update occurs.
     */
    fun setServerOffset(date: String?) {
        if (date == null) return
        val format = SimpleDateFormat(
            CDC_SERVER_OFFSET_FORMAT,
            Locale.ENGLISH
        )
        try {
            val serverDate = format.parse(date) ?: return
            serverOffset = (serverDate.time - timeProvider()) / 1000
            CDCDebuggable.log(Api.LOG_TAG, "serverOffset - set: $serverOffset")
        } catch (e: java.text.ParseException) {
            // Invalid date format - ignore and keep current offset
            CDCDebuggable.log(Api.LOG_TAG, "Invalid date format, keeping current serverOffset: $serverOffset")
        }
    }
}
