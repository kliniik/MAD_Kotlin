package es.upm.btb.madproject

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
//import org.osmdroid.views.overlay.Polyline
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.location.Location
import android.os.Build
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.lifecycleScope
import es.upm.btb.madproject.room.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.views.overlay.Polyline

class OpenStreetMapActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private var latestLocation: Location? = null

    companion object {
        private const val TAG = "OpenStreetMapActivity"
    }


    // Flood risk zones in Valencia
    private val gymkhanaCoords = listOf(
        GeoPoint(39.4699, -0.3774), // Valencia Zentrum
        GeoPoint(39.4636, -0.3783), // Turia Bridge
        GeoPoint(39.4598, -0.3751), // Turia Gardens
        GeoPoint(39.4573, -0.3710), // Albufera Bridge
        GeoPoint(39.4605, -0.3845)  // Turia-Flussgebiet
    )

//    // Flood risk zones around the current Turia River
//    private val gymkhanaCoords = listOf(
//        GeoPoint(39.4358, -0.3183), // Turia River Mouth
//        GeoPoint(39.4273, -0.3199), // Pinedo Beach
//        GeoPoint(39.3911, -0.3165), // El Saler Wetlands
//        GeoPoint(39.3541, -0.3190), // Albufera Natural Park
//        GeoPoint(39.4441, -0.3442)  // Nazaret Neighborhood
//    )


    private val gymkhanaNames = listOf(
        "Valencia City Center",
        "Turia Bridge",
        "Turia Gardens",
        "Albufera Bridge",
        "Turia River Area"

    // flood risk zones in Valencia
    // private val gymkhanaCoords = listOf(
//        GeoPoint(39.4699, -0.3774), // Near the Turia River, central Valencia
//        GeoPoint(39.4636, -0.3783), // Near the Turia Bridge
//        GeoPoint(39.4598, -0.3751), // Near the Turia Gardens
//        GeoPoint(39.4573, -0.3710), // Near the Albufera Bridge
//        GeoPoint(39.4605, -0.3845)  // Near parks and bike paths along the rive
//
//        GeoPoint(39.4705, -0.3768), // Bridge in the centre of Valencia
//        GeoPoint(39.4665, -0.3755), // Turia garden - low-lying area
//        GeoPoint(39.4622, -0.3740), // Close to oceanarium, historical flooding
//        GeoPoint(39.4581, -0.3730), // Recreational areas close to river
//        GeoPoint(39.4715, -0.3800), // High risk of flooding - old river channel
//
//
//    )

    )

//    private val gymkhanaDescriptions = listOf(
//        "The mouth of the Turia River, where heavy rains and storm surges can cause flooding, affecting nearby beaches and the Valencia Port area.",
//        "A coastal area vulnerable to rising sea levels and river overflows, especially during storms and high tides.",
//        "A natural reserve that absorbs excess water but is at risk during extreme weather events, potentially impacting wildlife and nearby infrastructure.",
//        "A low-lying wetland area where river floods mix with seawater, threatening both agriculture and biodiversity.",
//        "A historically flood-prone district near the river, where heavy rain can overwhelm drainage systems, causing urban flooding."
//    )

    private val gymkhanaDescriptions = listOf(
        "An urban area prone to flooding due to heavy rainfall and outdated drainage.",
        "A key bridge vulnerable to river surges and temporary closures during storms.",
        "A park that can be overwhelmed by floods, affecting its ecosystem and infrastructure.",
        "A bridge at risk of disruption from river overflows and high tides during storms.",
        "A floodplain susceptible to heavy river overflows, threatening infrastructure and land."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_street_map)

        // Initialize the DrawerLayout and NavigationView
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.navigation_view)


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // set status bar color
        //window.statusBarColor = getColor(R.color.primaryColor)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.primaryColor));
        }

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


        // OpenStreetMap konfigurieren
        //Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().userAgentValue = "es.upm.btb.madproject"
        Configuration.getInstance()
            .load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))

        val bundle = intent.getBundleExtra("locationBundle")
        //Deprecated: val location: Location? = bundle?.getParcelable("location")
        val location: Location? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle?.getParcelable("location", Location::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle?.getParcelable("location")
        }

        val startPoint = if (location != null) {
            Log.d(
                TAG,
                "onCreate: Location[${location.altitude}][${location.latitude}][${location.longitude}]"
            )

            GeoPoint(location.latitude, location.longitude)
            //GeoPoint(39.426714, -0.339140)
        } else {
            Log.d(TAG, "onCreate: Location is null, using default coordinates")
            GeoPoint(39.426714, -0.339140) // Valencia, Spain
        }

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.controller.setZoom(14.0) // 🟢 Startzoom auf Valencia setzen
        map.controller.setCenter(gymkhanaCoords[0]) // 🟢 Kamera auf Valencia setzen
        map.setMultiTouchControls(true)


        // Markierungen hinzufügen
        addGymkhanaMarkers()
        // Route zwischen Markern zeichnen
        drawPolyline()

        // load the coordinates from Room and show with the other marker
        loadDatabaseMarkers()

        // bottom navigation
        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation_view)
        bottomNavigationView.selectedItemId = R.id.navigation_map // Markiert "Karte" als aktiv

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.navigation_map -> {
                    true
                }
                R.id.navigation_list -> {
                    startActivity(Intent(this, SecondActivity::class.java))
                    finish()
                    true // Already in "list" ,so don't do anything
                }
                else -> false
            }
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
            R.id.nav_home -> {
                startActivity(Intent(this, MainActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }


    private fun addGymkhanaMarkers() {
        for (i in gymkhanaCoords.indices) {
            val marker = Marker(map)
            marker.position = gymkhanaCoords[i]
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = gymkhanaNames[i]
            marker.snippet = gymkhanaDescriptions[i]
            marker.icon = vectorToBitmapDrawable(this, R.drawable.icon_map_marker, 50, 50)
            map.overlays.add(marker)
        }
        map.invalidate()
    }

    private fun drawPolyline() {
        val polyline = Polyline()
        polyline.setPoints(gymkhanaCoords)
        map.overlays.add(polyline)
    }

    private fun vectorToBitmapDrawable(context: Context, drawableId: Int, width: Int, height: Int): BitmapDrawable? {
        val drawable = ContextCompat.getDrawable(context, drawableId) ?: return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDrawable(context.resources, bitmap)
    }

    private fun loadDatabaseMarkers() {
        val db = AppDatabase.getDatabase(this)
        lifecycleScope.launch(Dispatchers.IO) {
            val dbCoordinates = db.coordinatesDao().getAll()
            val roomGeoPoints = dbCoordinates.map {
                GeoPoint(it.latitude, it.longitude)
            }
            Log.d(TAG, "Coordinates obtained from Room: $roomGeoPoints")

            withContext(Dispatchers.Main) {
                addDatabaseMarkers(map, roomGeoPoints, this@OpenStreetMapActivity)
            }
        }
    }

    private fun addDatabaseMarkers(map: MapView, coords: List<GeoPoint>, context: Context) {
        for (geoPoint in coords) {
            val marker = Marker(map)
            marker.position = geoPoint
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.icon = ContextCompat.getDrawable(context, android.R.drawable.ic_delete) as BitmapDrawable
            marker.title = "Saved Coordinate"
            map.overlays.add(marker)
        }
    }

}
