package com.ramphal.myapplication // REMEMBER TO REPLACE THIS WITH YOUR ACTUAL PACKAGE NAME

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import java.util.ArrayList

// StatusViewPagerAdapter is the adapter for the outer ViewPager2 in StatusFragment.
// It creates and manages instances of StatusPageFragment.
class StatusViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    // The list of data for each page. Each item is an ArrayList of MediaData.
    private var listOfMediaLists: List<ArrayList<MediaData>> = emptyList()

    /**
     * Updates the data set for the adapter.
     * @param newList A list where each element is an ArrayList of MediaData
     * corresponding to a page in the outer ViewPager2.
     */
    fun updateData(newList: List<ArrayList<MediaData>>) {
        listOfMediaLists = newList
        notifyDataSetChanged()
    }

    // Returns the total number of pages (status items) in the outer ViewPager2.
    override fun getItemCount(): Int {
        return listOfMediaLists.size
    }

    // Creates a new fragment for a given position in the outer ViewPager2.
    override fun createFragment(position: Int): Fragment {
        // Create an instance of StatusPageFragment, passing the specific list of MediaData
        // for that page's inner ViewPager2.
        return StatusPageFragment.newInstance(listOfMediaLists[position])
    }
}