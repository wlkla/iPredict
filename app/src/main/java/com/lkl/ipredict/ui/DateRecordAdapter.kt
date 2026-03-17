package com.lkl.ipredict.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.lkl.ipredict.databinding.ItemDateRecordBinding

class DateRecordAdapter : RecyclerView.Adapter<DateRecordAdapter.RecordViewHolder>() {

    private val items = mutableListOf<DateRecord>()
    var onItemClick: ((DateRecord) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordViewHolder {
        val binding = ItemDateRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecordViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordViewHolder, position: Int) {
        val item = items[position]
        holder.bind(item)
        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size

    fun submitList(list: List<DateRecord>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun getItem(position: Int): DateRecord? {
        return items.getOrNull(position)
    }

    class RecordViewHolder(
        private val binding: ItemDateRecordBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: DateRecord) {
            val accent = AccentThemeManager.currentPalette(binding.root.context)
            binding.viewAccent.setBackgroundColor(accent.primary)
            binding.txtDate.text = item.date
            binding.txtInterval.text = if (item.interval != null) {
                "间隔: ${item.interval}天"
            } else {
                "首次记录"
            }
        }
    }
}
