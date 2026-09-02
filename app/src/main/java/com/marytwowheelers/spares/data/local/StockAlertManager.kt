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
        if (isInitialized) return
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = sp.getStringSet(KEY_ACKNOWLEDGED_KEYS, emptySet()) ?: emptySet()
        _acknowledgedKeys.value = saved
        isInitialized = true
    }

    /**
     * Unique key for a part's alert state (e.g. partId + currentStock)
     * If currentStock changes (e.g. drops from 4 to 0), it generates a fresh key and becomes a new unread alert.
     */
    fun createAlertKey(partId: String, currentStock: Int): String = "${partId}_$currentStock"

    fun markAllAsReviewed(context: Context, alertKeys: Set<String>) {
        init(context)
        val updated = _acknowledgedKeys.value + alertKeys
        _acknowledgedKeys.value = updated
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_ACKNOWLEDGED_KEYS, updated)
            .apply()
    }

    fun markPartAsReviewed(context: Context, alertKey: String) {
        init(context)
        val updated = _acknowledgedKeys.value + alertKey
        _acknowledgedKeys.value = updated
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_ACKNOWLEDGED_KEYS, updated)
            .apply()
    }

    fun clearAllReviewed(context: Context) {
        init(context)
        _acknowledgedKeys.value = emptySet()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_ACKNOWLEDGED_KEYS)
            .apply()
    }
}
