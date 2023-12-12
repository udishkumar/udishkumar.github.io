package com.udish.ebaysearch.fragments.productInfo

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.denzcoskun.imageslider.ImageSlider
import com.denzcoskun.imageslider.models.SlideModel
import com.udish.ebaysearch.R
import org.json.JSONObject

class ProductInfoAdapter(private val productInfo: JSONObject, private val productMetaData: JSONObject) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_IMAGE_SLIDER = 0
        private const val TYPE_PRICE_BRAND = 1
        private const val TYPE_HIGHLIGHTS = 2
        private const val TYPE_SPECIFICATIONS = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> TYPE_IMAGE_SLIDER
            1 -> TYPE_PRICE_BRAND
            2 -> TYPE_HIGHLIGHTS
            else -> TYPE_SPECIFICATIONS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_IMAGE_SLIDER -> ImageSliderViewHolder(inflater.inflate(R.layout.product_info_imageslider, parent, false))
            TYPE_PRICE_BRAND -> PriceBrandViewHolder(inflater.inflate(R.layout.product_info_price_and_brand, parent, false))
            TYPE_HIGHLIGHTS -> HighlightsViewHolder(inflater.inflate(R.layout.product_info_highlights, parent, false))
            else -> SpecificationsViewHolder(inflater.inflate(R.layout.product_info_specifications, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is ImageSliderViewHolder -> holder.bindData(productInfo)
            is PriceBrandViewHolder -> holder.bindData(productInfo, productMetaData)
            is HighlightsViewHolder -> holder.bindData(productInfo)
            is SpecificationsViewHolder -> holder.bindData(productInfo)
        }
    }

    override fun getItemCount(): Int {
        return 4 // Assuming you have four types of items
    }

    // ViewHolders
    class ImageSliderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imageSlider: ImageSlider = view.findViewById(R.id.image_slider)

        fun bindData(data: JSONObject) {
            val imagesJsonArray = data.getJSONArray("product_images")
            val imageList = ArrayList<SlideModel>()
            for (i in 0 until imagesJsonArray.length()) {
                val imageUrl = imagesJsonArray.getString(i)
                imageList.add(SlideModel(imageUrl))
            }
            imageSlider.setImageList(imageList)
        }
    }

    class PriceBrandViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDetail1: TextView = view.findViewById(R.id.tvProdDetail1_1)
        private val tvDetail2: TextView = view.findViewById(R.id.tvProdDetail1_2)

        fun bindData(data: JSONObject, metaData: JSONObject) {
            Log.d("price", data.toString())
            Log.d("price", metaData.toString())
            val productName = metaData.optString("productName", "Unknown")
            tvDetail1.text = productName

            val sCost = metaData.optString("shippingCost")
            val pPrice = metaData.optString("productPrice")

            if (sCost == "") {
                tvDetail2.text = "\$$pPrice with Free Shipping"
                return
            }else{
                tvDetail2.text = "\$$pPrice with \$$sCost shipping"
            }
        }
    }

    class HighlightsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvHighlights: TextView = view.findViewById(R.id.tvProdDetail2_2)

        fun bindData(data: JSONObject) {
            val itemSpecificsArray = data.getJSONArray("itemSpecifics")
            val highlightsBuilder = StringBuilder()

            // Extract and append price
            val price = data.optString("product_price", "\$123.00")
            highlightsBuilder.append("Price    $price\n")

            // Find and append brand information
            for (i in 0 until itemSpecificsArray.length()) {
                val item = itemSpecificsArray.getJSONObject(i)
                val name = item.getString("Name")
                if (name == "Brand") {
                    val value = item.getString("Value")
                    highlightsBuilder.append("Brand    $value\n")
                    break // Stop loop after finding the brand
                }
            }

            tvHighlights.text = highlightsBuilder.toString()
        }
    }




    class SpecificationsViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvSpecifications: TextView = view.findViewById(R.id.tvProdDetail3_2)

        fun bindData(data: JSONObject) {
            val itemSpecificsArray = data.getJSONArray("itemSpecifics")
            val otherSpecificsBuilder = StringBuilder()
            for (i in 0 until itemSpecificsArray.length()) {
                val item = itemSpecificsArray.getJSONObject(i)
                val name = item.getString("Name")
                val value = item.getString("Value")

                if (name != "Brand") {
                    otherSpecificsBuilder.append("\u2022 $value\n")
                }
            }
            tvSpecifications.text = otherSpecificsBuilder.toString()
        }
    }
}
