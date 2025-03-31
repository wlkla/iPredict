package com.chouchou.ipredict.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.chouchou.ipredict.data.EventDate
import com.chouchou.ipredict.data.db.EventDateDao
import com.chouchou.ipredict.data.db.EventDateEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class EventDateRepository(private val eventDateDao: EventDateDao) {

    // 获取所有事件日期（转换为领域模型）
    val allEventDates: LiveData<List<EventDate>> = eventDateDao.getAllEventDates().map { entities ->
        entities.map { entity ->
            EventDate(entity.id, entity.date)
        }
    }

    // 获取最新的事件日期
    val latestEventDate: LiveData<EventDate?> = eventDateDao.getLatestEventDate().map { entity ->
        entity?.let { EventDate(it.id, it.date) }
    }

    // 根据事件类型获取日期
    fun getEventDatesByType(eventTypeId: Int): LiveData<List<EventDate>> {
        return eventDateDao.getEventDatesByType(eventTypeId).map { entities ->
            entities.map { entity ->
                EventDate(entity.id, entity.date)
            }
        }
    }

    // 获取指定事件类型的最新日期
    fun getLatestEventDateByType(eventTypeId: Int): LiveData<EventDate?> {
        return eventDateDao.getLatestEventDateByType(eventTypeId).map { entity ->
            entity?.let { EventDate(it.id, it.date) }
        }
    }

    // 添加新的事件日期
    suspend fun insertEventDate(date: Date) {
        val eventDateEntity = EventDateEntity(date = date)
        eventDateDao.insertEventDate(eventDateEntity)
    }

    // 添加新的事件日期，带事件类型ID
    suspend fun insertEventDate(date: Date, eventTypeId: Int) {
        val eventDateEntity = EventDateEntity(date = date, eventTypeId = eventTypeId)
        eventDateDao.insertEventDate(eventDateEntity)
    }

    // 删除事件日期
    suspend fun deleteEventDate(eventDate: EventDate) {
        val eventDateEntity = EventDateEntity(id = eventDate.id, date = eventDate.date)
        eventDateDao.deleteEventDate(eventDateEntity)
    }

    // 删除特定事件类型的所有记录
    suspend fun deleteEventDatesByType(eventTypeId: Int) {
        eventDateDao.deleteEventDatesByType(eventTypeId)
    }

    // 根据ID删除事件日期
    suspend fun deleteEventDateById(id: Int) {
        eventDateDao.deleteEventDateById(id)
    }

    // 同步获取所有事件日期(不通过LiveData)
    suspend fun getAllEventDatesSync(): List<EventDate> {
        return withContext(Dispatchers.IO) {
            eventDateDao.getAllEventDatesSync().map { entity ->
                EventDate(entity.id, entity.date)
            }
        }
    }

    // 同步获取指定事件类型的所有日期
    suspend fun getEventDatesByTypeSync(eventTypeId: Int): List<EventDate> {
        return withContext(Dispatchers.IO) {
            eventDateDao.getEventDatesByTypeSync(eventTypeId).map { entity ->
                EventDate(entity.id, entity.date)
            }
        }
    }
}