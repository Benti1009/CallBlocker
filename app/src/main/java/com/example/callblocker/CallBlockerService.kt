package com.example.callblocker

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log

class CallBlockerService : CallScreeningService() {

    // Préfixes ARCEP à bloquer
    private val arcepPrefixes = listOf(
        "0162","0163","0270","0271","0377","0378",
        "0424","0425","0568","0569","0948","0949"
    )

    override fun onScreenCall(callDetails: Call.Details) {
        val incomingNumber = callDetails.handle.schemeSpecificPart ?: return
        val context = applicationContext

        // Charger la blacklist persistante
        val blacklist = loadBlacklist(context)

        // Vérifier si le numéro doit être bloqué
        val shouldBlock = arcepPrefixes.any { incomingNumber.startsWith(it) } ||
                blacklist.any { incomingNumber.startsWith(it) }

        if (shouldBlock) {
            respondToCall(
                callDetails,
                CallResponse.Builder()
                    .setDisallowCall(true)
                    .setRejectCall(true)  // optionnel : rejeter l'appel
                    .build()
            )
            Log.d("CallBlockerService", "Appel bloqué : $incomingNumber")
        } else {
            respondToCall(
                callDetails,
                CallResponse.Builder()
                    .setDisallowCall(false)
                    .build()
            )
            Log.d("CallBlockerService", "Appel autorisé : $incomingNumber")
        }
    }
}
