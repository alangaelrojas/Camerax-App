package com.apps.aggr.cameraxapp.utils

import android.app.Activity
import java.io.File

object Constants {

    const val CROPPED_PATH_FOLDER = "/cropped"
    const val NORMAL_PATH_FOLDER = "/normal"

    fun Activity.getOutputMediaFile(folder: String, fileName: String): File? {
        var cache: String
        val f: File? = externalCacheDir
        return f?.let {
            cache = it.absolutePath + folder
            val newFile = File(cache)
            if (!newFile.exists()) newFile.mkdirs()
            File(cache, fileName)
        }
    }
}