package com.tavrida.example.ElectroCounters.detection

import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class DetectionStorage(val storageDirectory: File) {
    val items = loadItems(storageDirectory)

    fun hasItems() = items.isNotEmpty()

    fun newItem(filesAction: (originalImg: File, detectionsImg: File, detectionsInfo: File) -> Unit) {
        val stamp = createTimestamp()
        File(storageDirectory, stamp)
                .apply { mkdirs() }
                .also { dir ->
                    val originalImg = File(dir, ORIGINAL_IMG_FILE)
                    val detectionsImg = File(dir, DETECTIONS_IMG_FILE)
                    val detectionInfo = File(dir, DETECTIONS_INFO_FILE)

                    filesAction(originalImg, detectionsImg, detectionInfo)

                    val item = StorageItem(dir, originalImg, detectionsImg, detectionInfo)
                    items.add(0, item)
                }
    }

    companion object {
        private const val ORIGINAL_IMG_FILE = "frame.jpg"
        private const val DETECTIONS_IMG_FILE = "detections.jpg"
        private const val DETECTIONS_INFO_FILE = "detections_info.txt"
        private const val TIMESTAMP_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private fun createTimestamp() = SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(System.currentTimeMillis())

        fun loadItems(storageDirectory: File): MutableList<StorageItem> {
            val files = storageDirectory
                    .apply { mkdir() }
                    .listFiles { f -> f.isDirectory }
                    .map { dir ->
                        StorageItem(
                                itemDir = dir,
                                originalImg = File(dir, ORIGINAL_IMG_FILE),
                                detectionsImg = File(dir, DETECTIONS_IMG_FILE),
                                detectionsInfo = File(dir, DETECTIONS_INFO_FILE)
                        )
                    }
                    .filter { it.detectionsImg.exists() }
                    .sortedByDescending { it.itemDir }
                    .toMutableList()
            return files
        }
    }
}

data class StorageItem(val itemDir: File,
                       val originalImg: File, val detectionsImg: File, val detectionsInfo: File)