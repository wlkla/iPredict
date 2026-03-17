package com.lkl.ipredict.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import com.lkl.ipredict.R
import com.lkl.ipredict.data.TrackerEvent
import com.lkl.ipredict.databinding.ItemDrawerEventBinding

class DrawerEventAdapter(
    private val onClick: (TrackerEvent) -> Unit,
    private val onLongClick: (TrackerEvent) -> Unit,
    private val accentProvider: () -> AccentPalette
) : RecyclerView.Adapter<DrawerEventAdapter.EventHolder>() {

    private val items = mutableListOf<TrackerEvent>()
    private var currentEventName: String = ""

    fun submit(list: List<TrackerEvent>, currentName: String) {
        items.clear()
        items.addAll(list)
        currentEventName = currentName
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventHolder {
        val binding = ItemDrawerEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventHolder(binding)
    }

    override fun onBindViewHolder(holder: EventHolder, position: Int) {
        val item = items[position]
        val selected = item.name == currentEventName
        holder.bind(item, selected, accentProvider())
        holder.itemView.setOnClickListener { onClick(item) }
        holder.itemView.setOnLongClickListener {
            onLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = items.size

    class EventHolder(private val binding: ItemDrawerEventBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TrackerEvent, selected: Boolean, accent: AccentPalette) {
            binding.txtEventName.text = if (selected) "✓ ${item.name}" else item.name
            if (selected) {
                val bgColor = if (accent.isGradient) {
                    ColorUtils.blendARGB(accent.track, accent.secondary, 0.35f)
                } else {
                    accent.track
                }
                val selectedTextColor = if (ColorUtils.calculateLuminance(bgColor) > 0.55) {
                    Color.parseColor("#FF2A2140")
                } else {
                    ContextCompat.getColor(binding.root.context, R.color.text_primary)
                }
                binding.txtEventName.setTextColor(selectedTextColor)
                val shape = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(14f)
                    if (accent.isGradient) {
                        orientation = GradientDrawable.Orientation.LEFT_RIGHT
                        colors = intArrayOf(accent.track, accent.secondary)
                    } else {
                        setColor(accent.track)
                    }
                    setStroke(dp(1f).toInt(), accent.primary)
                }
                binding.txtEventName.background = shape
            } else {
                binding.txtEventName.setTextColor(
                    ContextCompat.getColor(binding.root.context, R.color.text_primary)
                )
                binding.txtEventName.setBackgroundResource(R.drawable.bg_neu_soft)
            }
        }

        private fun dp(value: Float): Float {
            return value * binding.root.resources.displayMetrics.density
        }
    }
}
