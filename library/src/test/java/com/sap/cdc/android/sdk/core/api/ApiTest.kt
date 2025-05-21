package com.sap.cdc.android.sdk.core.api

import com.sap.cdc.android.sdk.core.CoreClient
import com.sap.cdc.android.sdk.core.network.NetworkClient
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ApiTest {

    private lateinit var coreClient: CoreClient
    private lateinit var api: Api
    private lateinit var mockNetworkClient: NetworkClient

    @Before
    fun setUp() {
        mockNetworkClient = mock()
        coreClient = CoreClient(mock(), mockNetworkClient)
        api = mock()
    }

    @Test
    fun `test get request success`() = runBlocking {
        val mockEngine = MockEngine { request: HttpRequestData ->
            respond(
                content = """{ "statusCode": 200,
                           "errorCode": 0,
                           "statusReason": "OK",
                           "callId": "8fb3eaf37a424cae8c3e6fe3f53cc177",
                           "time": "2015-03-22T11:42:25.943Z"
                          }""",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type" to listOf("application/json"))
            )
        }
        val mockHttpClient = HttpClient(mockEngine)
        whenever(mockNetworkClient.http()).thenReturn(mockHttpClient)
        whenever(api.networkAvailable()).thenReturn(true)

        val request = CDCRequest(coreClient.siteConfig).apply {
            api = "https://example.com/api"
        }

        // Simulate the actual API call and get the JSON response
        val responseJson = mockHttpClient.get {
            url(request.api)
        }.bodyAsText()
        val response = CDCResponse().fromJSON(responseJson)

        assertEquals(200, response.intField("statusCode") as Int)
        assertEquals("OK", response.stringField("statusReason"))
        assertNotNull(response.stringField("callId"))
    }

    @Test
    fun `test post request success`() = runBlocking {
        val mockEngine = MockEngine { request: HttpRequestData ->
            respond(
                content = """{ "statusCode": 201,
                       "errorCode": 0,
                       "statusReason": "Created",
                       "callId": "8fb3eaf37a424cae8c3e6fe3f53cc177",
                       "time": "2015-03-22T11:42:25.943Z"
                      }""",
                status = HttpStatusCode.Created,
                headers = headersOf("Content-Type" to listOf("application/json"))
            )
        }
        val mockHttpClient = HttpClient(mockEngine)
        whenever(mockNetworkClient.http()).thenReturn(mockHttpClient)
        whenever(api.networkAvailable()).thenReturn(true)

        val request = CDCRequest(coreClient.siteConfig).apply {
            api = "https://example.com/api"
        }

        // Simulate the actual API call and get the JSON response
        val responseJson = mockHttpClient.post {
            url(request.api)
        }.bodyAsText()
        val response = CDCResponse().fromJSON(responseJson)

        assertEquals(201, response.intField("statusCode") as Int)
        assertEquals("Created", response.stringField("statusReason"))
        assertNotNull(response.stringField("callId"))
    }
}