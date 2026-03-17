package com.lkl.ipredict.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.TextView
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.lkl.ipredict.R
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.Description
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.github.mikephil.charting.animation.Easing
import com.lkl.ipredict.data.EventRepository
import com.lkl.ipredict.databinding.FragmentAnalysisBinding
import com.lkl.ipredict.util.ShareImageUtils
import com.lkl.ipredict.util.CycleCalculator
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlin.math.roundToInt

class AnalysisFragment : Fragment(), AccentAware {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!

    private var latestDistribution: List<Pair<Int, Int>> = emptyList()
    private var latestIntervals: List<Int> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.btnShareAnalysis.setOnClickListener {
            shareBeautified()
        }
        setupChartInteractions()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun setupChartInteractions() {
        binding.lineChart.setTouchEnabled(true)
        binding.lineChart.setDragEnabled(true)
        binding.lineChart.setScaleEnabled(true)
        binding.lineChart.setPinchZoom(true)

        binding.barChart.setTouchEnabled(true)
        binding.barChart.setDragEnabled(true)
        binding.barChart.setScaleEnabled(true)
        binding.barChart.setPinchZoom(true)

        binding.pieChart.setTouchEnabled(true)
        binding.pieChart.isRotationEnabled = true
        binding.pieChart.isHighlightPerTapEnabled = true

        val neutralListener = object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                // Keep highlight only; no intrusive hint toast.
            }

            override fun onNothingSelected() = Unit
        }

        binding.lineChart.setOnChartValueSelectedListener(neutralListener)
        binding.barChart.setOnChartValueSelectedListener(neutralListener)
        binding.pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val pie = e as? PieEntry ?: return
                val total = latestDistribution.sumOf { it.second }.coerceAtLeast(1)
                val percent = pie.value * 100f / total
                binding.pieChart.setDrawCenterText(true)
                binding.pieChart.centerText = "${String.format("%.1f", percent)}%\n${pie.label}"
                binding.pieChart.invalidate()
            }

            override fun onNothingSelected() {
                binding.pieChart.centerText = ""
                binding.pieChart.invalidate()
            }
        })
    }

    private fun render() {
        val dates = EventRepository.getDates(requireContext())
        latestIntervals = CycleCalculator.intervals(dates).filter { it > 0 }
        latestDistribution = latestIntervals
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedBy { it.first }

        if (latestIntervals.isEmpty()) {
            renderEmpty()
            return
        }

        renderLineChart(latestIntervals)
        renderBarChart(latestDistribution)
        renderPieChart(latestDistribution)
    }

    private fun renderEmpty() {
        binding.lineChart.clear()
        binding.barChart.clear()
        binding.pieChart.clear()
    }

    private fun renderLineChart(intervals: List<Int>) {
        val accent = AccentThemeManager.currentPalette(requireContext())
        val entries = intervals.reversed().mapIndexed { index, value ->
            Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = LineDataSet(entries, "周期(天)").apply {
            color = accent.primary
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.chart_value_text)
            lineWidth = 2.2f
            circleRadius = 4.2f
            setCircleColor(accent.secondary)
            setDrawValues(true)
            setDrawFilled(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            valueFormatter = object : ValueFormatter() {
                override fun getPointLabel(entry: Entry?): String {
                    return "${entry?.y?.roundToInt() ?: 0}"
                }
            }
        }

        binding.lineChart.data = LineData(dataSet)
        binding.lineChart.setNoDataText("暂无数据")
        binding.lineChart.axisRight.isEnabled = false
        binding.lineChart.axisLeft.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        binding.lineChart.axisLeft.gridColor = ContextCompat.getColor(requireContext(), R.color.divider)
        binding.lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(
            entries.indices.map { (it + 1).toString() }
        )
        binding.lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.lineChart.xAxis.granularity = 1f
        binding.lineChart.xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        binding.lineChart.xAxis.axisLineColor = ContextCompat.getColor(requireContext(), R.color.divider)
        binding.lineChart.xAxis.setDrawGridLines(false)
        val minY = intervals.minOrNull()?.toFloat() ?: 0f
        val maxY = intervals.maxOrNull()?.toFloat() ?: 0f
        val span = (maxY - minY).coerceAtLeast(1f)
        val padding = (span * 0.2f).coerceAtLeast(1f)
        binding.lineChart.axisLeft.axisMinimum = (minY - padding).coerceAtLeast(0f)
        binding.lineChart.axisLeft.axisMaximum = maxY + padding
        val avg = intervals.average().toFloat()
        val limitLine = LimitLine(avg, "均值 ${avg.roundToInt()}天").apply {
            lineColor = ContextCompat.getColor(requireContext(), R.color.chart_limit_line)
            textColor = ContextCompat.getColor(requireContext(), R.color.chart_limit_text)
            lineWidth = 1.4f
            textSize = 10f
        }
        binding.lineChart.axisLeft.removeAllLimitLines()
        binding.lineChart.axisLeft.addLimitLine(limitLine)
        binding.lineChart.legend.form = Legend.LegendForm.LINE
        binding.lineChart.legend.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        binding.lineChart.description = Description().apply { text = "" }
        binding.lineChart.invalidate()
        binding.lineChart.animateX(500, Easing.EaseInOutCubic)
    }

    private fun renderBarChart(distribution: List<Pair<Int, Int>>) {
        val accent = AccentThemeManager.currentPalette(requireContext())
        val entries = distribution.mapIndexed { index, pair ->
            BarEntry(index.toFloat(), pair.second.toFloat())
        }
        val colors = listOf(
            accent.primary,
            accent.secondary,
            Color.parseColor("#90CAF9"),
            Color.parseColor("#FFE082")
        )

        val dataSet = BarDataSet(entries, "间隔次数").apply {
            setColors(colors)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.chart_value_text)
            valueTextSize = 11f
            valueFormatter = object : ValueFormatter() {
                override fun getBarLabel(barEntry: BarEntry?): String {
                    return "${barEntry?.y?.toInt() ?: 0}"
                }
            }
        }

        binding.barChart.data = BarData(dataSet).apply { barWidth = 0.5f }
        binding.barChart.setNoDataText("暂无数据")
        binding.barChart.axisRight.isEnabled = false
        binding.barChart.axisLeft.axisMinimum = 0f
        binding.barChart.axisLeft.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        binding.barChart.axisLeft.gridColor = ContextCompat.getColor(requireContext(), R.color.divider)
        binding.barChart.xAxis.valueFormatter = IndexAxisValueFormatter(
            distribution.map { "${it.first}天" }
        )
        binding.barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        binding.barChart.xAxis.granularity = 1f
        binding.barChart.xAxis.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        binding.barChart.xAxis.axisLineColor = ContextCompat.getColor(requireContext(), R.color.divider)
        binding.barChart.xAxis.setDrawGridLines(false)
        binding.barChart.description = Description().apply { text = "" }
        binding.barChart.legend.form = Legend.LegendForm.SQUARE
        binding.barChart.legend.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        binding.barChart.invalidate()
        binding.barChart.animateY(500, Easing.EaseInOutCubic)
    }

    private fun renderPieChart(distribution: List<Pair<Int, Int>>) {
        val accent = AccentThemeManager.currentPalette(requireContext())
        val total = distribution.sumOf { it.second }.coerceAtLeast(1)
        val entries = distribution.map { (day, count) -> PieEntry(count.toFloat(), "${day}天") }

        val dataSet = PieDataSet(entries, "")
        dataSet.colors = listOf(
            accent.secondary,
            accent.primary,
            Color.parseColor("#90CAF9"),
            Color.parseColor("#CE93D8")
        )
        dataSet.valueTextColor = ContextCompat.getColor(requireContext(), R.color.chart_label_text)
        dataSet.valueTextSize = 11f
        dataSet.sliceSpace = 2f
        dataSet.selectionShift = 6f
        dataSet.setDrawValues(false)

        val data = PieData(dataSet)
        data.setValueFormatter(object : ValueFormatter() {
            override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                val percent = if (total == 0) 0f else value * 100f / total
                return "${String.format("%.1f", percent)}%"
            }
        })

        binding.pieChart.data = data
        binding.pieChart.setNoDataText("暂无数据")
        binding.pieChart.setUsePercentValues(false)
        binding.pieChart.setDrawEntryLabels(false)
        binding.pieChart.setEntryLabelColor(
            ContextCompat.getColor(requireContext(), R.color.chart_label_text)
        )
        binding.pieChart.setDrawCenterText(false)
        binding.pieChart.centerText = ""
        binding.pieChart.setHoleColor(Color.TRANSPARENT)
        binding.pieChart.setTransparentCircleAlpha(0)
        binding.pieChart.transparentCircleRadius = binding.pieChart.holeRadius
        binding.pieChart.setTransparentCircleColor(Color.TRANSPARENT)
        binding.pieChart.description = Description().apply { text = "" }
        binding.pieChart.legend.isEnabled = true
        binding.pieChart.legend.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
        binding.pieChart.invalidate()
        binding.pieChart.animateY(500, Easing.EaseInOutCubic)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onAccentChanged() {
        if (_binding != null) {
            render()
        }
    }

    private fun shareBeautified() {
        val dates = EventRepository.getDates(requireContext())
        if (latestIntervals.isEmpty()) {
            Toast.makeText(requireContext(), "暂无数据可分享", Toast.LENGTH_SHORT).show()
            return
        }
        showShareThemePicker(dates)
    }

    private fun showShareThemePicker(dates: List<String>) {
        val context = requireContext()
        val themes = ShareImageUtils.themes(context)
        val lineBmp = ShareImageUtils.buildBitmapFromView(binding.lineChart) ?: return
        val barBmp = ShareImageUtils.buildBitmapFromView(binding.barChart) ?: return
        val pieBmp = ShareImageUtils.buildBitmapFromView(binding.pieChart) ?: return
        val previews = themes.mapNotNull { theme ->
            ShareImageUtils.buildBitmapFromLayout(
                context,
                R.layout.share_card_analysis,
                theme
            ) { view ->
                view.findViewById<TextView>(R.id.shareAnalysisTitle).text = "周期分析"
                view.findViewById<TextView>(R.id.shareAnalysisSubtitle).text = "事件：${EventRepository.getCurrentEventName(context)}"
                view.findViewById<TextView>(R.id.shareAnalysisAvg).text =
                    if (latestIntervals.isEmpty()) "--" else "${latestIntervals.average().roundToInt()}天"
                view.findViewById<TextView>(R.id.shareAnalysisSamples).text = "${latestIntervals.size}次"
                view.findViewById<TextView>(R.id.shareAnalysisLatest).text = dates.firstOrNull() ?: "--"
                view.findViewById<TextView>(R.id.shareAnalysisIntervals).text = if (latestIntervals.isEmpty()) {
                    "最近间隔：暂无数据"
                } else {
                    "最近间隔：" + latestIntervals.take(4).joinToString(" · ") { "${it}天" }
                }
                view.findViewById<TextView>(R.id.shareAnalysisFooter).text = "iPredict · 数据可视化"
                view.findViewById<ImageView>(R.id.shareLineImage).setImageBitmap(lineBmp)
                view.findViewById<ImageView>(R.id.shareBarImage).setImageBitmap(barBmp)
                view.findViewById<ImageView>(R.id.sharePieImage).setImageBitmap(pieBmp)
            }
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
            ShareImageUtils.shareBitmap(context, selected, "analysis_share.png", "分享周期分析")
            dialog.dismiss()
        }
        dialog.setContentView(view)
        dialog.behavior.peekHeight = (resources.displayMetrics.heightPixels * 0.85f).toInt()
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.show()
    }
}
