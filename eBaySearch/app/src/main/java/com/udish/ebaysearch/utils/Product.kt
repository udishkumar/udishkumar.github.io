package com.udish.ebaysearch.utils

import org.json.JSONObject
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val productId: String,
    val productName: String,
    val productImage: String,
    val productUrl: String,
    val productCategory: String,
    val productCondition: String,
    val productPrice: String,
    val topRated: Boolean,
    val shippingCost: String,
    val shippingLocations: String,
    val handlingTime: String,
    val expeditedShipping: Boolean,
    val oneDayShipping: Boolean,
    val returnAccepted: Boolean,
    val feedbackScore: Int,
    val popularity: Double,
    val feedbackRatingStar: String,
    val storeName: String,
    val buyProductAt: String,
    val shippingType: String,
    val zipCode: String,
    val seller: String
) : Parcelable {

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("productId", productId)
            put("productName", productName)
            put("productImage", productImage)
            put("productUrl", productUrl)
            put("productCategory", productCategory)
            put("productCondition", productCondition)
            put("productPrice", productPrice)
            put("topRated", topRated)
            put("shippingCost", shippingCost)
            put("shippingLocations", shippingLocations)
            put("handlingTime", handlingTime)
            put("expeditedShipping", expeditedShipping)
            put("oneDayShipping", oneDayShipping)
            put("returnAccepted", returnAccepted)
            put("feedbackScore", feedbackScore)
            put("popularity", popularity)
            put("feedbackRatingStar", feedbackRatingStar)
            put("storeName", storeName)
            put("buyProductAt", buyProductAt)
            put("shippingType", shippingType)
            put("zipCode", zipCode)
            put("seller", seller)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): Product {
            return Product(
                productId = json.getString("productId"),
                productName = json.getString("productName"),
                productImage = json.getString("productImage"),
                productUrl = json.getString("productUrl"),
                productCategory = json.getString("productCategory"),
                productCondition = json.getString("productCondition"),
                productPrice = json.getString("productPrice"),
                topRated = json.getBoolean("topRated"),
                shippingCost = json.getString("shippingCost"),
                shippingLocations = json.getString("shippingLocations"),
                handlingTime = json.getString("handlingTime"),
                expeditedShipping = json.getBoolean("expeditedShipping"),
                oneDayShipping = json.getBoolean("oneDayShipping"),
                returnAccepted = json.getBoolean("returnAccepted"),
                feedbackScore = json.getInt("feedbackScore"),
                popularity = json.getDouble("popularity"),
                feedbackRatingStar = json.getString("feedbackRatingStar"),
                storeName = json.getString("storeName"),
                buyProductAt = json.getString("buyProductAt"),
                shippingType = json.getString("shippingType"),
                zipCode = json.getString("zipCode"),
                seller = json.getString("seller")
            )
        }
    }
}
