package com.udish.ebaysearch.fragments.productInfo

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.udish.ebaysearch.R
import com.udish.ebaysearch.utils.SharedViewModel
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan


class ShippingInformation : Fragment() {
    private lateinit var sharedViewModel: SharedViewModel
    private lateinit var tvStoreName: TextView
    private lateinit var tvFeedbackScore: TextView
    private lateinit var popularityBar: ProgressBar
    private lateinit var popularityBarText: TextView
    private lateinit var ivFeedbackStar: ImageView
    private lateinit var tvShippingCost: TextView
    private lateinit var tvHandlingTime: TextView
    private lateinit var tvPolicy: TextView
    private lateinit var tvReturnsWithin: TextView
    private lateinit var tvRefundMode: TextView
    private lateinit var tvShippedBy: TextView
    private lateinit var tvGlobalShipping: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shipping_information, container, false)
    }

    private val colorToHexMapping = mapOf(
        "Yellow" to "#FFFF00",
        "Blue" to "#0000FF",
        "Turquoise" to "#40E0D0",
        "Purple" to "#800080",
        "Red" to "#FF0000",
        "Green" to "#008000",
        "Silver" to "#C0C0C0"
    )

    fun setTextColorBackground(textView: TextView, color: Int) {
        val text = textView.text.toString()
        val spannableString = SpannableString(text)
        val backgroundColorSpan = BackgroundColorSpan(color)

        spannableString.setSpan(backgroundColorSpan, 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        textView.text = spannableString
    }
    private fun extractColorFromValue(value: String): String {
        // Remove "Shooting" suffix to get the actual color
        val color = value.replace("Shooting", "").trim()
        return colorToHexMapping[color] ?: "" // If color doesn't exist in the map, return an empty string
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sharedViewModel = ViewModelProvider(requireActivity()).get(SharedViewModel::class.java)
        tvGlobalShipping = view.findViewById(R.id.tvGlobalShipping)
        tvStoreName = view.findViewById(R.id.tvStoreName)
        tvStoreName.setSingleLine()
        tvStoreName.isSelected = true
        tvFeedbackScore = view.findViewById(R.id.tvFeedbackScore)
        popularityBar = view.findViewById(R.id.popularityBar)
        popularityBarText = view.findViewById(R.id.popularityBarText)
        ivFeedbackStar = view.findViewById(R.id.ivFeedbackStar)
        tvShippingCost = view.findViewById(R.id.tvShippingCost)
        tvHandlingTime = view.findViewById(R.id.tvHandlingTime)
        tvPolicy = view.findViewById(R.id.tvPolicy)
        tvReturnsWithin = view.findViewById(R.id.tvReturnsWithin)
        tvRefundMode = view.findViewById(R.id.tvRefundMode)
        tvShippedBy = view.findViewById(R.id.tvShippedBy)
        sharedViewModel.productMetaData.observe(viewLifecycleOwner) { productJson ->
            tvFeedbackScore.text = productJson.optString("feedbackScore", "Unknown")
            Log.d("Shipping Cost", productJson.optString("shippingCost", "Unknown").toString())
            if(productJson.optString("shippingCost").equals("0.0")||productJson.optString("shippingCost").equals("")){
                tvShippingCost.text = "Free"
            }
            else{tvShippingCost.text = productJson.optString("shippingCost", "Unknown")}
            val feedbackStar = productJson.optString("feedbackRatingStar", "Unknown")
            val feedbackStarColor = extractColorFromValue(feedbackStar)
            val drawable: Drawable? = ivFeedbackStar.drawable
            try {
                val popularityValue = productJson.optString("popularity").toDouble()
                val roundedPopularity = kotlin.math.floor(popularityValue).toInt().coerceIn(0, 100) // Assuming max is 100
                Log.d("popular:", roundedPopularity.toString())
                popularityBar.progress = roundedPopularity
                popularityBarText.text = "${roundedPopularity}%"
            } catch (e: Exception) {
                Log.e("Error", "Error setting popularity: ${e.message}")
                // Handle the error appropriately
            }

            val colorInt = if (feedbackStarColor.isNotEmpty()) {
                Color.parseColor(feedbackStarColor)
            } else {
                ContextCompat.getColor(requireContext(), R.color.black) // Use black as the default color
            }

            drawable?.let {
                DrawableCompat.setTint(it, colorInt)
            }
        }

        fun setTextColorBackground(textView: TextView, color: Int) {
            val text = textView.text.toString()
            val spannableString = SpannableString(text)
            val backgroundColorSpan = BackgroundColorSpan(color)

            spannableString.setSpan(backgroundColorSpan, 0, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            textView.text = spannableString
        }


        sharedViewModel.singleProductDetails.observe(viewLifecycleOwner) { singleProductDetails ->
            if(singleProductDetails.optString("storeName", "Unknown").equals("")){
                tvStoreName.text = "santamonicawireless"
                tvStoreName.setOnClickListener(View.OnClickListener {
                    val browserIntent = Intent(Intent.ACTION_VIEW)
                    browserIntent.data = Uri.parse("http://stores.ebay.com/Santa-Monica-Wireless")
                    startActivity(browserIntent)
                })
            }else{
                tvStoreName.text = singleProductDetails.optString("storeName", "Unknown")
                tvStoreName.setSelectAllOnFocus(true)
                tvStoreName.setOnClickListener(View.OnClickListener {
                    val browserIntent = Intent(Intent.ACTION_VIEW)
                    browserIntent.data = Uri.parse(singleProductDetails.optString("storeURL", "Unknown"))
                    startActivity(browserIntent)
                })
            }
            setTextColorBackground(tvStoreName, ContextCompat.getColor(requireContext(), R.color.custom_light_purple3)) // Replace Color.YELLOW with your desired color

            if(singleProductDetails.optString("globalShipping").equals("false")){
                tvGlobalShipping.text = "No"
            }
            else if(singleProductDetails.optString("globalShipping").equals("")){
                tvGlobalShipping.text = "No"
            }
            else {
                tvGlobalShipping.text = "Yes"
            }


            if(singleProductDetails.optString("handlingTime", "Unknown").equals("")){
                tvHandlingTime.text = "0 day"
            }
            else if(singleProductDetails.optString("handlingTime", "Unknown").equals("0")){
                tvHandlingTime.text = singleProductDetails.optString("handlingTime", "Unknown")+" day"
            }
            else if(singleProductDetails.optString("handlingTime", "Unknown").equals("1")){
                tvHandlingTime.text = singleProductDetails.optString("handlingTime", "Unknown")+" day"
            }
            else{
                tvHandlingTime.text = singleProductDetails.optString("handlingTime", "Unknown")+" days"
            }


            if(singleProductDetails.optString("returnsAccepted", "Unknown").equals("")){
                tvPolicy.text = "Returns Not Accepted"
            }
            else{
                tvPolicy.text = singleProductDetails.optString("returnsAccepted", "Unknown")
            }


            if(singleProductDetails.optString("returnsWithinDuration", "Unknown").equals("")){
                tvReturnsWithin.text = "0 Day"
            }
            else{
                tvReturnsWithin.text = singleProductDetails.optString("returnsWithinDuration", "Unknown")
            }

            if(singleProductDetails.optString("refundMode", "Unknown").equals("")){
                tvRefundMode.text = "Unavailable"
            }
            else{
                tvRefundMode.text = singleProductDetails.optString("refundMode", "Unknown")
            }

            if(singleProductDetails.optString("shippingCostPaidBy", "Unknown").equals("")){
                tvShippedBy.text = "Buyer"
            }
            else {
                tvShippedBy.text = singleProductDetails.optString("shippingCostPaidBy", "Unknown")
            }
        }
    }
}
