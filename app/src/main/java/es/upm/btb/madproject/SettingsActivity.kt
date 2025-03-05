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

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        val editTextUserIdentifier: EditText = findViewById(R.id.editTextUserIdentifier)
        val buttonSave: Button = findViewById(R.id.buttonSave)

        // Setze gespeicherte Werte, falls vorhanden
        editTextUserIdentifier.setText(sharedPreferences.getString("userIdentifier", ""))

        buttonSave.setOnClickListener {
            val userInput = editTextUserIdentifier.text.toString()
            if (userInput.isNotBlank()) {
                sharedPreferences.edit().apply {
                    putString("userIdentifier", userInput)
                    apply()
                }
                Toast.makeText(this, "User ID gespeichert: $userInput", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "User ID darf nicht leer sein!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}