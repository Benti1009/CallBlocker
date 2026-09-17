package com.example.callblocker

import android.telecom.Call
import android.telecom.CallScreeningService

class CallFilterService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val incomingNumber = callDetails.handle?.schemeSpecificPart ?: return

        val shouldBlock = isNumberBlocked(applicationContext, incomingNumber)

        val response = if (shouldBlock) {
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(true)
                .setSkipNotification(true)
                .build()
        } else {
            CallResponse.Builder()
                .setDisallowCall(false)
                .build()
        }

        respondToCall(callDetails, response)
    }
}
