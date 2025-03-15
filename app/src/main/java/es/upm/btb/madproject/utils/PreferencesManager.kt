package es.upm.btb.madproject.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val API_KEY = "api_key"
        // API key only valid until 15 July 2025
        private const val DEFAULT_API_KEY = "https://api.pegelalarm.at/api/station/1.0/a/lara_gerlach/"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun init(context: Context) {  // Initialisierungsmethode
            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = PreferencesManager(context)
                }
            }
        }

        fun getInstance(): PreferencesManager {
            return INSTANCE ?: throw IllegalStateException("PreferencesManager wurde nicht initialisiert!")
        }
    }

    fun getApiKey(): String {
        return prefs.getString(API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
    }

    fun setApiKey(key: String) {
        prefs.edit().putString(API_KEY, key).apply()
    }
}
