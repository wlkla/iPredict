package com.chouchou.ipredict.ui.analysis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.switchMap
import com.chouchou.ipredict.IPredictApplication
import com.chouchou.ipredict.data.EventDate
import com.chouchou.ipredict.data.EventType
import com.chouchou.ipredict.data.repository.EventDateRepository
import com.chouchou.ipredict.data.repository.EventTypeRepository
import java.util.concurrent.TimeUnit

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val eventDateRepository: EventDateRepository = (application as IPredictApplication).eventDateRepository
    private val eventTypeRepository: EventTypeRepository = (application as IPredictApplication).eventTypeRepository

    private val _dayDiffData = MutableLiveData<List<Int>>()
    val dayDiffData: LiveData<List<Int>> = _dayDiffData

    private val _cycleFrequencyData = MutableLiveData<Map<Int, Int>>()
    val cycleFrequencyData: LiveData<Map<Int, Int>> = _cycleFrequencyData

    // 当前活动的事件类型
    val activeEventType: LiveData<EventType?> = eventTypeRepository.activeEventType

    // 使用这种方式替换 Transformations.switchMap
    val currentEventDates: LiveData<List<EventDate>> = activeEventType.switchMap { eventType ->
        eventType?.let {
            eventDateRepository.getEventDatesByType(it.id)
        } ?: eventDateRepository.allEventDates
    }

    // 设置观察者监听数据变化
    private val eventDatesObserver = Observer<List<EventDate>> { dates ->
        if (dates.size >= 2) {
            // 计算并更新分析数据
            updateAnalysisData(dates)
        } else {
            // 数据不足，清空分析结果
            _dayDiffData.value = emptyList()
            _cycleFrequencyData.value = emptyMap()
        }
    }

    init {
        // 观察当前事件类型的日期变化
        currentEventDates.observeForever(eventDatesObserver)
    }

    override fun onCleared() {
        super.onCleared()
        // 移除观察者避免内存泄漏
        currentEventDates.removeObserver(eventDatesObserver)
    }

    private fun updateAnalysisData(dates: List<EventDate>) {
        // 确保日期是按时间升序排列的（从最早到最近）
        val sortedDates = dates.sortedBy { it.date }

        // 计算连续事件之间的间隔天数
        val dayDiffs = mutableListOf<Int>()
        for (i in 0 until sortedDates.size - 1) {
            val diffInMillis = sortedDates[i + 1].date.time - sortedDates[i].date.time
            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis).toInt()
            dayDiffs.add(diffInDays)
        }

        // 计算周期频率
        val frequencyMap = dayDiffs.groupingBy { it }.eachCount()

        // 更新分析数据
        _dayDiffData.postValue(dayDiffs)
        _cycleFrequencyData.postValue(frequencyMap)
    }
}