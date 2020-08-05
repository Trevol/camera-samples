package com.tavrida.ElectroCounters.detection

import android.os.Build
import androidx.camera.core.ImageProxy
import com.tavrida.ElectroCounters.utils.*
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream

class CounterDetectionManager(
        val screenDetector: DarknetDetector,
        val digitsDetector: DarknetDetector,
        storageDirectory: File
) {
    val storage = DetectionStorage(storageDirectory)

    fun process(image: ImageProxy) {
        val (rgbMat, bgrMat) = image.jpeg2RgbBgrMats()
        process(rgbMat, bgrMat)
    }

    fun process(bgrMat: Mat) {
        val rgbMat = bgrMat.bgr2rgb()
        process(rgbMat, bgrMat)
    }

    private fun process(rgbMat: Mat, bgrMat: Mat) {
        // detect screen
        val screenResult = screenDetector.detect(rgbMat)
        // extract screen image
        val screenDetection = screenResult.detections.firstOrNull { r -> r.classId == screenClassId }

        var screenRgbImg: Mat? = null
        var digitsResult: DarknetDetector.DetectionResult? = null
        if (screenDetection != null) {
            screenRgbImg = rgbMat.roi(screenDetection.box, 5)
            digitsResult = digitsDetector.detect(screenRgbImg)
        }

        val digitsBgrImg = screenRgbImg?.rgb2bgr()
        val visualizationImg = visualize(
                ImgDetections(bgrMat.copy(), screenResult.detections),
                digitsResult?.let { ImgDetections(digitsBgrImg!!.copy(), digitsResult.detections) }
        )

        // save(bgrMat, visImg, detections, Timings(detectMs = t1 - t0))
        Saver(storage)
    }

    private data class ImgDetections(val img: Mat, val detections: Collection<ObjectDetectionResult>)

    private fun visualize(screen: ImgDetections, digits: ImgDetections?): Mat {
        screen.detections.forEach { Imgproc.rectangle(screen.img, it.box.toRect(), bgrClassColors[it.classId], 4) }
        if (digits == null)
            return screen.img
        val digitsOnlyImg = digits.img.copy()
        digits.detections.forEach { d ->
            Imgproc.rectangle(digits.img, d.box.toRect(), bgrGreen, 1)
            Imgproc.rectangle(digitsOnlyImg, d.box.toRect(), bgrGreen, 1)
            val labelOrd = Point(d.box.x + 2, d.box.y + d.box.height - 2)
            Imgproc.putText(digitsOnlyImg, d.classId.toString(), labelOrd, Imgproc.FONT_HERSHEY_SIMPLEX, .75, bgrGreen)
        }
        vstack(
                hstack(digits.img, digitsOnlyImg).resize(width=screen.img.width()),
                screen.img
        )
        return Mat()
    }

    private fun visualize(detections: Collection<ObjectDetectionResult>, rgbImg: Mat) =
            detections.forEach { Imgproc.rectangle(rgbImg, it.box.toRect(), bgrClassColors[it.classId], 4) }


    class Saver(private val storage: DetectionStorage) {
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
            sb.appendln(deviceName()).appendln(androidInfo())
                    .append("detectMs: ").appendln(timings.detectMs)
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
    }


    companion object {
        const val screenClassId = 1
        private val bgrRed = Scalar(0, 0, 255)
        private val bgrGreen = Scalar(0, 255, 0)
        private val bgrClassColors = arrayOf(bgrRed, bgrGreen)

        fun Scalar(v0: Int, v1: Int, v2: Int) = Scalar(v0.toDouble(), v1.toDouble(), v2.toDouble())
        fun Rect2d.toDisplayStr() = "xywh( $x, $y, $width, $height )"

        fun deviceName(): String {
            val manufacturer: String = Build.MANUFACTURER
            val model: String = Build.MODEL
            return "$manufacturer $model"
        }

        fun androidInfo(): String {
            return "${Build.VERSION.SDK_INT} ${Build.VERSION.RELEASE} ${Build.VERSION.CODENAME}"
        }

    }


}