package com.chouchou.ipredict.ui.analysis

import android.annotation.SuppressLint
import android.content.res.Configuration
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.chouchou.ipredict.databinding.FragmentAnalysisBinding
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
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
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ViewPortHandler
import kotlin.random.Random

class AnalysisFragment : Fragment() {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AnalysisViewModel

    // 马卡龙配色列表
    private val macaronColors = listOf(
        Color.parseColor("#F2BED1"),  // 粉色
        Color.parseColor("#86C9B4"),  // 绿色
        Color.parseColor("#F8E9B8"),  // 黄色
        Color.parseColor("#B7D2E4"),  // 蓝色
        Color.parseColor("#E9C5DD"),  // 浅紫色
        Color.parseColor("#A5D7D2"),  // 薄荷色
        Color.parseColor("#F5C0A0"),  // 桃色
        Color.parseColor("#D2E8C9")   // 嫩绿色
    )

    // 获取当前主题的文本颜色
    private fun getTextColor(context: Context): Int {
        val typedValue = TypedValue()
        val result =
            context.theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)

        // 记录颜色获取的结果
        Log.d("AnalysisFragment", "获取文本颜色: result = $result, data = ${typedValue.data}")

        // 如果获取失败，则根据当前主题选择合适的颜色
        if (!result || typedValue.data == 0) {
            val nightMode = (context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            return if (nightMode) Color.WHITE else Color.BLACK
        }

        return typedValue.data
    }

    // 获取当前主题的次要文本颜色
    private fun getSecondaryTextColor(context: Context): Int {
        val typedValue = TypedValue()
        val result =
            context.theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)

        // 记录颜色获取的结果
        Log.d("AnalysisFragment", "获取次要文本颜色: result = $result, data = ${typedValue.data}")

        // 如果获取失败，则根据当前主题选择合适的颜色
        if (!result || typedValue.data == 0) {
            val nightMode = (context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            return if (nightMode) Color.LTGRAY else Color.DKGRAY
        }

        return typedValue.data
    }

    // 随机颜色生成器
    private fun getRandomMacaronColors(count: Int): List<Int> {
        return macaronColors.shuffled().take(count.coerceAtMost(macaronColors.size))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(AnalysisViewModel::class.java)

        _binding = FragmentAnalysisBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupCharts()
        observeData()

        return root
    }

    private fun setupCharts() {
        // 获取当前主题的文本颜色
        val themeTextColor = getTextColor(requireContext())
        val secondaryTextColor = getSecondaryTextColor(requireContext())

        // 配置折线图 - 设置主题相关颜色
        with(binding.lineChart) {
            description.isEnabled = false
            legend.isEnabled = false  // 关闭图例说明
            setTouchEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            setDrawBorders(false)
            animateX(1000)

            // 确保所有文本颜色正确应用
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                textColor = secondaryTextColor  // 设置X轴文字颜色
                textSize = 11f
                labelCount = 5
                setDrawGridLines(false)
            }

            axisLeft.apply {
                textColor = secondaryTextColor  // 设置Y轴文字颜色
                textSize = 11f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#20000000") // 半透明网格线
            }

            axisRight.isEnabled = false
            setNoDataText("暂无数据")
            setNoDataTextColor(themeTextColor)
        }

        // 配置饼图 - 设置主题相关颜色，并增大饼图半径
        with(binding.pieChart) {
            description.isEnabled = false
            setUsePercentValues(true)
            legend.isEnabled = false  // 关闭图例说明

            setEntryLabelColor(themeTextColor)
            setEntryLabelTextSize(12f)
            setCenterTextColor(themeTextColor)
            setHoleColor(Color.TRANSPARENT)
            setDrawCenterText(false)
            setNoDataTextColor(themeTextColor)

            // 调整饼图大小参数 - 减小中心空白区域，增大整体饼图半径
            holeRadius = 30f       // 减小中心孔半径(原始值为30f)
            transparentCircleRadius = 35f  // 减小透明圆圈半径(原始值为35f)

            // 增加饼图的整体尺寸
            setExtraOffsets(10f, 10f, 10f, 10f)  // 设置饼图边距，留出标签空间

            // 启用交互
            setTouchEnabled(true)
            isRotationEnabled = true     // 允许旋转
            animateY(1200)
        }

        // 配置柱状图 - 设置主题相关颜色
        with(binding.barChart) {
            description.isEnabled = false
            legend.isEnabled = false  // 关闭图例说明

            setTouchEnabled(true)
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            setDrawBorders(false)
            animateY(1200)

            // 确保所有文本颜色正确应用
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                granularity = 1f
                textColor = secondaryTextColor  // 设置X轴文字颜色
                textSize = 11f
                setDrawGridLines(false)
            }

            axisLeft.apply {
                textColor = secondaryTextColor  // 设置Y轴文字颜色
                textSize = 11f
                setDrawGridLines(true)
                gridColor = Color.parseColor("#20000000") // 半透明网格线
            }

            axisRight.isEnabled = false
            setNoDataText("暂无数据")
            setNoDataTextColor(themeTextColor)
        }
    }

    private fun observeData() {
        // 观察间隔天数数据
        viewModel.dayDiffData.observe(viewLifecycleOwner) { dayDiffs ->
            if (dayDiffs.isEmpty()) {
                // 没有数据时，显示"暂无数据"布局，隐藏图表卡片
                binding.layoutNoData.visibility = View.VISIBLE
                binding.cardLineChart.visibility = View.GONE
                binding.cardBarChart.visibility = View.GONE
                binding.cardPieChart.visibility = View.GONE
            } else {
                // 有数据时，隐藏"暂无数据"布局，显示图表卡片
                binding.layoutNoData.visibility = View.GONE
                binding.cardLineChart.visibility = View.VISIBLE
                binding.cardBarChart.visibility = View.VISIBLE
                binding.cardPieChart.visibility = View.VISIBLE

                // 更新折线图
                updateLineChart(dayDiffs)
            }
        }

        // 观察周期频率数据
        viewModel.cycleFrequencyData.observe(viewLifecycleOwner) { frequencyMap ->
            if (frequencyMap.isEmpty()) {
                // 如果频率数据为空，也要确保饼图和柱状图不显示
                binding.cardBarChart.visibility = View.GONE
                binding.cardPieChart.visibility = View.GONE

                // 检查是否所有图表都需要隐藏
                if (viewModel.dayDiffData.value?.isEmpty() != false) {
                    binding.layoutNoData.visibility = View.VISIBLE
                }
            } else {
                // 确保饼图和柱状图可见
                binding.layoutNoData.visibility = View.GONE
                binding.cardBarChart.visibility = View.VISIBLE
                binding.cardPieChart.visibility = View.VISIBLE

                // 更新饼图和柱状图
                updatePieChart(frequencyMap)
                updateBarChart(frequencyMap)
            }
        }
    }

    @SuppressLint("DefaultLocale")
    private fun updateLineChart(dayDiffs: List<Int>) {
        val entries = dayDiffs.mapIndexed { index, dayDiff ->
            Entry(index.toFloat(), dayDiff.toFloat())
        }

        // 随机选择一个颜色
        val mainColor = macaronColors[Random.nextInt(macaronColors.size)]
        val themeTextColor = getTextColor(requireContext())
        val secondaryTextColor = getSecondaryTextColor(requireContext())

        val dataSet = LineDataSet(entries, "间隔天数").apply {
            color = mainColor
            setCircleColor(mainColor)
            lineWidth = 2.5f
            circleRadius = 5f
            setDrawCircleHole(false)
            valueTextSize = 11f
            valueTextColor = themeTextColor
            setDrawFilled(true)
            fillColor = mainColor
            fillAlpha = 30
            valueFormatter = DefaultValueFormatter(0)

            // 使用平滑曲线连接点
            mode = LineDataSet.Mode.CUBIC_BEZIER

            // 确保显示数据点和数值
            setDrawCircles(true)
            setDrawValues(true)

            // 高亮效果
            highLightColor = Color.parseColor("#50000000")
            setDrawHorizontalHighlightIndicator(false)
        }

        val lineData = LineData(dataSet)
        binding.lineChart.data = lineData

        binding.lineChart.xAxis.textColor = secondaryTextColor
        binding.lineChart.axisLeft.textColor = secondaryTextColor

        // 计算平均值
        val average = dayDiffs.average().toFloat()

        // 创建平均值线 - 确保颜色正确应用
        val limitLine = LimitLine(average, String.format("%.1f", average)).apply {
            lineWidth = 1.5f
            lineColor = secondaryTextColor  // 使用次要文本颜色
            textColor = secondaryTextColor
            enableDashedLine(10f, 10f, 0f)
            labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            textSize = 10f
        }

        // 清除旧的限制线并添加新的
        binding.lineChart.axisLeft.removeAllLimitLines()
        binding.lineChart.axisLeft.addLimitLine(limitLine)

        // 设置X轴标签
        binding.lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(
            dayDiffs.mapIndexed { index, _ -> "${index + 1}" }
        )

        // 设置图表可见数据点范围
        binding.lineChart.setVisibleXRangeMaximum(entries.size.toFloat())

        // 先重置图表
        binding.lineChart.invalidate()

        // 使用X轴动画，实现从左到右绘制效果
        binding.lineChart.animateX(1500)
    }

    private fun updatePieChart(frequencyMap: Map<Int, Int>) {
        // 检查数据是否为空
        if (frequencyMap.isEmpty()) {
            binding.pieChart.setNoDataText("暂无数据")
            binding.pieChart.invalidate()
            return
        }

        // 创建一个映射来存储天数和它们的标签
        val daysLabelMap = mutableMapOf<Float, String>()

        val entries = frequencyMap.map { (days, count) ->
            // 存储天数值和对应的标签
            daysLabelMap[count.toFloat()] = "$days 天"
            PieEntry(count.toFloat(), "$days 天")
        }

        // 随机抽取颜色
        val randomColors = getRandomMacaronColors(entries.size)
        val themeTextColor = getTextColor(requireContext())

        val dataSet = PieDataSet(entries, "周期频率").apply {
            colors = randomColors
            valueTextSize = 14f
            valueTextColor = if (themeTextColor != 0) themeTextColor else Color.BLACK
            sliceSpace = 1.5f  // 减小间隙，使饼图看起来更大

            // 标签配置
            yValuePosition = PieDataSet.ValuePosition.INSIDE_SLICE  // 将值显示在饼图外部
            valueLinePart1OffsetPercentage = 0.95f  // 连接线起点偏移量
            valueLinePart1Length = 0.4f  // 连接线第一段长度
            valueLinePart2Length = 0.6f  // 连接线第二段长度
            valueLineWidth = 1.5f        // 调整连接线宽度
            valueLineColor = themeTextColor  // 设置连接线颜色
            xValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE  // 标签也显示在外部
            selectionShift = 8f // 增大选中时突出显示的距离
        }

        // 自定义的值格式化器，根据原始值找到对应的标签
        val customFormatter = object : PercentFormatter(binding.pieChart) {
            override fun getFormattedValue(value: Float): String {
                // 计算原始值（基于总和和百分比）
                val total = entries.sumOf { it.value.toDouble() }.toFloat()
                val originalValue = value * total / 100f

                // 查找最接近的原始值对应的标签
                val closestEntry = entries.minByOrNull {
                    Math.abs(it.value - originalValue)
                }

                // 获取标签
                val label = closestEntry?.label ?: ""

                // 格式化百分比
                val percent = super.getFormattedValue(value)

                // 返回"标签 (百分比)"格式
                return "$label ($percent)"
            }
        }

        val pieData = PieData(dataSet).apply {
            setValueFormatter(customFormatter)
            setValueTextSize(12f)
            setValueTextColor(if (themeTextColor != 0) themeTextColor else Color.BLACK)
        }

        // 清除现有数据，然后设置新数据
        binding.pieChart.clear()
        binding.pieChart.data = pieData

        // 饼图配置
        binding.pieChart.setDrawEntryLabels(false)  // 禁用内部标签，使用外部标签
        binding.pieChart.setEntryLabelColor(if (themeTextColor != 0) themeTextColor else Color.BLACK)
        binding.pieChart.setEntryLabelTextSize(11f)
        binding.pieChart.isRotationEnabled = true   // 允许旋转
        binding.pieChart.setUsePercentValues(true)  // 使用百分比值
        binding.pieChart.description.isEnabled = false
        binding.pieChart.legend.isEnabled = false   // 关闭图例说明

        // 调整中心空白区域大小
        binding.pieChart.holeRadius = 20f          // 减小中心孔以增大饼图面积
        binding.pieChart.transparentCircleRadius = 40f  // 减小透明圈半径
        binding.pieChart.setHoleColor(Color.TRANSPARENT)

        // 设置最小角度来确保小饼块也能显示
        binding.pieChart.setMinAngleForSlices(15f)

        // 强制刷新并添加动画
        binding.pieChart.invalidate()
        binding.pieChart.animateY(1200)
    }

    private fun updateBarChart(frequencyMap: Map<Int, Int>) {
        // 将周期天数排序
        val sortedMap = frequencyMap.toSortedMap()

        val entries = sortedMap.entries.mapIndexed { index, (days, count) ->
            BarEntry(index.toFloat(), count.toFloat())
        }

        // 随机抽取颜色
        val randomColors = getRandomMacaronColors(entries.size)
        val themeTextColor = getTextColor(requireContext())
        val secondaryTextColor = getSecondaryTextColor(requireContext())

        val dataSet = BarDataSet(entries, "周期频率").apply {
            colors = randomColors
            valueTextSize = 12f
            valueTextColor = themeTextColor  // 使用主题文本颜色
            setValueFormatter(DefaultValueFormatter(0))

            // 高亮效果
            highLightColor = Color.parseColor("#50000000")
        }

        val barData = BarData(dataSet)
        barData.setValueTextColor(themeTextColor)
        barData.setValueTextSize(12f)
        barData.barWidth = 0.7f // 调整柱子宽度

        // 确保坐标轴颜色正确
        binding.barChart.xAxis.textColor = secondaryTextColor
        binding.barChart.axisLeft.textColor = secondaryTextColor

        // 保留X轴标签为天数
        binding.barChart.xAxis.valueFormatter =
            IndexAxisValueFormatter(sortedMap.keys.map { "$it" })
        binding.barChart.xAxis.labelRotationAngle = 0f // 确保标签水平显示

        binding.barChart.data = barData
        binding.barChart.invalidate()
        // 添加从下到上的动画效果
        binding.barChart.animateY(1200)
    }

    // 当配置变化（如主题切换）时重新设置图表颜色
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)

        // 重新获取当前主题的颜色
        val themeTextColor = getTextColor(requireContext())
        val secondaryTextColor = getSecondaryTextColor(requireContext())

        Log.d(
            "AnalysisFragment",
            "配置变化: textColor = $themeTextColor, secondaryTextColor = $secondaryTextColor"
        )

        // 重新设置所有图表的颜色
        setupCharts()

        // 如果有数据，重新刷新图表
        viewModel.dayDiffData.value?.let {
            if (it.isNotEmpty()) {
                updateLineChart(it)

                // 强制更新折线图的轴颜色
                binding.lineChart.xAxis.textColor = secondaryTextColor
                binding.lineChart.axisLeft.textColor = secondaryTextColor
                binding.lineChart.invalidate()
                // 添加动画
                binding.lineChart.animateX(1000)
            }
        }

        viewModel.cycleFrequencyData.value?.let {
            if (it.isNotEmpty()) {
                updatePieChart(it)
                updateBarChart(it)

                // 强制更新柱状图的轴颜色
                binding.barChart.xAxis.textColor = secondaryTextColor
                binding.barChart.axisLeft.textColor = secondaryTextColor
                binding.barChart.invalidate()
                // 添加动画
                binding.barChart.animateY(800)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}