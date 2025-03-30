package com.chouchou.ipredict.ui.countdown

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.chouchou.ipredict.IPredictApplication
import com.chouchou.ipredict.data.repository.EventDateRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class CountdownViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EventDateRepository = (application as IPredictApplication).repository

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

    // 平均周期天数（默认为28天）
    private var averageCycleDays = 28

    init {
        // 监听最新事件日期的变化，更新倒计时
        repository.allEventDates.map { dates ->
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
            dates.firstOrNull()?.date
        }.observeForever { latestDate ->
            latestDate?.let { updateCountdown(it) }
        }
    }

    // 记录事件方法
    fun recordEvent() {
        // 添加今天的事件记录
        viewModelScope.launch {
            repository.insertEventDate(Date())
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

        if (diffInDays >= 0) {
            // 还有天数到预期日期
            _countdown.value = diffInDays
            _countdownLabel.value = "倒计时"

            // 计算周期进度
            val daysPassed = averageCycleDays - diffInDays
            val progressPercentage = 100f * daysPassed / averageCycleDays
            _cycleProgress.value = progressPercentage
        } else {
            // 已经超过预期日期
            _countdown.value = -diffInDays
            _countdownLabel.value = "已过期"

            // 设置进度为100%
            _cycleProgress.value = 100f
        }
    }
}