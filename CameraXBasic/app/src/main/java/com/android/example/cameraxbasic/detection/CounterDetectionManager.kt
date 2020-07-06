package com.android.example.cameraxbasic.detection

import android.media.Image
import android.util.Log
import com.android.example.cameraxbasic.utils.jpegSaveBuffer
import com.android.example.cameraxbasic.utils.jpegToBitmap
import com.android.example.cameraxbasic.utils.jpegToRgbMat
import com.android.example.cameraxbasic.utils.toRgbMat
import org.opencv.core.Mat
import java.io.File

class CounterDetectionManager(
        val detector: DarknetDetector,
        val storageDirectory: File
) {
    fun process(image: Image) {
        val rgbMat = image.jpegToRgbMat()
        val detections = detector.detect(rgbMat)
        val visImg: Mat = visualize(detections, rgbMat)
        // save(rgbMat, visImg, detections)
    }

    private fun save(originalRgb: Mat, visRgb: Mat, detections: Collection<ObjectDetectionResult>) {
        TODO("Not yet implemented")
    }

    private fun visualize(detections: Collection<ObjectDetectionResult>, rgbMat: Mat): Mat {
        TODO("Not yet implemented")
    }

}