package com.tavrida.ElectroCounters.utils

import android.content.Context
import android.os.Environment
import java.io.*

object Asset {
    /**
     * Copies specified asset to the file in /files app directory and returns this file absolute path.
     *
     * @return absolute file path
     */
    @Throws(IOException::class)
    fun getFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        context.assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { os ->
                inputStream.copyTo(os)
                os.flush()
            }
            return file.absolutePath
        }
    }

    @Throws(IOException::class)
    fun getFilePath(context: Context, assetName: String, deleteOtherVersions: Boolean): String {
        val filePath = getFilePath(context, assetName)
        if (deleteOtherVersions) {
            deleteOtherVersions(filePath)
        }
        return filePath
    }

    fun deleteOtherVersions(filePath: String) {
        val (name, version) = splitNameVersion(filePath)

        TODO()
    }

    fun fileInDownloads(childObj: String = "") = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), childObj)
}