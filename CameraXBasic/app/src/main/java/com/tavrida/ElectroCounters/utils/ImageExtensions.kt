package com.tavrida.ElectroCounters.utils

import android.util.Log
import androidx.camera.core.ImageProxy
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer

fun ImageProxy.jpegToRgbMat___() =
        planes[0].buffer.toArray()
                .let { Imgcodecs.imdecode(MatOfByte(*it), Imgcodecs.IMREAD_COLOR) }
                .bgr2rgbInplace()

fun ImageProxy.jpegToRgbMat(): Mat {
    Log.d("TP_TIMINGS", "jpegToRgbMat.enter ${System.currentTimeMillis()}")

    val buffer = planes[0].buffer
    Log.d("TP_TIMINGS", "jpegToRgbMat.planes[0].buffer ${System.currentTimeMillis()}")

    val byteArray = buffer.toArray()
    Log.d("TP_TIMINGS", "jpegToRgbMat.buffer.toArray() ${System.currentTimeMillis()}")

    val matOfByte = MatOfByte(*byteArray)
    Log.d("TP_TIMINGS", "jpegToRgbMat.MatOfByte(*byteArray) ${System.currentTimeMillis()}")

    val bgrMat = Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR)
    Log.d("TP_TIMINGS", "jpegToRgbMat.Imgcodecs.imdecode ${System.currentTimeMillis()}")

    val rgbMat = bgrMat.bgr2rgbInplace()
    Log.d("TP_TIMINGS", "jpegToRgbMat.bgr2rgbInplace() ${System.currentTimeMillis()}")

    return rgbMat
}


fun Mat.bgr2rgbInplace() = this.also { Imgproc.cvtColor(this, it, Imgproc.COLOR_BGR2RGB) }
fun Mat.rgb2bgr() = Mat().also { Imgproc.cvtColor(this, it, Imgproc.COLOR_RGB2BGR) }

private fun ByteBuffer.toArray() = ByteArray(this.capacity())
        .also {
            this.rewind()
            this.get(it)
        }