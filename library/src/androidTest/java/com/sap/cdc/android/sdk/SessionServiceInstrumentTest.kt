package com.sap.cdc.android.sdk

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.sap.cdc.android.sdk.auth.session.Session
import com.sap.cdc.android.sdk.auth.session.SessionService
import com.sap.cdc.android.sdk.core.SiteConfig
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit


@RunWith(AndroidJUnit4::class)
class SessionServiceInstrumentTest {

    private var testApiKey: String = BuildConfig.TEST_API_KEY

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
     */
    @Test
    fun testSessionServiceExpiration() {
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
                expiration = 5000L
            )
        )

        val latch = CountDownLatch(1)

        Handler(Looper.getMainLooper()).postDelayed({
            assert(!sessionService.availableSession())
        }, 10000)

        latch.await(15, TimeUnit.SECONDS)
    }
}