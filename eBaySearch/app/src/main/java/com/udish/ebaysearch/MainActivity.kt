package com.udish.ebaysearch

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.viewpager.widget.ViewPager
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.tabs.TabLayout
import com.udish.ebaysearch.adapter.TwoTabsAdapter
import com.udish.ebaysearch.utils.Product
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Thread.sleep(2000)
        installSplashScreen()
        setContentView(R.layout.activity_main)
        sharedPreferences = getSharedPreferences("EbaySearchApp", Context.MODE_PRIVATE)

        tabLayout = findViewById(R.id.tabLayout)
        viewPager = findViewById(R.id.viewPager)

        tabLayout.addTab(tabLayout.newTab().setText(getText(R.string.search_title)))
        tabLayout.addTab(tabLayout.newTab().setText(getText(R.string.wishlist_title)))
        tabLayout.tabGravity = TabLayout.GRAVITY_FILL

        val adapter = TwoTabsAdapter(this, supportFragmentManager, tabLayout.tabCount)
        viewPager.adapter = adapter
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))

        val selectedColor = ContextCompat.getColor(this@MainActivity, R.color.selected_tab_color)
        val unselectedColor = ContextCompat.getColor(this@MainActivity, R.color.unselected_tab_color)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                viewPager.currentItem = tab!!.position
                tabLayout.setTabTextColors(unselectedColor, selectedColor)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tabLayout.setTabTextColors(unselectedColor, unselectedColor)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    fun saveWishlistToSharedPreferences(wishlist: List<Product>) {
        val jsonArray = JSONArray()
        wishlist.forEach { product ->
            jsonArray.put(product.toJson())
        }
        sharedPreferences.edit().putString("wishlist", jsonArray.toString()).apply()
    }

    fun loadWishlistFromSharedPreferences(): MutableList<Product> {
        val wishlistJson = sharedPreferences.getString("wishlist", "[]")
        val jsonArray = JSONArray(wishlistJson)
        val wishlist = mutableListOf<Product>()
        for (i in 0 until jsonArray.length()) {
            val productJson = jsonArray.getJSONObject(i)
            wishlist.add(Product.fromJson(productJson))
        }
        return wishlist
    }

    fun verifyWishlistWithMongoDB(wishlist: MutableList<Product>, onUpdate: (MutableList<Product>) -> Unit) {
        val requestQueue = Volley.newRequestQueue(this)
        val verifiedWishlist = mutableListOf<Product>()
        var processedCount = 0

        wishlist.forEach { product ->
            val url = "https://uk-hw3.wl.r.appspot.com/wishlist/${product.productId}"
            val stringRequest = StringRequest(
                Request.Method.GET, url,
                { response ->
                    // If the product is found in MongoDB, add it to verifiedWishlist
                    if (!response.contains("Product not found")) {
                        verifiedWishlist.add(product)
                    }
                    processedCount++
                    if (processedCount == wishlist.size) {
                        onUpdate(verifiedWishlist)
                    }
                },
                { error ->
                    processedCount++
                    Log.e("MainActivity", "Error verifying product: ${product.productId}", error)
                    if (processedCount == wishlist.size) {
                        onUpdate(verifiedWishlist)
                    }
                })
            requestQueue.add(stringRequest)
        }
    }
}
