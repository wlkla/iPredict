package com.chouchou.ipredict.ui.dates

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chouchou.ipredict.R
import com.chouchou.ipredict.data.EventDate
import com.chouchou.ipredict.databinding.ItemDateBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class DateAdapter(private val onDeleteClick: (EventDate, RecyclerView.ViewHolder) -> Unit) :
    ListAdapter<EventDate, DateAdapter.DateViewHolder>(DateDiffCallback()) {

    private lateinit var recyclerView: RecyclerView

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val binding = ItemDateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DateViewHolder(binding)
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
        private val binding: ItemDateBinding
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

    // 滑动删除的ItemTouchHelper回调实现
    class SwipeToDeleteCallback(
        private val context: Context,
        private val adapter: DateAdapter,
        private val onDelete: (EventDate, RecyclerView.ViewHolder) -> Unit
    ) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

        // 根据主题选择背景颜色
        private val backgroundColor = if (isNightMode(context)) {
            Color.parseColor("#FF9FB4") // 浅粉色 - 深色主题下使用
        } else {
            Color.parseColor("#FF4573") // 深粉色 - 浅色主题下使用
        }

        private val background = ColorDrawable(backgroundColor)

        // 获取删除图标
        private val deleteIcon = ContextCompat.getDrawable(
            context,
            R.drawable.ic_delete // 删除图标
        )

        // 设置文字画笔
        private val textPaint = Paint().apply {
            color = Color.WHITE
            textSize = 42f
            textAlign = Paint.Align.LEFT
            isAntiAlias = true
        }

        // 检查是否为夜间模式
        private fun isNightMode(context: Context): Boolean {
            return (context.resources.configuration.uiMode and
                    Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return false // 不支持上下拖动
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val eventDate = adapter.getItem(position)
                onDelete(eventDate, viewHolder)
            }
        }

        override fun onChildDraw(
            c: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            dX: Float,
            dY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            val itemView = viewHolder.itemView
            val itemHeight = itemView.bottom - itemView.top

            // 绘制背景
            background.setBounds(
                itemView.right + dX.toInt(),
                itemView.top,
                itemView.right,
                itemView.bottom
            )
            background.draw(c)

            // 计算图标位置 - 放大并放在文字左侧
            val iconSize = 60 // 增大图标尺寸
            val iconLeftMargin = 200 // 图标到右边界的距离
            val iconTop = itemView.top + (itemHeight - iconSize) / 2
            val iconBottom = iconTop + iconSize
            val iconRight = itemView.right - iconLeftMargin
            val iconLeft = iconRight - iconSize

            // 绘制删除图标
            deleteIcon?.let {
                it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                it.draw(c)
            }

            // 绘制"删除"文字 - 放在图标右侧
            val textX = itemView.right - iconLeftMargin - 1f // 文字位置调整到图标左侧
            val textY = itemView.top + itemHeight / 2f + 15f // 文字垂直居中
            c.drawText("删除", textX, textY, textPaint)

            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    // 将SwipeToDeleteCallback附加到RecyclerView的方法
    fun attachSwipeHelper(recyclerView: RecyclerView) {
        this.recyclerView = recyclerView
        val swipeHandler = SwipeToDeleteCallback(recyclerView.context, this, this::handleSwipeDelete)
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    // 处理滑动删除的方法
    private fun handleSwipeDelete(eventDate: EventDate, viewHolder: RecyclerView.ViewHolder) {
        // 调用传入的删除回调，并传递ViewHolder以便后续恢复
        onDeleteClick(eventDate, viewHolder)
    }

    // 恢复滑动状态的公共方法
    fun restoreItem(position: Int) {
        notifyItemChanged(position)
    }
}