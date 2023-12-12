package com.udish.ebaysearch.utils

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.json.JSONArray
import org.json.JSONObject

class SharedViewModel : ViewModel() {
    val productMetaData = MutableLiveData<JSONObject>()
    val searchData = MutableLiveData<JSONObject>()
    val singleProductDetails = MutableLiveData<JSONObject>()
    val similarItems = MutableLiveData<JSONArray>()
    val additionalPhotos = MutableLiveData<JSONObject>()
    val productShare = MutableLiveData<Product>()  // For the shared product details
    val isInWishlist = MutableLiveData<Boolean>()  // To store the current product's wishlist status
    val wishlistStatusMap = MutableLiveData<Map<String, Boolean>>()
    val wishlistProducts = MutableLiveData<MutableList<Product>>(mutableListOf())

    fun updateWishlistStatus(productId: String, isInWishlist: Boolean) {
        val currentStatusMap = wishlistStatusMap.value.orEmpty()
        wishlistStatusMap.postValue(currentStatusMap + (productId to isInWishlist))
    }
    // Function to add or remove a product from the wishlist
    fun updateWishlist(product: Product, isInWishlist: Boolean) {
        val currentList = wishlistProducts.value ?: mutableListOf()
        if (isInWishlist) {
            // Add only if the product is not already in the list
            if (!currentList.any { it.productId == product.productId }) {
                currentList.add(product)
            }
        } else {
            currentList.removeAll { it.productId == product.productId }
        }
        wishlistProducts.postValue(currentList)
    }


    fun initSharedPreferences(context: Context) {
        val sharedPreferences = context.getSharedPreferences("WishlistPreferences", Context.MODE_PRIVATE)
        // Load wishlist from SharedPreferences
        val wishlistJson = sharedPreferences.getString("wishlist", "[]")
        val wishlistArray = JSONArray(wishlistJson)
        val loadedWishlist = mutableListOf<Product>()
        for (i in 0 until wishlistArray.length()) {
            val productJson = wishlistArray.getJSONObject(i)
            val product = Product.fromJson(productJson)
            loadedWishlist.add(product)
        }
        wishlistProducts.postValue(loadedWishlist)
    }

}