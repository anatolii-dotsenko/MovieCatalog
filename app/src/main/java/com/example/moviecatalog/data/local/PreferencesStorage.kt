package com.example.moviecatalog.data.local

import android.content.Context

interface IPreferencesStorage {
    fun set(key: String, value: String?)
    fun get(key: String, defaultValue: String? = null): String?
    fun remove(key: String)
    fun clear()
}

/**
 * Unencrypted key-value storage (SharedPreferences) for non-sensitive public profile settings.
 */
class PreferencesStorage(context: Context) : IPreferencesStorage {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun set(key: String, value: String?) {
        if (value == null) {
            remove(key)
        } else {
            prefs.edit().putString(key, value).apply()
        }
    }

    override fun get(key: String, defaultValue: String?): String? =
        prefs.getString(key, defaultValue)

    override fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val PREFS_NAME = "movie_catalog_preferences"
    }
}
