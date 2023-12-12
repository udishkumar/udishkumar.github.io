package com.udish.ebaysearch.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.udish.ebaysearch.MainActivity
import com.udish.ebaysearch.R
import com.udish.ebaysearch.adapter.SearchResultAdapter
import com.udish.ebaysearch.utils.Product
import com.udish.ebaysearch.utils.SharedViewModel
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class SearchResults : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var progressBarsearchProducts: ProgressBar
    private lateinit var progressBarText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var backButton: ImageView
    private lateinit var norecordCard: CardView
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_results, container, false)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        progressBarsearchProducts = view.findViewById(R.id.progressBarsearchProducts)
        progressBarText = view.findViewById(R.id.tvsearchProducts)
        recyclerView = view.findViewById(R.id.recyclerView)
        norecordCard = view.findViewById(R.id.empty_records)
        setupObservers(view)
        return view

    }

    override fun onResume() {
        super.onResume()
        recyclerView.adapter?.notifyDataSetChanged()
    }

    private fun setupObservers(view: View) {
        sharedViewModel.searchData.observe(viewLifecycleOwner, Observer { data ->
            val url = generateApiUrl(data)
            makeApiCall(url)
        })
    }

    private fun makeApiCall(url: String) {
        progressBarsearchProducts.visibility = View.VISIBLE
        progressBarText.visibility = View.VISIBLE
        val requestQueue = Volley.newRequestQueue(requireContext())

        val jsonArrayRequest = JsonArrayRequest(
            Request.Method.GET, url, null,
            Response.Listener { response ->
                progressBarsearchProducts.visibility = View.GONE
                progressBarText.visibility = View.GONE
                if(response.toString().contains("No results found for given search criteria")){
                    norecordCard.visibility = View.VISIBLE
                }
                else{
                    val parsedProducts = parseProducts(response)
                    recyclerView.adapter = SearchResultAdapter(
                        parsedProducts,
                        sharedViewModel,
                        { product -> navigateToProductDetails(product) },
                        viewLifecycleOwner,
                        activity as MainActivity  // Pass MainActivity context here
                    )
                }
            },
            Response.ErrorListener { error ->
                progressBarsearchProducts.visibility = View.GONE
                progressBarText.visibility = View.GONE
                norecordCard.visibility = View.VISIBLE
                Log.e("API Error", error.toString())
            }
        )
        requestQueue.add(jsonArrayRequest)
    }



    private fun navigateToProductDetails(product: Product) {
        val keywords = sharedViewModel.searchData.value?.getString("keywords") ?: "default"
//        val shippingType = sharedViewModel.searchData.value?.getString("shippingType") ?: "default"
        val detailsFragment = ProductDetails().apply {
            arguments = Bundle().apply {
                putParcelable("product", product)
                putString("keywords", keywords)
            }
        }
        fragmentManager?.beginTransaction()
            ?.replace(R.id.fragment_search_results, detailsFragment)
            ?.addToBackStack(null)
            ?.commit()
    }



    private fun parseProducts(jsonArray: JSONArray): List<Product> {
        val productList = mutableListOf<Product>()
        for (i in 0 until jsonArray.length()) {
            val jsonObject = jsonArray.getJSONObject(i)
            val product = Product(
                productId = jsonObject.getString("product_id"),
                productName = jsonObject.getString("product_name"),
                productImage = jsonObject.getString("product_image"),
                productUrl = jsonObject.getString("product_url"),
                productCategory = jsonObject.getString("product_category"),
                productCondition = jsonObject.getString("product_condition"),
                productPrice = jsonObject.getString("product_price"),
                topRated = jsonObject.getBoolean("top_rated"),
                shippingCost = jsonObject.optString("shipping_cost"),  // optString for nullable fields
                shippingLocations = jsonObject.getString("shipping_locations"),
                handlingTime = jsonObject.getString("handling_time"),
                expeditedShipping = jsonObject.getBoolean("expedited_shipping"),
                oneDayShipping = jsonObject.getBoolean("one_day_shipping"),
                returnAccepted = jsonObject.getBoolean("return_accepted"),
                feedbackScore = jsonObject.getInt("feedback_score"),
                popularity = jsonObject.getDouble("popularity"),
                feedbackRatingStar = jsonObject.getString("feedback_rating_star"),
                storeName = jsonObject.optString("store_name"),  // optString for nullable fields
                buyProductAt = jsonObject.optString("buy_product_at"),  // optString for nullable fields
                shippingType = jsonObject.getString("shippingType"),
                zipCode = jsonObject.getString("zipCode"),
                seller = jsonObject.getString("seller")
            )
            productList.add(product)
        }
        return productList
    }
    private fun generateApiUrl(jsonObject: JSONObject): String {
        val baseUrl = "https://uk-hw3.wl.r.appspot.com/getEbayData"
        val queryParams = mutableListOf<String>()

        jsonObject.keys().forEach { key ->
            val value = jsonObject.getString(key)
            queryParams.add("$key=${URLEncoder.encode(value, "UTF-8")}")
        }

        return "$baseUrl?${queryParams.joinToString("&")}"
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            activity?.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backButton = view.findViewById(R.id.backButton)
        backButton.setOnClickListener {
            // Pop the current fragment off the stack
            fragmentManager?.popBackStack()
        }
    }



}