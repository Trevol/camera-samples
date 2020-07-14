package com.tavrida.example.ElectroCounters.utils

import androidx.camera.core.ImageProxy
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer

fun ImageProxy.jpegToRgbMat() =
        planes[0].buffer.toArray()
                .let { Imgcodecs.imdecode(MatOfByte(*it), Imgcodecs.IMREAD_COLOR) }
                .bgr2rgbInplace()


fun Mat.bgr2rgbInplace() = this.also { Imgproc.cvtColor(this, it, Imgproc.COLOR_BGR2RGB) }
fun Mat.rgb2bgr() = Mat().also { Imgproc.cvtColor(this, it, Imgproc.COLOR_RGB2BGR) }

private fun ByteBuffer.toArray() = ByteArray(this.capacity())
        .also {
            this.rewind()
            this.get(it)
        }