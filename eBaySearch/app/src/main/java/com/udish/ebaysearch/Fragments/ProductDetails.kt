package com.udish.ebaysearch.fragments

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.udish.ebaysearch.MainActivity
import com.udish.ebaysearch.R
import com.udish.ebaysearch.adapter.FourTabsAdapter
import com.udish.ebaysearch.utils.NonSwipeableViewPager
import com.udish.ebaysearch.utils.Product
import com.udish.ebaysearch.utils.SharedViewModel
import org.json.JSONArray
import org.json.JSONObject

class ProductDetails : Fragment() {
    private lateinit var tabLayout: TabLayout
//    private lateinit var viewPager: ViewPager
    private lateinit var viewPager: NonSwipeableViewPager
    private lateinit var productName: TextView
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var keywords: String
    private lateinit var product_name: String
    private lateinit var backButton: ImageView
    private lateinit var facebookButton: ImageView


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_product_details, container, false)
        backButton = view.findViewById(R.id.backButton)
        facebookButton = view.findViewById(R.id.facebookButton)

        facebookButton.setOnClickListener {
            // Assuming productUrl is the URL you want to share
            val productUrl = sharedViewModel.productShare.value?.productUrl ?: ""
            val hashtag = getText(R.string.facebook_text) // Replace 'YourHashtag' with the hashtag you want to include
            val encodedHashtag = Uri.encode(hashtag.toString()) // Encoding the hashtag to ensure it's URL-safe
            val shareUrl = "https://www.facebook.com/sharer/sharer.php?u=$productUrl&hashtag=$encodedHashtag"

            // Intent to open the URL in the browser
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(shareUrl))
            startActivity(browserIntent)
        }


        backButton.setOnClickListener {
            fragmentManager?.popBackStack()
        }

        val fabWishlist = view.findViewById<FloatingActionButton>(R.id.fabWishlist)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedViewModel.isInWishlist.observe(viewLifecycleOwner) { isInWishlist ->
            fabWishlist.setImageResource(if (isInWishlist) R.drawable.cart_remove else R.drawable.cart_plus)
        }
        tabLayout = view.findViewById(R.id.tabLayout)
        viewPager = view.findViewById(R.id.viewPager)

// Add tabs
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.information_variant).setText("PRODUCT"))
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.truck_delivery).setText("SHIPPING"))
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.google).setText("PHOTOS"))
        tabLayout.addTab(tabLayout.newTab().setIcon(R.drawable.equal).setText("SIMILAR"))
        tabLayout.tabMode = TabLayout.MODE_FIXED
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

// Set up the adapter
        val adapter = FourTabsAdapter(requireContext(), childFragmentManager, tabLayout.tabCount)
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))

// Select the first tab by default
        viewPager.currentItem = 0
        tabLayout.getTabAt(0)?.select()

        val selectedColor = ContextCompat.getColor(requireContext(), R.color.selected_tab_color)
        val unselectedColor = ContextCompat.getColor(requireContext(), R.color.unselected_tab_icon_color)

// Color the first tab icon as selected
        colorTabIcon(tabLayout.getTabAt(0)!!, selectedColor)

// Color other tab icons as unselected
        for (i in 1 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(i)
            tab?.let { colorTabIcon(it, unselectedColor) }
        }

        // Inside ProductDetails.kt

        fabWishlist.setOnClickListener {
            val currentProduct = sharedViewModel.productShare.value
            val isInWishlist = sharedViewModel.isInWishlist.value ?: false
            currentProduct?.let {
                sharedViewModel.updateWishlist(it, !isInWishlist)
                if (isInWishlist) {
                    // Product is currently in the wishlist, so remove it
                    showCustomEllipsizedToast(requireContext(), it.productName, false)
                    currentProduct.productId?.let {
                        deleteProductFromWishlist(it)
                        fabWishlist.setImageResource(R.drawable.cart_plus)  // Change icon to 'cart_plus'
                        sharedViewModel.isInWishlist.postValue(false)  // Update LiveData
                        sharedViewModel.updateWishlist(currentProduct, false) // Update wishlistProducts
                    }
                } else {
                    // Product is not in the wishlist, so add it
                    showCustomEllipsizedToast(requireContext(), it.productName, true)
                    addProductToWishlist(it)
                    fabWishlist.setImageResource(R.drawable.cart_remove)  // Change icon to 'cart_remove'
                    sharedViewModel.isInWishlist.postValue(true)  // Update LiveData
                    sharedViewModel.updateWishlist(it, true) // Update wishlistProducts
                }

                // Save the updated wishlist to shared preferences
                (activity as? MainActivity)?.saveWishlistToSharedPreferences(sharedViewModel.wishlistProducts.value.orEmpty())
            }
        }

// Add tab selected listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    viewPager.currentItem = it.position
                    colorTabIcon(it, selectedColor)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.let { colorTabIcon(it, unselectedColor) }
            }
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        keywords = arguments?.getString("keywords") ?: "default"
        val product = arguments?.getParcelable<Product>("product")
        product?.let {
            val productJson = JSONObject().apply {
                put("productId", it.productId)
                put("productName", it.productName)
                put("productImage", it.productImage)
                put("productUrl", it.productUrl)
                put("productCategory", it.productCategory)
                put("productCondition", it.productCondition)
                put("productPrice", it.productPrice)
                put("topRated", it.topRated)
                put("shippingCost", it.shippingCost)
                put("shippingLocations", it.shippingLocations)
                put("handlingTime", it.handlingTime)
                put("expeditedShipping", it.expeditedShipping)
                put("oneDayShipping", it.oneDayShipping)
                put("returnAccepted", it.returnAccepted)
                put("feedbackScore", it.feedbackScore)
                put("popularity", it.popularity)
                put("feedbackRatingStar", it.feedbackRatingStar)
                put("storeName", it.storeName)
                put("buyProductAt", it.buyProductAt)
                put("shippingType", it.shippingType)
                put("zipCode", it.zipCode)
                put("seller", it.seller)
            }
            sharedViewModel.productMetaData.postValue(productJson)
        }
        productName = view.findViewById(R.id.productName)
        productName.text = product?.productName
        product_name = product?.productName.toString()

        product?.let {
            fetchProductDetails(it.productId) { productDetails ->
                fetchAdditionalPhotos { additionalPhotos ->
                    updateUI(productDetails, additionalPhotos)
                }
            }
        }
        return view
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

    private fun addProductToWishlist(product: Product) {
        val postData = JSONObject().apply {
            put("product_id", product.productId)
            put("product_image", product.productImage)
            put("product_name", product.productName)
            put("product_price", product.productPrice)
            put("shippingType", product.shippingType)
        }

        val url = "https://uk-hw3.wl.r.appspot.com/wishlist"
        val requestQueue = Volley.newRequestQueue(requireContext())
        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, postData,
            { response ->
                // Successfully added to wishlist
                // You can perform additional actions here if needed
                Log.d("VolleyResponse", "Product added to wishlist: $response")
            },
            { error ->
                // Error occurred
                Log.e("VolleyError", "Error adding product to wishlist: ${error.message}")
            })
        requestQueue.add(jsonObjectRequest)
        sharedViewModel.updateWishlistStatus(product.productId, true)
    }

    private fun deleteProductFromWishlist(productId: String) {
        val url = "https://uk-hw3.wl.r.appspot.com/wishlist/$productId"
        val requestQueue = Volley.newRequestQueue(requireContext())
        val stringRequest = StringRequest(Request.Method.DELETE, url,
            { response ->
                // Successfully removed from wishlist
                // You can perform additional actions here if needed
                Log.d("VolleyResponse", "Product removed from wishlist: $response")
            },
            { error ->
                // Error occurred
                Log.e("VolleyError", "Error removing product from wishlist: ${error.message}")
            })
        requestQueue.add(stringRequest)
        sharedViewModel.updateWishlistStatus(productId, false)
    }

    private fun colorTabIcon(tab: TabLayout.Tab, @ColorInt color: Int) {
        tab.icon?.setTint(color)
    }

    private fun fetchProductDetails(productId: String, callback: (JSONObject) -> Unit) {
        val url = "https://uk-hw3.wl.r.appspot.com/getEbayData/$productId"
        val requestQueue = Volley.newRequestQueue(context)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                callback(response)
            },
            { error ->
                error.printStackTrace()
            }
        )
        requestQueue.add(jsonObjectRequest)
    }

    private fun fetchAdditionalPhotos(callback: (JSONObject) -> Unit) {
        val url = "https://uk-hw3.wl.r.appspot.com/getPhotos/$product_name"
        val requestQueue = Volley.newRequestQueue(context)
        val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
            { response ->
                callback(response)
            },
            { error ->
                error.printStackTrace()
            }
        )
        requestQueue.add(jsonObjectRequest)
    }

    private fun updateUI(productDetailsJson: JSONObject, additionalPhotosJson: JSONObject) {

        // Splitting the JSON response
        val similarItemsArray = productDetailsJson.optJSONArray("similarItems") ?: JSONArray()
        productDetailsJson.remove("similarItems")

        // Posting values to LiveData in ViewModel
        sharedViewModel.singleProductDetails.postValue(productDetailsJson)
        sharedViewModel.similarItems.postValue(similarItemsArray)
        // Sending additional photos to the GooglePhotos fragment
        sharedViewModel.additionalPhotos.postValue(additionalPhotosJson)

        // You can also handle additionalPhotosJson here if needed
        // For example, if you want to display these photos somewhere in UI.
        // Assuming you have some method to handle this:
        // displayAdditionalPhotos(additionalPhotosJson)
    }

}
