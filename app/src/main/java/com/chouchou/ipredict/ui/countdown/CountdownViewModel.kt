package com.chouchou.ipredict.ui.countdown

import android.Manifest
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.chouchou.ipredict.IPredictApplication
import com.chouchou.ipredict.MainActivity
import com.chouchou.ipredict.R
import com.chouchou.ipredict.data.EventType
import com.chouchou.ipredict.data.repository.EventDateRepository
import com.chouchou.ipredict.data.repository.EventTypeRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class CountdownViewModel(application: Application) : AndroidViewModel(application) {

    private val eventDateRepository: EventDateRepository = (application as IPredictApplication).eventDateRepository
    private val eventTypeRepository: EventTypeRepository = (application as IPredictApplication).eventTypeRepository

    private val _nextEventDate = MutableLiveData<String>()
    val nextEventDate: LiveData<String> = _nextEventDate

    private val _countdown = MutableLiveData<Int>()
    val countdown: LiveData<Int> = _countdown

    private val _countdownLabel = MutableLiveData<String>()
    val countdownLabel: LiveData<String> = _countdownLabel

    // 新增：平均周期天数
    private val _cycleDays = MutableLiveData<Int>()
    val cycleDays: LiveData<Int> = _cycleDays

    // 新增：当前周期进度
    private val _cycleProgress = MutableLiveData<Float>()
    val cycleProgress: LiveData<Float> = _cycleProgress

    // 当前活动的事件类型
    val activeEventType: LiveData<EventType?> = eventTypeRepository.activeEventType

    // 平均周期天数（默认为28天）
    private var averageCycleDays = 28

    private val activeEventTypeObserver = Observer<EventType?> { eventType ->
        eventType?.let {
            // 当活动事件类型变化时，更新数据
            loadEventDatesByType(it.id)
        }
    }

    init {
        // 观察活动事件类型的变化
        activeEventType.observeForever(activeEventTypeObserver)
    }

    override fun onCleared() {
        super.onCleared()
        activeEventType.removeObserver(activeEventTypeObserver)
    }

    // 加载指定事件类型的日期数据
    private fun loadEventDatesByType(eventTypeId: Int) {
        eventDateRepository.getEventDatesByType(eventTypeId).observeForever { dates ->
            if (dates.size >= 2) {
                // 计算平均周期天数
                var totalDays = 0
                for (i in 0 until dates.size - 1) {
                    val diff = dates[i].date.time - dates[i + 1].date.time
                    totalDays += TimeUnit.MILLISECONDS.toDays(diff).toInt()
                }
                averageCycleDays = totalDays / (dates.size - 1)
                _cycleDays.value = averageCycleDays
            }

            // 获取最新日期
            dates.firstOrNull()?.let { updateCountdown(it.date) }
        }
    }

    // 记录事件方法
    fun recordEvent() {
        // 获取当前活动的事件类型ID
        val eventTypeId = activeEventType.value?.id ?: 1

        // 添加今天的事件记录
        viewModelScope.launch {
            eventDateRepository.insertEventDate(Date(), eventTypeId)
        }
    }

    private fun updateCountdown(lastEventDate: Date) {
        val calendar = Calendar.getInstance()
        val today = calendar.time

        // 复制一个日历对象，设置为最后事件日期
        val lastEventCalendar = Calendar.getInstance()
        lastEventCalendar.time = lastEventDate

        // 计算下一个预期事件日期
        lastEventCalendar.add(Calendar.DAY_OF_MONTH, averageCycleDays)
        val nextDate = lastEventCalendar.time

        // 格式化下一个事件日期
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        _nextEventDate.value = dateFormat.format(nextDate)

        // 计算倒计时天数
        val diffInMillis = nextDate.time - today.time
        val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()

        if (diffInDays > 0) {
            // 还有天数到预期日期
            _countdown.value = diffInDays
            _countdownLabel.value = "倒计时"

            // 计算周期进度 - 剩余天数与总周期的比例
            val daysPassed = averageCycleDays - diffInDays
            // 使用浮点数计算准确的进度比例，范围从0到1
            val progress = daysPassed.toFloat() / averageCycleDays.toFloat()
            // 将进度值直接发送到LiveData，不需要转换为百分比
            _cycleProgress.value = progress
        } else if (diffInDays == 0) {
            // 今天是预期日期
            _countdown.value = 0
            _countdownLabel.value = "今日到期"
            _cycleProgress.value = 1.0f
        } else {
            // 已经超过预期日期
            _countdown.value = -diffInDays
            _countdownLabel.value = "已过期"
            _cycleProgress.value = 1.0f
        }
    }
}