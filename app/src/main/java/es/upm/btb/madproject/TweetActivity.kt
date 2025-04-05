package es.upm.btb.madproject

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class TweetActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tweetAdapter: TweetAdapter
    private lateinit var fabAddTweet: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tweet)

        recyclerView = findViewById(R.id.recyclerTweets)
        fabAddTweet = findViewById(R.id.fabAddTweet)

        tweetAdapter = TweetAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = tweetAdapter

        tweetAdapter.loadTweets()

        fabAddTweet.setOnClickListener {
            showTweetDialog()
        }
    }

    private fun showTweetDialog() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_new_tweet, null)

        val etTweet = view.findViewById<EditText>(R.id.etTweetDialog)
        val tvHeader = view.findViewById<TextView>(R.id.tvDialogHeader)

        val (locationName, lat, lon) = getUserLocationData()
        tvHeader.text = "${user.displayName ?: "Anonymous"} in $locationName\n($lat, $lon)"

        AlertDialog.Builder(this)
            .setTitle("New Tweet")
            .setView(view)
            .setPositiveButton("Post") { _, _ ->
                val message = etTweet.text.toString().trim()
                if (message.isNotEmpty()) {
                    val tweet = Tweet(
                        userId = user.uid,
                        userName = user.displayName ?: "Anonymous",
                        message = message,
                        location = locationName,
                        latitude = lat,
                        longitude = lon,
                        timestamp = System.currentTimeMillis()
                    )
                    FirebaseFirestore.getInstance()
                        .collection("tweets")
                        .add(tweet)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Tweet posted!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Error posting tweet", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun getUserLocationData(): Triple<String, Double, Double> {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) return Triple("Unknown", 0.0, 0.0)

        val loc: Location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) ?: return Triple("Unknown", 0.0, 0.0)
        val geocoder = Geocoder(this, Locale.getDefault())
        val address = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)

        val locationName = if (!address.isNullOrEmpty()) {
            "${address[0].locality ?: "City"}, ${address[0].countryName ?: "Country"}"
        } else "Unknown"

        return Triple(locationName, loc.latitude, loc.longitude)
    }
}