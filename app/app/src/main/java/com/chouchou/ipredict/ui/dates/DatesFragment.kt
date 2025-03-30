package com.chouchou.ipredict.ui.dates

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chouchou.ipredict.R
import com.chouchou.ipredict.data.EventDate
import com.chouchou.ipredict.databinding.DialogAddDateBinding
import com.chouchou.ipredict.databinding.FragmentDatesBinding
import java.util.Calendar

class DatesFragment : Fragment() {

    private var _binding: FragmentDatesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DatesViewModel
    private lateinit var adapter: DateAdapter

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
            } else {
                binding.recyclerDates.visibility = View.VISIBLE
                binding.layoutNoData.visibility = View.GONE

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

    private fun showAddDateDialog() {
        val dialogBinding = DialogAddDateBinding.inflate(layoutInflater)

        // 设置日期选择器初始为今天
        val today = Calendar.getInstance()
        dialogBinding.datePicker.init(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH),
            null
        )

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val calendar = Calendar.getInstance()
                calendar.set(
                    dialogBinding.datePicker.year,
                    dialogBinding.datePicker.month,
                    dialogBinding.datePicker.dayOfMonth
                )
                viewModel.addEventDate(calendar.time)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.show()
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