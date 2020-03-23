package com.harsh.autoviewpagersample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.harsh.autoviewpager.AutoViewPager
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private var currentDataSet = 1
    lateinit var demoAdapter: DemoInfiniteAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        demoAdapter = DemoInfiniteAdapter(this, createDummyItems(), true)
        viewPager.adapter = demoAdapter
        indicatorView.count = viewPager.getIndicatorCount()
        viewPager.setIndicatorSmart(true)
        viewPager.setIndicatorPageChangeListener(object :
            AutoViewPager.IndicatorPageChangeListener {
            override fun onIndicatorProgress(
                selectingPosition: Int,
                progress: Float
            ) {
                indicatorView.setProgress(selectingPosition, progress)
            }

            override fun onIndicatorPageChange(newIndicatorPosition: Int) {
                indicatorView.selection = newIndicatorPosition
            }
        })
    }

    override fun onResume() {
        super.onResume()
        viewPager.resumeAutoScroll()
    }

    override fun onPause() {
        viewPager.pauseAutoScroll()
        super.onPause()
    }

    private fun createDummyItems(): ArrayList<Int> {
        val items = ArrayList<Int>()
        items.add(R.drawable.image1)
        items.add(R.drawable.image2)
        items.add(R.drawable.image3)
        items.add(R.drawable.image4)
        items.add(R.drawable.image5)
        items.add(R.drawable.image6)
        return items
    }
}