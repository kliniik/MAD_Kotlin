package es.upm.btb.madproject

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CommentActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tweetId: String

    private lateinit var tvTweetUser: TextView
    private lateinit var tvTweetMessage: TextView
    private lateinit var tvTweetCoords: TextView
    private lateinit var etComment: EditText
    private lateinit var btnPostComment: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comment)

        db = FirebaseFirestore.getInstance()
        tweetId = intent.getStringExtra("tweetId") ?: return

        loadTweetInfo()
        loadComments()

        tvTweetUser = findViewById(R.id.tvTweetUser)
        tvTweetMessage = findViewById(R.id.tvTweetMessage)
        tvTweetCoords = findViewById(R.id.tvTweetCoords)
        etComment = findViewById(R.id.etComment)
        btnPostComment = findViewById(R.id.btnPostComment)
        recyclerView = findViewById(R.id.recyclerComments)

        recyclerView.layoutManager = LinearLayoutManager(this)
        commentAdapter = CommentAdapter()
        recyclerView.adapter = commentAdapter

        btnPostComment.setOnClickListener {
            postComment()
        }

        // Set status bar color
        window.statusBarColor = ContextCompat.getColor(this, R.color.primaryColor)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.colorBottomNavBackground)

        // Set up the toolbar
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Set up the bottom navigation
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation_view)
        bottomNav.selectedItemId = R.id.navigation_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.navigation_map -> {
                    startActivity(Intent(this, OpenStreetMapActivity::class.java))
                    true
                }
                R.id.navigation_list -> {
                    startActivity(Intent(this, SecondActivity::class.java))
                    true
                }
                else -> false
            }.also { finish() }
        }

    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun loadTweetInfo() {
        db.collection("tweets").document(tweetId).get()
            .addOnSuccessListener { doc ->
                val tweet = doc.toObject(Tweet::class.java)
                tweet?.let {
                    tvTweetUser.text = it.userName
                    tvTweetMessage.text = it.message
                    tvTweetCoords.text = "Lat: ${it.latitude}, Lon: ${it.longitude}"
                    findViewById<TextView>(R.id.tvTweetTimestamp).text = formatTimestamp(it.timestamp)
                }
            }
    }

    private fun loadComments() {
        db.collection("tweets")
            .document(tweetId)
            .collection("comments")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                val comments = snapshot?.documents?.mapNotNull { it.toObject(Comment::class.java) } ?: emptyList()
                commentAdapter.submitList(comments)
            }
    }

    private fun postComment() {
        val text = etComment.text.toString().trim()
        val user = FirebaseAuth.getInstance().currentUser ?: return

        if (text.isEmpty()) return

        val comment = Comment(
            userId = user.uid,
            userName = user.displayName ?: "Anonymous",
            message = text,
            timestamp = System.currentTimeMillis()
        )

        db.collection("tweets")
            .document(tweetId)
            .collection("comments")
            .add(comment)
            .addOnSuccessListener {
                etComment.text.clear()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to post comment", Toast.LENGTH_SHORT).show()
            }
    }
}