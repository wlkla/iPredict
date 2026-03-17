package com.lkl.ipredict.ui

import android.content.Context
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.graphics.ColorUtils

data class AccentPalette(
    val key: String,
    val label: String,
    @ColorInt val primary: Int,
    @ColorInt val secondary: Int,
    @ColorInt val track: Int,
    val isGradient: Boolean = false
)

object AccentThemeManager {
    private const val PREF_NAME = "ipredict_theme_prefs"
    private const val KEY_ACCENT = "accent_key"
    private const val DEFAULT_ACCENT = "blue"

    // Light mode palettes
    private val lightPalettes = listOf(
        AccentPalette(
            key = "red",
            label = "朱砂",
            primary = Color.parseColor("#FF6B6B"),
            secondary = Color.parseColor("#FF8E8E"),
            track = Color.parseColor("#FFF1F1")
        ),
        AccentPalette(
            key = "orange",
            label = "金橘",
            primary = Color.parseColor("#FFB067"),
            secondary = Color.parseColor("#FFC588"),
            track = Color.parseColor("#FFF5EC")
        ),
        AccentPalette(
            key = "yellow",
            label = "杏黄",
            primary = Color.parseColor("#F4D03F"),
            secondary = Color.parseColor("#F7DC6F"),
            track = Color.parseColor("#FEF9E7")
        ),
        AccentPalette(
            key = "green",
            label = "翠竹",
            primary = Color.parseColor("#58D68D"),
            secondary = Color.parseColor("#82E0AA"),
            track = Color.parseColor("#E9F7EF")
        ),
        AccentPalette(
            key = "blue",
            label = "天青",
            primary = Color.parseColor("#5DADE2"),
            secondary = Color.parseColor("#85C1E9"),
            track = Color.parseColor("#EBF5FB")
        ),
        AccentPalette(
            key = "indigo",
            label = "黛紫",
            primary = Color.parseColor("#8E44AD"),
            secondary = Color.parseColor("#A569BD"),
            track = Color.parseColor("#F4ECF7")
        ),
        AccentPalette(
            key = "purple",
            label = "紫罗兰",
            primary = Color.parseColor("#B79CFF"),
            secondary = Color.parseColor("#D1B2FF"),
            track = Color.parseColor("#F1EAFE")
        )
    )

    // Dark mode palettes with darker track colors matching each theme
    private val darkPalettes = listOf(
        AccentPalette(
            key = "red",
            label = "朱砂",
            primary = Color.parseColor("#FF6B6B"),
            secondary = Color.parseColor("#FF8E8E"),
            track = Color.parseColor("#3D2020")
        ),
        AccentPalette(
            key = "orange",
            label = "金橘",
            primary = Color.parseColor("#FFB067"),
            secondary = Color.parseColor("#FFC588"),
            track = Color.parseColor("#3D2A15")
        ),
        AccentPalette(
            key = "yellow",
            label = "杏黄",
            primary = Color.parseColor("#F4D03F"),
            secondary = Color.parseColor("#F7DC6F"),
            track = Color.parseColor("#3D3A20")
        ),
        AccentPalette(
            key = "green",
            label = "翠竹",
            primary = Color.parseColor("#58D68D"),
            secondary = Color.parseColor("#82E0AA"),
            track = Color.parseColor("#1A3D28")
        ),
        AccentPalette(
            key = "blue",
            label = "天青",
            primary = Color.parseColor("#5DADE2"),
            secondary = Color.parseColor("#85C1E9"),
            track = Color.parseColor("#1A2D40")
        ),
        AccentPalette(
            key = "indigo",
            label = "黛紫",
            primary = Color.parseColor("#8E44AD"),
            secondary = Color.parseColor("#A569BD"),
            track = Color.parseColor("#2D1A3D")
        ),
        AccentPalette(
            key = "purple",
            label = "紫罗兰",
            primary = Color.parseColor("#B79CFF"),
            secondary = Color.parseColor("#D1B2FF"),
            track = Color.parseColor("#2B2440")
        )
    )

    fun allPalettes(): List<AccentPalette> = lightPalettes

    fun currentPalette(context: Context): AccentPalette {
        return currentPalette(context, isNightMode(context))
    }

    fun currentPalette(context: Context, isDark: Boolean): AccentPalette {
        val key = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ACCENT, DEFAULT_ACCENT)
        val targetPalettes = if (isDark) darkPalettes else lightPalettes
        return targetPalettes.firstOrNull { it.key == key } ?: targetPalettes.first { it.key == DEFAULT_ACCENT }
    }

    fun savePalette(context: Context, key: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCENT, key)
            .apply()
    }

    fun isNightMode(context: Context): Boolean {
        val nightMode = AppCompatDelegate.getDefaultNightMode()
        return when (nightMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> true
            AppCompatDelegate.MODE_NIGHT_NO -> false
            else -> {
                // Check system configuration
                val config = context.resources.configuration
                val uiMode = config.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                uiMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
        }
    }

    @ColorInt
    fun ringLateColor(palette: AccentPalette): Int {
        return ColorUtils.blendARGB(palette.secondary, Color.parseColor("#F28E8E"), 0.55f)
    }

    fun indicatorColors(palette: AccentPalette, ratio: Float): IntArray {
        return if (palette.isGradient) {
            intArrayOf(palette.primary, palette.secondary)
        } else {
            val color = when {
                ratio > 0.66f -> palette.primary
                ratio > 0.33f -> palette.secondary
                else -> ringLateColor(palette)
            }
            intArrayOf(color)
        }
    }
}
