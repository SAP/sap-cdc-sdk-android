package com.sap.cdc.bitsnbytes.cdc

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.sap.cdc.android.sdk.CDCMessageEventBus
import com.sap.cdc.android.sdk.MessageEvent


class AppMessagingService() : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        CDCMessageEventBus.emitMessageEvent(MessageEvent.EventWithToken(token))

    }

    override fun onMessageReceived(message: RemoteMessage) {
        CDCMessageEventBus.emitMessageEvent(MessageEvent.EventWithRemoteMessageData(message.data))
    }

}