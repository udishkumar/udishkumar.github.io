package com.udish.ebaysearch.fragments.productInfo

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.udish.ebaysearch.Adapter.SimilarItemsAdapter
import com.udish.ebaysearch.R
import com.udish.ebaysearch.utils.SharedViewModel
import com.udish.ebaysearch.utils.SimilarItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import android.util.Log

class SimilarProducts : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_similar_products, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val spinner1: Spinner = view.findViewById(R.id.spinner1)
        val spinner2: Spinner = view.findViewById(R.id.spinner2)

        ArrayAdapter.createFromResource(requireContext(), R.array.sort_by, android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner1.adapter = adapter
        }

        ArrayAdapter.createFromResource(requireContext(), R.array.sort_order, android.R.layout.simple_spinner_item).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner2.adapter = adapter
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewSimilarProducts)
        recyclerView.layoutManager = GridLayoutManager(context, 1)
        val initialAdapter = SimilarItemsAdapter(mutableListOf()) // Initialize with an empty list
        recyclerView.adapter = initialAdapter

        spinner1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                view?.let {
                    val selectedItem = parent.getItemAtPosition(position).toString()
                    spinner2.isEnabled = selectedItem != "Default"
                    sortAndUpdateRecyclerView(selectedItem, spinner2.selectedItem.toString(), recyclerView)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinner2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                view?.let {
                    sortAndUpdateRecyclerView(spinner1.selectedItem.toString(), parent.getItemAtPosition(position).toString(), recyclerView)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedViewModel.similarItems.observe(viewLifecycleOwner) { similarItemsJson ->
            val similarItems = parseJson(similarItemsJson.toString())
            (recyclerView.adapter as? SimilarItemsAdapter)?.updateData(similarItems.toMutableList())
        }
    }

    private fun sortAndUpdateRecyclerView(sortType: String, sortOrder: String, recyclerView: RecyclerView) {
        val adapter = recyclerView.adapter as? SimilarItemsAdapter
        adapter?.sortItems(sortType, sortOrder) ?: Log.e("SimilarProducts", "Adapter is null or not a SimilarItemsAdapter")
    }

    private fun parseJson(jsonString: String): List<SimilarItem> {
        return try {
            val gson = Gson()
            val itemType = object : TypeToken<List<SimilarItem>>() {}.type
            gson.fromJson(jsonString, itemType)
        } catch (e: Exception) {
            Log.e("SimilarProducts", "Error parsing JSON", e)
            emptyList()
        }
    }
}
