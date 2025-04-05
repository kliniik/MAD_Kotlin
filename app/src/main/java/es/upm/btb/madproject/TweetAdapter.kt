package es.upm.btb.madproject

import android.view.LayoutInflater
import com.google.firebase.firestore.FirebaseFirestore
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.Query

class TweetAdapter : RecyclerView.Adapter<TweetAdapter.TweetViewHolder>() {

    private val tweets = mutableListOf<Tweet>()
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TweetViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tweet, parent, false)
        return TweetViewHolder(view)
    }

    override fun onBindViewHolder(holder: TweetViewHolder, position: Int) {
        val tweet = tweets[position]
        holder.bind(tweet)
    }

    override fun getItemCount(): Int = tweets.size

    fun loadTweets() {
        db.collection("tweets")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                tweets.clear()
                if (snapshot != null) {
                    for (doc in snapshot.documents) {
                        doc.toObject(Tweet::class.java)?.let { tweets.add(it) }
                    }
                    notifyDataSetChanged()
                }
            }
    }

    class TweetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(tweet: Tweet) {
            itemView.findViewById<TextView>(R.id.tvUser).text = tweet.userName
            itemView.findViewById<TextView>(R.id.tvCoordinates).text = "Lat: ${tweet.latitude}, Lon: ${tweet.longitude}"
            itemView.findViewById<TextView>(R.id.tvMessage).text = tweet.message
        }
    }
}