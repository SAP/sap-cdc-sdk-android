package com.sap.cdc.android.sdk

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sap.cdc.android.sdk.auth.session.Session
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.SiteConfig
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class SessionServiceInstrumentTest {

    private var testApiKey: String = BuildConfig.TEST_API_KEY

    private lateinit var sessionService: SessionService
    private lateinit var context: Context

    @Before
    fun setup() {
        // This method can be used to set up any preconditions before each test
        // For example, you can initialize shared resources or reset states.
        context = InstrumentationRegistry.getInstrumentation().targetContext
        sessionService = SessionService(
            SiteConfig(
                applicationContext = context,
                apiKey = testApiKey,
                domain = "us1-gigya.com",
                cname = null
            )
        )
    }

    /**
     * Test to check if the SessionService can set and get a session correctly.
     */
    @Test
    fun testSessionServiceGetSet() {
        val appContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
        val sessionService = SessionService(
            SiteConfig(
                applicationContext = appContext,
                apiKey = testApiKey,
                domain = "us1-gigya.com",
                cname = null
            )
        )
        sessionService.setSession(
            Session(
                token = "testToken",
                secret = "testSecret",
                expiration = 0L
            )
        )

        val session = sessionService.getSession()
        assert(session != null)
        assert(session?.token == "testToken")
        assert(session?.secret == "testSecret")
    }

    /**
     * Test to check if the SessionService can set and get a session correctly with expiration.
     * Using CountDownLatch.
     */
    @Test
    fun testSessionServiceExpiration() {
        sessionService.setSession(
            Session(
                token = "testToken",
                secret = "testSecret",
                expiration = 5000L
            )
        )

        val latch = CountDownLatch(1)

        Handler(Looper.getMainLooper()).postDelayed({
            assert(!sessionService.availableSession())
        }, 10000)

        latch.await(15, TimeUnit.SECONDS)
    }

    /**
     * Test to check if the SessionService can set and get a session correctly with expiration.
     * Using sleep.
     */
    @Test
    fun testSessionExpiration() = runBlocking {
        // Create a session with expiration time
        val expirationTimeInSeconds = 5L
        val session = Session(
            token = "mockToken",
            secret = "mockSecret",
            expiration = expirationTimeInSeconds,
        )

        // Set the session
        sessionService.setSession(session)

        // Verify session is available
        assertTrue(sessionService.availableSession())

        // Wait for expiration time to pass
        TimeUnit.SECONDS.sleep(expirationTimeInSeconds + 1)

        // Verify session is cleared automatically
        assertFalse(sessionService.availableSession())
    }

    /**
     * Test multiple sessions secure with different SiteConfig.
     */
    @Test
    fun testReloadWithSiteConfigKeepsSessions() {
        // First SiteConfig and SessionService
        val siteConfig1 = SiteConfig(
            applicationContext = context,
            apiKey = "apiKey1",
            domain = "us1-gigya.com",
            cname = null
        )
        val sessionService1 = SessionService(siteConfig1)
        val session1 = Session(
            token = "token1",
            secret = "secret1",
            expiration = 0L
        )
        sessionService1.setSession(session1)

        // Verify session for siteConfig1
        val retrievedSession1 = sessionService1.getSession()
        assertNotNull(retrievedSession1)
        assertEquals("token1", retrievedSession1?.token)
        assertEquals("secret1", retrievedSession1?.secret)

        // Second SiteConfig and SessionService
        val siteConfig2 = SiteConfig(
            applicationContext = context,
            apiKey = "apiKey2",
            domain = "eu1-gigya.com",
            cname = null
        )
        val sessionService2 = SessionService(siteConfig2)
        val session2 = Session(
            token = "token2",
            secret = "secret2",
            expiration = 0L
        )
        sessionService2.setSession(session2)

        // Verify session for siteConfig2
        val retrievedSession2 = sessionService2.getSession()
        assertNotNull(retrievedSession2)
        assertEquals("token2", retrievedSession2?.token)
        assertEquals("secret2", retrievedSession2?.secret)

        // Reload sessionService1 with siteConfig2
        sessionService1.reloadWithSiteConfig(siteConfig2)

        // Verify session for siteConfig2 after reload
        val reloadedSession = sessionService1.getSession()
        assertNotNull(reloadedSession)
        assertEquals("token2", reloadedSession?.token)
        assertEquals("secret2", reloadedSession?.secret)
    }
}