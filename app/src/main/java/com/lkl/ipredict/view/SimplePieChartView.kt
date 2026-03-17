package com.lkl.ipredict.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class SimplePieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var values: List<Float> = emptyList()

    private val colors = listOf(
        "#FFCC80", "#A5D6A7", "#90CAF9", "#CE93D8"
    ).map { Color.parseColor(it) }

    private val segmentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
        if (values.isEmpty() || values.sum() <= 0f) {
            canvas.drawText("暂无数据", width / 2f, height / 2f, emptyPaint)
            return
        }

        val size = minOf(width, height).toFloat()
        val radius = size * 0.38f
        val cx = width / 2f
        val cy = height / 2f
        val rect = RectF(cx - radius, cy - radius, cx + radius, cy + radius)

        val total = values.sum().coerceAtLeast(1f)
        var start = -90f

        values.forEachIndexed { index, value ->
            if (value <= 0f) return@forEachIndexed
            val sweep = value / total * 360f
            segmentPaint.color = colors[index % colors.size]
            canvas.drawArc(rect, start, sweep, true, segmentPaint)
            start += sweep
        }

        segmentPaint.color = Color.WHITE
        canvas.drawCircle(cx, cy, radius * 0.46f, segmentPaint)
    }
}
