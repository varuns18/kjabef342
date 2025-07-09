package com.ramphal.myapplication

import android.os.Parcelable
import kotlinx.parcelize.Parcelize // Import for @Parcelize annotation

// Data class to represent a single media item (image or video).
// It holds the URL and the type of media.
// @Parcelize annotation automatically generates Parcelable implementation.
@Parcelize
data class MediaData(val url: String, val type: String) : Parcelable