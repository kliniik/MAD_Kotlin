//package es.upm.btb.madproject.utils
//
//import android.content.Context
//import android.content.SharedPreferences
//
//class PreferencesManager(context: Context) {
//    private val prefs: SharedPreferences =
//        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
//
//    companion object {
//        private const val API_KEY = "api_key"
//        // API key only valid until 15 July 2025
//        private const val DEFAULT_API_KEY = "https://api.pegelalarm.at/api/station/1.0/a/lara_gerlach/"
//
//        @Volatile
//        private var INSTANCE: PreferencesManager? = null
//
//        fun init(context: Context) {  // Initialisierungsmethode
//            synchronized(this) {
//                if (INSTANCE == null) {
//                    INSTANCE = PreferencesManager(context)
//                }
//            }
//        }
//
//        fun getInstance(): PreferencesManager {
//            return INSTANCE ?: throw IllegalStateException("PreferencesManager wurde nicht initialisiert!")
//        }
//    }
//
//    fun getApiKey(): String {
//        return prefs.getString(API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
//    }
//
//    fun setApiKey(key: String) {
//        prefs.edit().putString(API_KEY, key).apply()
//    }
//}package es.upm.btb.madproject.utils
//
//import android.content.Context
//import android.content.SharedPreferences
//import android.util.Log
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import java.net.HttpURLConnection
//import java.net.URL
//
//class PreferencesManager(context: Context) {
//    private val prefs: SharedPreferences =
//        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
//
//    companion object {
//        private const val API_KEY = "api_key"
//        // API key only valid until 15 July 2025
//        private const val DEFAULT_API_KEY = "https://api.pegelalarm.at/api/station/1.0/a/lara_gerlach/"
//
//        @Volatile
//        private var INSTANCE: PreferencesManager? = null
//
//        fun init(context: Context) {
//            synchronized(this) {
//                if (INSTANCE == null) {
//                    INSTANCE = PreferencesManager(context)
//                }
//            }
//        }
//
//        fun getInstance(): PreferencesManager {
//            return INSTANCE ?: throw IllegalStateException("PreferencesManager nie został zainicjalizowany!")
//        }
//    }
//
//    fun getApiKey(): String {
//        return prefs.getString(API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
//    }
//
//    fun setApiKey(key: String) {
//        prefs.edit().putString(API_KEY, key).apply()
//    }
//
//    // Sprawdzanie poprawności API
//    suspend fun validateAndSetApiKey(apiKey: String): Boolean {
//        return withContext(Dispatchers.IO) {
//            try {
//                // Sprawdzamy, czy API działa poprawnie
//                val url = URL(apiKey)
//                val connection = url.openConnection() as HttpURLConnection
//                connection.requestMethod = "GET"
//                connection.connectTimeout = 5000
//                connection.connect()
//
//                val responseCode = connection.responseCode
//                connection.disconnect()
//
//                val isValid = responseCode in 200..299
//
//                if (isValid) {
//                    // API jest poprawne, zapisujemy je
//                    setApiKey(apiKey)
//                    true
//                } else {
//                    // API zwraca błąd, używamy domyślnego
//                    Log.e("PreferencesManager", "Niepoprawne API: kod odpowiedzi $responseCode")
//                    false
//                }
//            } catch (e: Exception) {
//                Log.e("PreferencesManager", "Błąd podczas sprawdzania API", e)
//                false
//            }
//        }
//    }
//
//    // Pomocnicza funkcja do resetowania API do domyślnego
//    fun resetToDefaultApi() {
//        setApiKey(DEFAULT_API_KEY)
//    }
//}

package es.upm.btb.madproject.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val API_KEY = "api_key"
        // API key only valid until 15 July 2025
        private const val DEFAULT_API_KEY = "https://api.pegelalarm.at/api/station/1.0/a/lara_gerlach/"

        @Volatile
        private var INSTANCE: PreferencesManager? = null

        fun init(context: Context) {
            synchronized(this) {
                if (INSTANCE == null) {
                    INSTANCE = PreferencesManager(context)
                }
            }
        }

        fun getInstance(): PreferencesManager {
            return INSTANCE ?: throw IllegalStateException("PreferencesManager has not been initialized!")
        }
    }

    fun getApiKey(): String {
        return prefs.getString(API_KEY, DEFAULT_API_KEY) ?: DEFAULT_API_KEY
    }

    fun setApiKey(key: String) {
        prefs.edit().putString(API_KEY, key).apply()
    }

    // API validation
    suspend fun validateAndSetApiKey(apiKey: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Check if API works correctly
                val url = URL(apiKey)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.connect()

                val responseCode = connection.responseCode
                connection.disconnect()

                val isValid = responseCode in 200..299

                if (isValid) {
                    // API is valid, save it
                    setApiKey(apiKey)
                    true
                } else {
                    // API returns error, use default
                    Log.e("PreferencesManager", "Invalid API: response code $responseCode")
                    false
                }
            } catch (e: Exception) {
                Log.e("PreferencesManager", "Error while validating API", e)
                false
            }
        }
    }

    // Helper function to reset API to default
    fun resetToDefaultApi() {
        setApiKey(DEFAULT_API_KEY)
    }
}
