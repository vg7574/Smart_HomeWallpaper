package com.example.wiggly_woggly_wallpaper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TimePicker
import android.widget.ToggleButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var timePicker: TimePicker
    private lateinit var toggleButton: ToggleButton
    private lateinit var viewModel: MainViewModel  // ViewModel to retain data

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()


        timePicker = findViewById(R.id.timePicker)
        toggleButton = findViewById(R.id.toggleButton)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)

        val days = mapOf(
            "MONDAY" to R.id.mondayButton,
            "TUESDAY" to R.id.tuesdayButton,
            "WEDNESDAY" to R.id.wednesdayButton,
            "THURSDAY" to R.id.thursdayButton,
            "FRIDAY" to R.id.fridayButton,
            "SATURDAY" to R.id.saturdayButton,
            "SUNDAY" to R.id.sundayButton
        )

        days.forEach { (day, buttonId) ->
            findViewById<Button>(buttonId).setOnClickListener {
                viewModel.selectedDay = day
                pickImage()
            }
        }

        toggleButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                setDailyWallpaperAlarm()
            } else {
                cancelAlarm()
            }
        }

        // Restore Image Preview After Rotation
        viewModel.selectedImageUri?.let { showImagePreviewDialog(it) }
    }

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri: Uri? = result.data?.data
            uri?.let {
                viewModel.selectedImageUri = it  // Store image URI in ViewModel
                showImagePreviewDialog(it)
            }
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        imagePickerLauncher.launch(intent)
    }

    private fun showImagePreviewDialog(imageUri: Uri) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_preview, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.previewDialogImage)

        imageView.setImageURI(imageUri)

        AlertDialog.Builder(this)
            .setTitle("Preview Wallpaper")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                contentResolver.takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                saveWallpaperUri(viewModel.selectedDay, imageUri.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveWallpaperUri(day: String, uri: String) {
        val prefs = getSharedPreferences("WALLPAPER_PREFS", Context.MODE_PRIVATE)
        prefs.edit().putString(day, uri).apply()
    }

    private fun setDailyWallpaperAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, timePicker.hour)
            set(Calendar.MINUTE, timePicker.minute)
            set(Calendar.SECOND, 0)
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun cancelAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        alarmManager.cancel(pendingIntent)
    }
}

// ViewModel to retain data across screen rotations
class MainViewModel : ViewModel() {
    var selectedImageUri: Uri? = null
    var selectedDay: String = ""
}
