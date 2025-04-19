package com.chouchou.ipredict.ui.dates

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chouchou.ipredict.R
import com.chouchou.ipredict.data.EventDate
import com.chouchou.ipredict.databinding.DialogAddDateBinding
import com.chouchou.ipredict.databinding.FragmentDatesBinding
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import java.util.Calendar

class DatesFragment : Fragment() {

    private var _binding: FragmentDatesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DatesViewModel
    private lateinit var adapter: DateAdapter

    // 文件选择器结果处理
    private val importFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importDataFromUri(uri)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        updateButtonIconColors()
    }

    private fun updateButtonIconColors() {
        val context = requireContext()
        val colorStateList = ColorStateList.valueOf(
            context.getColorStateList(R.color.button_icon_color)?.defaultColor
                ?: context.getColor(android.R.color.darker_gray)
        )

        binding.buttonImport.iconTint = colorStateList
        binding.buttonExport.iconTint = colorStateList
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModel = ViewModelProvider(this).get(DatesViewModel::class.java)

        _binding = FragmentDatesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        setupRecyclerView()
        setupFab()
        setupExportButton()
        setupImportButton()

        return root
    }

    private fun setupRecyclerView() {
        // 更新适配器构造，传递新的删除回调函数
        adapter = DateAdapter { eventDate, viewHolder ->
            // 显示删除确认对话框，并在用户取消时恢复滑动状态
            showDeleteConfirmationDialog(eventDate, viewHolder.adapterPosition)
        }

        binding.recyclerDates.adapter = adapter
        binding.recyclerDates.layoutManager = LinearLayoutManager(requireContext())

        // 添加左滑删除功能
        adapter.attachSwipeHelper(binding.recyclerDates)

        // 观察数据变化并更新 UI
        viewModel.eventDates.observe(viewLifecycleOwner) { dates ->
            if (dates.isEmpty()) {
                binding.recyclerDates.visibility = View.GONE
                binding.layoutNoData.visibility = View.VISIBLE
                // 没有数据时禁用导出按钮
                binding.buttonExport.isEnabled = false
            } else {
                binding.recyclerDates.visibility = View.VISIBLE
                binding.layoutNoData.visibility = View.GONE
                // 有数据时启用导出按钮
                binding.buttonExport.isEnabled = true

                // 强制刷新列表
                adapter.submitList(null)
                adapter.submitList(dates)
            }
        }
    }

    private fun setupFab() {
        binding.fabAddDate.setOnClickListener {
            showAddDateDialog()
        }
    }

    private fun setupExportButton() {
        binding.buttonExport.setOnClickListener {
            // 显示选择菜单：导出当前事件类型或所有数据
            val popup = PopupMenu(requireContext(), binding.buttonExport)
            popup.menuInflater.inflate(R.menu.export_options, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_export_current -> {
                        exportCurrentEventTypeData()
                        true
                    }
                    R.id.action_export_all -> {
                        exportAllEventTypesData()
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun exportCurrentEventTypeData() {
        // 显示进度提示
        val progressSnackbar = Snackbar.make(binding.root, "正在准备导出...", Snackbar.LENGTH_INDEFINITE)
        progressSnackbar.show()

        lifecycleScope.launch {
            try {
                val result = viewModel.exportDataToCsv()
                progressSnackbar.dismiss()

                if (result.success && result.fileUri != null) {
                    shareExportedFile(result.fileUri)
                } else {
                    Snackbar.make(
                        binding.root,
                        result.errorMessage ?: "导出失败，请稍后重试",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                progressSnackbar.dismiss()
                Snackbar.make(
                    binding.root,
                    "导出出错: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun exportAllEventTypesData() {
        // 显示进度提示
        val progressSnackbar = Snackbar.make(binding.root, "正在准备导出所有数据...", Snackbar.LENGTH_INDEFINITE)
        progressSnackbar.show()

        lifecycleScope.launch {
            try {
                val result = viewModel.exportAllEventTypesData()
                progressSnackbar.dismiss()

                if (result.success && result.fileUri != null) {
                    shareExportedFile(result.fileUri)
                } else {
                    Snackbar.make(
                        binding.root,
                        result.errorMessage ?: "导出失败，请稍后重试",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                progressSnackbar.dismiss()
                Snackbar.make(
                    binding.root,
                    "导出出错: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun shareExportedFile(fileUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "iPredict 数据导出")
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(intent, "分享数据"))
        } catch (e: Exception) {
            Snackbar.make(binding.root, "无法分享文件: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun setupImportButton() {
        binding.buttonImport.setOnClickListener {
            // 显示选择菜单：导入到当前事件类型或导入所有数据
            val popup = PopupMenu(requireContext(), binding.buttonImport)
            popup.menuInflater.inflate(R.menu.import_options, popup.menu)

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_import_current -> {
                        launchFilePicker(false)
                        true
                    }
                    R.id.action_import_all -> {
                        launchFilePicker(true)
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }

    private fun launchFilePicker(importAll: Boolean) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "text/plain"
            ))
        }

        if (importAll) {
            importAllFileLauncher.launch(intent)
        } else {
            importFileLauncher.launch(intent)
        }
    }

    // 添加新的文件选择器结果处理
    private val importAllFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                importAllDataFromUri(uri)
            }
        }
    }

    private fun importAllDataFromUri(uri: Uri) {
        lifecycleScope.launch {
            val result = viewModel.importAllEventTypesData(uri)
            val message = when {
                result.errorMessage != null -> "导入失败: ${result.errorMessage}"
                result.successCount == 0 && result.errorCount == 0 -> "没有找到日期数据"
                result.errorCount == 0 -> result.errorMessage ?: "成功导入 ${result.successCount} 条记录"
                else -> "导入了 ${result.successCount} 条记录，${result.errorCount} 条无效"
            }

            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun launchFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
            // 支持的MIME类型
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "text/csv",
                "text/comma-separated-values",
                "application/csv",
                "text/plain"
            ))
        }

        importFileLauncher.launch(intent)
    }

    private fun importDataFromUri(uri: Uri) {
        lifecycleScope.launch {
            val result = viewModel.importDataFromCsv(uri)
            val message = when {
                result.errorMessage != null -> "导入失败: ${result.errorMessage}"
                result.successCount == 0 && result.errorCount == 0 -> "没有找到日期数据"
                result.errorCount == 0 -> "成功导入 ${result.successCount} 条记录"
                else -> "导入了 ${result.successCount} 条记录，${result.errorCount} 条无效"
            }

            Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showAddDateDialog() {
        val today = Calendar.getInstance()

        val datePickerBuilder = MaterialDatePicker.Builder.datePicker()
            .setTitleText("选择日期")
            .setSelection(today.timeInMillis)

        val datePicker = datePickerBuilder.build()

        datePicker.addOnPositiveButtonClickListener { timeInMillis ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = timeInMillis
            viewModel.addEventDate(calendar.time)
        }

        datePicker.show(parentFragmentManager, "DATE_PICKER")
    }

    private fun showDeleteConfirmationDialog(eventDate: EventDate, position: Int) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_event_title)
            .setMessage(R.string.delete_event_message)
            .setPositiveButton(R.string.confirm) { _, _ ->
                viewModel.deleteEventDate(eventDate)
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                // 用户取消删除，恢复滑动状态
                adapter.restoreItem(position)
            }
            .setOnCancelListener {
                // 对话框被取消（如按返回键），也要恢复滑动状态
                adapter.restoreItem(position)
            }
            .create()
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}