package com.ramphal.myapplication


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide // Import the Glide library
import com.ramphal.myapplication.databinding.FragmentStatusImageBinding

// StatusImageFragment displays a single image loaded from a URL.
// It is used as a page within the inner ViewPager2.
class StatusImageFragment : Fragment() {

    private var _binding: FragmentStatusImageBinding? = null
    private val binding get() = _binding!!

    companion object {
        // Argument key for passing the image URL to this fragment.
        private const val ARG_IMAGE_URL = "image_url"

        /**
         * Creates a new instance of StatusImageFragment with a given image URL.
         * @param imageUrl The URL of the image to display.
         * @return A new StatusImageFragment instance.
         */
        fun newInstance(imageUrl: String): StatusImageFragment {
            val fragment = StatusImageFragment()
            val args = Bundle()
            args.putString(ARG_IMAGE_URL, imageUrl)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatusImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve the image URL from the fragment's arguments.
        arguments?.getString(ARG_IMAGE_URL)?.let { imageUrl ->
            // Use Glide to load the image from the URL into the ImageView.
            // .with(this) provides the lifecycle context for Glide.
            // .load(imageUrl) specifies the image source.
            // .placeholder() shows a drawable while the image is loading.
            // .error() shows a drawable if the image fails to load.
            // .into() specifies the ImageView target.
            Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.progress_horizontal) // System default loading spinner
                .error(android.R.drawable.ic_menu_close_clear_cancel) // System default error icon
                .into(binding.statusImageView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clear the binding object to prevent memory leaks.
        _binding = null
    }
}
