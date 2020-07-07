package com.android.example.cameraxbasic.detection

import android.media.Image
import androidx.camera.core.ImageProxy
import com.android.example.cameraxbasic.utils.jpegToRgbMat
import com.android.example.cameraxbasic.utils.rgb2bgr
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CounterDetectionManager(
        val detector: DarknetDetector,
        val storageDirectory: File
) {
    fun process(image: ImageProxy) {
        val rgbMat = image.jpegToRgbMat()
        val detections = detector.detect(rgbMat)
        val visImg: Mat = visualize(detections, rgbMat)
        save(rgbMat, visImg, detections)
    }

    private fun save(originalRgb: Mat, visRgb: Mat, detections: Collection<ObjectDetectionResult>) {
        val stamp = createTimestamp()
        File(storageDirectory, stamp)
                .apply { mkdirs() }
                .also {
                    val imgName = File(it, "frame.jpg").toString()
                    val params = MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, 100)
                    Imgcodecs.imwrite(imgName, originalRgb.rgb2bgr(), params)
                }
                .also {
                    val imgName = File(it, "detection.jpg").toString()
                    Imgcodecs.imwrite(imgName, visRgb.rgb2bgr())
                }
    }

    private fun visualize(detections: Collection<ObjectDetectionResult>, rgbImg: Mat) = Mat()
            .apply { rgbImg.copyTo(this) }
            .apply { detections.forEach { Imgproc.rectangle(this, it.box.toRect(), rgbClassColors[it.classId], 2) } }


    companion object {
        private const val TAG = "CameraXBasic"
        private const val TIMESTAMP_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"

        private val rgbRed = Scalar(200, 0, 0)
        private val rgbGreen = Scalar(0, 200, 0)
        private val rgbClassColors = arrayOf(rgbRed, rgbGreen)

        fun Scalar(v0: Int, v1: Int, v2: Int) = Scalar(v0.toDouble(), v1.toDouble(), v2.toDouble())
        fun Rect2d.toRect() = Rect(x.toInt(), y.toInt(), width.toInt(), height.toInt())

        private fun createTimestamp() = SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(System.currentTimeMillis())
    }
}