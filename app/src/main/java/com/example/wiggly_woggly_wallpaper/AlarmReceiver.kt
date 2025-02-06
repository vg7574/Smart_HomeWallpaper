package com.example.wiggly_woggly_wallpaper

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.WindowManager
import java.io.InputStream
import java.util.*

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val wallpaperManager = WallpaperManager.getInstance(context)
        val dayOfWeek = getCurrentDayOfWeek()

        val prefs = context.getSharedPreferences("WALLPAPER_PREFS", Context.MODE_PRIVATE)
        val uriString = prefs.getString(dayOfWeek, null)

        uriString?.let {
            try {
                val uri = Uri.parse(it)
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)

                bitmap?.let {
                    val (width, height) = getScreenDimensions(context)

                    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
                    wallpaperManager.setBitmap(scaledBitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                }
            } catch (e: Exception) {
                Log.e("AlarmReceiver", "Error setting wallpaper: ${e.message}")
            }
        }
    }

    private fun getCurrentDayOfWeek(): String {
        return when (Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> "MONDAY"
            Calendar.TUESDAY -> "TUESDAY"
            Calendar.WEDNESDAY -> "WEDNESDAY"
            Calendar.THURSDAY -> "THURSDAY"
            Calendar.FRIDAY -> "FRIDAY"
            Calendar.SATURDAY -> "SATURDAY"
            else -> "SUNDAY"
        }
    }

    private fun getScreenDimensions(context: Context): Pair<Int, Int> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ (API 30+)
            val windowMetrics = context.getSystemService(WindowManager::class.java).currentWindowMetrics
            val bounds = windowMetrics.bounds
            Pair(bounds.width(), bounds.height())
        } else {
            // Below Android 11
            val displayMetrics = context.resources.displayMetrics
            Pair(displayMetrics.widthPixels, displayMetrics.heightPixels)
        }
    }
}
