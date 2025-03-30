package com.chouchou.ipredict.ui.dates

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
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
        adapter = DateAdapter { eventDate ->
            showDeleteConfirmationDialog(eventDate)
        }

        binding.recyclerDates.adapter = adapter
        binding.recyclerDates.layoutManager = LinearLayoutManager(requireContext())

        // 观察数据变化并更新 UI
        // 在 DatesFragment.kt 中
        viewModel.eventDates.observe(viewLifecycleOwner) { dates ->
            if (dates.isEmpty()) {
                binding.textNoData.visibility = View.VISIBLE
                binding.recyclerDates.visibility = View.GONE
            } else {
                binding.textNoData.visibility = View.GONE
                binding.recyclerDates.visibility = View.VISIBLE

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

    private fun showDeleteConfirmationDialog(eventDate: EventDate) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_event_title)
            .setMessage(R.string.delete_event_message)
            .setPositiveButton(R.string.confirm) { _, _ ->
                viewModel.deleteEventDate(eventDate)
            }
            .setNegativeButton(R.string.cancel, null)
            .create()
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}