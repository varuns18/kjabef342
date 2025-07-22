package com.ramphal.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.tus.android.client.TusAndroidUpload
import io.tus.android.client.TusPreferencesURLStore
import io.tus.java.client.TusClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.ConcurrentLinkedQueue // For thread-safe queue

class UploadService : Service() {

    private lateinit var client: TusClient
    private var uploadJob: Job? = null
    private val uploadQueue: ConcurrentLinkedQueue<Uri> = ConcurrentLinkedQueue() // Queue for multiple files
    private var currentFileIndex: Int = 0 // To track which file is currently uploading
    private var totalFiles: Int = 0 // Total number of files in the current batch

    private lateinit var notificationManager: NotificationManager
    private val NOTIFICATION_ID = 101
    private val CHANNEL_ID = "TusUploadChannel"

    companion object {
        const val ACTION_PAUSE_UPLOAD = "com.ramphal.myapplication.ACTION_PAUSE_UPLOAD"
        const val ACTION_RESUME_UPLOAD = "com.ramphal.myapplication.ACTION_RESUME_UPLOAD"
        const val ACTION_STOP_SERVICE = "com.ramphal.myapplication.ACTION_STOP_SERVICE"

        const val EXTRA_FILE_URI = "extra_file_uri" // For single file (legacy)
        const val EXTRA_FILE_URIS = "extra_file_uris" // For multiple files
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        try {
            val pref: SharedPreferences = getSharedPreferences("tus", 0)
            client = TusClient()
            // Make sure this URL is correct for your tusd server
            client.uploadCreationURL = URL("https://tusd.tusdemo.net/files/")
            client.enableResuming(TusPreferencesURLStore(pref))
        } catch (e: Exception) {
            showErrorNotification("Service initialization error: ${e.message}")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE_UPLOAD -> pauseUpload()
            ACTION_RESUME_UPLOAD -> resumeUpload()
            ACTION_STOP_SERVICE -> stopSelf()
            Intent.ACTION_VIEW -> {
                // Handle multiple URIs first
                val uris = intent.getParcelableArrayListExtra<Uri>(EXTRA_FILE_URIS)
                if (!uris.isNullOrEmpty()) {
                    uploadQueue.clear() // Clear any existing queue before adding new batch
                    uploadQueue.addAll(uris)
                    totalFiles = uris.size
                    currentFileIndex = 0 // Reset index for new batch
                    // If no upload is active, start the next one
                    if (uploadJob == null || !uploadJob!!.isActive) {
                        processNextFileInQueue()
                    } else {
                        // If an upload is active, just update notification for queue size
                        updateNotification("Added ${uris.size} files to queue.", 0, true)
                    }
                } else {
                    // Fallback for single URI (if you still want to support it)
                    val uri = intent.getParcelableExtra<Uri>(EXTRA_FILE_URI)
                    if (uri != null) {
                        uploadQueue.clear()
                        uploadQueue.add(uri)
                        totalFiles = 1
                        currentFileIndex = 0
                        if (uploadJob == null || !uploadJob!!.isActive) {
                            processNextFileInQueue()
                        }
                    } else {
                        showErrorNotification("No file URI(s) provided for upload.")
                        stopSelf()
                    }
                }
            }
            else -> {
                // If service is restarted by system, or just started, try to continue queue
                if (uploadQueue.isNotEmpty() && (uploadJob == null || !uploadJob!!.isActive)) {
                    processNextFileInQueue()
                } else {
                    // Initial state or no files in queue
                    startForeground(NOTIFICATION_ID, buildProgressNotification("Waiting for files...", 0, true))
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        uploadJob?.cancel()
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Tus Uploads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for file uploads using Tus protocol"
            }
            notificationManager.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildProgressNotification(statusText: String, progress: Int, isIndeterminate: Boolean = false): Notification {
        val pauseIntent = PendingIntent.getService(
            this, 0, Intent(this, UploadService::class.java).apply { action = ACTION_PAUSE_UPLOAD },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val resumeIntent = PendingIntent.getService(
            this, 1, Intent(this, UploadService::class.java).apply { action = ACTION_RESUME_UPLOAD },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 2, Intent(this, UploadService::class.java).apply { action = ACTION_STOP_SERVICE },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val activityIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Add file counter to title
        val fileCounterText = if (totalFiles > 1) "Files: ${currentFileIndex + 1}/$totalFiles" else ""

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("File Upload $fileCounterText")
            .setContentText(statusText)
            .setContentIntent(activityIntent)
            .setOngoing(true)
            .apply {
                if (isIndeterminate) {
                    setProgress(0, 0, true)
                } else {
                    setProgress(100, progress, false)
                }
            }

        if (uploadJob?.isActive == true) {
            builder.addAction(0, "Pause", pauseIntent)
            builder.addAction(0, "Stop", stopIntent)
        } else {
            builder.addAction(0, "Resume", resumeIntent)
            builder.addAction(0, "Stop", stopIntent)
        }

        return builder.build()
    }

    private fun updateNotification(statusText: String, progress: Int, isIndeterminate: Boolean = false) {
        val notification = buildProgressNotification(statusText, progress, isIndeterminate)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showErrorNotification(message: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle("Upload Error")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(false)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun pauseUpload() {
        uploadJob?.cancel()
        updateNotification("Upload paused.", progressBar.progress, isIndeterminate = false)
    }

    private fun resumeUpload() {
        // If an upload is paused, resume it on the current file
        if (uploadJob?.isActive == false && uploadQueue.isNotEmpty()) {
            processNextFileInQueue() // Will effectively resume the last one
        } else if (uploadQueue.isEmpty()) {
            showErrorNotification("No files in queue to resume.")
            stopSelf()
        } else {
            // Already active or just called resume without a clear pause
            updateNotification("Upload is active.", progressBar.progress)
        }
    }

    private fun processNextFileInQueue() {
        val nextUri = uploadQueue.peek() // Peek to get the next URI without removing it yet

        if (nextUri == null) {
            // Queue is empty, all uploads finished
            updateNotification("All files uploaded!", 100, false)
            stopForeground(false)
            stopSelf()
            return
        }

        // Increment index before starting upload for display purposes
        currentFileIndex = totalFiles - uploadQueue.size + 1

        uploadJob?.cancel() // Cancel any ongoing job before starting a new one

        uploadJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Resolve file name for better notification text
                val fileName = getFileName(nextUri)
                val initialStatus = "Uploading $fileName..."
                startForeground(NOTIFICATION_ID, buildProgressNotification(initialStatus, 0, true))

                val upload = TusAndroidUpload(nextUri, this@UploadService)
                val uploader = client.resumeOrCreateUpload(upload)
                val totalBytes = upload.size
                var uploadedBytes = uploader.offset

                uploader.chunkSize = 1024 * 1024

                while (isActive && uploader.uploadChunk() > 0) {
                    uploadedBytes = uploader.offset
                    val progress = ((uploadedBytes.toDouble() / totalBytes) * 100).toInt()
                    val statusMsg = "Uploading $fileName: ${progress}% | $uploadedBytes/$totalBytes."
                    updateNotification(statusMsg, progress)
                }

                uploader.finish()
                val uploadURL = uploader.uploadURL
                updateNotification("Finished $fileName!", 100, false)

                // Successfully uploaded, remove from queue and process next
                uploadQueue.poll() // Remove the just-uploaded file from the queue
                processNextFileInQueue() // Recursively call to process the next file
            } catch (e: Exception) {
                if (e is CancellationException) {
                    updateNotification("Upload of ${getFileName(nextUri)} cancelled.", 0, false)
                } else {
                    showErrorNotification("Upload of ${getFileName(nextUri)} failed: ${e.message}")
                }
                // Do not remove from queue on error/cancellation if you want to retry later
                // For now, it will remain in queue and user can try to resume.
                stopForeground(false) // Keep notification visible for status
            }
        }
    }

    // Helper function to get file name from Uri
    private fun getFileName(uri: Uri): String {
        return contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    cursor.getString(nameIndex)
                } else {
                    uri.lastPathSegment ?: "unknown_file"
                }
            } else {
                uri.lastPathSegment ?: "unknown_file"
            }
        } ?: uri.lastPathSegment ?: "unknown_file"
    }
}
