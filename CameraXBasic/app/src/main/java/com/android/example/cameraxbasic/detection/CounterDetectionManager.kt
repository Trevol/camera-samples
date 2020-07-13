package com.android.example.cameraxbasic.detection

import android.os.Build
import androidx.camera.core.ImageProxy
import com.android.example.cameraxbasic.utils.jpegToRgbMat
import com.android.example.cameraxbasic.utils.rgb2bgr
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream

class CounterDetectionManager(
        val detector: DarknetDetector,
        val storageDirectory: File
) {
    val storage = DetectionStorage(storageDirectory)

    fun process(image: ImageProxy) {
        val rgbMat = image.jpegToRgbMat()

        val t0 = System.currentTimeMillis()
        val detections = detector.detect(rgbMat)
        val t1 = System.currentTimeMillis()

        val visImg: Mat = visualize(detections, rgbMat)
        save(rgbMat, visImg, detections, Timings(detectMs = t1 - t0))
    }

    private fun save(originalRgb: Mat, visRgb: Mat, detections: Collection<ObjectDetectionResult>, timings: Timings) {
        storage.newItem { originalImg: File, detectionsImg: File, detectionsInfo: File ->
            save(originalImg, originalRgb, 100)
            save(detectionsImg, visRgb)
            save(detectionsInfo, detections, timings)
        }

    }

    private fun save(file: File, rgbImg: Mat, jpegQuality: Int? = null) {
        val params = MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, jpegQuality ?: 50)
        Imgcodecs.imwrite(file.toString(), rgbImg.rgb2bgr(), params)
    }

    private fun save(file: File, detections: Collection<ObjectDetectionResult>, timings: Timings) {
        val sb = StringBuilder()
        sb.appendln(deviceName()).append("detectMs: ").appendln(timings.detectMs)
        detections.forEach {
            sb.append("classId: ").append(it.classId)
                    .append("  classScore: ").append(it.classScore)
                    .append("  box: ").appendln(it.box.toDisplayStr())
        }
        FileOutputStream(file).use { fs ->
            fs.writer().use { wr ->
                wr.write(sb.toString())
            }
        }
    }

    private fun visualize(detections: Collection<ObjectDetectionResult>, rgbImg: Mat) = Mat()
            .apply { rgbImg.copyTo(this) }
            .apply { detections.forEach { Imgproc.rectangle(this, it.box.toRect(), rgbClassColors[it.classId], 2) } }


    companion object {
        private val rgbRed = Scalar(200, 0, 0)
        private val rgbGreen = Scalar(0, 200, 0)
        private val rgbClassColors = arrayOf(rgbRed, rgbGreen)

        fun Scalar(v0: Int, v1: Int, v2: Int) = Scalar(v0.toDouble(), v1.toDouble(), v2.toDouble())
        fun Rect2d.toRect() = Rect(x.toInt(), y.toInt(), width.toInt(), height.toInt())
        fun Rect2d.toDisplayStr() = "xywh( $x, $y, $width, $height )"

        fun deviceName(): String? {
            val manufacturer: String = Build.MANUFACTURER
            val model: String = Build.MODEL
            return if (model.toLowerCase().startsWith(manufacturer.toLowerCase())) {
                capitalize(model)
            } else {
                capitalize(manufacturer) + " " + model
            }
        }


        private fun capitalize(s: String?): String {
            if (s == null || s.isEmpty()) {
                return ""
            }
            val first = s[0]
            return if (Character.isUpperCase(first)) {
                s
            } else {
                Character.toUpperCase(first).toString() + s.substring(1)
            }
        }
    }

    data class Timings(val detectMs: Long)
}