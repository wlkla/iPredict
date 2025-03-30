package com.chouchou.ipredict.ui.countdown

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置编辑按钮点击事件
        binding.buttonEditTip.setOnClickListener {
            showEditTipDialog()
        }

        // 加载保存的自定义提示
        loadCustomTip()
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