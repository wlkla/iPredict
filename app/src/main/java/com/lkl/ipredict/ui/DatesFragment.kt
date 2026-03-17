package com.lkl.ipredict.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.MaterialDatePicker
import com.lkl.ipredict.R
import com.lkl.ipredict.data.EventRepository
import com.lkl.ipredict.databinding.FragmentDatesBinding
import com.lkl.ipredict.util.CycleCalculator
import com.lkl.ipredict.util.DateUtils
import com.lkl.ipredict.util.ExchangeFormat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

class DatesFragment : Fragment(), AccentAware {

    private var _binding: FragmentDatesBinding? = null
    private val binding get() = _binding!!

    private val adapter = DateRecordAdapter()

    private val createExportFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            exportToFile(uri)
        }
    }

    private val openImportFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            importFromFile(uri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDatesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerDates.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDates.adapter = adapter

        adapter.onItemClick = { item ->
            showDatePicker(initialDate = item.date) { newDate ->
                val updated = EventRepository.updateDate(requireContext(), item.date, newDate)
                val message = if (updated) "已更新为 $newDate" else "更新失败：日期重复或无效"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                render()
            }
        }

        attachSwipeDelete()

        binding.btnImport.setOnClickListener {
            openImportFileLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
        }

        binding.btnExport.setOnClickListener {
            if (EventRepository.getEvents(requireContext()).isEmpty()) {
                Toast.makeText(requireContext(), "暂无可导出事件", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            createExportFileLauncher.launch("ipredict_${DateUtils.today()}.ipd")
        }

        binding.fabAdd.setOnClickListener {
            showDatePicker(initialDate = DateUtils.today()) { value ->
                val added = EventRepository.addDate(requireContext(), value)
                val message = if (added) "已添加 $value" else "日期已存在或格式无效"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                render()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val dates = EventRepository.getDates(requireContext())
        val records = dates.mapIndexed { index, date ->
            DateRecord(date = date, interval = CycleCalculator.intervalForIndex(dates, index))
        }
        adapter.submitList(records)
        binding.recyclerDates.layoutAnimation =
            AnimationUtils.loadLayoutAnimation(requireContext(), R.anim.layout_stagger_top)
        binding.recyclerDates.scheduleLayoutAnimation()
    }

    private fun attachSwipeDelete() {
        val helper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val item = adapter.getItem(position)
                if (item != null) {
                    EventRepository.removeDate(requireContext(), item.date)
                    Toast.makeText(requireContext(), "已删除 ${item.date}", Toast.LENGTH_SHORT).show()
                }
                render()
            }
        })
        helper.attachToRecyclerView(binding.recyclerDates)
    }

    private fun showDatePicker(initialDate: String, onPicked: (String) -> Unit) {
        val initialMillis = DateUtils.parse(initialDate)?.time ?: MaterialDatePicker.todayInUtcMilliseconds()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTheme(R.style.ThemeOverlayIPredictDatePicker)
            .setTitleText("选择日期")
            .setSelection(initialMillis)
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                time = Date(millis)
            }
            onPicked(
                DateUtils.fromPicker(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
            )
        }
        picker.show(parentFragmentManager, "date_picker")
    }

    private fun exportToFile(uri: Uri) {
        val events = EventRepository.getEvents(requireContext())
        val content = ExchangeFormat.serialize(events)

        val ok = runCatching {
            requireContext().contentResolver.openOutputStream(uri)?.use { output ->
                OutputStreamWriter(output, Charsets.UTF_8).use { it.write(content) }
            }
        }.isSuccess

        val message = if (ok) "导出成功" else "导出失败"
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun importFromFile(uri: Uri) {
        val content = runCatching {
            requireContext().contentResolver.openInputStream(uri)?.use { input ->
                BufferedReader(InputStreamReader(input, Charsets.UTF_8)).readText()
            }
        }.getOrNull()

        if (content.isNullOrBlank()) {
            Toast.makeText(requireContext(), "导入失败：文件为空", Toast.LENGTH_SHORT).show()
            return
        }

        val payload = ExchangeFormat.parse(content)
        if (payload == null) {
            Toast.makeText(requireContext(), "导入失败：文件格式不正确", Toast.LENGTH_SHORT).show()
            return
        }

        val result = EventRepository.mergeImportedEvents(requireContext(), payload.events)
        render()
        Toast.makeText(
            requireContext(),
            "导入完成：新增事件 ${result.addedEvents} 个，替换事件 ${result.replacedEvents} 个",
            Toast.LENGTH_SHORT
        ).show()
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
}
