package com.example.callblocker

import android.telecom.CallScreeningService
import android.telecom.Call.Details
import android.telecom.CallScreeningService.CallResponse
import android.content.Context

class CallFilterService : CallScreeningService() {

    override fun onScreenCall(callDetails: Details) {
        val incomingNumber = callDetails.handle.schemeSpecificPart ?: return
        val context = this

        val blacklist = loadBlacklist(context)

        val shouldBlock = blacklist.any { prefix ->
            incomingNumber.startsWith(prefix)
        }

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
