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
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.os.Build
import android.provider.Settings

import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import es.upm.btb.madproject.room.AppDatabase
import es.upm.btb.madproject.room.CoordinatesEntity
import kotlinx.coroutines.launch
import es.upm.btb.madproject.utils.PreferencesManager
import es.upm.btb.madproject.network.RetrofitClient
import es.upm.btb.madproject.network.PegelalarmResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

import com.google.firebase.auth.FirebaseAuth
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import android.app.Activity

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var locationManager: LocationManager
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var locationSwitch: Switch
    private val locationPermissionCode = 2
    private var latestLocation: Location? = null

    private lateinit var auth: FirebaseAuth
    // companion is the same than an static object in java
    companion object {
        private const val TAG = "MainActivity"
        private const val RC_SIGN_IN = 123
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.initPreferences(this)
        PreferencesManager.init(this)  // Initializes preferences
        val apiKey = PreferencesManager.getInstance().getApiKey()

        setContentView(R.layout.activity_main)

        fetchHighestWaterLevel()
        val floodImageUrl = "https://www.science.org/do/10.1126/science.abl5271/abs/germanyfloods_1280x720.jpg"

        Glide.with(this)
            .load(floodImageUrl)
            .placeholder(R.drawable.icon_placeholder)
            .into(findViewById(R.id.imageFlood))

        // set status bar color
        //window.statusBarColor = getColor(R.color.primaryColor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.primaryColor))
        }

        // Change the color of the navigation bar (bottom navigation bar)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.setNavigationBarColor(resources.getColor(R.color.colorBottomNavBackground))
        }


        // Toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

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

        // Init authentication flow
        launchSignInFlow()

    }

    override fun onResume() {
        super.onResume()

        // Restore the location state when returning to Home
        val isLocationEnabled = sharedPreferences.getBoolean("location_enabled", false)
        locationSwitch.isChecked = isLocationEnabled
        if (isLocationEnabled) {
            startLocationUpdates()
        }

        updateUIWithUsername()
    }

    // Toolbar menu
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.top_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                // Go to settings activity
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                // user login succeeded
                val user = FirebaseAuth.getInstance().currentUser
                Toast.makeText(this, R.string.signed_in, Toast.LENGTH_SHORT).show()
                Log.i(TAG, "onActivityResult " + getString(R.string.signed_in))
            } else {
                // user login failed
                Log.e(TAG, "Error starting auth session: ${response?.error?.errorCode}")
                Toast.makeText(this, R.string.signed_cancelled, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun updateUIWithUsername() {
        val user = FirebaseAuth.getInstance().currentUser
        val userNameTextView: TextView = findViewById(R.id.userNameTextView)
        user?.let {
            val name = user.displayName ?: "No Name"
            userNameTextView.text = "\uD83D\uDCA7 " + name + " \uD83D\uDCA7"
        }
    }

    private fun launchSignInFlow() {
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build()
        )
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )
    }
    private fun logout() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                // Restart activity after finishing
                val intent = Intent(this, MainActivity::class.java)
                // Clean back stack so that user cannot retake activity after logout
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
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

    private fun fetchHighestWaterLevel() {
        val commonIds = listOf(
            "jucar-0O01DQG2-es", "jucar-0E03DQG2-es", "jucar-0O04DQG0-es", "jucar-0O03DQG0-es",
            "jucar-0O02DQG0-es", "jucar-0E04EVI1-es", "jucar-0E01EVI1-es", "jucar-7C01DQG2-es",
            "jucar-7A02DQG1-es", "jucar-0R04DQG0-es", "jucar-7E03EVI1-es", "jucar-1E04EVI1-es",
            "jucar-6E03EVI1-es", "jucar-6E02DQG1-es", "jucar-6A02DQG1-es", "jucar-9O03DQG4-es",
            "jucar-1E07DQG4-es", "jucar-1E09DQG1-es", "jucar-1E06EVI1-es", "jucar-1E03DQG3-es"
        )

        var maxLevel = 0.0
        var highestStation = "Unknown"
        var responsesReceived = 0

        for (commonId in commonIds) {
            RetrofitClient.api.getWaterLevelData(commonId).enqueue(object : Callback<PegelalarmResponse> {
                override fun onResponse(call: Call<PegelalarmResponse>, response: Response<PegelalarmResponse>) {
                    responsesReceived++
                    if (response.isSuccessful) {
                        response.body()?.let { data ->
                            if (data.payload.stations.isNotEmpty()) {
                                val station = data.payload.stations[0]
                                val waterLevel = station.data.firstOrNull()?.value ?: 0.0
                                if (waterLevel > maxLevel) {
                                    maxLevel = waterLevel
                                    highestStation = station.name
                                }
                            }
                        }
                    }
                    if (responsesReceived == commonIds.size) {
                        updateUI(maxLevel, highestStation)
                    }
                }

                override fun onFailure(call: Call<PegelalarmResponse>, t: Throwable) {
                    responsesReceived++
                    if (responsesReceived == commonIds.size) {
                        updateUI(maxLevel, highestStation)
                    }
                }
            })
        }
    }

    private fun updateUI(maxLevel: Double, highestStation: String) {
        val thresholdGreen = if (maxLevel > 0) maxLevel / 3 else 0.0
        val thresholdYellow = if (maxLevel > 0) 2 * maxLevel / 3 else 0.0

        val format = "%.2f"
        val formattedGreen = String.format(Locale.ENGLISH, format, thresholdGreen)
        val formattedYellow = String.format(Locale.ENGLISH, format, thresholdYellow)
        val formattedMax = String.format(Locale.ENGLISH, format, maxLevel)

        runOnUiThread {
            findViewById<TextView>(R.id.tvHighestStation).text = "Station with highest water level: \n$highestStation ($formattedMax cm)"
            findViewById<TextView>(R.id.tvThresholds).text = "Thresholds: \n🟢 $formattedGreen cm, 🟡 $formattedYellow cm, 🔴 $formattedMax cm"
        }
    }

}
