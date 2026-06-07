package com.visiontrack.ml

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.visiontrack.domain.model.DetectedObject
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.min

/**
 * Custom View that renders bounding boxes, labels, and confidence scores
 * on top of the CameraX PreviewView.
 *
 * Designed for minimal allocation per frame (Paint objects and Rect reused).
 */
class BoundingBoxOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle) {

    companion object {
        private val CLASS_COLORS = intArrayOf(
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
            Color.CYAN, Color.MAGENTA, 0xFFFF5722.toInt(), 0xFF9C27B0.toInt(),
            0xFF03A9F4.toInt(), 0xFF8BC34A.toInt(), 0xFFFF9800.toInt(), 0xFF607D8B.toInt()
        )
        private const val BOX_STROKE  = 4f
        private const val LABEL_PAD   = 8f
        private const val TEXT_SIZE   = 36f
    }

    private val boxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style       = Paint.Style.STROKE
        strokeWidth = BOX_STROKE
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style   = Paint.Style.FILL
        alpha   = 150
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color     = Color.WHITE
        textSize  = TEXT_SIZE
        isFakeBoldText = true
    }
    private val textBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val textBounds = Rect()

    // Current detections — updated via [setDetections]
    private var detections: List<DetectedObject> = emptyList()
    private var scaleX = 1f
    private var scaleY = 1f

    // FPS overlay
    private var fpsText: String = ""
    private val fpsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color    = Color.WHITE
        textSize = 48f
        isFakeBoldText = true
    }

    fun setDetections(objects: List<DetectedObject>, frameWidth: Int, frameHeight: Int) {
        if (width == 0 || height == 0) return
        scaleX = width.toFloat()  / frameWidth
        scaleY = height.toFloat() / frameHeight
        detections = objects
        invalidate()
    }

    fun setFps(fps: Float, latencyMs: Float) {
        fpsText = "%.1f FPS  |  %.0f ms".format(fps, latencyMs)
        invalidate()
    }

    fun clear() {
        detections = emptyList()
        fpsText    = ""
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        detections.forEachIndexed { _, obj -> drawDetection(canvas, obj) }
        if (fpsText.isNotEmpty()) drawFps(canvas)
    }

    private fun drawDetection(canvas: Canvas, obj: DetectedObject) {
        val color = CLASS_COLORS[obj.classIndex() % CLASS_COLORS.size]
        val scaledBox = RectF(
            obj.boundingBox.left   * scaleX,
            obj.boundingBox.top    * scaleY,
            obj.boundingBox.right  * scaleX,
            obj.boundingBox.bottom * scaleY
        )

        // Semi-transparent fill
        fillPaint.color = color
        canvas.drawRect(scaledBox, fillPaint)

        // Bounding box border
        boxPaint.color = color
        canvas.drawRect(scaledBox, boxPaint)

        // Label background + text
        val label = "${obj.label}  ${obj.confidencePercent}%"
        textPaint.getTextBounds(label, 0, label.length, textBounds)

        val labelLeft   = scaledBox.left
        val labelTop    = maxOf(0f, scaledBox.top - textBounds.height() - LABEL_PAD * 2)
        val labelRight  = labelLeft + textBounds.width() + LABEL_PAD * 2
        val labelBottom = labelTop  + textBounds.height() + LABEL_PAD * 2

        textBgPaint.color = color
        canvas.drawRect(labelLeft, labelTop, labelRight, labelBottom, textBgPaint)
        canvas.drawText(label, labelLeft + LABEL_PAD, labelBottom - LABEL_PAD, textPaint)
    }

    private fun drawFps(canvas: Canvas) {
        val bgPaint = Paint().apply { color = 0xCC000000.toInt(); style = Paint.Style.FILL }
        val textH   = 58f
        canvas.drawRect(0f, 0f, width.toFloat(), textH + 8f, bgPaint)
        canvas.drawText(fpsText, 16f, textH - 8f, fpsPaint)
    }

    private fun DetectedObject.classIndex(): Int =
        label.hashCode().and(0x7FFFFFFF) % CLASS_COLORS.size
}
