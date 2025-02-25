package es.upm.btb.madproject

import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import android.content.Context
import org.osmdroid.views.overlay.Polyline
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.VectorDrawable
import android.graphics.drawable.Drawable


class OpenStreetMapActivity : AppCompatActivity() {
    private val TAG = "btaOpenStreetMapActivity"
    private lateinit var map: MapView

    private val gymkhanaCoords = listOf(
        GeoPoint(40.38779608214728, -3.627687914352839), // Tennis
        GeoPoint(40.38788595319803, -3.627048250272035), // Futsal outdoors
        GeoPoint(40.3887315224542, -3.628643539758645), // Fashion and design
        GeoPoint(40.38926842612264, -3.630067893975619), // Topos
        GeoPoint(40.38956358584258, -3.629046081389352), // Teleco
        GeoPoint(40.38992125672989, -3.6281366497769714), // ETSISI
        GeoPoint(40.39037466191718, -3.6270256763598447), // Library
        GeoPoint(40.389855884803005, -3.626782180787362) // CITSEM
    )
    private val gymkhanaNames = listOf(
        "Tennis",
        "Futsal Outdoors",
        "Fashion and Design School",
        "Topography School",
        "Telecommunications School",
        "ETSISI",
        "Library",
        "CITSEM"
    )

    private val gymkhanaDescriptions = listOf(
        "Outdoor tennis courts for practice and matches.",
        "Open-air futsal field for small-sided games.",
        "Institution focused on fashion and creative design studies.",
        "School specializing in land surveying and mapping technologies.",
        "Faculty dedicated to communication and network technologies.",
        "School of Information Systems Engineering and Informatics.",
        "A resource center with books, study spaces, and digital materials.",
        "Research center for technology and applied sciences."
    )


    private val gymkhanaIcons = listOf(
        R.drawable.icon_tennis,
        R.drawable.icon_futsal,
        R.drawable.icon_fashion,
        R.drawable.icon_topography,
        R.drawable.icon_telecom,
        R.drawable.icon_etsisi,
        R.drawable.icon_library,
        R.drawable.icon_citsem
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_open_street_map)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Configure the user agent before loading the configuration
        Configuration.getInstance().userAgentValue = "es.upm.btb.madproject"
        Configuration.getInstance()
            .load(applicationContext, getSharedPreferences("osm", MODE_PRIVATE))

        Log.d(TAG, "OSMDroid Cache Directory: ${cacheDir.absolutePath}")

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
        } else {
            Log.d(TAG, "onCreate: Location is null, using default coordinates")
            GeoPoint(40.389683644051864, -3.627825356970311)
        }

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.controller.setZoom(18.0)
        map.controller.setCenter(startPoint)
        map.setMultiTouchControls(true)

        // Add starting point marker
        val marker = Marker(map)
        marker.position = startPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.icon =
            ContextCompat.getDrawable(this, android.R.drawable.ic_delete) as BitmapDrawable
        marker.title = "My current location"
        map.overlays.add(marker)

        // Add list of markers
        addGymkhanaMarkers(map, gymkhanaCoords, gymkhanaNames, gymkhanaIcons, gymkhanaDescriptions, this)
        addRouteMarkers(map, gymkhanaCoords, this)
    }

    private fun addGymkhanaMarkers(
        map: MapView,
        coords: List<GeoPoint>,
        names: List<String>,
        icons: List<Int>,
        descriptions: List<String>,
        context: Context
    ) {
        for (i in coords.indices) {
            val iconSize = 50

            for (i in coords.indices) {
                val marker = Marker(map)
                marker.position = coords[i]
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                // marker.icon = ContextCompat.getDrawable(context, android.R.drawable.ic_menu_compass) as BitmapDrawable

                if (i < icons.size) {
                    val resizedIcon = vectorToBitmapDrawable(context, icons[i], iconSize, iconSize)
                    if (resizedIcon != null) {
                        marker.icon = resizedIcon
                    } else {
                        Log.e(TAG, "addGymkhanaMarkers: Error resizing icon for ${names[i]}")
                    }
                } else {
                    Log.e(TAG, "addGymkhanaMarkers: No icon for ${names[i]}")
                }
                //marker.icon = ContextCompat.getDrawable(context, icons[i]) as BitmapDrawable
                marker.title = names[i]
                marker.snippet = descriptions[i]
                map.overlays.add(marker)
            }
        }
        map.invalidate()
    }

    private fun addRouteMarkers(map: MapView, coords: List<GeoPoint>, context: Context) {
        val polyline = Polyline()
        polyline.setPoints(coords)
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
}
