package com.marytwowheelers.spares.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")
private val THEME_KEY = stringPreferencesKey("theme_mode")

object ThemePreference {
    private const val PREFS_NAME = "theme_prefs_sync"
    private const val PREF_KEY_THEME = "theme_mode"

    /**
     * Synchronously returns the user's saved theme mode on cold start.
     * Uses SharedPreferences cache to ensure instant theme rendering without DataStore async latency.
     */
    fun getInitialThemeMode(context: Context): ThemeMode {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return when (sp.getString(PREF_KEY_THEME, null)) {
            ThemeMode.LIGHT.name -> ThemeMode.LIGHT
            ThemeMode.DARK.name  -> ThemeMode.DARK
            else                 -> ThemeMode.SYSTEM
        }
    }

    /**
     * Reactive Flow of the active theme mode.
     */
    fun getThemeMode(context: Context): Flow<ThemeMode> =
        context.themeDataStore.data.map { prefs ->
            when (prefs[THEME_KEY]) {
                ThemeMode.LIGHT.name -> ThemeMode.LIGHT
                ThemeMode.DARK.name  -> ThemeMode.DARK
                else                 -> ThemeMode.SYSTEM
            }
        }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        // 1. Update synchronous cache immediately
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(PREF_KEY_THEME, mode.name)
            .apply()

        // 2. Update DataStore for reactive flow emissions
        context.themeDataStore.edit { prefs ->
            prefs[THEME_KEY] = mode.name
        }
    }
}
