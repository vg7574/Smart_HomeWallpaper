package com.example.wiggly_woggly_wallpaper

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TimePicker
import android.widget.ToggleButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var timePicker: TimePicker
    private lateinit var toggleButton: ToggleButton
    private lateinit var previewImage: ImageView

    private var selectedDay: String = ""

    // Register activity result launcher
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            showImagePreviewDialog(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()

        timePicker = findViewById(R.id.timePicker)
        toggleButton = findViewById(R.id.toggleButton)
        // previewImage = findViewById(R.id.previewImage)

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
                selectedDay = day
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
    }

    private fun pickImage() {
        pickImageLauncher.launch(arrayOf("image/*"))
    }

    private fun showImagePreviewDialog(imageUri: Uri) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_preview, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.previewDialogImage)

        val bitmap = getScaledBitmapForScreen(imageUri)
        imageView.setImageBitmap(bitmap)

        AlertDialog.Builder(this)
            .setTitle("Preview Wallpaper")
            .setView(dialogView)
            .setPositiveButton("OK") { _, _ ->
                // ✅ User confirms -> Save & Proceed
                contentResolver.takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                saveWallpaperUri(selectedDay, imageUri.toString())
            }
            .setNegativeButton("Cancel", null) // ❌ User cancels -> Do nothing
            .show()
    }

    private fun getScaledBitmapForScreen(imageUri: Uri): Bitmap {
        val displayMetrics = resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val inputStream = contentResolver.openInputStream(imageUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        return Bitmap.createScaledBitmap(originalBitmap, screenWidth, screenHeight, true)
    }


    private fun saveWallpaperUri(day: String, uri: String) {
        val prefs = getSharedPreferences("WALLPAPER_PREFS", Context.MODE_PRIVATE)
        prefs.edit().putString(day, uri).apply()
    }

    private fun setDailyWallpaperAlarm() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

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
        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarmManager.cancel(pendingIntent)
    }
}
