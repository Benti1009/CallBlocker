package com.example.callblocker

import android.content.Context
import androidx.core.content.edit

// Préfixes de numéros de démarchage téléphonique définis par l'ARCEP
val ARCEP_PREFIXES = listOf(
    "0162", "0163", "0270", "0271", "0377", "0378",
    "0424", "0425", "0568", "0569", "0948", "0949"
)

// Normalise un numéro : garde uniquement les chiffres (et le + initial),
// et convertit le format international français (+33 / 0033) en format local 0X...
fun normalizePhoneNumber(number: String): String {
    val digits = number.filter { it.isDigit() || it == '+' }
    return when {
        digits.startsWith("+33") -> "0" + digits.removePrefix("+33")
        digits.startsWith("0033") -> "0" + digits.removePrefix("0033")
        else -> digits
    }
}

fun loadBlacklist(context: Context): MutableList<String> {
    val prefs = context.getSharedPreferences("callblocker_prefs", Context.MODE_PRIVATE)
    val set = prefs.getStringSet("blacklist", emptySet()) ?: emptySet()
    return set.toMutableList()
}

fun saveBlacklist(context: Context, blacklist: List<String>) {
    val prefs = context.getSharedPreferences("callblocker_prefs", Context.MODE_PRIVATE)
    prefs.edit {
        putStringSet("blacklist", blacklist.toSet())
    }
}

// Ajoute un numéro (normalisé) à la blacklist et renvoie la valeur normalisée
// stockée (ou null si le numéro saisi est invalide/vide), pour que l'UI reste
// synchronisée avec ce qui est réellement persisté.
fun addToBlacklist(context: Context, number: String): String? {
    val normalized = normalizePhoneNumber(number)
    if (normalized.isBlank()) return null
    val list = loadBlacklist(context)
    if (!list.contains(normalized)) {
        list.add(normalized)
        saveBlacklist(context, list)
    }
    return normalized
}

fun removeFromBlacklist(context: Context, number: String) {
    val normalized = normalizePhoneNumber(number)
    val list = loadBlacklist(context)
    if (list.contains(normalized)) {
        list.remove(normalized)
        saveBlacklist(context, list)
    }
}

// Vérifie si un numéro doit être bloqué : préfixes ARCEP + blacklist personnelle
fun isNumberBlocked(context: Context, number: String): Boolean {
    val normalized = normalizePhoneNumber(number)
    val blacklist = loadBlacklist(context)
    return ARCEP_PREFIXES.any { normalized.startsWith(it) } ||
            blacklist.any { normalized.startsWith(it) }
}
