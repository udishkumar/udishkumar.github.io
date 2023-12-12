package com.udish.ebaysearch.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.udish.ebaysearch.MainActivity
import com.udish.ebaysearch.R
import com.udish.ebaysearch.utils.Product
import com.udish.ebaysearch.utils.SharedViewModel

class WishlistAdapter(
    private val context: Context,
    private var wishlistItems: MutableList<Product>,
    private val sharedViewModel: SharedViewModel,
    private val mainActivity: MainActivity
) : RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder>() {

    class WishlistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageViewProduct: ImageView = view.findViewById(R.id.ivProductImage)
        val textViewProductName: TextView = view.findViewById(R.id.tvProductName)
        val textViewZipCode: TextView = view.findViewById(R.id.tvProductZip)
        val textViewShippingType: TextView = view.findViewById(R.id.tvProductShippingType)
        val textViewProductCondition: TextView = view.findViewById(R.id.tvProductCondition)
        val textViewProductPrice: TextView = view.findViewById(R.id.tvProductPrice)
        val buttonRemove: ImageButton = view.findViewById(R.id.btnWishlist)
        // Add other views if present in your layout
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishlistViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_product_card, parent, false)
        return WishlistViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: WishlistViewHolder, position: Int) {
        val product = wishlistItems[position]

        // Set the remove icon for wishlist items since they are already in the wishlist
        holder.buttonRemove.setImageResource(R.drawable.cart_remove)
        holder.buttonRemove.tag = true

        Glide.with(holder.imageViewProduct.context).load(product.productImage).placeholder(R.drawable.placeholder).into(holder.imageViewProduct)
        holder.textViewProductName.text = product.productName
        holder.textViewZipCode.text = product.zipCode
        holder.textViewShippingType.text = clipShippingType(product.shippingType)
        holder.textViewProductCondition.text = clipCondition(product.productCondition)
        holder.textViewProductPrice.text = product.productPrice

        holder.buttonRemove.setOnClickListener {
            Log.d("WishlistAdapter", "Removing product: ${product.productId}")
            removeProductFromWishlist(product, position) // Pass the product here
        }
    }

    override fun getItemCount(): Int {
        return wishlistItems.size
    }

    private fun checkProductInWishlist(context: Context, productId: String, onResult: (Boolean) -> Unit) {
        val url = "https://uk-hw3.wl.r.appspot.com/wishlist/$productId"
        val requestQueue = Volley.newRequestQueue(context)
        val stringRequest = StringRequest(Request.Method.GET, url,
            { response ->
                val isInWishlist = !response.contains("Product not found")
                onResult(isInWishlist)
            },
            { error ->
                Log.e("VolleyError", "Error: ${error.message}")
                onResult(false)
            })
        requestQueue.add(stringRequest)
    }

    private fun removeProductFromWishlist(product: Product, position: Int) {
        val productId = product.productId
        val url = "https://uk-hw3.wl.r.appspot.com/wishlist/$productId"
        val requestQueue = Volley.newRequestQueue(context)
        val stringRequest = StringRequest(Request.Method.DELETE, url,
            { response ->
                showCustomEllipsizedToast(context, product.productName, false)
                wishlistItems.removeAt(position)
                notifyItemRemoved(position)
                sharedViewModel.updateWishlist(product, false)
                mainActivity.saveWishlistToSharedPreferences(wishlistItems)
            },
            { error ->
                // Handle error
            })
        requestQueue.add(stringRequest)
    }

    fun showCustomEllipsizedToast(context: Context, productName: String, isAdded: Boolean, maxLength: Int = 40) {
        val actionPart = if (isAdded) " was added to wishlist" else " was removed from wishlist"
        val ellipsis = "..."

        // Truncate and ellipsize the product name after the first 10 characters
        val ellipsizedProductName = if (productName.length > 10) {
            productName.substring(0, 10) + ellipsis
        } else {
            productName
        }

        // Construct the full message
        val fullMessage = "$ellipsizedProductName$actionPart"

        // Show the toast
        Toast.makeText(context, fullMessage, Toast.LENGTH_SHORT).show()
    }

    private fun clipCondition(text: String): String {
        val keywords = listOf("New", "Used", "Good", "Very Good", "Acceptable")
        for (keyword in keywords) {
            if (text.contains(keyword, ignoreCase = true)) {
                return keyword
            }
        }
        return "Used"
    }

    private fun clipShippingType(text: String): String {
        return if (text.length > 4) text.substring(0, 4) else text
    }

    // Add this method in WishlistAdapter
    fun calculateTotal(): Pair<Int, Double> {
        var totalCount = 0
        var totalPrice = 0.0
        wishlistItems.forEach { product ->
            totalCount++
            totalPrice += product.productPrice.toDoubleOrNull() ?: 0.0
        }
        return Pair(totalCount, totalPrice)
    }
}
