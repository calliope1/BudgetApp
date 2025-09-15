package com.example.budgetapp

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class WeekPagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
    override fun getItemCount(): Int = Int.MAX_VALUE
    override fun createFragment(position: Int): Fragment {
        val offset = position - START_POS
        return WeekFragment.newInstance(offset)
    }
    companion object { const val START_POS = Int.MAX_VALUE / 2 }
}