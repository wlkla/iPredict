package com.lkl.ipredict.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.content.ContextCompat
import android.view.View
import android.widget.ScrollView
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ShareImageUtils {

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

    private fun buildBitmapFromView(view: View): Bitmap? {
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
}
