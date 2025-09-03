package com.example.callblocker

import android.content.Context
import androidx.core.content.edit


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

fun addToBlacklist(context: Context, number: String) {
    val list = loadBlacklist(context)
    if (!list.contains(number)) {
        list.add(number)
        saveBlacklist(context, list)
    }
}

fun removeFromBlacklist(context: Context, number: String) {
    val list = loadBlacklist(context)
    if (list.contains(number)) {
        list.remove(number)
        saveBlacklist(context, list)
    }
}

fun isNumberBlocked(context: Context, number: String): Boolean {
    val list = loadBlacklist(context)
    return list.any { number.startsWith(it) }  // utile pour préfixes ARCEP
}
