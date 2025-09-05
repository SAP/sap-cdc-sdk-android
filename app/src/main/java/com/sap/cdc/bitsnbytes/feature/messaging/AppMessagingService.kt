package com.sap.cdc.bitsnbytes.feature.messaging

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sap.cdc.android.sdk.events.emitRemoteMessageReceived
import com.sap.cdc.android.sdk.events.emitTokenReceived

class AppMessagingService() : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        emitTokenReceived(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        emitRemoteMessageReceived(message.data)
    }

}