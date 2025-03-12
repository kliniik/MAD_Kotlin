package es.upm.btb.madproject

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import es.upm.btb.madproject.room.AppDatabase
import es.upm.btb.madproject.room.CoordinatesEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ThirdActivity : AppCompatActivity() {
    private val TAG = "btaThirdActivity"

    private lateinit var etTimestamp: EditText
    private lateinit var etLatitude: EditText
    private lateinit var etLongitude: EditText
    private lateinit var etAltitude: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_third)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Pobranie referencji do EditText
        etTimestamp = findViewById(R.id.etTimestamp)
        etLatitude = findViewById(R.id.etLatitude)
        etLongitude = findViewById(R.id.etLongitude)
        etAltitude = findViewById(R.id.etAltitude)

        // Pobranie danych z Intent
        val timestamp = intent.getStringExtra("timestamp")
        val latitude = intent.getStringExtra("latitude")
        val longitude = intent.getStringExtra("longitude")
        val altitude = intent.getStringExtra("altitude")

        Log.d(TAG, "Latitude: $latitude, Longitude: $longitude, Altitude: $altitude")

        // Przypisanie danych do EditText
        etTimestamp.setText(timestamp)
        etLatitude.setText(latitude)
        etLongitude.setText(longitude)
        etAltitude.setText(altitude)

        // Przycisk powrotu do SecondActivity
        findViewById<Button>(R.id.buttonToSecond).setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
            finish()
        }

        // Przycisk usunięcia koordynatu
        findViewById<Button>(R.id.buttonDelete).setOnClickListener {
            if (!timestamp.isNullOrEmpty()) {
                showDeleteConfirmationDialog(timestamp.toLong())
            }
        }

        // Przycisk aktualizacji koordynatu
        findViewById<Button>(R.id.buttonUpdate).setOnClickListener {
            showUpdateConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog(timestamp: Long) {
        AlertDialog.Builder(this)
            .setTitle("Confirm Delete")
            .setMessage(
                "Are you sure you want to delete this coordinate?\n\n" +
                        "📍 Timestamp: $timestamp\n" +
                        "📍 Latitude: ${etLatitude.text}\n" +
                        "📍 Longitude: ${etLongitude.text}\n" +
                        "📍 Altitude: ${etAltitude.text}"
            )
            .setPositiveButton("Delete") { _, _ ->
                deleteCoordinate(timestamp)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteCoordinate(timestamp: Long) {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch(Dispatchers.IO) {
            db.coordinatesDao().deleteWithTimestamp(timestamp)
            Log.d(TAG, "Coordinate with timestamp $timestamp deleted.")
            withContext(Dispatchers.Main) {
                startActivity(Intent(this@ThirdActivity, SecondActivity::class.java))
                finish()
            }
        }
    }

    private fun showUpdateConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Update")
            .setMessage(
                "Are you sure you want to update this coordinate?\n\n" +
                        "📍 Timestamp: ${etTimestamp.text}\n" +
                        "📍 Latitude: ${etLatitude.text}\n" +
                        "📍 Longitude: ${etLongitude.text}\n" +
                        "📍 Altitude: ${etAltitude.text}"
            )
            .setPositiveButton("Update") { _, _ ->
                updateCoordinate()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateCoordinate() {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch(Dispatchers.IO) {
            val timestamp = etTimestamp.text.toString().toLongOrNull()
            if (timestamp == null) {
                Log.e(TAG, "Invalid timestamp value")
                return@launch
            }

            val existingCoordinate = db.coordinatesDao().getCoordinateByTimestamp(timestamp)
            if (existingCoordinate != null) {
                val updatedCoordinate = CoordinatesEntity(
                    timestamp = timestamp,
                    latitude = etLatitude.text.toString().toDoubleOrNull() ?: 0.0,
                    longitude = etLongitude.text.toString().toDoubleOrNull() ?: 0.0,
                    altitude = etAltitude.text.toString().toDoubleOrNull() ?: 0.0
                )
                db.coordinatesDao().updateCoordinate(updatedCoordinate)
                Log.d(TAG, "Coordinate updated: $updatedCoordinate")
            } else {
                Log.e(TAG, "No coordinate found with timestamp $timestamp")
            }
            withContext(Dispatchers.Main) {
                startActivity(Intent(this@ThirdActivity, SecondActivity::class.java))
                finish()
            }
        }
    }
}
