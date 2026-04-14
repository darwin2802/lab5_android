package com.example.lab5
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import java.util.Locale
class AnalyticChartView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val dataPoints = ArrayList<Float>()
    private var scaleFactor = 1.0f
    private var translateX = 0f
    private var translateY = 0f
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            scaleFactor *= detector.scaleFactor
            scaleFactor = scaleFactor.coerceIn(1.0f, 8.0f)
            invalidate()
            return true
        }
    })
    private val linePaint = Paint().apply {
        color = Color.parseColor("#38BDF8")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val pointPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 20f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    fun setData(newData: ArrayList<Float>) {
        dataPoints.clear()
        dataPoints.addAll(newData)
        invalidate()
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> { lastTouchX = event.x; lastTouchY = event.y }
            MotionEvent.ACTION_MOVE -> {
                if (!scaleDetector.isInProgress) {
                    translateX += event.x - lastTouchX
                    translateY += event.y - lastTouchY
                    invalidate()
                }
                lastTouchX = event.x
                lastTouchY = event.y
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (dataPoints.size < 2) return

        canvas.save()
        canvas.translate(translateX, translateY)
        canvas.scale(scaleFactor, scaleFactor, width / 2f, height / 2f)

        val padding = 60f
        val chartW = width - (padding * 2)
        val chartH = height - (padding * 2)
        val stepX = chartW / (dataPoints.size - 1).coerceAtLeast(1)
        val maxVal = (dataPoints.maxOrNull() ?: 1f).coerceAtLeast(3f)

        val path = Path()
        val coordinates = ArrayList<PointF>()

        for (i in dataPoints.indices) {
            val x = padding + (i * stepX)
            val y = height - padding - (dataPoints[i] / maxVal * chartH)
            coordinates.add(PointF(x, y))

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        canvas.drawPath(path, linePaint)

        for (i in dataPoints.indices) {
            val current = dataPoints[i]
            val isPeak = when {
                i == 0 -> dataPoints[i] > dataPoints[i + 1]
                i == dataPoints.size - 1 -> dataPoints[i] > dataPoints[i - 1]
                else -> dataPoints[i] > dataPoints[i - 1] && dataPoints[i] >= dataPoints[i + 1]
            }

            if (isPeak && current > 1.2f) {
                val coord = coordinates[i]

                canvas.drawCircle(coord.x, coord.y, 6f / scaleFactor, pointPaint)

                textPaint.textSize = 22f / scaleFactor
                canvas.drawText(
                    String.format(Locale.US, "%.1f", current),
                    coord.x,
                    coord.y - (14f / scaleFactor),
                    textPaint
                )
            }
        }
        canvas.restore()
    }
}