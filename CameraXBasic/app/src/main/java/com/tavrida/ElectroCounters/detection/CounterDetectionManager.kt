package com.tavrida.ElectroCounters.detection

import android.os.Build
import android.util.Log
import androidx.camera.core.ImageProxy
import com.tavrida.ElectroCounters.utils.jpeg2RgbBgrMats
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream

class CounterDetectionManager(
        val detector: DarknetDetector,
        storageDirectory: File
) {
    val storage = DetectionStorage(storageDirectory)

    fun process(image: ImageProxy) {
        val (rgbMat, bgrMat) = image.jpeg2RgbBgrMats()

        val t0 = System.currentTimeMillis()
        val detections = detector.detect(rgbMat)
        val t1 = System.currentTimeMillis()

        val visImg: Mat = visualize(detections, bgrMat)
        save(bgrMat, visImg, detections, Timings(detectMs = t1 - t0))
    }

    private fun save(originalBgr: Mat, visBgr: Mat, detections: Collection<ObjectDetectionResult>, timings: Timings) {
        storage.newStorageItem { originalImgFile: File, detectionsImgFile: File, detectionsInfoFile: File ->
            save(originalImgFile, originalBgr, 100)
            save(detectionsImgFile, visBgr)
            save(detectionsInfoFile, detections, timings)
        }

    }

    private fun save(file: File, bgrImg: Mat, jpegQuality: Int? = null) {
        val params = MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, jpegQuality ?: 50)
        Imgcodecs.imwrite(file.toString(), bgrImg, params)
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
            .apply { detections.forEach { Imgproc.rectangle(this, it.box.toRect(), rgbClassColors[it.classId], 4) } }


    companion object {
        private val rgbRed = Scalar(255, 0, 0)
        private val rgbGreen = Scalar(0, 255, 0)
        private val rgbClassColors = arrayOf(rgbRed, rgbGreen)

        fun Scalar(v0: Int, v1: Int, v2: Int) = Scalar(v0.toDouble(), v1.toDouble(), v2.toDouble())
        fun Rect2d.toRect() = Rect(x.toInt(), y.toInt(), width.toInt(), height.toInt())
        fun Rect2d.toDisplayStr() = "xywh( $x, $y, $width, $height )"

        fun deviceName(): String {
            val manufacturer: String = Build.MANUFACTURER
            val model: String = Build.MODEL
            return "$manufacturer $model"
        }


    }

    data class Timings(val detectMs: Long)
}