package com.lkl.ipredict.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max

class SimpleLineChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var values: List<Float> = emptyList()

    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E6E6E6")
        strokeWidth = 2f
    }

    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#57D9B8")
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#57D9B8")
        style = Paint.Style.FILL
    }

    private val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#999999")
        textAlign = Paint.Align.CENTER
        textSize = 34f
    }

    fun setData(newValues: List<Float>) {
        values = newValues
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (values.isEmpty()) {
            canvas.drawText("暂无数据", width / 2f, height / 2f, emptyPaint)
            return
        }

        val left = paddingLeft + 12f
        val right = width - paddingRight - 12f
        val top = paddingTop + 12f
        val bottom = height - paddingBottom - 18f

        val minValue = values.minOrNull() ?: 0f
        val maxValue = max(values.maxOrNull() ?: 0f, minValue + 1f)
        val range = maxValue - minValue

        val rows = 4
        for (i in 0..rows) {
            val y = top + (bottom - top) * i / rows
            canvas.drawLine(left, y, right, y, gridPaint)
        }

        if (values.size == 1) {
            canvas.drawCircle((left + right) / 2f, (top + bottom) / 2f, 8f, pointPaint)
            return
        }

        val stepX = (right - left) / (values.size - 1)

        for (i in 0 until values.lastIndex) {
            val x1 = left + i * stepX
            val x2 = left + (i + 1) * stepX
            val y1 = bottom - ((values[i] - minValue) / range) * (bottom - top)
            val y2 = bottom - ((values[i + 1] - minValue) / range) * (bottom - top)
            canvas.drawLine(x1, y1, x2, y2, linePaint)
        }

        values.forEachIndexed { index, value ->
            val x = left + index * stepX
            val y = bottom - ((value - minValue) / range) * (bottom - top)
            canvas.drawCircle(x, y, 7f, pointPaint)
        }
    }
}
