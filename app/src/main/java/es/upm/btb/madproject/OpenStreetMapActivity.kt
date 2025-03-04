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

    // Flood risk zones around the current Turia River
    private val gymkhanaCoords = listOf(
        GeoPoint(39.4358, -0.3183), // Turia River Mouth
        GeoPoint(39.4273, -0.3199), // Pinedo Beach
        GeoPoint(39.3911, -0.3165), // El Saler Wetlands
        GeoPoint(39.3541, -0.3190), // Albufera Natural Park
        GeoPoint(39.4441, -0.3442)  // Nazaret Neighborhood
    )

    private val gymkhanaNames = listOf(
        "Turia River Mouth",
        "Pinedo Beach",
        "El Saler Wetlands",
        "Albufera Natural Park",
        "Nazaret Neighborhood"
    )

    private val gymkhanaDescriptions = listOf(
        "The mouth of the Turia River, where heavy rains and storm surges can cause flooding, affecting nearby beaches and the Valencia Port area.",
        "A coastal area vulnerable to rising sea levels and river overflows, especially during storms and high tides.",
        "A natural reserve that absorbs excess water but is at risk during extreme weather events, potentially impacting wildlife and nearby infrastructure.",
        "A low-lying wetland area where river floods mix with seawater, threatening both agriculture and biodiversity.",
        "A historically flood-prone district near the river, where heavy rain can overwhelm drainage systems, causing urban flooding."
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
            //GeoPoint(location.latitude, location.longitude)
            //GeoPoint(39.4702, -0.3768) // Valencia, Spain
            GeoPoint(39.426714, -0.339140)
        } else {
            Log.d(TAG, "onCreate: Location is null, using default coordinates")
            //GeoPoint(40.389683644051864, -3.627825356970311)
            GeoPoint(39.426714, -0.339140) // Valencia, Spain
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
            ContextCompat.getDrawable(this, R.drawable.river_svgrepo_com) as BitmapDrawable
        marker.title = "Turia River, Valencia, Spain"
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
                    val resizedIcon = vectorToBitmapDrawable(context, R.drawable.danger_svgrepo_com, iconSize, iconSize)
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
