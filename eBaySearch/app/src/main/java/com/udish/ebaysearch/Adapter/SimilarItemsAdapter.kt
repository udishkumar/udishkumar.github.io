package com.udish.ebaysearch.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.udish.ebaysearch.R
import com.udish.ebaysearch.utils.SimilarItem

class SimilarItemsAdapter(private var items: MutableList<SimilarItem>) : RecyclerView.Adapter<SimilarItemsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivProductPhoto: ImageView = view.findViewById(R.id.ivProductPhoto)
        val productName: TextView = view.findViewById(R.id.tvProductName)
        val shippingCost: TextView = view.findViewById(R.id.tvShippingCost)
        val daysLeft: TextView = view.findViewById(R.id.tvDaysLeft)
        val price: TextView = view.findViewById(R.id.tvPrice)
        // Add more views if needed
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_similar, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        Glide.with(holder.ivProductPhoto.context)
            .load(item.image_url)
            .placeholder(R.drawable.placeholder) // Replace with your placeholder
            .into(holder.ivProductPhoto)

        holder.productName.text = item.product_name
        holder.shippingCost.text = item.shipping_cost
        holder.daysLeft.text = item.daysLeft+" Days Left"
        holder.price.text = item.price
    }

    override fun getItemCount() = items.size
    fun updateData(newItems: MutableList<SimilarItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    fun sortItems(sortType: String, sortOrder: String) {
        when (sortType) {
            "Name" -> {
                if (sortOrder == "Ascending") {
                    items.sortBy { it.product_name }
                } else {
                    items.sortByDescending { it.product_name }
                }
            }
            "Days" -> {
                if (sortOrder == "Ascending") {
                    items.sortBy { it.daysLeft.toIntOrNull() ?: Int.MAX_VALUE }
                } else {
                    items.sortByDescending { it.daysLeft.toIntOrNull() ?: Int.MIN_VALUE }
                }
            }
            "Price" -> {
                if (sortOrder == "Ascending") {
                    items.sortBy { it.price.toDoubleOrNull() ?: Double.MAX_VALUE }
                } else {
                    items.sortByDescending { it.price.toDoubleOrNull() ?: Double.MIN_VALUE }
                }
            }
//            "Shipping Cost" -> {
//                if (sortOrder == "Ascending") {
//                    items.sortBy { it.shipping_cost.toDoubleOrNull() ?: Double.MAX_VALUE }
//                } else {
//                    items.sortByDescending { it.shipping_cost.toDoubleOrNull() ?: Double.MIN_VALUE }
//                }
//            }
            // Add additional cases as needed for other sorting criteria
        }
        notifyDataSetChanged()
    }
}
