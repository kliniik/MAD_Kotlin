package es.upm.btb.madproject.utils

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val API_KEY = "api_key"
        private const val DEFAULT_API_KEY = "https://api.pegelalarm.at/api/station/1.0/a/lara_gerlach/"
    }

    fun getApiKey(): String {
        return prefs.getString(API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
    }

    fun setApiKey(key: String) {
        prefs.edit().putString(API_KEY, key).apply()
    }
}
