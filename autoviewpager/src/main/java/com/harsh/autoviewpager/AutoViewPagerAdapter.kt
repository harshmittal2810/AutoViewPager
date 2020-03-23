package com.harsh.autoviewpager

import android.content.Context
import android.util.SparseArray
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter

abstract class AutoViewPagerAdapter<T>(
    context: Context,
    itemList: MutableList<T>,
    isInfinite: Boolean
) : PagerAdapter() {
    var mItemList: List<T> = itemList
    var mContext: Context = context
    var isInfinite = isInfinite
    var viewCache = SparseArray<View>()
    var canInfinite = true

    private var dataSetChangeLock = false

    init {
        setItemList(itemList)
    }

    open fun setItemList(itemList: List<T>) {
        viewCache = SparseArray<View>()
        mItemList = itemList
        canInfinite = itemList.size > 1
        notifyDataSetChanged()
    }

    /**
     * Child should override this method and return the View that it wish to inflate.
     * View binding with data should be in another method - bindView().
     *
     * @param listPosition The current list position for you to determine your own view type.
     */
    protected abstract fun inflateView(
        viewType: Int,
        container: ViewGroup?,
        listPosition: Int
    ): View?

    /**
     * Child should override this method to bind the View with data.
     * If you wish to implement ViewHolder pattern, you may use setTag() on the convertView and
     * pass in your ViewHolder.
     *
     * @param convertView  The View that needs to bind data with.
     * @param listPosition The current list position for you to get data from itemList.
     */
    protected abstract fun bindView(
        convertView: View?,
        listPosition: Int,
        viewType: Int
    )

    open fun getItem(listPosition: Int): T? {
        return if (listPosition >= 0 && listPosition < mItemList.size) {
            mItemList[listPosition]
        } else {
            null
        }
    }


    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val listPosition =
            if (isInfinite && canInfinite) getListPosition(position) else position
        val viewType: Int = getItemViewType(listPosition)
        val convertView: View?
        if (viewCache[viewType, null] == null) {
            convertView = inflateView(viewType, container, listPosition)
        } else {
            convertView = viewCache[viewType]
            viewCache.remove(viewType)
        }
        bindView(convertView, listPosition, viewType)
        container.addView(convertView)
        return convertView!!
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        val listPosition =
            if (isInfinite && canInfinite) getListPosition(position) else position
        container.removeView(`object` as View?)
        if (!dataSetChangeLock) viewCache.put(
            getItemViewType(listPosition),
            `object`
        )
    }

    override fun notifyDataSetChanged() {
        dataSetChangeLock = true
        super.notifyDataSetChanged()
        dataSetChangeLock = false
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    override fun getCount(): Int {
        var count = mItemList.size
        return if (isInfinite && canInfinite) {
            count + 2
        } else {
            count
        }
    }

    /**
     * Allow child to implement view type by overriding this method.
     * instantiateItem() will call this method to determine which view to recycle.
     *
     * @param listPosition Determine view type using listPosition.
     * @return a key (View type ID) in the form of integer,
     */
    protected open fun getItemViewType(listPosition: Int): Int {
        return 0
    }

    open fun getListCount(): Int = mItemList.size

    private fun getListPosition(position: Int): Int {
        if (!(isInfinite && canInfinite)) return position
        return when {
            position == 0 -> {
                count - 1 - 2
            }
            position > count - 2 -> {
                0
            }
            else -> {
                position - 1
            }
        }
    }

    open fun getLastItemPosition(): Int {
        mItemList.let { itemList ->
            return if (isInfinite) {
                itemList.size
            } else {
                itemList.size - 1
            }
        }
    }
}