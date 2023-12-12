package com.udish.ebaysearch.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.udish.ebaysearch.MainActivity
import com.udish.ebaysearch.R
import com.udish.ebaysearch.adapter.WishlistAdapter
import com.udish.ebaysearch.utils.SharedViewModel

class Wishlist : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var wishlistRecyclerView: RecyclerView
    private lateinit var emptyWishlistCard: CardView
    private var mainActivity: MainActivity? = null

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mainActivity = activity as? MainActivity
        val wishlist = mainActivity?.loadWishlistFromSharedPreferences() ?: mutableListOf()

        mainActivity?.verifyWishlistWithMongoDB(wishlist) { verifiedWishlist ->
            sharedViewModel.wishlistProducts.postValue(verifiedWishlist)
            mainActivity?.saveWishlistToSharedPreferences(verifiedWishlist)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_wishlist, container, false)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)

        wishlistRecyclerView = view.findViewById(R.id.recyclerViewWishlist)
        wishlistRecyclerView.layoutManager = GridLayoutManager(context, 2)
        emptyWishlistCard = view.findViewById(R.id.empty_wishlist_card)
        setupWishlistObserver()
        return view
    }

    private fun setupWishlistObserver() {
        sharedViewModel.wishlistProducts.observe(viewLifecycleOwner, Observer { wishlist ->
            if (wishlist.isEmpty()) {
                emptyWishlistCard.visibility = View.VISIBLE
                wishlistRecyclerView.visibility = View.GONE
                view?.findViewById<LinearLayout>(R.id.wishlist_total_layout)?.visibility = View.GONE
            } else {
                emptyWishlistCard.visibility = View.GONE
                wishlistRecyclerView.visibility = View.VISIBLE
                val totalLayout = view?.findViewById<LinearLayout>(R.id.wishlist_total_layout)
                val totalText = view?.findViewById<TextView>(R.id.wishlist_total_value)
                val wishlist_total_text = view?.findViewById<TextView>(R.id.wishlist_total_text)

                val adapter = if (mainActivity != null) WishlistAdapter(requireContext(), wishlist, sharedViewModel, mainActivity!!) else null
                wishlistRecyclerView.adapter = adapter

                adapter?.let {
                    val (count, total) = it.calculateTotal()
                    totalLayout?.visibility = View.VISIBLE
                    wishlist_total_text?.text = String.format("Wishlist Total (%d Items)", count)
                    totalText?.text = String.format("$%.2f", total)
                }
            }
        })
    }
}
