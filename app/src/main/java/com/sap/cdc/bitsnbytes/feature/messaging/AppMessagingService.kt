package com.sap.cdc.bitsnbytes.feature.messaging

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sap.cdc.android.sdk.events.emitRemoteMessageReceived
import com.sap.cdc.android.sdk.events.emitTokenReceived

/**
 * Firebase Messaging Service to handle incoming messages and token updates.
 * Integrates with the CIAM event bus to emit relevant events.
 */
class AppMessagingService() : FirebaseMessagingService() {

    /**
     * Called when a new token for the default Firebase project is generated.
     * This is invoked after app install when a token is first generated, and again if the
     * token changes.
     */
    override fun onNewToken(token: String) {
        emitTokenReceived(token)
    }

    /**
     * Called when a message is received.
     *
     * @param message Object representing the message received from Firebase Cloud Messaging.
     */
    override fun onMessageReceived(message: RemoteMessage) {
        emitRemoteMessageReceived(message.data)
    }

}