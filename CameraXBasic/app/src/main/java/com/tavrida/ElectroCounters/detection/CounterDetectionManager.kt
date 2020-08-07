package com.tavrida.ElectroCounters.detection

import androidx.camera.core.ImageProxy
import com.tavrida.ElectroCounters.utils.*
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CounterDetectionManager(
        val screenDetector: DarknetDetector,
        val digitsDetector: DarknetDetector,
        storageDirectory: File
) {
    val storage = Storage(storageDirectory)

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

        // save(bgrMat, visImg, detections, Timings(detectMs = t1 - t0))
        storage.saveResults(bgrMat, resultVisualization, counterVisualization, screenBgrImg, digitsVisualization, counterResult, digitsResult)
    }

    private fun visualize(counter: ImgDetections, digits: ImgDetections?): Triple<Mat, Mat, Mat?> {
        counter.detections.forEach { Imgproc.rectangle(counter.img, it.box.toRect(), bgrClassColors[it.classId], 4) }

        if (digits == null)
            return Triple(counter.img, counter.img, null)

        val digitsOnlyImg = Mat(digits.img.size(), digits.img.type(), Scalar(0))
        digits.detections.forEach { d ->
            Imgproc.rectangle(digits.img, d.box.toRect(), bgrGreen, 1)
            Imgproc.rectangle(digitsOnlyImg, d.box.toRect(), bgrGreen, 1)
            val labelOrd = Point(d.box.x + 2, d.box.y + d.box.height - 2)
            Imgproc.putText(digitsOnlyImg, d.classId.toString(), labelOrd, Imgproc.FONT_HERSHEY_SIMPLEX, .75, bgrGreen)
        }
        val digitsVisualization = hstack(digits.img, digitsOnlyImg)
        val resultVisualization = vstack(
                digitsVisualization.resize(width = counter.img.width()),
                Mat(10, counter.img.cols(), counter.img.type(), Scalar(0)),
                counter.img
        )
        return Triple(resultVisualization, counter.img, digitsVisualization)
    }

    fun galleryFiles() = storage.galleryFiles()

    private data class ImgDetections(val img: Mat, val detections: Collection<ObjectDetectionResult>)

    companion object {
        const val screenClassId = 1
        private val bgrRed = Scalar(0, 0, 255)
        private val bgrGreen = Scalar(0, 255, 0)
        private val bgrClassColors = arrayOf(bgrRed, bgrGreen)
    }

}


class Storage(private val storageDirectory: File) {
    init {
        storageDirectory.mkdir()
    }

    companion object {
        private const val resultsJpg = "results.jpg"
        private const val TIMESTAMP_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private fun createTimestamp() = SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(System.currentTimeMillis())
    }

    private val galleryFiles = listGalleryFiles().toMutableList()

    private fun listGalleryFiles(): List<File> {
        storageDirectory.isDirectory.assert()
        val files = storageDirectory
                .listFiles { f -> f.isDirectory }!!
                .map { dir ->
                    dir.listFiles { f -> f.name.endsWith(resultsJpg) }!!
                            .firstOrNull()
                }
                .filter { it != null }
                .map { it!! }
                .sortedByDescending { it!!.parentFile }
                .toMutableList()
        return files
    }

    fun saveResults(originalBgr: Mat, resultVisualization: Mat, counterVisualization: Mat,
                    screenImage: Mat?, digitsVisualization: Mat?,
                    counterResult: DarknetDetector.DetectionResult, digitsResult: DarknetDetector.DetectionResult?) {
        val (resultsDirectory, timestamp) = makeResultDirectory()

        saveImage(File(resultsDirectory, "${timestamp}.jpg"), originalBgr, 100)

        val resultsImgFile = File(resultsDirectory, "${timestamp}_${resultsJpg}")
        saveImage(resultsImgFile, resultVisualization)
        galleryFiles.add(0, resultsImgFile)

        saveImage(File(resultsDirectory, "${timestamp}_counter.jpg"), counterVisualization)
        if (screenImage != null)
            saveImage(File(resultsDirectory, "${timestamp}_screen.jpg"), screenImage)
        if (digitsVisualization != null)
            saveImage(File(resultsDirectory, "${timestamp}_digits.jpg"), digitsVisualization)

        saveInfo(
                File(resultsDirectory, "${timestamp}_info.txt"),
                listOf(Pair("CounterScreen", counterResult), Pair("Digits", digitsResult))
        )
    }

    private fun makeResultDirectory(): Pair<File, String> {
        val stamp = createTimestamp()
        return Pair(File(storageDirectory, stamp).apply { mkdir() }, stamp)
    }

    fun galleryFiles(): List<File> = galleryFiles.toList()

    private fun saveImage(file: File, bgrImg: Mat, jpegQuality: Int? = null) {
        val params = MatOfInt(Imgcodecs.IMWRITE_JPEG_QUALITY, jpegQuality ?: 50)
        Imgcodecs.imwrite(file.toString(), bgrImg, params)
    }

    private fun saveInfo(file: File, detectionResults: List<Pair<String, DarknetDetector.DetectionResult?>>) {
        val sb = StringBuilder()
        sb.appendln(deviceName()).appendln(androidInfo())

        detectionResults.forEachIndexed() { index, (name, detectionResult) ->
            sb.appendln("Detection stage #$index: $name")
            if (detectionResult == null) {
                sb.appendln("none")
            } else {
                sb.append("Duration (ms): ").appendln(detectionResult.durationInMs)
                detectionResult.detections.forEach {
                    sb.append("classId: ").append(it.classId)
                            .append("  classScore: ").append(it.classScore)
                            .append("  box: ").appendln(it.box.toDisplayStr())
                }
            }
            sb.appendln("-------------------------------------------")
        }


        FileOutputStream(file).use { fs ->
            fs.writer().use { wr ->
                wr.write(sb.toString())
            }
        }
    }


}
