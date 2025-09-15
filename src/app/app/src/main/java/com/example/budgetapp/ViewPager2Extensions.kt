package com.example.budgetapp

import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Scroller
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

fun ViewPager2.reduceDragSensitivity(factor: Int = 4) {
    try {
        val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
        recyclerViewField.isAccessible = true
        val recyclerView = recyclerViewField.get(this) as RecyclerView
        val touchSlopField = RecyclerView::class.java.getDeclaredField("mTouchSlop")
        touchSlopField.isAccessible = true
        val touchSlop = touchSlopField.get(recyclerView) as Int
        touchSlopField.set(recyclerView, touchSlop / factor) // smaller = more sensitive
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun ViewPager2.setScrollDuration(duration: Int = 300) { // ms
    try {
        val recyclerViewField = ViewPager2::class.java.getDeclaredField("mRecyclerView")
        recyclerViewField.isAccessible = true
        val recyclerView = recyclerViewField.get(this) as RecyclerView
        val scrollerField = RecyclerView::class.java.getDeclaredField("mViewFlinger")
        scrollerField.isAccessible = true
        val flinger = scrollerField.get(recyclerView)

        val interpolator = AccelerateDecelerateInterpolator()
        val scroller = object : Scroller(this.context, interpolator) {
            override fun startScroll(
                startX: Int, startY: Int,
                dx: Int, dy: Int,
                durationOrig: Int
            ) {
                // use the extension function’s duration argument
                super.startScroll(startX, startY, dx, dy, duration)
            }
        }

        val scrollerFieldInner = flinger.javaClass.getDeclaredField("mScroller")
        scrollerFieldInner.isAccessible = true
        scrollerFieldInner.set(flinger, scroller)

    } catch (e: Exception) {
        e.printStackTrace()
    }
}
