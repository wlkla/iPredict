package com.chouchou.ipredict.ui.countdown

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.chouchou.ipredict.databinding.FragmentCountdownBinding

class CountdownFragment : Fragment() {

    private var _binding: FragmentCountdownBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CountdownViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(CountdownViewModel::class.java)

        _binding = FragmentCountdownBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 初始化进度环
        initProgress()

        // 观察预期事件日期变化
        viewModel.nextEventDate.observe(viewLifecycleOwner) {
            binding.textNextEvent.text = "下一个预期事件日期：$it"
        }

        // 观察倒计时天数变化
        viewModel.countdown.observe(viewLifecycleOwner) { countdown ->
            binding.textCountdown.text = countdown.toString()

            // 计算并更新进度环 (假设平均周期为28天)
            val averageCycle = 28f
            val progress = 1f - (countdown.toFloat() / averageCycle).coerceIn(0f, 1f)
            binding.progressCycle.setProgress(progress)
        }

        // 更新按钮点击事件
        binding.buttonUpdate.setOnClickListener {
            viewModel.recordEvent() // 记录今天的事件
        }

        return root
    }

    private fun initProgress() {
        // 初始进度值
        binding.progressCycle.setProgress(0.7f)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}