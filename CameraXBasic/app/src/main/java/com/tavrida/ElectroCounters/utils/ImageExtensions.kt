package com.tavrida.ElectroCounters.utils

import android.util.Log
import androidx.camera.core.ImageProxy
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.nio.ByteBuffer

fun ImageProxy.jpeg2RgbBgrMats() =
        planes[0].buffer.toArray()
                .let { Imgcodecs.imdecode(MatOfByte(*it), Imgcodecs.IMREAD_COLOR) }
                .let { bgr -> Pair(bgr.bgr2rgb(), bgr) }


fun Mat.bgr2rgbInplace() = this.also { Imgproc.cvtColor(this, it, Imgproc.COLOR_BGR2RGB) }
fun Mat.bgr2rgb() = Mat().also { rgb -> Imgproc.cvtColor(this, rgb, Imgproc.COLOR_RGB2BGR) }
fun Mat.rgb2bgr() = Mat().also { bgr -> Imgproc.cvtColor(this, bgr, Imgproc.COLOR_RGB2BGR) }


private fun ByteBuffer.toArray() = ByteArray(this.capacity())
        .also {
            this.rewind()
            this.get(it)
        }