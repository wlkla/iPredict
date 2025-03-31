package com.chouchou.ipredict.ui.event

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.chouchou.ipredict.IPredictApplication
import com.chouchou.ipredict.data.EventType
import kotlinx.coroutines.launch

class EventTypeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as IPredictApplication).eventTypeRepository

    // 获取所有事件类型
    val allEventTypes: LiveData<List<EventType>> = repository.allEventTypes

    // 获取当前激活的事件类型
    val activeEventType: LiveData<EventType?> = repository.activeEventType

    // 添加新的事件类型
    fun addEventType(name: String, color: String = "#000000") {
        viewModelScope.launch {
            repository.insertEventType(name, color)
        }
    }

    // 设置激活的事件类型
    fun setActiveEventType(id: Int) {
        viewModelScope.launch {
            repository.setActiveEventType(id)
        }
    }

    // 删除事件类型
    fun deleteEventType(eventType: EventType) {
        viewModelScope.launch {
            repository.deleteEventType(eventType)
        }
    }

    fun deleteEventTypeAndEvents(eventType: EventType) {
        viewModelScope.launch {
            // 首先删除该事件类型的所有日期记录
            // 这需要在 EventDateRepository 中添加一个方法
            (getApplication() as IPredictApplication).eventDateRepository.deleteEventDatesByType(eventType.id)

            // 然后删除事件类型
            repository.deleteEventType(eventType)
        }
    }

    // 更新事件类型
    fun updateEventType(eventType: EventType) {
        viewModelScope.launch {
            repository.updateEventType(eventType)
        }
    }
}