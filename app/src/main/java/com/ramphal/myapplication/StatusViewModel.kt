package com.ramphal.myapplication // REMEMBER TO REPLACE THIS WITH YOUR ACTUAL PACKAGE NAME

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.util.ArrayList

// StatusViewModel manages the data for the status screen.
// It holds a LiveData object that contains a list of lists of MediaData (image or video).
// Each inner list represents the media items for one page in the outer ViewPager2.
class StatusViewModel : ViewModel() {

    // MutableLiveData to hold the main data structure:
    // List<ArrayList<MediaData>> where:
    // - Outer List: Represents pages for the outer ViewPager2.
    // - Inner ArrayList<MediaData>: Represents media items (image or video) for the inner ViewPager2.
    private val _statusPagesWithMedia = MutableLiveData<List<ArrayList<MediaData>>>()

    // Publicly exposed LiveData for UI components to observe.
    val statusPagesWithMedia: LiveData<List<ArrayList<MediaData>>>
        get() = _statusPagesWithMedia

    // Counter to simulate different sets of data on refresh.
    private var updateCount = 0

    init {
        // Initialize the LiveData with some default media items (images and videos).
        _statusPagesWithMedia.value = listOf(
            arrayListOf(
                MediaData("https://picsum.photos/id/10/400/600", "image"),
                MediaData("https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4", "video"), // Sample MP4
                MediaData("https://picsum.photos/id/30/400/600", "image")
            ),
            arrayListOf(
                MediaData("https://picsum.photos/id/40/400/600", "image"),
                MediaData("https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4", "video"), // Another sample MP4
                MediaData("https://picsum.photos/id/60/400/600", "image")
            ),
            arrayListOf(
                MediaData("https://picsum.photos/id/70/400/600", "image")
            )
        )
    }

    /**
     * Simulates fetching or updating all status pages with new media data.
     * In a real application, this method would typically involve making network requests
     * or querying a local database to get actual status data and media URLs.
     * The LiveData would then be updated with the new data.
     */
    fun refreshAllStatuses() {
        updateCount++
        val newListOfMediaLists = mutableListOf<ArrayList<MediaData>>()

        // Example: Generate new sets of media items for different pages.
        newListOfMediaLists.add(
            arrayListOf(
                MediaData("https://picsum.photos/id/${100 + updateCount}/400/600", "image"),
                MediaData("https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4", "video"),
                MediaData("https://picsum.photos/id/${102 + updateCount}/400/600", "image")
            )
        )
        newListOfMediaLists.add(
            arrayListOf(
                MediaData("https://picsum.photos/id/${200 + updateCount}/400/600", "image"),
                MediaData("https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4", "video")
            )
        )
        newListOfMediaLists.add(
            arrayListOf(
                MediaData("https://picsum.photos/id/${300 + updateCount}/400/600", "image"),
                MediaData("https://picsum.photos/id/${301 + updateCount}/400/600", "image"),
                MediaData("https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4", "video"),
                MediaData("https://picsum.photos/id/${303 + updateCount}/400/600", "image")
            )
        )

        if (updateCount % 2 == 0) {
            newListOfMediaLists.add(
                arrayListOf(
                    MediaData("https://picsum.photos/id/${400 + updateCount}/400/600", "image"),
                    MediaData("https://www.learningcontainer.com/wp-content/uploads/2020/05/sample-mp4-file.mp4", "video")
                )
            )
        }

        _statusPagesWithMedia.value = newListOfMediaLists
    }
}