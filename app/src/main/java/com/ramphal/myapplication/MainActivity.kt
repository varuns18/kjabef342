package com.ramphal.myapplication

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.ContentLoadingProgressBar
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var status: TextView
    private lateinit var pauseButton: MaterialButton
    private lateinit var resumeButton: MaterialButton
    private lateinit var progressBar: ContentLoadingProgressBar
    private var fileUri: Uri? = null // Keep track of the selected URI in case service needs restart

    // Using ActivityResultLauncher for selecting files
    private val selectFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                beginUpload(uri)
            }
        }
    }

    // Broadcast Receiver to get updates from UploadService
    private val uploadUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let { action ->
                when (action) {
                    UploadService.UPLOAD_PROGRESS_BROADCAST -> {
                        val progress = intent.getIntExtra(UploadService.EXTRA_PROGRESS, 0)
                        val message = intent.getStringExtra(UploadService.EXTRA_STATUS_MESSAGE) ?: "Uploading..."
                        setStatus(message)
                        setUploadProgress(progress)
                        setPauseButtonEnabled(true) // Enable pause when upload is active
                    }
                    UploadService.UPLOAD_STATUS_BROADCAST -> {
                        val message = intent.getStringExtra(UploadService.EXTRA_STATUS_MESSAGE) ?: "Upload status update."
                        val uploadUrl = intent.getStringExtra(UploadService.EXTRA_UPLOAD_URL)
                        setStatus(message)
                        if (uploadUrl != null) {
                            setUploadProgress(100)
                            setPauseButtonEnabled(false) // Disable buttons on completion
                        } else if (message.contains("paused") || message.contains("cancelled") || message.contains("failed")) {
                            setPauseButtonEnabled(false) // Enable resume/disable pause
                        }
                    }
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.status)
        progressBar = findViewById(R.id.progressBar)

        val button: Button = findViewById(R.id.button)
        button.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
            }
            selectFileLauncher.launch(Intent.createChooser(intent, "Select file to upload"))
        }

        pauseButton = findViewById(R.id.pause_button)
        pauseButton.setOnClickListener {
            // Send command to service to pause
            val serviceIntent = Intent(this, UploadService::class.java).apply {
                action = UploadService.ACTION_PAUSE_UPLOAD
            }
            startService(serviceIntent) // Use startService to send commands
        }

        resumeButton = findViewById(R.id.resume_button)
        resumeButton.setOnClickListener {
            // Send command to service to resume (or start if not already running)
            fileUri?.let {
                val serviceIntent = Intent(this, UploadService::class.java).apply {
                    action = Intent.ACTION_VIEW // A general action to indicate starting upload
                    putExtra(UploadService.EXTRA_FILE_URI, it)
                }
                startService(serviceIntent) // Use startService
            } ?: run {
                showError(IllegalStateException("No file selected to resume upload."))
            }
        }

        // Initial state for buttons
        setPauseButtonEnabled(false)
        setStatus("Select a file to upload.")
    }

    override fun onResume() {
        super.onResume()
        // Register broadcast receiver when activity is in foreground
        val filter = IntentFilter().apply {
            addAction(UploadService.UPLOAD_PROGRESS_BROADCAST)
            addAction(UploadService.UPLOAD_STATUS_BROADCAST)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(uploadUpdateReceiver, filter)
    }

    override fun onPause() {
        super.onPause()
        // Unregister broadcast receiver when activity goes to background
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uploadUpdateReceiver)
    }

    private fun beginUpload(uri: Uri) {
        fileUri = uri // Store the URI
        // Start the UploadService with the file URI
        val serviceIntent = Intent(this, UploadService::class.java).apply {
            action = Intent.ACTION_VIEW // A general action to indicate starting upload
            putExtra(UploadService.EXTRA_FILE_URI, uri)
        }
        // Use startForegroundService for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun setPauseButtonEnabled(enabled: Boolean) {
        pauseButton.isEnabled = enabled
        resumeButton.isEnabled = !enabled
    }

    private fun setStatus(text: String) {
        status.text = text
    }

    private fun setUploadProgress(progress: Int) {
        progressBar.progress = progress
    }

    private fun showError(e: Exception) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Error")
        builder.setMessage(e.message)
        val dialog = builder.create()
        dialog.show()
        e.printStackTrace()
    }
}