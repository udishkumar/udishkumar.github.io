package com.udish.ebaysearch.fragments.productInfo

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.denzcoskun.imageslider.models.SlideModel
import com.udish.ebaysearch.Adapter.GooglePhotosAdapter
import com.udish.ebaysearch.R
import com.udish.ebaysearch.utils.SharedViewModel


class GooglePhotos : Fragment() {
//    private lateinit var textView: TextView
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var photoRecyclerView: RecyclerView
    private val photoUrls = ArrayList<String>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_google_photos, container, false)
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        textView = view.findViewById(R.id.tv)
//        textView.movementMethod = ScrollingMovementMethod() // Make TextView scrollable
        photoRecyclerView = view.findViewById(R.id.photoRecyclerView)
        photoRecyclerView.layoutManager = LinearLayoutManager(context)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        sharedViewModel.additionalPhotos.observe(viewLifecycleOwner) { additionalPhotos ->
            photoUrls.clear()
            if (additionalPhotos.length() == 0) {
                // Add placeholder image URL from drawable
                val placeholderUrl = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcTUaY8AD_x6kRMw6dHReqBbsbiCIo_hNKv4AOVphg67lw&s"
                photoUrls.add(placeholderUrl)
            }
            else{
                for (key in additionalPhotos.keys()) {
                    additionalPhotos.optString(key)?.let { url ->
                        photoUrls.add(url)
                    }
                }
            }

            photoRecyclerView.adapter = GooglePhotosAdapter(photoUrls)
        }
    }
}
