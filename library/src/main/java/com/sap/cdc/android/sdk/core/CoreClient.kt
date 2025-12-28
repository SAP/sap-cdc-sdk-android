package com.sap.cdc.android.sdk.core

import com.sap.cdc.android.sdk.core.network.HttpClientProvider
import com.sap.cdc.android.sdk.core.network.KtorHttpClientProvider
import com.sap.cdc.android.sdk.core.network.NetworkClient


/**
 * Core client for SAP CDC (Customer Data Cloud) SDK operations.
 * 
 * This class serves as the central entry point for SDK network operations, coordinating
 * between site configuration and network communication. It manages the HTTP client lifecycle
 * and provides access to the network layer for making CDC API requests.
 * 
 * The CoreClient integrates:
 * - Site configuration (API key, domain, CNAME settings)
 * - HTTP client provisioning through a configurable provider
 * - Network client for executing API requests
 * 
 * @property siteConfig The SAP CDC site configuration containing API credentials and domain settings
 * @property httpClientProvider Provider for creating and configuring HTTP client instances.
 *                              Defaults to [KtorHttpClientProvider] for production use.
 *                              Can be overridden for testing or custom HTTP client configurations.
 * 
 * @property networkClient The network client instance used for executing HTTP requests to CDC APIs.
 *                         Automatically initialized with the provided HTTP client provider.
 * 
 * @constructor Creates a CoreClient with the specified site configuration and optional HTTP client provider.
 * 
 * @author Tal Mirmelshtein
 * @since 18/06/2024
 * 
 * Copyright: SAP LTD.
 * 
 * @see com.sap.cdc.android.sdk.core.SiteConfig
 * @see com.sap.cdc.android.sdk.core.network.NetworkClient
 * @see com.sap.cdc.android.sdk.core.network.HttpClientProvider
 */
class CoreClient(
    val siteConfig: SiteConfig,
    private val httpClientProvider: HttpClientProvider = KtorHttpClientProvider()
) {
    val networkClient: NetworkClient = NetworkClient(httpClientProvider)
}
