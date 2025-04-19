package com.chouchou.ipredict.ui.countdown

import android.Manifest
import android.graphics.Color
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.chouchou.ipredict.R
import com.chouchou.ipredict.databinding.FragmentCountdownBinding
import com.google.android.material.snackbar.Snackbar

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

        // 观察倒计时标签变化
        viewModel.countdownLabel.observe(viewLifecycleOwner) { label ->
            binding.textCountdownLabel.text = label
        }

        // 观察倒计时天数变化
        viewModel.countdown.observe(viewLifecycleOwner) { countdown ->
            binding.textCountdown.text = countdown.toString()
        }

        // 观察周期进度变化，直接使用计算好的进度值
        viewModel.cycleProgress.observe(viewLifecycleOwner) { progress ->
            // 直接使用ViewModel计算好的进度值
            binding.progressCycle.setProgress(progress)
        }

        // 观察当前活动事件类型
        viewModel.activeEventType.observe(viewLifecycleOwner) { eventType ->
            eventType?.let {
                // 更新标题以显示当前事件类型名称
                binding.textTitle.text = it.name

                // 可选：使用事件类型的颜色设置一些UI元素
                try {
                    val color = Color.parseColor(it.color)
                    binding.progressCycle.setProgressColor(color)
                    // 这里需要在 CircularProgressView 中添加 setProgressColor 方法
                } catch (e: Exception) {
                    // 颜色解析错误处理
                }
            }
        }

        // 更新按钮点击事件
        binding.buttonUpdate.setOnClickListener {
            viewModel.recordEvent() // 记录今天的事件
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置编辑按钮点击事件
        binding.buttonEditTip.setOnClickListener {
            showEditTipDialog()
        }

        // 加载保存的自定义提示
        loadCustomTip()
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 100
    }

    private fun initProgress() {
        // 初始进度值
        binding.progressCycle.setProgress(0.7f)
    }

    private fun showEditTipDialog() {
        val editText = EditText(requireContext()).apply {
            setText(binding.textTipContent.text)
            setHint("输入自定义提示内容")
            gravity = Gravity.TOP or Gravity.START
            minLines = 3
            maxLines = 5
            isSingleLine = false
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE

            // 设置内边距
            setPadding(
                resources.getDimensionPixelSize(R.dimen.dialog_padding),
                resources.getDimensionPixelSize(R.dimen.dialog_padding),
                resources.getDimensionPixelSize(R.dimen.dialog_padding),
                resources.getDimensionPixelSize(R.dimen.dialog_padding)
            )
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("编辑贴心小提示")
            .setView(editText)
            .setPositiveButton("保存") { _, _ ->
                val newTip = editText.text.toString().trim()
                if (newTip.isNotEmpty()) {
                    binding.textTipContent.text = newTip
                    // 保存到 SharedPreferences
                    saveCustomTip(newTip)
                }
            }
            .setNegativeButton("取消", null)
            .create()

        dialog.show()
    }

    private fun saveCustomTip(tip: String) {
        val sharedPrefs = requireActivity().getSharedPreferences("ipredict_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("custom_tip", tip).apply()
    }

    private fun loadCustomTip() {
        val sharedPrefs = requireActivity().getSharedPreferences("ipredict_prefs", Context.MODE_PRIVATE)
        val customTip = sharedPrefs.getString("custom_tip", null)
        if (!customTip.isNullOrEmpty()) {
            binding.textTipContent.text = customTip
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}