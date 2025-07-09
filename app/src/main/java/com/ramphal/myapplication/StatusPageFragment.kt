package com.ramphal.myapplication // REMEMBER TO REPLACE THIS WITH YOUR ACTUAL PACKAGE NAME

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.ramphal.myapplication.databinding.FragmentStatusPageBinding
import java.util.ArrayList

class StatusPageFragment : Fragment() {

    private var _binding: FragmentStatusPageBinding? = null
    private val binding get() = _binding!!

    private lateinit var innerViewPager2Adapter: StatusImageViewPagerAdapter
    private var tabLayoutMediator: TabLayoutMediator? = null

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            super.onPageSelected(position)
            updateNavigationButtonsVisibility(position)
        }
    }

    companion object {
        // Argument key for passing a list of MediaData objects to this fragment.
        private const val ARG_MEDIA_DATA_LIST = "media_data_list"

        /**
         * Creates a new instance of StatusPageFragment with a list of MediaData objects.
         * @param mediaDataList An ArrayList of MediaData objects for the inner ViewPager2.
         * @return A new StatusPageFragment instance.
         */
        fun newInstance(mediaDataList: ArrayList<MediaData>): StatusPageFragment {
            val fragment = StatusPageFragment()
            val args = Bundle()
            // Note: MediaData needs to be Parcelable or Serializable to be passed directly.
            // For simplicity, we'll assume it's directly passable or convert it if needed.
            // A common approach is to pass a Bundle of individual URLs and types.
            // For now, let's assume MediaData is Parcelable if you haven't made it so,
            // or we'll adjust if it causes issues.
            args.putParcelableArrayList(ARG_MEDIA_DATA_LIST, mediaDataList) // Requires MediaData to be Parcelable
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatusPageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        innerViewPager2Adapter = StatusImageViewPagerAdapter(this)
        binding.innerStatusViewPager.adapter = innerViewPager2Adapter

        binding.innerStatusViewPager.offscreenPageLimit = 2

        // Retrieve the list of MediaData objects from arguments.
        arguments?.getParcelableArrayList<MediaData>(ARG_MEDIA_DATA_LIST)?.let { mediaList ->
            innerViewPager2Adapter.updateData(mediaList)
            updateNavigationButtonsVisibility(0)

            // Link TabLayout with ViewPager2
            if (mediaList.isNotEmpty()) {
                tabLayoutMediator = TabLayoutMediator(
                    binding.tabLayoutIndicator,
                    binding.innerStatusViewPager
                ) { tab, position ->
                    // No text needed for dot indicators
                }
                tabLayoutMediator?.attach()
            } else {
                binding.tabLayoutIndicator.visibility = View.GONE
            }
        }

        binding.btnInnerLeft.setOnClickListener {
            val currentItem = binding.innerStatusViewPager.currentItem
            if (currentItem > 0) {
                binding.innerStatusViewPager.currentItem = currentItem - 1
            }
        }

        binding.btnInnerRight.setOnClickListener {
            val currentItem = binding.innerStatusViewPager.currentItem
            if (currentItem < (innerViewPager2Adapter.itemCount - 1)) {
                binding.innerStatusViewPager.currentItem = currentItem + 1
            }
        }

        binding.innerStatusViewPager.registerOnPageChangeCallback(pageChangeCallback)
    }

    private fun updateNavigationButtonsVisibility(position: Int) {
        val itemCount = innerViewPager2Adapter.itemCount
        binding.btnInnerLeft.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
        binding.btnInnerRight.visibility = if (position == itemCount - 1) View.INVISIBLE else View.VISIBLE

        binding.tabLayoutIndicator.visibility = if (itemCount > 1) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tabLayoutMediator?.detach()
        binding.innerStatusViewPager.unregisterOnPageChangeCallback(pageChangeCallback)
        _binding = null
    }
}