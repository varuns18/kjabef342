package com.ramphal.myapplication

import android.app.Notification
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
import androidx.localbroadcastmanager.content.LocalBroadcastManager
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

class UploadService : Service() {

    private lateinit var client: TusClient
    private var uploadJob: Job? = null
    private var fileUri: Uri? = null

    private lateinit var notificationManager: NotificationManager
    private val NOTIFICATION_ID = 101
    private val CHANNEL_ID = "TusUploadChannel"

    // Action constants for PendingIntents
    companion object {
        const val ACTION_PAUSE_UPLOAD = "com.ramphal.myapplication.ACTION_PAUSE_UPLOAD"
        const val ACTION_RESUME_UPLOAD = "com.ramphal.myapplication.ACTION_RESUME_UPLOAD"
        const val ACTION_STOP_SERVICE = "com.ramphal.myapplication.ACTION_STOP_SERVICE"

        const val EXTRA_FILE_URI = "extra_file_uri"

        // Broadcast actions for UI updates
        const val UPLOAD_PROGRESS_BROADCAST = "com.ramphal.myapplication.UPLOAD_PROGRESS"
        const val UPLOAD_STATUS_BROADCAST = "com.ramphal.myapplication.UPLOAD_STATUS"
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_STATUS_MESSAGE = "extra_status_message"
        const val EXTRA_UPLOAD_URL = "extra_upload_url"
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()

        try {
            val pref: SharedPreferences = getSharedPreferences("tus", 0)
            client = TusClient()
            client.uploadCreationURL = URL("https://tusd.tusdemo.net/files/")
            client.enableResuming(TusPreferencesURLStore(pref))
        } catch (e: Exception) {
            // Handle initialization error, maybe stop service or show persistent error notification
            showErrorNotification("Service initialization error: ${e.message}")
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE_UPLOAD -> pauseUpload()
            ACTION_RESUME_UPLOAD -> resumeUpload()
            ACTION_STOP_SERVICE -> stopSelf()
            Intent.ACTION_VIEW -> { // This is how MainActivity will typically start the upload
                intent.getParcelableExtra<Uri>(EXTRA_FILE_URI)?.let { uri ->
                    fileUri = uri
                    resumeUpload() // Start or resume the upload
                } ?: run {
                    showErrorNotification("No file URI provided for upload.")
                    stopSelf()
                }
            }
            else -> {
                // If service is restarted by system after being killed, attempt to resume
                // fileUri might be null if not explicitly passed
                if (fileUri != null) {
                    resumeUpload()
                } else {
                    // Start with an indefinite progress notification
                    startForeground(NOTIFICATION_ID, buildProgressNotification("Waiting for file selection...", 0))
                }
            }
        }
        return START_NOT_STICKY // Service will not be automatically restarted if killed by system
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // We don't need to bind to this service
    }

    override fun onDestroy() {
        super.onDestroy()
        uploadJob?.cancel() // Cancel any ongoing upload when service is destroyed
        notificationManager.cancel(NOTIFICATION_ID) // Clear the notification
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

        // Intent to open MainActivity when notification is tapped
        val activityIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Replace with your app icon
            .setContentTitle("File Upload")
            .setContentText(statusText)
            .setContentIntent(activityIntent) // Open app on tap
            .setOngoing(true) // Makes the notification non-dismissable by user
            .apply {
                if (isIndeterminate) {
                    setProgress(0, 0, true) // Indeterminate progress
                } else {
                    setProgress(100, progress, false) // Determinate progress
                }
            }

        // Add actions based on current upload state (simplified here)
        if (uploadJob?.isActive == true) { // Upload is active
            builder.addAction(0, "Pause", pauseIntent)
            builder.addAction(0, "Stop", stopIntent)
        } else { // Upload is paused or not started
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
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun pauseUpload() {
        uploadJob?.cancel()
        updateNotification("Upload paused.", 0, isIndeterminate = false) // Set progress to 0 or last known
        sendUploadStatusBroadcast("Upload paused.")
    }

    private fun resumeUpload() {
        fileUri?.let { uri ->
            uploadJob?.cancel() // Cancel any previous job

            uploadJob = CoroutineScope(Dispatchers.IO).launch { // Run coroutine on IO dispatcher directly
                try {
                    val upload = TusAndroidUpload(uri, this@UploadService) // Pass service context
                    withContext(Dispatchers.Main) { // Update notification and UI on Main thread
                        startForeground(NOTIFICATION_ID, buildProgressNotification("Upload selected...", 0, isIndeterminate = true))
                        sendUploadStatusBroadcast("Upload selected...")
                    }

                    val uploader = client.resumeOrCreateUpload(upload)
                    val totalBytes = upload.size
                    var uploadedBytes = uploader.offset

                    uploader.chunkSize = 1024 * 1024

                    while (isActive && uploader.uploadChunk() > 0) {
                        uploadedBytes = uploader.offset
                        withContext(Dispatchers.Main) {
                            val progress = ((uploadedBytes.toDouble() / totalBytes) * 100).toInt()
                            val statusMsg = "Uploading ${progress}% | $uploadedBytes/$totalBytes."
                            updateNotification(statusMsg, progress)
                            sendUploadProgressBroadcast(progress, statusMsg)
                        }
                    }

                    uploader.finish()
                    val uploadURL = uploader.uploadURL
                    withContext(Dispatchers.Main) {
                        updateNotification("Upload finished!", 100)
                        sendUploadStatusBroadcast("Upload finished!\n${uploadURL.toString()}", uploadURL.toString())
                        stopForeground(false) // Keep notification if you want to show final status
                        stopSelf() // Stop the service after successful upload
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        withContext(Dispatchers.Main) {
                            updateNotification("Upload cancelled.", 0)
                            sendUploadStatusBroadcast("Upload cancelled.")
                            // Do not stop foreground if you want notification to persist
                            // stopForeground(false)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            showErrorNotification("Upload failed: ${e.message}")
                            sendUploadStatusBroadcast("Upload failed: ${e.message}")
                            stopSelf() // Stop service on error
                        }
                    }
                }
            }
        } ?: run {
            showErrorNotification("No file URI available to resume upload.")
            stopSelf()
        }
    }

    private fun sendUploadProgressBroadcast(progress: Int, statusMessage: String) {
        val intent = Intent(UPLOAD_PROGRESS_BROADCAST).apply {
            putExtra(EXTRA_PROGRESS, progress)
            putExtra(EXTRA_STATUS_MESSAGE, statusMessage)
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun sendUploadStatusBroadcast(statusMessage: String, uploadUrl: String? = null) {
        val intent = Intent(UPLOAD_STATUS_BROADCAST).apply {
            putExtra(EXTRA_STATUS_MESSAGE, statusMessage)
            uploadUrl?.let { putExtra(EXTRA_UPLOAD_URL, it) }
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }
}