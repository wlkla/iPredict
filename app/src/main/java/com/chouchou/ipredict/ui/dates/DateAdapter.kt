package com.chouchou.ipredict.ui.dates

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chouchou.ipredict.data.EventDate
import com.chouchou.ipredict.databinding.ItemDateBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class DateAdapter(private val onDeleteClick: (EventDate) -> Unit) :
    ListAdapter<EventDate, DateAdapter.DateViewHolder>(DateDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val binding = ItemDateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DateViewHolder(binding, onDeleteClick)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val currentItem = getItem(position)
        val previousItem = if (position < itemCount - 1) getItem(position + 1) else null

        holder.bind(currentItem, previousItem)
    }

    // 重写 submitList 方法来确保列表更新时重新计算间隔信息
    override fun submitList(list: List<EventDate>?) {
        super.submitList(list?.let { ArrayList(it) })
    }

    class DateViewHolder(
        private val binding: ItemDateBinding,
        private val onDeleteClick: (EventDate) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        fun bind(eventDate: EventDate, previousDate: EventDate?) {
            binding.textDate.text = dateFormat.format(eventDate.date)

            if (previousDate != null) {
                val diffInMillies = eventDate.date.time - previousDate.date.time
                val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillies).toInt()
                binding.textDayDiff.text = "间隔: $diffInDays 天"
            } else {
                binding.textDayDiff.text = "首次记录"
            }

            // 设置长按删除功能
            binding.root.setOnLongClickListener {
                onDeleteClick(eventDate)
                true
            }
        }
    }

    class DateDiffCallback : DiffUtil.ItemCallback<EventDate>() {
        override fun areItemsTheSame(oldItem: EventDate, newItem: EventDate): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: EventDate, newItem: EventDate): Boolean {
            return oldItem.date.time == newItem.date.time
        }
    }
}