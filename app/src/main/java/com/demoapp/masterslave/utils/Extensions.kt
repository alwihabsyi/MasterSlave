package com.demoapp.masterslave.utils

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.widget.Toast
import java.io.File

fun Activity.toast(msg: String?) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

fun Activity.getFileFromUri(uri: Uri): File? {
    return try {
        val fileName = getFileNameFromUri(uri)
        val inputStream = contentResolver.openInputStream(uri)
        val file = File(cacheDir, fileName)

        inputStream?.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        file
    } catch (e: Exception) {
        Log.e("MASTER", "Error getting file from URI: $uri", e)
        null
    }
}

fun Activity.getFileNameFromUri(uri: Uri): String {
    var fileName = "temp_file"
    val cursor = contentResolver.query(uri, null, null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (displayNameIndex >= 0) {
                fileName = it.getString(displayNameIndex)
            }
        }
    }
    return fileName
}

fun Context.directoryName() = "Android/media/${packageName}/files"