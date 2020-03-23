package com.harsh.autoviewpagersample

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.harsh.autoviewpager.AutoViewPagerAdapter
import java.util.*

class DemoInfiniteAdapter(
    val context: Context,
    itemList: ArrayList<Int>,
    isInfinite: Boolean
) : AutoViewPagerAdapter<Int>(context, itemList, isInfinite) {

    override fun inflateView(
        viewType: Int,
        container: ViewGroup?,
        listPosition: Int
    ): View? {
        return LayoutInflater.from(mContext).inflate(R.layout.item_pager, container, false)
    }

    override fun bindView(
        convertView: View?,
        listPosition: Int,
        viewType: Int
    ) {
        val imageView = convertView!!.findViewById<AppCompatImageView>(R.id.imageView)
        Glide.with(context).load(ContextCompat.getDrawable(context, mItemList[listPosition]))
            .into(imageView)
    }
}