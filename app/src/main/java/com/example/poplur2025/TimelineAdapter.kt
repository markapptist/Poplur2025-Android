package com.example.poplur2025

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
// Import Glide or Picasso if you use them for image loading
// import com.bumptech.glide.Glide 
import com.example.poplur2025.R // Your R file
import com.example.poplur2025.Post // Your Post data class

class TimelineAdapter(private val onPostClicked: (Post) -> Unit) :
    ListAdapter<Post, TimelineAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_timeline, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
        holder.itemView.setOnClickListener { onPostClicked(post) }
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val usernameTextView: TextView = itemView.findViewById(R.id.tv_username)
        private val timestampTextView: TextView = itemView.findViewById(R.id.tv_timestamp)
        private val contentTextView: TextView = itemView.findViewById(R.id.tv_post_content)
        private val userAvatarImageView: ImageView = itemView.findViewById(R.id.iv_user_avatar)
        private val postImageView: ImageView = itemView.findViewById(R.id.iv_post_image)
        private val locationNameTextView: TextView = itemView.findViewById(R.id.tv_location_name)

        fun bind(post: Post) {
            usernameTextView.text = post.username
            // Format timestamp (e.g., "2 hours ago")
            timestampTextView.text = DateUtils.getRelativeTimeSpanString(
                post.timestamp,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            )
            contentTextView.text = post.textContent
            contentTextView.isVisible = !post.textContent.isNullOrEmpty()

            // Load user avatar (using Glide or Picasso)
            // Glide.with(itemView.context).load(post.userAvatarUrl).circleCrop().placeholder(R.drawable.ic_default_avatar).into(userAvatarImageView)
            if (post.userAvatarUrl != null) {
                // TODO: Load image with Glide/Picasso
            } else {
                userAvatarImageView.setImageResource(R.drawable.ic_launcher_foreground) // Placeholder
            }


            if (post.imageUrl != null) {
                postImageView.isVisible = true
                // Load post image (using Glide or Picasso)
                // Glide.with(itemView.context).load(post.imageUrl).centerCrop().into(postImageView)
                // TODO: Load image with Glide/Picasso
            } else {
                postImageView.isVisible = false
            }

            locationNameTextView.text = post.locationName
            locationNameTextView.isVisible = !post.locationName.isNullOrEmpty()
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.postId == newItem.postId
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}