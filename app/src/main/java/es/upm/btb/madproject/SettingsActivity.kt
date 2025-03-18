package es.upm.btb.madproject

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import es.upm.btb.madproject.utils.PreferencesManager
import kotlinx.coroutines.launch
import com.google.android.material.bottomnavigation.BottomNavigationView

class SettingsActivity : AppCompatActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        // set status bar color
        //window.statusBarColor = getColor(R.color.primaryColor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.primaryColor))
        }

        // set navigation bar color
        // Zmiana koloru paska nawigacji (dolnego paska nawigacji)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(resources.getColor(R.color.colorBottomNavBackground))
        }

        // Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation_view)
        bottomNavigationView.selectedItemId = R.id.navigation_home

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_map -> {
                    startActivity(Intent(this, OpenStreetMapActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_list -> {
                    true // Bereits in "Liste", also nichts tun
                }
                else -> false
            }
        }

        // Initialize PreferencesManager
        PreferencesManager.init(this)
        preferencesManager = PreferencesManager.getInstance()

        // Find views using correct IDs
        val editTextUserIdentifier: EditText = findViewById(R.id.editTextUserIdentifier)
        val editTextApiKey: EditText = findViewById(R.id.etApiKey) // Using ID from first file
        val buttonSave: Button = findViewById(R.id.buttonSave)

        // Find API Key save button - using ID from first file
        val btnSaveApiKey: Button = findViewById(R.id.btnSaveApiKey)

        // Load existing values
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val userIdentifier = sharedPreferences.getString("userIdentifier", "")

        editTextUserIdentifier.setText(userIdentifier)
        editTextApiKey.setText(preferencesManager.getApiKey())

        // Set up User ID save button
        buttonSave.setOnClickListener {
            val newUserIdentifier = editTextUserIdentifier.text.toString()

            // Save user identifier
            if (newUserIdentifier.isNotBlank()) {
                sharedPreferences.edit().apply {
                    putString("userIdentifier", newUserIdentifier)
                    apply()
                }
                Toast.makeText(this, "User ID saved: $newUserIdentifier", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "User ID cannot be empty!", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up API Key save button
        btnSaveApiKey.setOnClickListener {
            val newApiKey = editTextApiKey.text.toString()

            // Check and save API Key
            if (newApiKey.isNotBlank()) {
                lifecycleScope.launch {
                    val isValid = preferencesManager.validateAndSetApiKey(newApiKey)
                    if (isValid) {
                        Toast.makeText(this@SettingsActivity, "Valid API Key has been saved", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            this@SettingsActivity,
                            "Invalid API Key. Using default API.",
                            Toast.LENGTH_LONG
                        ).show()
                        preferencesManager.resetToDefaultApi()
                        editTextApiKey.setText(preferencesManager.getApiKey())
                    }
                }
            } else {
                Toast.makeText(this, "API Key cannot be empty!", Toast.LENGTH_SHORT).show()
                preferencesManager.resetToDefaultApi()
                editTextApiKey.setText(preferencesManager.getApiKey())
            }
        }
    }
}