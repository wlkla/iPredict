package com.chouchou.ipredict.ui.dates

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.chouchou.ipredict.IPredictApplication
import com.chouchou.ipredict.data.EventDate
import com.chouchou.ipredict.data.repository.EventDateRepository
import kotlinx.coroutines.launch
import java.util.Date

class DatesViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: EventDateRepository = (application as IPredictApplication).repository

    // 获取所有事件日期
    val eventDates: LiveData<List<EventDate>> = repository.allEventDates

    // 添加新的事件日期
    fun addEventDate(date: Date) {
        viewModelScope.launch {
            repository.insertEventDate(date)
            // 插入完成后，repository 的 LiveData 会自动更新，但这里我们不需要手动触发更新
        }
    }

    // 删除事件日期
    fun deleteEventDate(eventDate: EventDate) {
        viewModelScope.launch {
            repository.deleteEventDate(eventDate)
        }
    }
}