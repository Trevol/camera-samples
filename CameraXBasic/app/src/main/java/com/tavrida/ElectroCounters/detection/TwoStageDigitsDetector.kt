package com.tavrida.ElectroCounters.detection

import androidx.camera.core.ImageProxy
import com.tavrida.ElectroCounters.utils.*
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.imgproc.Imgproc
import java.io.File

class TwoStageDigitsDetector(
        val screenDetector: DarknetDetector,
        val digitsDetector: DarknetDetector,
        storageDirectory: File?
) {
    // val storage = if (storageDirectory != null) CounterDetectionStorage(storageDirectory) else null
    val storage = storageDirectory?.let { CounterDetectionStorage(storageDirectory) }

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
        val counterResult = screenDetector.detect(rgbMat)
        // extract screen image
        val screenDetection = counterResult.detections.firstOrNull { r -> r.classId == screenClassId }

        var screenRgbImg: Mat? = null
        var digitsResult: DarknetDetector.DetectionResult? = null
        if (screenDetection != null) {
            screenRgbImg = rgbMat.roi(screenDetection.box.toRect(), 30, 10)
            // TODO("Or double padding relative to box dimensions!!!")
            digitsResult = digitsDetector.detect(screenRgbImg)
        }

        val screenBgrImg = screenRgbImg?.rgb2bgr()
        val (resultVisualization, counterVisualization, digitsVisualization) = visualize(
                ImgDetections(bgrMat.copy(), counterResult.detections),
                digitsResult?.let { ImgDetections(screenBgrImg!!.copy(), digitsResult.detections) }
        )

        storage?.saveResults(bgrMat, resultVisualization, counterVisualization, screenBgrImg, digitsVisualization, counterResult, digitsResult)
    }

    private fun visualize(counter: ImgDetections, digits: ImgDetections?): Triple<Mat, Mat, Mat?> {
        counter.detections.forEach { Imgproc.rectangle(counter.img, it.box.toRect(), bgrClassColors[it.classId], 4) }

        if (digits == null)
            return Triple(counter.img, counter.img, null)

        val digitsOnlyImg = Mat(digits.img.size(), digits.img.type(), Scalar(0))
        digits.detections.forEach { d ->
            Imgproc.rectangle(digits.img, d.box.toRect(), bgrGreen, 1)
            Imgproc.rectangle(digitsOnlyImg, d.box.toRect(), bgrGreen, 1)

            val labelOrd = Point(d.box.x + 3, d.box.y + d.box.height - 3)

            Imgproc.putText(digitsOnlyImg, d.classId.toString(), labelOrd, Imgproc.FONT_HERSHEY_SIMPLEX, .65, bgrGreen)
        }
        val digitsVisualization = hstack(digits.img, digitsOnlyImg)
        val resultVisualization = vstack(
                digitsVisualization.resize(width = counter.img.width()),
                Mat(10, counter.img.cols(), counter.img.type(), Scalar(0)),
                counter.img
        )
        return Triple(resultVisualization, counter.img, digitsVisualization)
    }

    fun galleryFiles() = storage?.galleryFiles() ?: listOf()

    private data class ImgDetections(val img: Mat, val detections: Collection<ObjectDetectionResult>)

    companion object {
        const val screenClassId = 1
        private val bgrRed = Scalar(0, 0, 255)
        private val bgrGreen = Scalar(0, 255, 0)
        private val bgrClassColors = arrayOf(bgrRed, bgrGreen)
    }

}


