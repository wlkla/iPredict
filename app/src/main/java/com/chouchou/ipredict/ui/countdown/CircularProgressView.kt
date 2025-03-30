package com.chouchou.ipredict.ui.customviews

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.chouchou.ipredict.R

/**
 * 自定义环形进度条
 */
class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()

    // 将属性声明为私有，避免生成公开的setter
    private var progressValue = 0.7f // 默认进度为70%
    private val strokeWidth = 16f

    init {
        // 根据当前主题设置颜色
        updateColors()

        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.strokeWidth = strokeWidth
        backgroundPaint.strokeCap = Paint.Cap.ROUND

        progressPaint.style = Paint.Style.STROKE
        progressPaint.strokeWidth = strokeWidth
        progressPaint.strokeCap = Paint.Cap.ROUND
    }

    /**
     * 根据主题设置颜色
     */
    private fun updateColors() {
        val isNightMode = (context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        // 根据主题设置进度条颜色
        val primaryColor = ContextCompat.getColor(context,
            if (isNightMode) R.color.primary_dark else R.color.primary_light)

        // 设置背景轨道颜色 - 主题色的半透明版本
        val bgColor = if (isNightMode) {
            Color.argb(50, 255, 255, 255) // 浅白色透明背景
        } else {
            Color.argb(30, 0, 0, 0) // 浅黑色透明背景
        }

        backgroundPaint.color = bgColor
        progressPaint.color = primaryColor
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // 考虑描边宽度，确保圆环不会超出视图边界
        val padding = strokeWidth / 2
        bounds.set(padding, padding, width - padding, height - padding)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 绘制背景圆环
        canvas.drawArc(bounds, 0f, 360f, false, backgroundPaint)

        // 绘制进度圆环，从顶部开始顺时针
        val startAngle = -90f
        val sweepAngle = 360f * progressValue
        canvas.drawArc(bounds, startAngle, sweepAngle, false, progressPaint)
    }

    /**
     * 设置进度值 (0.0f - 1.0f)
     */
    fun setProgress(value: Float) {
        progressValue = value.coerceIn(0f, 1f)
        invalidate()
    }

    /**
     * 获取当前进度值
     */
    fun getProgress(): Float = progressValue

    /**
     * 在配置更改时更新颜色
     */
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateColors()
        invalidate()
    }
}