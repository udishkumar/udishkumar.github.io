package com.udish.ebaysearch.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.udish.ebaysearch.R

class GooglePhotosAdapter(private val photoUrls: List<String>) : RecyclerView.Adapter<GooglePhotosAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val photoImageView: ImageView = view.findViewById(R.id.photoImageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_card, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val imageUrl = photoUrls[position]
        // Load the image into the ImageView, for example using Glide or Picasso
        Glide.with(holder.photoImageView.context)
            .load(imageUrl)
            .placeholder(R.drawable.placeholder)
            .into(holder.photoImageView)
    }

    override fun getItemCount() = photoUrls.size
}