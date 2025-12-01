package com.spm.mynanasapp.utils

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.Duration
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {

    fun getTimeAgo(timestamp: String): String {
        // 1. Handle empty strings safely
        if (timestamp.isEmpty()) return ""

        try {
            // 2. Parse the ISO String (e.g., "2025-12-01T14:41:20.000000Z")
            // Instant.parse handles UTC 'Z' automatically.
            val timeCreated = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Instant.parse(timestamp)
            } else {
                // Fallback for very old Android versions if strictly needed (rare nowadays)
                return timestamp
            }

            val now = Instant.now()

            // 3. Calculate difference
            val duration = Duration.between(timeCreated, now)
            val seconds = duration.seconds

            // 4. Return formatted string based on duration
            return when {
                seconds < 60 -> "Just now"
                seconds < 3600 -> "${seconds / 60}m ago" // Minutes
                seconds < 86400 -> "${seconds / 3600}h ago" // Hours
                seconds < 604800 -> "${seconds / 86400}d ago" // Days
                else -> {
                    // If older than a week, show the actual date (e.g., "01 Dec")
                    val formatter = DateTimeFormatter.ofPattern("dd MMM", Locale.getDefault())
                        .withZone(ZoneId.systemDefault())
                    formatter.format(timeCreated)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return timestamp // Return original string if parsing fails
        }
    }
}