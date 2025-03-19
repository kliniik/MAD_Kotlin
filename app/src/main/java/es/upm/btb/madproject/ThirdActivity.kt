package es.upm.btb.madproject

import android.content.Intent
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
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
import java.util.Date
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

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
        Log.d("FirebaseTest", "Firebase initialized: ${FirebaseDatabase.getInstance().reference}")

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Retrieve references to EditText fields
        etTimestamp = findViewById(R.id.etTimestamp)
        etLatitude = findViewById(R.id.etLatitude)
        etLongitude = findViewById(R.id.etLongitude)
        etAltitude = findViewById(R.id.etAltitude)

        // Retrieve data from Intent
        val timestampString = intent.getStringExtra("timestamp")
        val latitude = intent.getStringExtra("latitude")
        val longitude = intent.getStringExtra("longitude")
        val altitude = intent.getStringExtra("altitude")

        Log.d(TAG, "Intent Data -> Timestamp: $timestampString, Latitude: $latitude, Longitude: $longitude, Altitude: $altitude")

        val timestamp: Long? = timestampString?.toLongOrNull()
        Log.d("IntentDebug", "Timestamp received: $timestamp")

        // Log error, if `timestamp` isn't valid
        if (timestamp == null) {
            Log.e(TAG, "ERROR: The timestamp couldn't be converted into long! Value from Intent: $timestampString")
        }

        Log.d(TAG, "Latitude: $latitude, Longitude: $longitude, Altitude: $altitude")

        // Assign data to EditText fields
        etTimestamp.setText(timestamp?.let { convertTimestamp(it) } ?: "Invalid value")
        etLatitude.setText(latitude ?: "N/A")
        etLongitude.setText(longitude ?: "N/A")
        etAltitude.setText(altitude ?: "N/A")

        // Button to return to SecondActivity
        findViewById<Button>(R.id.buttonToSecond).setOnClickListener {
            startActivity(Intent(this, SecondActivity::class.java))
            finish()
        }

        // Button to delete coordinate
        findViewById<Button>(R.id.buttonDelete).setOnClickListener {
            timestamp?.let { showDeleteConfirmationDialog(it) }
        }

        // Button to update coordinate
        findViewById<Button>(R.id.buttonUpdate).setOnClickListener {
            showUpdateConfirmationDialog()
        }

        // Add item to Firebase realtime database
        val addReportButton: Button = findViewById(R.id.addReportButton)
        val editTextReport: EditText = findViewById(R.id.editTextReport)
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.e("FirebaseAuth", "User is not logged in")
            Toast.makeText(this, "Please log in first", Toast.LENGTH_LONG).show()
        }
        val userId = user?.uid
        addReportButton.setOnClickListener {
            Log.d("ReportButton", "Button clicked")
            val reportText = editTextReport.text.toString().trim()
            if (reportText.isNotEmpty() && userId != null) {
                val report = mapOf(
                    "userId" to userId,
                    "timestamp" to (timestamp ?: 0L) as Any,  // Explicit cast to Any
                    "report" to reportText,
                    "latitude" to (latitude?.toDoubleOrNull() ?: 0.0) as Any,  // Explicit cast
                    "longitude" to (longitude?.toDoubleOrNull() ?: 0.0) as Any  // Explicit cast
                )
                Log.d("ReportData", "Report Text: $reportText, User ID: $userId, Timestamp: $timestamp, Lat: $latitude, Lon: $longitude")
                addReportToDatabase(report)
            } else {
                Toast.makeText(this, "Report name cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun convertTimestamp(timestamp: Long): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(timestamp))
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
            val coordinate = db.coordinatesDao().getCoordinateByTimestamp(timestamp)

            if (coordinate != null) {
                db.coordinatesDao().deleteWithTimestamp(timestamp)
                Log.d(TAG, "Coordinate with timestamp $timestamp deleted.")
            } else {
                Log.e(TAG, "No coordinate found with timestamp $timestamp. Deletion failed.")
            }

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
            val timestamp = intent.getStringExtra("timestamp")?.toLongOrNull()
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

    private fun addReportToDatabase(report: Map<String, Any>) {
        val databaseReference = FirebaseDatabase.getInstance().reference

        // Prüfen, ob "hotspots" existiert
        databaseReference.child("hotspots").get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                Log.w("Firebase", "Path 'hotspots' does not exist, creating it now.")
            }

            val newReportRef = databaseReference.child("hotspots").push()
            newReportRef.setValue(report)
                .addOnSuccessListener {
                    Log.d("Firebase", "Report added successfully to /hotspots")
                    Toast.makeText(this, "Report added!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Failed to add report: ${e.message}")
                    Toast.makeText(this, "Firebase Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }.addOnFailureListener {
            Log.e("Firebase", "Failed to check 'hotspots' path: ${it.message}")
        }
    }

}
