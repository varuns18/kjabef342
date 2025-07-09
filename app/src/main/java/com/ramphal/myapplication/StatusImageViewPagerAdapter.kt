package com.ramphal.myapplication // REMEMBER TO REPLACE THIS WITH YOUR ACTUAL PACKAGE NAME

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

// StatusImageViewPagerAdapter is the adapter for the inner ViewPager2 within StatusPageFragment.
// It creates and manages instances of StatusMediaFragment, each displaying a single image or video.
class StatusImageViewPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    // The list of MediaData objects (image or video) to display in the inner ViewPager2.
    private var mediaDataList: List<MediaData> = emptyList()

    /**
     * Updates the data set for the adapter.
     * @param newList A list of MediaData objects.
     */
    fun updateData(newList: List<MediaData>) {
        mediaDataList = newList
        // Notify the adapter that the data set has changed.
        notifyDataSetChanged()
    }

    // Returns the total number of media items (pages) in the inner ViewPager2.
    override fun getItemCount(): Int {
        return mediaDataList.size
    }

    // Creates a new fragment (StatusMediaFragment) for a given position.
    override fun createFragment(position: Int): Fragment {
        val mediaItem = mediaDataList[position]
        // Create an instance of StatusMediaFragment, passing the URL and type.
        return StatusMediaFragment.newInstance(mediaItem.url, mediaItem.type)
    }
}