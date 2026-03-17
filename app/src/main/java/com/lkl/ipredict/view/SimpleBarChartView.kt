package com.lkl.ipredict.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class SimpleBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var values: List<Float> = emptyList()

    private val colors = listOf(
        "#A5D6A7", "#90CAF9", "#FFE082", "#EF9A9A"
    ).map { Color.parseColor(it) }

    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E6E6E6")
        strokeWidth = 2f
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)

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

        val left = paddingLeft + 10f
        val right = width - paddingRight - 10f
        val top = paddingTop + 10f
        val bottom = height - paddingBottom - 10f

        canvas.drawLine(left, bottom, right, bottom, axisPaint)

        val maxValue = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val groupWidth = (right - left) / values.size
        val barWidth = groupWidth * 0.55f

        values.forEachIndexed { index, value ->
            val x1 = left + index * groupWidth + (groupWidth - barWidth) / 2f
            val x2 = x1 + barWidth
            val y1 = bottom - (value / maxValue) * (bottom - top)

            barPaint.color = colors[index % colors.size]
            canvas.drawRoundRect(x1, y1, x2, bottom, 10f, 10f, barPaint)
        }
    }
}
