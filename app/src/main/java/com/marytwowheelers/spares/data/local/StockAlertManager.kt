package com.marytwowheelers.spares.data.local

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object StockAlertManager {
    private const val PREFS_NAME = "stock_alerts_prefs"
    private const val KEY_ACKNOWLEDGED_KEYS = "acknowledged_alert_keys"

    private val _acknowledgedKeys = MutableStateFlow<Set<String>>(emptySet())
    val acknowledgedKeys: StateFlow<Set<String>> = _acknowledgedKeys.asStateFlow()
    private var isInitialized = false

    fun init(context: Context) {
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = sp.getStringSet(KEY_ACKNOWLEDGED_KEYS, emptySet()) ?: emptySet()
        _acknowledgedKeys.value = HashSet(saved)
        isInitialized = true
    }

    /**
     * Unique key for a part's alert state (e.g. partId + currentStock)
     * If currentStock changes (e.g. drops from 4 to 0), it generates a fresh key and becomes a new unread alert.
     */
    fun createAlertKey(partId: String, currentStock: Int): String = "${partId}_$currentStock"

    fun markAllAsReviewed(context: Context, alertKeys: Set<String>) {
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = sp.getStringSet(KEY_ACKNOWLEDGED_KEYS, emptySet()) ?: emptySet()
        val updated = HashSet(current).apply { addAll(alertKeys) }
        _acknowledgedKeys.value = HashSet(updated)
        sp.edit().putStringSet(KEY_ACKNOWLEDGED_KEYS, HashSet(updated)).apply()
    }

    fun markPartAsReviewed(context: Context, alertKey: String) {
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val current = sp.getStringSet(KEY_ACKNOWLEDGED_KEYS, emptySet()) ?: emptySet()
        val updated = HashSet(current).apply { add(alertKey) }
        _acknowledgedKeys.value = HashSet(updated)
        sp.edit().putStringSet(KEY_ACKNOWLEDGED_KEYS, HashSet(updated)).apply()
    }

    fun clearAllReviewed(context: Context) {
        _acknowledgedKeys.value = emptySet()
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACKNOWLEDGED_KEYS)
            .apply()
    }
}
