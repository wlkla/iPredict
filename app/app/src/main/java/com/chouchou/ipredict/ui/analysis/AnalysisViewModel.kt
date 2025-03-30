package com.chouchou.ipredict.ui.analysis

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.chouchou.ipredict.IPredictApplication
import com.chouchou.ipredict.data.EventDate
import com.chouchou.ipredict.data.repository.EventDateRepository
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class AnalysisViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EventDateRepository = (application as IPredictApplication).repository

    private val _dayDiffData = MutableLiveData<List<Int>>()
    val dayDiffData: LiveData<List<Int>> = _dayDiffData

    private val _cycleFrequencyData = MutableLiveData<Map<Int, Int>>()
    val cycleFrequencyData: LiveData<Map<Int, Int>> = _cycleFrequencyData

    init {
        // 观察数据库中的日期记录变化
        repository.allEventDates.observeForever { dates ->
            if (dates.size >= 2) {
                // 计算并更新分析数据
                updateAnalysisData(dates)
            } else {
                // 数据不足，清空分析结果
                _dayDiffData.value = emptyList()
                _cycleFrequencyData.value = emptyMap()
            }
        }
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