package com.lkl.ipredict.ui

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.lkl.ipredict.R
import com.lkl.ipredict.data.EventRepository
import com.lkl.ipredict.databinding.FragmentCountdownBinding
import com.lkl.ipredict.util.CycleCalculator
import com.lkl.ipredict.util.DateUtils
import com.lkl.ipredict.util.ShareImageUtils
import kotlin.math.roundToInt
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import androidx.viewpager2.widget.ViewPager2

class CountdownFragment : Fragment(), AccentAware {

    private var _binding: FragmentCountdownBinding? = null
    private val binding get() = _binding!!
    private var ringAnimator: ValueAnimator? = null

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
            val dates = EventRepository.getDates(requireContext())
            val state = CycleCalculator.countdownState(dates)
            val intervals = CycleCalculator.intervals(dates).filter { it > 0 }
            if (state == null) {
                Toast.makeText(requireContext(), "至少需要 2 条记录才能分享", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showShareThemePicker(state, dates, intervals)
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
            "最近间隔：暂无数据"
        } else {
            "最近间隔：${intervals.take(4).joinToString(" · ") { "${it}天" }}"
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

    private fun showShareThemePicker(state: com.lkl.ipredict.util.CountdownState, dates: List<String>, intervals: List<Int>) {
        val context = requireContext()
        val themes = ShareImageUtils.themes(context)
        val previews = themes.mapNotNull { theme ->
            val bmp = ShareImageUtils.buildCountdownCardBitmap(
                context = context,
                eventName = EventRepository.getCurrentEventName(context),
                daysLeft = state.daysLeft,
                nextDate = state.nextDate,
                averageCycle = state.averageCycle,
                latestDate = dates.firstOrNull(),
                intervals = intervals,
                theme = theme
            )
            bmp
        }
        if (previews.isEmpty()) return

        val dialog = BottomSheetDialog(context)
        val view = layoutInflater.inflate(R.layout.dialog_share_theme, null)
        val pager = view.findViewById<ViewPager2>(R.id.pagerPreview)
        val btnShare = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnShareConfirm)

        val adapter = SharePreviewPagerAdapter(previews)
        pager.adapter = adapter
        pager.offscreenPageLimit = 1
        var currentIndex = 0
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentIndex = position
            }
        })

        btnShare.setOnClickListener {
            val selected = previews[currentIndex]
            ShareImageUtils.shareBitmap(context, selected, "countdown_share.png", "分享周期倒计时")
            dialog.dismiss()
        }

        dialog.setContentView(view)
        dialog.behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.85f).toInt()
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.show()
    }
}
