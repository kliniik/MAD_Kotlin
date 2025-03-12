package es.upm.btb.madproject

import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import es.upm.btb.madproject.room.AppDatabase
import kotlinx.coroutines.launch

class SecondActivity : AppCompatActivity() {
    private val TAG = "SecondActivity"
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private var latestLocation: Location? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_second)

        createCsvFileIfNotExists()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // `ListView` with header for showing coordinates
        val coordinatesListView: ListView = findViewById(R.id.lvCoordinates)
        val headerView = layoutInflater.inflate(R.layout.listview_header, coordinatesListView, false)
        coordinatesListView.addHeaderView(headerView, null, false)

        val coordinatesAdapter = CoordinatesAdapter(this, readFileContents())
        coordinatesListView.adapter = coordinatesAdapter

        // database
        val db = AppDatabase.getDatabase(this)

        // Obtener datos desde Room y crear el adaptador con los datos directamente
        lifecycleScope.launch {
            val dbCoordinates = db.coordinatesDao().getAll()

            val roomCoordinates = dbCoordinates.map {
                listOf(it.timestamp.toString(), it.latitude.toString(), it.longitude.toString(), it.altitude.toString())
            }

            Log.d(TAG, "Data obtained from Room: $roomCoordinates")

            // Instanciar el adaptador con los datos de Room directamente
            val adapter = CoordinatesAdapter(this@SecondActivity, roomCoordinates)
            coordinatesListView.adapter = adapter  // Asignar el adaptador al ListView
        }

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation_view)
        bottomNavigationView.selectedItemId = R.id.navigation_list // Markiert "Liste" als aktiv

        // toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Initialize the DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)

        // Set up item selection listener for the Drawer
        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
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

    }



    private fun createCsvFileIfNotExists() {
        val file = File(filesDir, "gps_coordinates.csv")

        if (!file.exists()) {
            file.createNewFile()  // Datei erstellen
            file.writeText("Timestamp;Latitude;Longitude;Altitude\n") // Erste Zeile als Header
        }
    }

    // Reads simple lines of file `lvFileContents`
    private fun readFileLines(): List<String> {
        return try {
            openFileInput("gps_coordinates.csv").bufferedReader().readLines()
        } catch (e: IOException) {
            listOf("Error reading file: ${e.message}")
        }
    }

    // Reads CSV file & splits values for `lvCoordinates`
    private fun readFileContents(): List<List<String>> {
        return try {
            openFileInput("gps_coordinates.csv").bufferedReader().useLines { lines ->
                lines.drop(1).map { it.split(";").map(String::trim) }
                    .filter { it.size == 4 }
                    .toList()
            }
        } catch (e: IOException) {
            listOf(listOf("Error reading file: ${e.message}"))
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
                //startActivity(Intent(this, SecondActivity::class.java))
                true
            }
            R.id.nav_home -> {
                startActivity(Intent(this, MainActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    // Adapter zur Anzeige von Koordinaten-Daten in `lvCoordinates`
    private class CoordinatesAdapter(context: Context, private val coordinatesList: List<List<String>>) :
        ArrayAdapter<List<String>>(context, R.layout.listview_item, coordinatesList) {
        private val inflater: LayoutInflater = LayoutInflater.from(context)

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.listview_item, parent, false)

            val timestampTextView: TextView = view.findViewById(R.id.tvTimestamp)
            val latitudeTextView: TextView = view.findViewById(R.id.tvLatitude)
            val longitudeTextView: TextView = view.findViewById(R.id.tvLongitude)
            val altitudeTextView: TextView = view.findViewById(R.id.tvAltitude)

            val item = coordinatesList[position]
            if (item.size == 4) {
                timestampTextView.text = formatTimestamp(item[0].toLong())
                latitudeTextView.text = formatCoordinate(item[1].replace(",", ".").toDouble())
                longitudeTextView.text = formatCoordinate(item[2].replace(",", ".").toDouble())
                altitudeTextView.text = formatCoordinate(item[3].replace(",", ".").toDouble())
            } else {
                timestampTextView.text = "Invalid Data"
                latitudeTextView.text = ""
                longitudeTextView.text = ""
                altitudeTextView.text = ""
            }

            // Klick-Listener für einzelne Koordinaten-Einträge
            view.setOnClickListener {
                val intent = Intent(context, ThirdActivity::class.java).apply {
                    putExtra("latitude", item.getOrNull(1) ?: "N/A")
                    putExtra("longitude", item.getOrNull(2) ?: "N/A")
                    putExtra("altitude", item.getOrNull(3) ?: "N/A")
                }
                context.startActivity(intent)
            }

            return view
        }

        private fun formatTimestamp(timestamp: Long): String {
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return formatter.format(Date(timestamp))
        }

        private fun formatCoordinate(value: Double): String {
            return String.format("%.4f", value)
        }
    }
}