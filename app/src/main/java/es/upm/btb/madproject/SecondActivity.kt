package es.upm.btb.madproject

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SecondActivity : AppCompatActivity() {
    private val TAG = "SecondActivity"

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
                lines.map {
                    val data = it.split(";").map(String::trim)
                    Log.d("CSV_DATA", "Zeile: $data") // Debugging
                    data
                }.toList()
            }
        } catch (e: IOException) {
            listOf(listOf("Error reading file: ${e.message}"))
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
            if (item.size < 4) {
                timestampTextView.text = formatTimestamp(item[0].toLong())
                latitudeTextView.text = formatCoordinate(item[1].toDouble())
                longitudeTextView.text = formatCoordinate(item[2].toDouble())
                altitudeTextView.text = formatCoordinate(item[3].toDouble())
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