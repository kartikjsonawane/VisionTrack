package com.visiontrack.presentation.detection

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.visiontrack.ml.ObjectDetectionHelper
import timber.log.Timber

/**
 * CameraX [ImageAnalysis.Analyzer] implementation.
 *
 * Converts [ImageProxy] RGBA_8888 planes → [Bitmap], applies rotation
 * correction, then passes the frame to [ObjectDetectionHelper].
 */
class DetectionAnalyzer(
    private val helper: ObjectDetectionHelper,
    private val sessionId: String,
    private val onResults: (List<com.visiontrack.domain.model.DetectedObject>) -> Unit
) : ImageAnalysis.Analyzer {

    private var frameIndex = 0L

    override fun analyze(image: ImageProxy) {
        try {
            val bitmap = image.toBitmap()
            val rotated = rotateBitmap(bitmap, image.imageInfo.rotationDegrees.toFloat())
            val results = helper.processFrame(rotated, sessionId, frameIndex++)
            onResults(results)
        } catch (e: Exception) {
            Timber.e(e, "Frame analysis error at frame $frameIndex")
        } finally {
            image.close()
        }
    }

    private fun ImageProxy.toBitmap(): Bitmap {
        val plane = planes[0]
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bmp ->
            buffer.rewind()
            bmp.copyPixelsFromBuffer(buffer)
        }
    }

    private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return source
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
            .also { if (it !== source) source.recycle() }
    }
}
