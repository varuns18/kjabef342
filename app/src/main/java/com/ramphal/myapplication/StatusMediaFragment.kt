package com.ramphal.myapplication // REMEMBER TO REPLACE THIS WITH YOUR ACTUAL PACKAGE NAME

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem as ExoMediaItem // Alias to avoid conflict with our MediaData
import com.ramphal.myapplication.databinding.FragmentStatusMediaBinding // Import the new binding class

// StatusMediaFragment displays either an image or a video based on the provided URL and type.
class StatusMediaFragment : Fragment() {

    private var _binding: FragmentStatusMediaBinding? = null
    private val binding get() = _binding!!

    private var exoPlayer: ExoPlayer? = null
    private var mediaUrl: String? = null
    private var mediaType: String? = null

    companion object {
        private const val ARG_MEDIA_URL = "media_url"
        private const val ARG_MEDIA_TYPE = "media_type" // "image" or "video"

        /**
         * Creates a new instance of StatusMediaFragment.
         * @param url The URL of the media (image or video).
         * @param type The type of media ("image" or "video").
         * @return A new StatusMediaFragment instance.
         */
        fun newInstance(url: String, type: String): StatusMediaFragment {
            val fragment = StatusMediaFragment()
            val args = Bundle()
            args.putString(ARG_MEDIA_URL, url)
            args.putString(ARG_MEDIA_TYPE, type)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatusMediaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Retrieve media URL and type from arguments
        mediaUrl = arguments?.getString(ARG_MEDIA_URL)
        mediaType = arguments?.getString(ARG_MEDIA_TYPE)

        // Load media based on its type
        when (mediaType) {
            "image" -> {
                binding.mediaImageView.visibility = View.VISIBLE
                binding.mediaPlayerView.visibility = View.GONE
                mediaUrl?.let { url ->
                    Glide.with(this)
                        .load(url)
                        .placeholder(android.R.drawable.progress_horizontal)
                        .error(android.R.drawable.ic_menu_close_clear_cancel)
                        .into(binding.mediaImageView)
                }
            }
            "video" -> {
                binding.mediaImageView.visibility = View.GONE
                binding.mediaPlayerView.visibility = View.VISIBLE
                // Initialize ExoPlayer when the view is created
                initializePlayer()
            }
            else -> {
                // Handle unsupported media type or error
                binding.mediaImageView.visibility = View.VISIBLE
                binding.mediaPlayerView.visibility = View.GONE
                binding.mediaImageView.setImageResource(android.R.drawable.ic_dialog_alert) // Show a generic error icon
            }
        }
    }

    /**
     * Initializes the ExoPlayer instance and prepares it for video playback.
     */
    private fun initializePlayer() {
        if (exoPlayer == null) {
            exoPlayer = ExoPlayer.Builder(requireContext()).build()
            binding.mediaPlayerView.player = exoPlayer
        }
        mediaUrl?.let { url ->
            val mediaItem = ExoMediaItem.fromUri(url)
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            exoPlayer?.playWhenReady = true // Start playing automatically
        }
    }

    /**
     * Releases the ExoPlayer instance.
     */
    private fun releasePlayer() {
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onStart() {
        super.onStart()
        // Re-initialize player if it was released (e.g., after returning from background)
        if (mediaType == "video" && exoPlayer == null) {
            initializePlayer()
        } else if (mediaType == "video") {
            exoPlayer?.playWhenReady = true // Resume playback
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure player resumes if fragment becomes visible again
        if (mediaType == "video" && exoPlayer != null) {
            exoPlayer?.playWhenReady = true
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause playback when fragment is not visible
        if (mediaType == "video" && exoPlayer != null) {
            exoPlayer?.playWhenReady = false
        }
    }

    override fun onStop() {
        super.onStop()
        // Release player when fragment is no longer visible to save resources
        if (mediaType == "video") {
            releasePlayer()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Ensure player is released when view is destroyed
        if (mediaType == "video") {
            releasePlayer()
        }
        _binding = null
    }
}