package es.upm.btb.madproject

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.GravityCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout
import android.os.Build

import android.provider.Settings

import androidx.lifecycle.lifecycleScope
import es.upm.btb.madproject.room.AppDatabase
import es.upm.btb.madproject.room.CoordinatesEntity
import kotlinx.coroutines.launch
import es.upm.btb.madproject.utils.PreferencesManager

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var navigationView: NavigationView
    private lateinit var locationManager: LocationManager
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var locationSwitch: Switch
    private val locationPermissionCode = 2
    private var latestLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PreferencesManager.init(this)  // Initialisiert die Preferences
        val apiKey = PreferencesManager.getInstance().getApiKey()

        setContentView(R.layout.activity_main)

        // set status bar color
        //window.statusBarColor = getColor(R.color.primaryColor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.primaryColor))
        }

        // Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Initialize the DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        // Set up item selection listener for the Drawer
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.nav_open_street_map -> {
                    val intent = Intent(this, OpenStreetMapActivity::class.java)
                    latestLocation?.let {
                        val bundle = Bundle()
                        bundle.putParcelable("location", it)
                        intent.putExtra("locationBundle", bundle)
                    }
                    startActivity(intent)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.nav_second_activity -> {
                    startActivity(Intent(this, SecondActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                R.id.menu_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }

                else -> false
            }
        }

        // Bottom Navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation_view)
        bottomNavigationView.selectedItemId = R.id.navigation_home

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
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
                    finish()
                    true
                }

                R.id.navigation_list -> {
                    startActivity(Intent(this, SecondActivity::class.java))
                    finish()
                    true
                }

                else -> false
            }
        }

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("AppPreferences", Context.MODE_PRIVATE)

        // Location Manager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationSwitch = findViewById(R.id.locationSwitch)

        // Restore saved state from SharedPreferences
        val isLocationEnabled = sharedPreferences.getBoolean("location_enabled", false)
        locationSwitch.isChecked = isLocationEnabled
        locationSwitch.text = if (isLocationEnabled) "Disable location" else "Enable location"

        // Listen for location switch changes
        locationSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                locationSwitch.text = "Disable location"
                startLocationUpdates()
            } else {
                locationSwitch.text = "Enable location"
                stopLocationUpdates()
            }

            // Save the state IMMEDIATELY inside the listener
            with(sharedPreferences.edit()) {
                putBoolean("location_enabled", isChecked)
                apply()
            }
        }


        // If location was previously enabled, start updates
        if (isLocationEnabled) {
            startLocationUpdates()
        }

        // Request location permissions if not granted
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
        } else {
            Log.d("MainActivity", "Location permission already granted")
            // Check if GPS is enabled
            checkIfGpsIsEnabled()
        }
    }

    override fun onResume() {
        super.onResume()

        // Restore the location state when returning to Home
        val isLocationEnabled = sharedPreferences.getBoolean("location_enabled", false)
        locationSwitch.isChecked = isLocationEnabled
        if (isLocationEnabled) {
            startLocationUpdates()
        }
    }

    // Toolbar menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }

            R.id.nav_open_street_map -> {
                val intent = Intent(this, OpenStreetMapActivity::class.java)
                latestLocation?.let {
                    val bundle = Bundle()
                    bundle.putParcelable("location", it)
                    intent.putExtra("locationBundle", bundle)
                }
                startActivity(intent)
                true
            }

            R.id.nav_second_activity -> {
                startActivity(Intent(this, SecondActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // Location Updates
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestLocationPermission()
            return
        }

        Log.d("LOCATION_UPDATE", "Start GPS updates...")

        // Ensure GPS provider is enabled
        checkIfGpsIsEnabled()

        // Make sure latest location is stored correctly
        latestLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10f, this)
    }


    private fun stopLocationUpdates() {
        locationManager.removeUpdates(this)
    }

    private fun checkIfGpsIsEnabled() {
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!gpsEnabled) {
            Toast.makeText(this, "GPS is off, please turn it on", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onLocationChanged(location: Location) {
        latestLocation = location
        Log.d(
            "Location",
            "New Location: Lat: ${location.latitude}, Lon: ${location.longitude}, Alt: ${location.altitude}"
        )

        val textView: TextView = findViewById(R.id.mainTextView)
        textView.text = "Lat: ${location.latitude}, Lon: ${location.longitude}"

        //saveCoordinatesToFile(location.latitude, location.longitude, location.altitude)
        saveCoordinatesToDatabase(
            location.latitude,
            location.longitude,
            location.altitude,
            location.time
        )
    }

    private fun saveCoordinatesToDatabase(latitude: Double, longitude: Double, altitude: Double, timestamp: Long) {
        val coordinates = CoordinatesEntity(
            timestamp = timestamp,
            latitude = latitude,
            longitude = longitude,
            altitude = altitude
        )
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch {
            db.coordinatesDao().insert(coordinates)
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            Toast.makeText(
                this,
                "We need access to your location to track your position",
                Toast.LENGTH_LONG
            ).show()
            Log.d("MainActivity", "Permission rationale shown")
        }

        // Request the permissions
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            locationPermissionCode
        )
    }

    // Handle the result of the permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
                Toast.makeText(this, "Location permissions granted", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity", "Permission granted")
            } else {
                if (!ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    )
                ) {
                    Toast.makeText(
                        this,
                        "You need to manually enable location permissions in app settings",
                        Toast.LENGTH_LONG
                    ).show()
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = android.net.Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                    Log.d("MainActivity", "Permission denied, navigating to settings")
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                    Log.d("MainActivity", "Permission denied")
                }
            }
        }
    }
}
