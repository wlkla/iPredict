package com.lkl.ipredict

import android.os.Bundle
import android.text.InputType
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.TextView
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lkl.ipredict.data.EventRepository
import com.lkl.ipredict.databinding.ActivityMainBinding
import com.lkl.ipredict.ui.AnalysisFragment
import com.lkl.ipredict.ui.AccentThemeManager
import com.lkl.ipredict.ui.AccentAware
import com.lkl.ipredict.ui.CountdownFragment
import com.lkl.ipredict.ui.DatesFragment
import com.lkl.ipredict.ui.DrawerEventAdapter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerEventAdapter: DrawerEventAdapter

    private val countdownFragment = CountdownFragment()
    private val datesFragment = DatesFragment()
    private val analysisFragment = AnalysisFragment()

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"
    }
    private var accentExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applySystemInsets()

        setupDrawer()

        binding.navIcon.setOnClickListener {
            refreshDrawer()
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.tab_countdown -> {
                    showFragment(countdownFragment, getString(R.string.tab_countdown))
                    true
                }

                R.id.tab_dates -> {
                    showFragment(datesFragment, getString(R.string.tab_dates))
                    true
                }

                R.id.tab_analysis -> {
                    showFragment(analysisFragment, getString(R.string.tab_analysis))
                    true
                }

                else -> false
            }
        }

        val selectedTab = savedInstanceState?.getInt(KEY_SELECTED_TAB) ?: R.id.tab_countdown
        binding.bottomNav.selectedItemId = selectedTab
    }

    private fun applySystemInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, insets ->
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.updatePadding(top = statusTop)
            insets
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SELECTED_TAB, binding.bottomNav.selectedItemId)
    }

    private fun setupDrawer() {
        drawerEventAdapter = DrawerEventAdapter(
            onClick = { event ->
                EventRepository.setCurrentEvent(this, event.name)
                Toast.makeText(this, "已切换到 ${event.name}", Toast.LENGTH_SHORT).show()
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                refreshUiAfterEventChange()
            },
            onLongClick = { event ->
                showEventActionDialog(event.name)
            },
            accentProvider = { AccentThemeManager.currentPalette(this) }
        )

        binding.drawerEventRecycler.layoutManager = LinearLayoutManager(this)
        binding.drawerEventRecycler.adapter = drawerEventAdapter

        binding.drawerThemeLight.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            applyAccentUi()
            notifyAccentChanged()
        }
        binding.drawerThemeDark.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            applyAccentUi()
            notifyAccentChanged()
        }
        binding.drawerThemeSystem.setOnClickListener {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            applyAccentUi()
            notifyAccentChanged()
        }
        setupAccentOptions()

        binding.drawerAddEvent.setOnClickListener {
            showAddEventDialog()
        }

        binding.drawerAbout.setOnClickListener {
            MaterialAlertDialogBuilder(this, R.style.ThemeOverlayIPredictDialog)
                .setTitle("iPredict")
                .setMessage("一个用于事件日期记录、周期提醒和图表分析的本地应用。")
                .setPositiveButton("知道了", null)
                .show()
        }

        refreshDrawer()
        applyAccentUi()
    }

    private fun setupAccentOptions() {
        binding.drawerAccentToggle.setOnClickListener {
            accentExpanded = !accentExpanded
            updateAccentExpandedUi()
        }
        binding.drawerAccentRed.setOnClickListener { onAccentSelected("red") }
        binding.drawerAccentOrange.setOnClickListener { onAccentSelected("orange") }
        binding.drawerAccentYellow.setOnClickListener { onAccentSelected("yellow") }
        binding.drawerAccentGreen.setOnClickListener { onAccentSelected("green") }
        binding.drawerAccentBlue.setOnClickListener { onAccentSelected("blue") }
        binding.drawerAccentIndigo.setOnClickListener { onAccentSelected("indigo") }
        binding.drawerAccentPurple.setOnClickListener { onAccentSelected("purple") }
        updateAccentExpandedUi()
    }

    private fun onAccentSelected(key: String) {
        AccentThemeManager.savePalette(this, key)
        applyAccentUi()
        refreshDrawer()
        notifyAccentChanged()
    }

    private fun applyAccentUi() {
        val isDark = AccentThemeManager.isNightMode(this)
        val accent = AccentThemeManager.currentPalette(this, isDark)
        val barColor = if (accent.isGradient) {
            ColorUtils.blendARGB(accent.track, accent.primary, 0.35f)
        } else {
            accent.track
        }
        val dividerColor = ColorUtils.blendARGB(barColor, accent.primary, 0.28f)

        binding.toolbar.setBackgroundColor(barColor)
        binding.topDivider.setBackgroundColor(dividerColor)
        binding.bottomNav.setBackgroundColor(barColor)

        val unselected = ContextCompat.getColor(this, R.color.bottom_nav_unselected)
        val states = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        )
        val colors = intArrayOf(accent.primary, unselected)
        val colorState = ColorStateList(states, colors)
        binding.bottomNav.itemIconTintList = colorState
        binding.bottomNav.itemTextColor = colorState

        val options = mapOf(
            "red" to binding.drawerAccentRed,
            "orange" to binding.drawerAccentOrange,
            "yellow" to binding.drawerAccentYellow,
            "green" to binding.drawerAccentGreen,
            "blue" to binding.drawerAccentBlue,
            "indigo" to binding.drawerAccentIndigo,
            "purple" to binding.drawerAccentPurple
        )
        options.forEach { (key, view) ->
            val selected = key == accent.key
            styleAccentOption(view, selected, accent)
        }
    }

    private fun updateAccentExpandedUi() {
        binding.drawerAccentContainer.visibility = if (accentExpanded) android.view.View.VISIBLE else android.view.View.GONE
        binding.drawerAccentToggle.text = if (accentExpanded) "收起主题色 ▴" else "展开主题色 ▾"
    }

    private fun notifyAccentChanged() {
        val current = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (current is AccentAware) {
            current.onAccentChanged()
        }
    }

    private fun styleAccentOption(view: TextView, selected: Boolean, accent: com.lkl.ipredict.ui.AccentPalette) {
        if (!selected) {
            view.setBackgroundResource(R.drawable.bg_neu_soft)
            view.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
            return
        }
        val bgColor = if (accent.isGradient) {
            ColorUtils.blendARGB(accent.track, accent.secondary, 0.35f)
        } else {
            accent.track
        }
        val selectedTextColor = if (ColorUtils.calculateLuminance(bgColor) > 0.55) {
            Color.parseColor("#FF2A2140")
        } else {
            ContextCompat.getColor(this, R.color.text_primary)
        }
        view.setTextColor(selectedTextColor)
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(14f)
            if (accent.isGradient) {
                orientation = GradientDrawable.Orientation.LEFT_RIGHT
                colors = intArrayOf(accent.track, accent.secondary)
            } else {
                setColor(accent.track)
            }
            setStroke(dp(1f).toInt(), accent.primary)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    private fun refreshDrawer() {
        val events = EventRepository.getEvents(this)
        val current = EventRepository.getCurrentEventName(this)
        drawerEventAdapter.submit(events, current)
        applyAccentUi()
    }

    private fun showAddEventDialog() {
        val input = EditText(this).apply {
            hint = "例如：咳嗽 / 周期时间 / 复查"
            inputType = InputType.TYPE_CLASS_TEXT
        }

        MaterialAlertDialogBuilder(this, R.style.ThemeOverlayIPredictDialog)
            .setTitle("新增事件")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("添加") { _, _ ->
                val added = EventRepository.addEvent(this, input.text.toString())
                val message = if (added) "事件已创建" else "创建失败：名称为空或已存在"
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                refreshUiAfterEventChange()
            }
            .show()
    }

    private fun showEventActionDialog(name: String) {
        val options = mutableListOf("重命名事件")
        if (EventRepository.getEvents(this).size > 1) {
            options.add("删除事件")
        }

        MaterialAlertDialogBuilder(this, R.style.ThemeOverlayIPredictDialog)
            .setTitle(name)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "重命名事件" -> showRenameEventDialog(name)
                    "删除事件" -> showDeleteEventDialog(name)
                }
            }
            .show()
    }

    private fun showRenameEventDialog(oldName: String) {
        val input = EditText(this).apply {
            setText(oldName)
            inputType = InputType.TYPE_CLASS_TEXT
        }

        MaterialAlertDialogBuilder(this, R.style.ThemeOverlayIPredictDialog)
            .setTitle("重命名事件")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val ok = EventRepository.renameEvent(this, oldName, input.text.toString())
                Toast.makeText(this, if (ok) "重命名成功" else "重命名失败：名称已存在或无效", Toast.LENGTH_SHORT).show()
                refreshUiAfterEventChange()
            }
            .show()
    }

    private fun showDeleteEventDialog(name: String) {
        MaterialAlertDialogBuilder(this, R.style.ThemeOverlayIPredictDialog)
            .setTitle("删除事件")
            .setMessage("确定删除事件“$name”吗？")
            .setNegativeButton("取消", null)
            .setPositiveButton("删除") { _, _ ->
                val ok = EventRepository.deleteEvent(this, name)
                Toast.makeText(this, if (ok) "已删除" else "至少保留一个事件", Toast.LENGTH_SHORT).show()
                refreshUiAfterEventChange()
            }
            .show()
    }

    private fun refreshUiAfterEventChange() {
        refreshDrawer()
        reloadCurrentTab()
    }

    private fun reloadCurrentTab() {
        when (binding.bottomNav.selectedItemId) {
            R.id.tab_dates -> showFragment(datesFragment, getString(R.string.tab_dates))
            R.id.tab_analysis -> showFragment(analysisFragment, getString(R.string.tab_analysis))
            else -> showFragment(countdownFragment, getString(R.string.tab_countdown))
        }
    }

    private fun showFragment(fragment: Fragment, title: String) {
        binding.toolbarTitle.text = title
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
