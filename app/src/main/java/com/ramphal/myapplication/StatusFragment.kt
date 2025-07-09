package com.ramphal.myapplication // REMEMBER TO REPLACE THIS WITH YOUR ACTUAL PACKAGE NAME

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.ramphal.myapplication.databinding.FragmentStatusBinding

class StatusFragment : Fragment() {

    private var _binding: FragmentStatusBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: StatusViewModel
    private lateinit var outerViewPager2Adapter: StatusViewPagerAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(this).get(StatusViewModel::class.java)

        outerViewPager2Adapter = StatusViewPagerAdapter(this)
        binding.statusViewPager.adapter = outerViewPager2Adapter

        binding.statusViewPager.offscreenPageLimit = 2

        // Observe the LiveData containing lists of MediaData from the ViewModel.
        viewModel.statusPagesWithMedia.observe(viewLifecycleOwner) { listOfMediaLists ->
            outerViewPager2Adapter.updateData(listOfMediaLists)
        }

        binding.refreshStatusButton.setOnClickListener {
            viewModel.refreshAllStatuses()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}