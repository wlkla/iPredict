package com.lkl.ipredict.ui

import android.animation.ValueAnimator
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.lkl.ipredict.R
import com.lkl.ipredict.data.EventRepository
import com.lkl.ipredict.databinding.FragmentCountdownBinding
import com.lkl.ipredict.util.CycleCalculator
import com.lkl.ipredict.util.DateUtils
import com.lkl.ipredict.util.ReminderConfig
import com.lkl.ipredict.util.ReminderScheduler
import com.lkl.ipredict.util.ShareImageUtils
import kotlin.math.roundToInt

class CountdownFragment : Fragment(), AccentAware {

    private var _binding: FragmentCountdownBinding? = null
    private val binding get() = _binding!!
    private var ringAnimator: ValueAnimator? = null
    private var pendingScheduleConfig: ReminderConfig? = null

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val config = pendingScheduleConfig
            if (granted && config != null) {
                saveReminderConfig(config)
                ReminderScheduler.updateSchedule(requireContext())
            } else if (!granted && config != null) {
                Toast.makeText(requireContext(), "未授予通知权限，提醒未开启", Toast.LENGTH_SHORT).show()
                saveReminderConfig(config.copy(enabled = false))
            }
            pendingScheduleConfig = null
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCountdownBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAddToday.setOnClickListener {
            val today = DateUtils.today()
            val added = EventRepository.addDate(requireContext(), today)
            val message = if (added) {
                "已记录今天：$today"
            } else {
                "今天已经记录过了"
            }
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            render()
        }

        binding.btnShareCountdown.setOnClickListener {
            ShareImageUtils.shareViewAsImage(
                context = requireContext(),
                view = binding.cardCountdown,
                fileName = "countdown_snapshot.png",
                chooserTitle = "分享倒计时图片"
            )
        }

        binding.btnReminderSettings.setOnClickListener {
            showReminderDialog()
        }

        binding.btnGoDates.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.selectedItemId = R.id.tab_dates
        }
        binding.btnGoAnalysis.setOnClickListener {
            activity?.findViewById<BottomNavigationView>(R.id.bottomNav)?.selectedItemId = R.id.tab_analysis
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val dates = EventRepository.getDates(requireContext())
        val state = CycleCalculator.countdownState(dates)
        val intervals = CycleCalculator.intervals(dates).filter { it > 0 }

        binding.txtInsightTitle.text = "周期洞察"
        binding.txtInsightLatest.text = dates.firstOrNull() ?: "--"
        binding.txtInsightSamples.text = intervals.size.toString()
        binding.txtInsightAverage.text = if (intervals.isEmpty()) {
            "--"
        } else {
            "${intervals.average().roundToInt()}天"
        }
        binding.txtInsightRecentIntervals.text = if (intervals.isEmpty()) {
            "近几次间隔：暂无数据"
        } else {
            "近几次间隔：${intervals.take(4).joinToString(" · ") { "${it}天" }}"
        }

        if (state == null) {
            val accent = AccentThemeManager.currentPalette(requireContext())
            binding.progressCountdown.max = 360
            animateRing(fromDegree = 360, toDegree = 0)
            binding.progressCountdown.setIndicatorColor(*AccentThemeManager.indicatorColors(accent, 1f))
            binding.progressCountdown.trackColor = accent.track
            binding.txtDaysLeft.text = "--"
            binding.txtNextDate.text = if (dates.isEmpty()) {
                "请先记录至少 2 次事件日期"
            } else {
                "还需再记录一次，才能计算周期"
            }
            binding.txtSummary.text = "记录次数：${dates.size}"
            ReminderScheduler.updateSchedule(requireContext())
            return
        }

        binding.progressCountdown.max = 360
        val targetDegree = ((state.progress.toFloat() / state.averageCycle) * 360f)
            .roundToInt()
            .coerceIn(0, 360)
        animateRing(fromDegree = 360, toDegree = targetDegree)
        val ratio = if (state.averageCycle == 0) 0f else state.daysLeft.toFloat() / state.averageCycle
        val accent = AccentThemeManager.currentPalette(requireContext())
        binding.progressCountdown.setIndicatorColor(*AccentThemeManager.indicatorColors(accent, ratio))
        binding.progressCountdown.trackColor = accent.track

        // Display countdown or overdue text
        if (state.daysLeft >= 0) {
            binding.txtCountdownLabel.text = "倒计时"
            binding.txtCountdownLabel.setTextColor(resources.getColor(R.color.text_secondary, null))
            binding.txtDaysLeft.text = state.daysLeft.toString()
            binding.txtNextDate.text = "下一个预期事件日期：${state.nextDate}"
        } else {
            val overdueDays = kotlin.math.abs(state.daysLeft)
            binding.txtCountdownLabel.text = "已过期"
            binding.txtCountdownLabel.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
            binding.txtDaysLeft.text = overdueDays.toString()
            binding.txtNextDate.text = "已过期：${state.nextDate}"
        }
        binding.txtSummary.text = "平均周期 ${state.averageCycle} 天，距离上次已 ${state.daysSinceLast} 天"

        // 每次渲染时同步更新提醒计划
        ReminderScheduler.updateSchedule(requireContext())
    }

    private fun animateRing(fromDegree: Int, toDegree: Int) {
        ringAnimator?.cancel()
        binding.progressCountdown.max = 360

        ringAnimator = ValueAnimator.ofFloat(fromDegree.toFloat(), toDegree.toFloat()).apply {
            duration = 500L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animator ->
                val value = (animator.animatedValue as Float)
                    .roundToInt()
                    .coerceIn(0, 360)
                binding.progressCountdown.progress = value
            }
            start()
        }
    }

    private fun showReminderDialog() {
        val context = requireContext()
        val eventName = EventRepository.getCurrentEventName(context)
        val config = ReminderScheduler.getConfig(context, eventName)

        val dialogView = layoutInflater.inflate(R.layout.dialog_reminder_settings, null)
        val switchEnable = dialogView.findViewById<SwitchMaterial>(R.id.switchEnable)
        val pickerLead = dialogView.findViewById<NumberPicker>(R.id.pickerLeadDays)
        val txtTime = dialogView.findViewById<TextView>(R.id.txtReminderTime)

        switchEnable.isChecked = config.enabled
        pickerLead.minValue = 0
        pickerLead.maxValue = 30
        pickerLead.value = config.leadDays

        var selectedHour = config.hour
        var selectedMinute = config.minute

        fun refreshTimeLabel() {
            txtTime.text = String.format("%02d:%02d", selectedHour, selectedMinute)
        }
        refreshTimeLabel()

        txtTime.setOnClickListener {
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(selectedHour)
                .setMinute(selectedMinute)
                .setTitleText("选择提醒时间")
                .build()
            picker.addOnPositiveButtonClickListener {
                selectedHour = picker.hour
                selectedMinute = picker.minute
                refreshTimeLabel()
            }
            picker.show(parentFragmentManager, "time_picker")
        }

        MaterialAlertDialogBuilder(context, R.style.ThemeOverlayIPredictDialog)
            .setTitle("倒计时提醒")
            .setView(dialogView)
            .setNegativeButton("取消", null)
            .setPositiveButton("保存") { _, _ ->
                val newConfig = ReminderConfig(
                    enabled = switchEnable.isChecked,
                    leadDays = pickerLead.value,
                    hour = selectedHour,
                    minute = selectedMinute
                )
                handleReminderSave(newConfig)
            }
            .show()
    }

    private fun handleReminderSave(config: ReminderConfig) {
        if (!config.enabled) {
            saveReminderConfig(config)
            ReminderScheduler.updateSchedule(requireContext())
            Toast.makeText(requireContext(), "已关闭提醒", Toast.LENGTH_SHORT).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = requireContext().checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            if (!granted) {
                pendingScheduleConfig = config
                requestNotificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                return
            }
        }

        saveReminderConfig(config)
        ReminderScheduler.updateSchedule(requireContext())
        Toast.makeText(requireContext(), "提醒已更新", Toast.LENGTH_SHORT).show()
    }

    private fun saveReminderConfig(config: ReminderConfig) {
        val eventName = EventRepository.getCurrentEventName(requireContext())
        ReminderScheduler.saveConfig(requireContext(), eventName, config)
    }

    override fun onDestroyView() {
        ringAnimator?.cancel()
        ringAnimator = null
        super.onDestroyView()
        _binding = null
    }

    override fun onAccentChanged() {
        if (_binding != null) {
            render()
        }
    }
}
