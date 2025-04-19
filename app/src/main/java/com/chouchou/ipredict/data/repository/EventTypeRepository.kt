package com.chouchou.ipredict.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.chouchou.ipredict.data.EventType
import com.chouchou.ipredict.data.db.EventTypeDao
import com.chouchou.ipredict.data.db.EventTypeEntity

class EventTypeRepository(private val eventTypeDao: EventTypeDao) {

    // 获取所有事件类型
    val allEventTypes: LiveData<List<EventType>> = eventTypeDao.getAllEventTypes().map { entities ->
        entities.map { entity ->
            EventType(entity.id, entity.name, entity.isActive, entity.color)
        }
    }

    // 获取当前激活的事件类型
    val activeEventType: LiveData<EventType?> = eventTypeDao.getActiveEventType().map { entity ->
        entity?.let { EventType(it.id, it.name, it.isActive, it.color) }
    }

    // 添加新的事件类型
    suspend fun insertEventType(name: String, color: String = "#000000") {
        val eventTypeEntity = EventTypeEntity(name = name, color = color)
        eventTypeDao.insertEventType(eventTypeEntity)
    }

    // 设置激活的事件类型
    suspend fun setActiveEventType(id: Int) {
        eventTypeDao.clearActiveEventTypes()
        eventTypeDao.setActiveEventType(id)
    }

    // 删除事件类型
    suspend fun deleteEventType(eventType: EventType) {
        val eventTypeEntity = EventTypeEntity(id = eventType.id, name = eventType.name,
            isActive = eventType.isActive, color = eventType.color)
        eventTypeDao.deleteEventType(eventTypeEntity)
    }

    // 更新事件类型
    suspend fun updateEventType(eventType: EventType) {
        val eventTypeEntity = EventTypeEntity(id = eventType.id, name = eventType.name,
            isActive = eventType.isActive, color = eventType.color)
        eventTypeDao.updateEventType(eventTypeEntity)
    }

    suspend fun getAllEventTypesSync(): List<EventType> {
        return eventTypeDao.getAllEventTypesSync().map { entity ->
            EventType(entity.id, entity.name, entity.isActive, entity.color)
        }
    }

    suspend fun insertEventType(eventType: EventTypeEntity): Long {
        return eventTypeDao.insertEventType(eventType)
    }
}