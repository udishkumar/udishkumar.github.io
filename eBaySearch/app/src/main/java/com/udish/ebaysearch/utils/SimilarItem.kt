package com.udish.ebaysearch.utils

data class SimilarItem(
    val product_id: String,
    val image_url: String,
    val product_name: String,
    val product_url: String,
    val price: String,
    val shipping_cost: String,
    val daysLeft: String
)
