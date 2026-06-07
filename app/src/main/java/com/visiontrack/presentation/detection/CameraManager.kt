package com.visiontrack.presentation.detection

import android.content.Context
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages CameraX lifecycle binding, preview, and image analysis pipeline.
 *
 * Uses a dedicated single-thread executor for ML inference so the camera
 * pipeline never blocks the main thread. Frames are delivered via [ImageAnalysis]
 * in [ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST] mode, ensuring the detector
 * always works on the newest frame and drops stale ones automatically.
 */
@Singleton
class CameraManager @Inject constructor() {

    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalysis: ImageAnalysis? = null

    val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    fun startCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        analyzer: ImageAnalysis.Analyzer,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            cameraProvider = providerFuture.get()

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            imageAnalysis = ImageAnalysis.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also { it.setAnalyzer(analysisExecutor, analyzer) }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            try {
                cameraProvider!!.unbindAll()
                cameraProvider!!.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                Timber.d("CameraX bound — lens=$lensFacing")
            } catch (e: Exception) {
                Timber.e(e, "CameraX binding failed")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun switchCamera(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        analyzer: ImageAnalysis.Analyzer,
        currentFacing: Int
    ): Int {
        val newFacing = if (currentFacing == CameraSelector.LENS_FACING_BACK)
            CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
        startCamera(context, lifecycleOwner, previewView, analyzer, newFacing)
        return newFacing
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        Timber.d("CameraX unbound")
    }

    fun shutdown() {
        stopCamera()
        analysisExecutor.shutdown()
    }
}
