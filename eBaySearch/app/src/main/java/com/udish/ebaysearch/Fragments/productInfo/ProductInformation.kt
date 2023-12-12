package com.udish.ebaysearch.fragments.productInfo

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.udish.ebaysearch.R
import com.udish.ebaysearch.utils.SharedViewModel
import org.json.JSONObject

class ProductInformation : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressBarText: TextView
    private var isDataLoaded = false  // Flag to check if data has already been loaded

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_product_information, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize SharedViewModel
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        // Initialize views
        recyclerView = view.findViewById(R.id.rvProductInfo)
        recyclerView.layoutManager = LinearLayoutManager(context)
        progressBar = view.findViewById(R.id.progressBarFetchDetails)
        progressBarText = view.findViewById(R.id.tvfetchDetails)

        // Set up the RecyclerView if data is already loaded
        if (isDataLoaded) {
            sharedViewModel.singleProductDetails.value?.let { productJson ->
                sharedViewModel.productMetaData.value?.let { metaData ->
                    updateAdapter(productJson, metaData)
                }
            }
        } else {
            // If data is not loaded, show progress bar and observe ViewModel
            showProgressBar()
            observeViewModel()
        }
    }

    private fun updateAdapter(productJson: JSONObject, metaData: JSONObject) {
        val productInfoAdapter = ProductInfoAdapter(productJson, metaData)
        recyclerView.adapter = productInfoAdapter
        hideProgressBar()
        isDataLoaded = true  // Set flag to true as data is now loaded
    }

    private fun observeViewModel() {
        sharedViewModel.singleProductDetails.observe(viewLifecycleOwner) { productJson ->
            sharedViewModel.productMetaData.value?.let { metaData ->
                loadData(productJson, metaData)
            }
        }

        sharedViewModel.productMetaData.observe(viewLifecycleOwner) { metaData ->
            sharedViewModel.singleProductDetails.value?.let { productJson ->
                loadData(productJson, metaData)
            }
        }
    }

    private fun loadData(productJson: JSONObject, metaData: JSONObject) {
        showProgressBar()

        Handler(Looper.getMainLooper()).postDelayed({
            if (isAdded) { // Check if Fragment is still added
                updateAdapter(productJson, metaData)
            }
        }, 2000) // Delay of 2000 milliseconds (2 seconds)
    }

    private fun showProgressBar() {
        progressBar.visibility = View.VISIBLE
        progressBarText.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun hideProgressBar() {
        progressBar.visibility = View.GONE
        progressBarText.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
}

