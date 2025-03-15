package es.upm.btb.madproject

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.content.Context
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import es.upm.btb.madproject.utils.PreferencesManager

class SettingsActivity : AppCompatActivity() {
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val editTextUserIdentifier: EditText = findViewById(R.id.editTextUserIdentifier)
        val buttonSave: Button = findViewById(R.id.buttonSave)

        // Load existing user identifier if available
        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val userIdentifier = sharedPreferences.getString("userIdentifier", "")
        editTextUserIdentifier.setText(userIdentifier)

//        // Setze gespeicherte Werte, falls vorhanden
//        editTextUserIdentifier.setText(sharedPreferences.getString("userIdentifier", ""))

        buttonSave.setOnClickListener {
            val userInput = editTextUserIdentifier.text.toString()
            if (userInput.isNotBlank()) {
                sharedPreferences.edit().apply {
                    putString("userIdentifier", userInput)
                    apply()
                }
                Toast.makeText(this, "User ID saved: $userInput", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "User ID cannot be blank!", Toast.LENGTH_SHORT).show()
            }
        }

        preferencesManager = PreferencesManager(this)

        val etApiKey = findViewById<EditText>(R.id.etApiKey)
        val btnSave = findViewById<Button>(R.id.btnSaveApiKey)

        // API-Key anzeigen
        etApiKey.setText(preferencesManager.getApiKey())

        // Speichern
        btnSave.setOnClickListener {
            val newApiKey = etApiKey.text.toString()
            preferencesManager.setApiKey(newApiKey)
        }
    }
}