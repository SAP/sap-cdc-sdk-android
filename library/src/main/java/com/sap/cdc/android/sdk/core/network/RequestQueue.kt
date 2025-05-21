package com.sap.cdc.android.sdk.core.network

import com.sap.cdc.android.sdk.CDCDebuggable
import com.sap.cdc.android.sdk.core.api.CDCRequest
import io.ktor.client.HttpClient
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.CompletableDeferred

data class RequestWrapper(
    var requestData: CDCRequest,
    val execute: suspend (CDCRequest) -> HttpResponse
)

object RequestQueue {

    private const val LOG_TAG = "RequestQueue"

    private lateinit var client: HttpClient
    private val queue = mutableListOf<RequestWrapper>()

    var blockQueue = CompletableDeferred<Unit>().apply { complete(Unit) }

    fun initialize(httpClient: HttpClient) {
        client = httpClient
    }

    suspend fun addRequest(requestData: CDCRequest, execute: suspend (CDCRequest) -> HttpResponse) {
        queue.add(RequestWrapper(requestData, execute))
        processQueue()
    }

    fun updateAndResignRequests(signRequest: (CDCRequest) -> Unit) {
        if (queue.isEmpty()) {
            CDCDebuggable.log(LOG_TAG, "Queue is empty, nothing to update.")
            return
        }
        queue.forEach { wrapper ->
            signRequest(wrapper.requestData) // Re-sign the request
        }
    }

    private suspend fun processQueue() {
        while (queue.isNotEmpty()) {
            val wrapper = queue.first()
            try {
                blockQueue.await()
                wrapper.execute(wrapper.requestData)
                queue.removeAt(0)
            } catch (e: Exception) {
                break
            }
        }
    }

    suspend fun injectRequest(
        requestData: CDCRequest,
        execute: suspend (CDCRequest) -> HttpResponse
    ) {
        val wasBlocked = !blockQueue.isCompleted // Check if the queue was blocked
        if (wasBlocked) {
            blockQueue.complete(Unit) // Temporarily unblock the queue
        }

        try {
            // Add the injected request to the front of the queue
            queue.add(0, RequestWrapper(requestData, execute))
            processQueue() // Process the queue
        } finally {
            if (wasBlocked) {
                blockQueue = CompletableDeferred() // Restore the blocking mechanism
            }
        }
    }
}