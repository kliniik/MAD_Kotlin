package es.upm.btb.madproject

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.app.ActivityCompat
import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity(), LocationListener {
    private lateinit var bottomNavigationView: BottomNavigationView // bottom nav bar
    private lateinit var locationManager: LocationManager // location updates
    private lateinit var locationSwitch: Switch // enables/disables location tracking
    private val locationPermissionCode = 2 // location permissions
    private var latestLocation: Location? = null // stores last known GPS location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Toolbar with Overflow Menu (Settings)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Bottom Navigation View (Home: MainActivity, Map: OpenStreetMapActivity, List: SecondActivity)
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation_view)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true

                }
                R.id.navigation_map -> {
                    val intent = Intent(this, OpenStreetMapActivity::class.java)
                    latestLocation?.let {
                        val bundle = Bundle()
                        bundle.putParcelable("location", it)
                        intent.putExtra("locationBundle", bundle)
                    }
                    startActivity(intent)
                    true
                }
                R.id.navigation_list -> {
                    val intent = Intent(this, SecondActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Location Manager initialization
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager // retrieves location service
        locationSwitch = findViewById(R.id.locationSwitch) // finds switch UI element
        locationSwitch.setOnCheckedChangeListener { _, isChecked -> // toggles location tracking
            if (isChecked) {
                locationSwitch.text = "Disable location"
                startLocationUpdates()
            } else {
                locationSwitch.text = "Enable location"
                stopLocationUpdates()
            }
        }

        // Retrieve stored User ID + display toast message
        val userIdentifier = getUserIdentifier()
        if (userIdentifier == null) {
            Toast.makeText(this, "No User ID found", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "User ID: $userIdentifier", Toast.LENGTH_LONG).show()
        }
    }

    // Create menu (3 dots)
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    // Starts SettingsActivity when the setting menu item is clicked
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Start Location Updates every 5 minutes
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                locationPermissionCode
            )
        } else {
            Log.d("LOCATION_UPDATE", "Start GPS updates...")

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 5f, this)
        }
    }

    // Stop Location Updates
    private fun stopLocationUpdates() {
        locationManager.removeUpdates(this)
    }

    // Handle Location Permission Request (updates latestLocation and TextView with lat/lon + save GPS data)
    override fun onLocationChanged(location: Location) {
        latestLocation = location

        Log.d("Location", "New Location: Lat: ${location.latitude}, Lon: ${location.longitude}, Alt: ${location.altitude}")

        val textView: TextView = findViewById(R.id.mainTextView)
        val locationText = "Lat: ${location.latitude}, Lon: ${location.longitude}"
        textView.text = locationText
        saveCoordinatesToFile(location.latitude, location.longitude, location.altitude)
    }

    // User ID storage (retrieves user ID from SharedPreferences)
    private fun getUserIdentifier(): String? {
        val sharedPreferences = this.getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("userIdentifier", null)
    }

    // appends timestamped GPS data to a CSV file
    private fun saveCoordinatesToFile(latitude: Double, longitude: Double, altitude: Double) {
        val file = File(filesDir, "gps_coordinates.csv")

        val timestamp = System.currentTimeMillis().toString()
        val formattedData = "$timestamp;$latitude;$longitude;$altitude\n"

        try {
            file.appendText(formattedData)
            Log.d("FILE_WRITE", "GPS-Daten gespeichert: $formattedData")
        } catch (e: IOException) {
            Log.e("FILE_WRITE", "Fehler beim Speichern: ${e.message}")
        }
    }
}
