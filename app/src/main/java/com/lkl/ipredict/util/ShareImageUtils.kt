package com.lkl.ipredict.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.lkl.ipredict.R
import com.lkl.ipredict.ui.ShareTheme
import java.io.File
import java.io.FileOutputStream

object ShareImageUtils {

    fun themes(context: Context): List<ShareTheme> {
        return listOf(
            ShareTheme("青紫", 0xFF5DADE2.toInt(), 0xFF8E44AD.toInt()),
            ShareTheme("夕阳", 0xFFFF8E53.toInt(), 0xFFFF2D55.toInt()),
            ShareTheme("森林", 0xFF00C9A7.toInt(), 0xFF2E7D32.toInt()),
            ShareTheme("薄荷", 0xFF6DD5FA.toInt(), 0xFF2193B0.toInt()),
            ShareTheme("暮光", 0xFF6A11CB.toInt(), 0xFF2575FC.toInt())
        )
    }

    fun shareCountdownCard(
        context: Context,
        eventName: String,
        daysLeft: Int,
        nextDate: String,
        averageCycle: Int,
        latestDate: String?,
        intervals: List<Int>,
        theme: ShareTheme
    ) {
        val bitmap = buildCountdownCardBitmap(
            context, eventName, daysLeft, nextDate, averageCycle, latestDate, intervals, theme
        ) ?: return
        shareBitmap(context, bitmap, "countdown_share.png", "分享周期倒计时")
    }

    fun buildCountdownCardBitmap(
        context: Context,
        eventName: String,
        daysLeft: Int,
        nextDate: String,
        averageCycle: Int,
        latestDate: String?,
        intervals: List<Int>,
        theme: ShareTheme
    ): Bitmap? {
        return buildBitmapFromLayout(context, R.layout.share_card_countdown, theme) { view ->
            val title = view.findViewById<TextView>(R.id.shareTitle)
            val subtitle = view.findViewById<TextView>(R.id.shareSubtitle)
            val days = view.findViewById<TextView>(R.id.shareDaysLeft)
            val next = view.findViewById<TextView>(R.id.shareNextDate)
            val avg = view.findViewById<TextView>(R.id.shareAvgCycle)
            val last = view.findViewById<TextView>(R.id.shareLastDate)
            val samples = view.findViewById<TextView>(R.id.shareSamples)
            val intervalsText = view.findViewById<TextView>(R.id.shareIntervals)
            val footer = view.findViewById<TextView>(R.id.shareFooter)

            title.text = "周期倒计时"
            subtitle.text = "事件：$eventName"
            days.text = daysLeft.toString().padStart(2, '0')
            next.text = nextDate
            avg.text = "${averageCycle}天"
            last.text = latestDate ?: "--"
            samples.text = "${intervals.size}次"
            intervalsText.text = if (intervals.isEmpty()) {
                "最近间隔：暂无数据"
            } else {
                "最近间隔：" + intervals.take(6).joinToString(" · ") { "${it}天" }
            }
            footer.text = "iPredict · 记录周期 · 更懂自己"
        }
    }

    fun shareAnalysisCard(
        context: Context,
        eventName: String,
        averageCycle: Int?,
        latestDate: String?,
        intervals: List<Int>,
        lineView: View,
        barView: View,
        pieView: View,
        theme: ShareTheme
    ) {
        val lineBmp = buildBitmapFromView(lineView) ?: return
        val barBmp = buildBitmapFromView(barView) ?: return
        val pieBmp = buildBitmapFromView(pieView) ?: return

        val bitmap = buildBitmapFromLayout(context, R.layout.share_card_analysis, theme) { view ->
            view.findViewById<TextView>(R.id.shareAnalysisTitle).text = "周期分析"
            view.findViewById<TextView>(R.id.shareAnalysisSubtitle).text = "事件：$eventName"
            view.findViewById<TextView>(R.id.shareAnalysisAvg).text =
                averageCycle?.let { "${it}天" } ?: "--"
            view.findViewById<TextView>(R.id.shareAnalysisSamples).text = "${intervals.size}次"
            view.findViewById<TextView>(R.id.shareAnalysisLatest).text = latestDate ?: "--"
            view.findViewById<TextView>(R.id.shareAnalysisIntervals).text = if (intervals.isEmpty()) {
                "最近间隔：暂无数据"
            } else {
                "最近间隔：" + intervals.take(6).joinToString(" · ") { "${it}天" }
            }
            view.findViewById<TextView>(R.id.shareAnalysisFooter).text = "iPredict · 数据可视化"
            view.findViewById<ImageView>(R.id.shareLineImage).setImageBitmap(lineBmp)
            view.findViewById<ImageView>(R.id.shareBarImage).setImageBitmap(barBmp)
            view.findViewById<ImageView>(R.id.sharePieImage).setImageBitmap(pieBmp)
        } ?: return

        shareBitmap(context, bitmap, "analysis_share.png", "分享周期分析")
    }

    fun shareViewAsImage(
        context: Context,
        view: View,
        fileName: String,
        chooserTitle: String
    ) {
        val bitmap = buildBitmapFromView(view) ?: return

        val dir = File(context.cacheDir, "shared_images")
        if (!dir.exists()) dir.mkdirs()

        val output = File(dir, fileName)
        FileOutputStream(output).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            output
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun shareViewsAsImage(
        context: Context,
        views: List<View>,
        fileName: String,
        chooserTitle: String,
        spacingDp: Int = 12
    ) {
        val bitmap = buildBitmapFromViews(
            context = context,
            views = views,
            spacingDp = spacingDp
        ) ?: return

        val dir = File(context.cacheDir, "shared_images")
        if (!dir.exists()) dir.mkdirs()

        val output = File(dir, fileName)
        FileOutputStream(output).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            output
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun buildBitmapFromView(view: View): Bitmap? {
        if (view.width <= 0 || view.height <= 0) return null

        return if (view is ScrollView && view.childCount > 0) {
            val child = view.getChildAt(0)
            val width = view.width
            val height = child.height.coerceAtLeast(view.height)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            child.draw(canvas)
            bitmap
        } else {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            view.draw(canvas)
            bitmap
        }
    }

    private fun buildBitmapFromViews(
        context: Context,
        views: List<View>,
        spacingDp: Int
    ): Bitmap? {
        val validViews = views.filter { it.width > 0 && it.height > 0 }
        if (validViews.isEmpty()) return null

        val density = context.resources.displayMetrics.density
        val spacing = (spacingDp * density).toInt()
        val width = validViews.maxOf { it.width }
        val height = validViews.sumOf { it.height } + spacing * (validViews.size - 1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(ContextCompat.getColor(context, com.lkl.ipredict.R.color.page_bg))

        var yOffset = 0f
        validViews.forEachIndexed { index, view ->
            canvas.save()
            canvas.translate(0f, yOffset)
            view.draw(canvas)
            canvas.restore()
            yOffset += view.height.toFloat()
            if (index != validViews.lastIndex) {
                yOffset += spacing.toFloat()
            }
        }
        return bitmap
    }

    fun buildBitmapFromLayout(
        context: Context,
        layoutId: Int,
        theme: ShareTheme,
        binder: (View) -> Unit
    ): Bitmap? {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(layoutId, null)
        // set gradient background for root
        val bg = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(theme.startColor, theme.endColor)
        ).apply { cornerRadius = context.resources.displayMetrics.density * 22 }
        view.background = bg

        val displayMetrics = context.resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.92f).toInt()
        val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)

        view.layoutParams = ViewGroup.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        binder(view)
        view.measure(widthSpec, heightSpec)
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap = Bitmap.createBitmap(view.measuredWidth, view.measuredHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(ContextCompat.getColor(context, R.color.page_bg))
        view.draw(canvas)
        return bitmap
    }

    fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String, chooserTitle: String) {
        val dir = File(context.cacheDir, "shared_images")
        if (!dir.exists()) dir.mkdirs()

        val output = File(dir, fileName)
        FileOutputStream(output).use { stream ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            output
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }
}
