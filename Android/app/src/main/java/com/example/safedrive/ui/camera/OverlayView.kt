package com.example.safedrive.ui.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.example.safedrive.R
import java.util.LinkedList
import kotlin.math.max

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var results = listOf<BoundingBox>() // YOLO 객체 탐지 결과를 담는 리스트
    // start
    private var laneResults: Bitmap? = null  // 차선 인식 결과 비트맵을 저장할 변수
    // end
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var bounds = Rect()

    init {
        initPaints()
    }

    fun clear() {
        textPaint.reset()
        textBackgroundPaint.reset()
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        textBackgroundPaint.color = Color.BLACK
        textBackgroundPaint.style = Paint.Style.FILL
        textBackgroundPaint.textSize = 50f

        textPaint.color = Color.WHITE
        textPaint.style = Paint.Style.FILL
        textPaint.textSize = 50f

        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        results.forEach {
            val left = it.x1 * width
            val top = it.y1 * height
            val right = it.x2 * width
            val bottom = it.y2 * height

            canvas.drawRect(left, top, right, bottom, boxPaint)
            val drawableText = it.clsName

            textBackgroundPaint.getTextBounds(drawableText, 0, drawableText.length, bounds)
            val textWidth = bounds.width()
            val textHeight = bounds.height()
            canvas.drawRect(
                left,
                top,
                left + textWidth + BOUNDING_RECT_TEXT_PADDING,
                top + textHeight + BOUNDING_RECT_TEXT_PADDING,
                textBackgroundPaint
            )
            canvas.drawText(drawableText, left, top + bounds.height(), textPaint)

        }
    }

    // OverlayView에 YOLO 객체 탐지 및 차선 인식 결과 모두 그리기
    fun setResults(boundingBoxes: List<BoundingBox>, laneBitmap: Bitmap?) {
        results = boundingBoxes  // YOLO 탐지 결과 설정
        laneResults = laneBitmap  // 차선 인식 결과 비트맵 설정
        invalidate()  // 다시 그리기
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // YOLO 객체 탐지 결과 그리기
        results.forEach {
            val left = it.x1 * width
            val top = it.y1 * height
            val right = it.x2 * width
            val bottom = it.y2 * height
            canvas.drawRect(left, top, right, bottom, boxPaint)
            canvas.drawText(it.clsName, left, top + bounds.height(), textPaint)
        }

        // 차선 인식 결과 그리기 (YOLO 탐지 결과 후에 그리기)
        laneResults?.let {
            canvas.drawBitmap(it, null, Rect(0, 0, width, height), null)  // 비트맵을 화면 크기에 맞춰 그리기
        }
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}