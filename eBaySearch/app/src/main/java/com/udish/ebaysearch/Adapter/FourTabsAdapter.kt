package com.udish.ebaysearch.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.udish.ebaysearch.fragments.productInfo.GooglePhotos
import com.udish.ebaysearch.fragments.productInfo.ProductInformation
import com.udish.ebaysearch.fragments.productInfo.ShippingInformation
import com.udish.ebaysearch.fragments.productInfo.SimilarProducts

class FourTabsAdapter(context: Context?, fm: FragmentManager?, private val totalTabs: Int) :
    FragmentPagerAdapter(fm!!) {
    private val fragmentList: MutableList<Fragment> = ArrayList()

    init {
        initFragments()
    }

    private fun initFragments() {
        fragmentList.add(ProductInformation())
        fragmentList.add(ShippingInformation())
        fragmentList.add(GooglePhotos())
        fragmentList.add(SimilarProducts())
    }

    override fun getItem(position: Int): Fragment {
        return fragmentList[position]
    }

    override fun getCount(): Int {
        return totalTabs
    }
}