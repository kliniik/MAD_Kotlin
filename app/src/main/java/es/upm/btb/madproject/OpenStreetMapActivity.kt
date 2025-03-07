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
import org.osmdroid.views.overlay.Polyline
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import com.google.android.material.bottomnavigation.BottomNavigationView

class OpenStreetMapActivity : AppCompatActivity() {
    private lateinit var map: MapView

    // Flood risk zones in Valencia
    private val gymkhanaCoords = listOf(
        GeoPoint(39.4699, -0.3774), // Valencia Zentrum
        GeoPoint(39.4636, -0.3783), // Turia Bridge
        GeoPoint(39.4598, -0.3751), // Turia Gardens
        GeoPoint(39.4573, -0.3710), // Albufera Bridge
        GeoPoint(39.4605, -0.3845)  // Turia-Flussgebiet
    )

    private val gymkhanaNames = listOf(
        "Valencia Zentrum",
        "Turia Bridge",
        "Turia Gardens",
        "Albufera Bridge",
        "Turia-Flussgebiet"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_open_street_map)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // OpenStreetMap konfigurieren
        Configuration.getInstance().userAgentValue = packageName

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.controller.setZoom(14.0) // 🟢 Startzoom auf Valencia setzen
        map.controller.setCenter(gymkhanaCoords[0]) // 🟢 Kamera auf Valencia setzen
        map.setMultiTouchControls(true)

        // Markierungen hinzufügen
        addGymkhanaMarkers()

        // Route zwischen Markern zeichnen
        drawPolyline()

        val bottomNavigationView: BottomNavigationView = findViewById(R.id.bottom_navigation_view)
        bottomNavigationView.selectedItemId = R.id.navigation_list // Markiert "Liste" als aktiv

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

    private fun addGymkhanaMarkers() {
        for (i in gymkhanaCoords.indices) {
            val marker = Marker(map)
            marker.position = gymkhanaCoords[i]
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            marker.title = gymkhanaNames[i]
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
}
