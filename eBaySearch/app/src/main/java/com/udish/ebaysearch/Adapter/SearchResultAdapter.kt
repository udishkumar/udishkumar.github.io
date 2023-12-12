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
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.udish.ebaysearch.MainActivity
import com.udish.ebaysearch.R
import com.udish.ebaysearch.utils.Product
import com.udish.ebaysearch.utils.SharedViewModel
import org.json.JSONObject

class SearchResultAdapter(private val productList: List<Product>, private val sharedViewModel: SharedViewModel, private val itemClickListener: (Product) -> Unit, private val lifecycleOwner: LifecycleOwner, private val mainActivity: MainActivity
) : RecyclerView.Adapter<SearchResultAdapter.ProductViewHolder>() {

    class ProductViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageViewProduct: ImageView = view.findViewById(R.id.ivProductImage)
        val textViewProductName: TextView = view.findViewById(R.id.tvProductName)
        val textViewZipCode: TextView = view.findViewById(R.id.tvProductZip)
        val textViewShippingType: TextView = view.findViewById(R.id.tvProductShippingType)
        val textViewProductCondition: TextView = view.findViewById(R.id.tvProductCondition)
        val textViewProductPrice: TextView = view.findViewById(R.id.tvProductPrice)
        val buttonBuy: ImageButton = view.findViewById(R.id.btnWishlist)
    }

    init {
        sharedViewModel.wishlistStatusMap.observe(lifecycleOwner) { wishlistStatusMap ->
            productList.forEach { product ->
                val isInWishlist = wishlistStatusMap[product.productId] ?: false
                val position = productList.indexOf(product)
                notifyItemChanged(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_product_card, parent, false)
        return ProductViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        // Initially check if the product is in the wishlist
        checkProductInWishlist(holder.itemView.context, product.productId) { isInWishlist ->
            holder.buttonBuy.setImageResource(if (isInWishlist) R.drawable.cart_remove else R.drawable.cart_plus)
            holder.buttonBuy.tag = isInWishlist
        }

        Glide.with(holder.imageViewProduct.context).load(product.productImage).placeholder(R.drawable.placeholder).into(holder.imageViewProduct)
        holder.textViewProductName.text = product.productName
        holder.textViewZipCode.text = product.zipCode
        holder.textViewShippingType.text = clipShippingType(product.shippingType)
        holder.textViewProductCondition.text = clipCondition(product.productCondition)
        holder.textViewProductPrice.text = product.productPrice

        holder.itemView.setOnClickListener {
            itemClickListener(product)

            // Check if the product is in the wishlist and post to SharedViewModel
            checkProductInWishlist(holder.itemView.context, product.productId) { isInWishlist ->
                sharedViewModel.productShare.postValue(product)  // Post the product as 'productShare'
                sharedViewModel.isInWishlist.postValue(isInWishlist)
            }
        }

        holder.buttonBuy.setOnClickListener {
            val productInWishlist = holder.buttonBuy.tag as Boolean
            if (productInWishlist) {
                // Remove product from wishlist
                deleteProductFromWishlist(holder.itemView.context, product.productId) {
                    holder.buttonBuy.setImageResource(R.drawable.cart_plus)
                    holder.buttonBuy.tag = false
                    sharedViewModel.updateWishlist(product, false)
                    mainActivity.saveWishlistToSharedPreferences(sharedViewModel.wishlistProducts.value.orEmpty())
                    showCustomEllipsizedToast(holder.itemView.context, product.productName, false)
                }
            } else {
                // Add product to wishlist
                val jsonObject = JSONObject().apply {
                    put("product_id", product.productId)
                    put("product_image", product.productImage)
                    put("product_name", product.productName)
                    put("product_price", product.productPrice)
                    put("shippingType", product.shippingType)
                }
                sendPostRequest(holder.itemView.context, jsonObject) {
                    holder.buttonBuy.setImageResource(R.drawable.cart_remove)
                    holder.buttonBuy.tag = true
                    sharedViewModel.updateWishlist(product, true)
                    mainActivity.saveWishlistToSharedPreferences(sharedViewModel.wishlistProducts.value.orEmpty())
                    showCustomEllipsizedToast(holder.itemView.context, product.productName, true)
                }
            }
        }
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

    private fun sendPostRequest(context: Context, postData: JSONObject, onSuccess: () -> Unit) {
        val url = "https://uk-hw3.wl.r.appspot.com/wishlist"
        val requestQueue = Volley.newRequestQueue(context)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, postData,
            { response ->
                Log.d("VolleyResponse", "Response: $response")
                onSuccess()
            },
            { error ->
                Log.e("VolleyError", "Error: ${error.message}")
            })
        requestQueue.add(jsonObjectRequest)
    }

    private fun deleteProductFromWishlist(context: Context, productId: String, onSuccess: () -> Unit) {
        val url = "https://uk-hw3.wl.r.appspot.com/wishlist/$productId"
        val requestQueue = Volley.newRequestQueue(context)
        val stringRequest = StringRequest(Request.Method.DELETE, url,
            { response ->
                Log.d("VolleyResponse", "Response: $response")
                onSuccess()
            },
            { error ->
                Log.e("VolleyError", "Error: ${error.message}")
            })
        requestQueue.add(stringRequest)
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

    override fun getItemCount() = productList.size
}
