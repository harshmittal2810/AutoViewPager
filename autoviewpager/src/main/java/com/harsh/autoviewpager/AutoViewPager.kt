package com.harsh.autoviewpager

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import kotlin.math.abs
import kotlin.math.roundToInt

open class AutoViewPager : ViewPager {

    companion object {
        private const val TAG = "AutoViewPager"
    }

    private var isInfinite = true
    private var isAutoScroll = false
    private var wrapContent = true
    private var aspectRatio = 0f

    //AutoScroll
    private var interval = 5000
    private var previousPosition = 0
    private var currentPagePosition = 0
    private var isAutoScrollResumed = false
    private val autoScrollHandler = Handler()

    //For Indicator
    private var indicatorPageChangeListener: IndicatorPageChangeListener? = null
    private var previousScrollState = SCROLL_STATE_IDLE
    private var scrollState = SCROLL_STATE_IDLE
    private var isToTheRight = true
    private var isIndicatorSmart = false

    private val autoScrollRunnable = Runnable {
        if (adapter == null || !isAutoScroll || adapter!!.count < 2) return@Runnable
        if (!isInfinite && adapter!!.count - 1 == currentPagePosition) {
            currentPagePosition = 0
        } else {
            currentPagePosition++
        }
        setCurrentItem(currentPagePosition, true)
    }


    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        val a =
            context.theme.obtainStyledAttributes(attrs, R.styleable.AutoViewPager, 0, 0)
        try {
            isInfinite = a.getBoolean(R.styleable.AutoViewPager_isInfinite, false)
            isAutoScroll = a.getBoolean(R.styleable.AutoViewPager_autoScroll, false)
            wrapContent = a.getBoolean(R.styleable.AutoViewPager_wrap_content, true)
            interval = a.getInt(R.styleable.AutoViewPager_scrollInterval, 5000)
            aspectRatio = a.getFloat(R.styleable.AutoViewPager_viewpagerAspectRatio, 0f)
            isAutoScrollResumed = isAutoScroll
        } finally {
            a.recycle()
        }
        init()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var heightMeasure = heightMeasureSpec
        val width = MeasureSpec.getSize(widthMeasureSpec)
        if (aspectRatio > 0) {
            val height =
                (MeasureSpec.getSize(widthMeasureSpec).toFloat() / aspectRatio).roundToInt()
            val finalWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
            val finalHeightMeasureSpec =
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
            super.onMeasure(finalWidthMeasureSpec, finalHeightMeasureSpec)
        } else {
            if (wrapContent) {
                val mode = MeasureSpec.getMode(heightMeasure)
                if (mode == MeasureSpec.UNSPECIFIED || mode == MeasureSpec.AT_MOST) {
                    super.onMeasure(widthMeasureSpec, heightMeasure)
                    var height = 0
                    // Remove padding from width
                    val childWidthSize = width - paddingLeft - paddingRight
                    // Make child width MeasureSpec
                    val childWidthMeasureSpec =
                        MeasureSpec.makeMeasureSpec(childWidthSize, MeasureSpec.EXACTLY)
                    for (i in 0 until childCount) {
                        val child = getChildAt(i)
                        child.measure(
                            childWidthMeasureSpec,
                            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
                        )
                        val h = child.measuredHeight
                        if (h > height) {
                            height = h
                        }
                    }
                    // Add padding back to child height
                    height += paddingTop + paddingBottom
                    heightMeasure = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
                }
            }
            super.onMeasure(widthMeasureSpec, heightMeasure)
        }
    }


    private fun init() {
        addOnPageChangeListener(object : OnPageChangeListener {
            var currentPosition = 0f
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                isToTheRight = position + positionOffset >= currentPosition
                if (positionOffset == 0f) currentPosition = position.toFloat()
                val realPosition: Int = getSelectingIndicatorPosition(isToTheRight)
                val progress: Float
                progress = if (scrollState == SCROLL_STATE_SETTLING && abs(
                        currentPagePosition - previousPosition
                    ) > 1
                ) {
                    val pageDiff = abs(currentPagePosition - previousPosition)
                    if (isToTheRight) {
                        (position - previousPosition).toFloat() / pageDiff + positionOffset / pageDiff
                    } else {
                        (previousPosition - (position + 1)).toFloat() / pageDiff + (1 - positionOffset) / pageDiff
                    }
                } else {
                    if (isToTheRight) positionOffset else 1 - positionOffset
                }
                if (progress > 1) return
                if (isIndicatorSmart) {
                    if (scrollState != SCROLL_STATE_DRAGGING) return
                    indicatorPageChangeListener?.onIndicatorProgress(realPosition, progress)
                } else {
                    if (scrollState == SCROLL_STATE_DRAGGING) {
                        if (isToTheRight && abs(realPosition - currentPagePosition) == 2 ||
                            !isToTheRight && realPosition == currentPagePosition
                        ) {
                            return
                        }
                    }
                    indicatorPageChangeListener?.onIndicatorProgress(realPosition, progress)
                }
            }

            override fun onPageSelected(position: Int) {
                previousPosition = currentPagePosition
                currentPagePosition = position
                indicatorPageChangeListener?.onIndicatorPageChange(getIndicatorPosition())
                if (isAutoScrollResumed) {
                    autoScrollHandler.removeCallbacks(autoScrollRunnable)
                    autoScrollHandler.postDelayed(autoScrollRunnable, interval.toLong())
                }
            }

            override fun onPageScrollStateChanged(state: Int) {
                if (!isIndicatorSmart) {
                    if (scrollState == SCROLL_STATE_SETTLING && state == SCROLL_STATE_DRAGGING) {
                        indicatorPageChangeListener?.onIndicatorProgress(
                            getSelectingIndicatorPosition(isToTheRight), 1f
                        )
                    }
                }
                previousScrollState = scrollState
                scrollState = state
                if (state == SCROLL_STATE_IDLE) {
                    if (isInfinite) {
                        if (adapter == null) return
                        val itemCount = adapter!!.count
                        if (itemCount < 2) {
                            return
                        }
                        val index = currentItem
                        if (index == 0) {
                            setCurrentItem(itemCount - 2, false) //Real last item
                        } else if (index == itemCount - 1) {
                            setCurrentItem(1, false) //Real first item
                        }
                    }
                    indicatorPageChangeListener?.onIndicatorProgress(getIndicatorPosition(), 1f)
                }
            }
        })
        if (isInfinite) setCurrentItem(1, false)
    }

    fun getSelectingIndicatorPosition(isToTheRight: Boolean): Int {
        if (scrollState == SCROLL_STATE_SETTLING || scrollState == SCROLL_STATE_IDLE || previousScrollState == SCROLL_STATE_SETTLING && scrollState == SCROLL_STATE_DRAGGING
        ) {
            return getIndicatorPosition()
        }
        val delta = if (isToTheRight) 1 else -1
        return if (isInfinite) {
            if (adapter !is AutoViewPagerAdapter<*>) return currentPagePosition + delta
            if (currentPagePosition == 1 && !isToTheRight) {
                (adapter as AutoViewPagerAdapter<*>).getLastItemPosition() - 1
            } else if (currentPagePosition == (adapter as AutoViewPagerAdapter<*>).getLastItemPosition()
                && isToTheRight
            ) {
                0
            } else {
                currentPagePosition + delta - 1
            }
        } else {
            currentPagePosition + delta
        }
    }

    fun getIndicatorCount(): Int {
        return if (adapter is AutoViewPagerAdapter<*>) {
            (adapter as AutoViewPagerAdapter<*>).getListCount()
        } else {
            adapter!!.count
        }
    }


    fun reset() {
        currentPagePosition = if (isInfinite) {
            setCurrentItem(1, false)
            1
        } else {
            setCurrentItem(0, false)
            0
        }
    }

    fun setIndicatorSmart(isIndicatorSmart: Boolean) {
        this.isIndicatorSmart = isIndicatorSmart
    }

    fun setIndicatorPageChangeListener(callback: IndicatorPageChangeListener) {
        indicatorPageChangeListener = callback
    }

    interface IndicatorPageChangeListener {
        fun onIndicatorProgress(selectingPosition: Int, progress: Float)
        fun onIndicatorPageChange(newIndicatorPosition: Int)
    }

    fun setInterval(interval: Int) {
        this.interval = interval
        resetAutoScroll()
    }

    private fun resetAutoScroll() {
        pauseAutoScroll()
        resumeAutoScroll()
    }

    override fun setAdapter(adapter: PagerAdapter?) {
        super.setAdapter(adapter)
        if (isInfinite) setCurrentItem(1, false)
    }

    fun resumeAutoScroll() {
        isAutoScrollResumed = true
        autoScrollHandler.postDelayed(autoScrollRunnable, interval.toLong())
    }

    fun pauseAutoScroll() {
        isAutoScrollResumed = false
        autoScrollHandler.removeCallbacks(autoScrollRunnable)
    }

    fun getIndicatorPosition(): Int {
        return if (!isInfinite) {
            currentPagePosition
        } else {
            if (adapter is AutoViewPagerAdapter<*>) return currentPagePosition
            when (currentPagePosition) {
                0 -> { //Dummy last item is selected. Indicator should be at the last one
                    (adapter as AutoViewPagerAdapter<*>).getListCount() - 1
                }
                (adapter as AutoViewPagerAdapter<*>).getLastItemPosition() + 1 -> { //Dummy first item is selected. Indicator should be at the first one
                    0
                }
                else -> {
                    currentPagePosition - 1
                }
            }
        }
    }


}