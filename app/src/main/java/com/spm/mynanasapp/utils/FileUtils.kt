package com.spm.mynanasapp.utils

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import android.provider.OpenableColumns

object FileUtils {
    fun getFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val fileName = "temp_upload_${System.currentTimeMillis()}.jpg"
            val file = File(context.cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getFileSize(context: Context, uri: Uri): Long {
        // CHANGE: Use 'context.contentResolver' instead of 'requireContext()'
        val cursor = context.contentResolver.query(uri, null, null, null, null)

        return cursor?.use {
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            if (it.moveToFirst()) it.getLong(sizeIndex) else 0
        } ?: 0
    }
}