package es.upm.btb.madproject

import android.content.Intent
import android.view.LayoutInflater
import com.google.firebase.firestore.FirebaseFirestore
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class TweetAdapter : RecyclerView.Adapter<TweetAdapter.TweetViewHolder>() {

    private val tweets = mutableListOf<Tweet>()
    private val tweetIds = mutableListOf<String>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TweetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tweet, parent, false)
        return TweetViewHolder(view)
    }

    override fun onBindViewHolder(holder: TweetViewHolder, position: Int) {
        val tweet = tweets[position]
        holder.bind(tweet)

        holder.itemView.setOnClickListener {
            val context = holder.itemView.context
            val tweetId = tweetIds[position]
            val intent = Intent(context, CommentActivity::class.java)
            intent.putExtra("tweetId", tweetId)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = tweets.size

    fun loadTweets() {
        db.collection("tweets")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                tweets.clear()
                tweetIds.clear()
                if (snapshot != null) {
                    for (doc in snapshot.documents) {
                        doc.toObject(Tweet::class.java)?.let {
                            tweets.add(it)
                            tweetIds.add(doc.id)
                        }
                    }
                    notifyDataSetChanged()
                }
            }
    }

    class TweetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private fun formatTimestamp(timestamp: Long): String {
            val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

        fun bind(tweet: Tweet) {
            itemView.findViewById<TextView>(R.id.tvUser).text = tweet.userName
            itemView.findViewById<TextView>(R.id.tvTimestamp).text = formatTimestamp(tweet.timestamp)
            itemView.findViewById<TextView>(R.id.tvCoordinates).text =
                itemView.context.getString(R.string.coordinates_format, tweet.latitude, tweet.longitude)
            itemView.findViewById<TextView>(R.id.tvMessage).text = tweet.message
        }
    }
}
