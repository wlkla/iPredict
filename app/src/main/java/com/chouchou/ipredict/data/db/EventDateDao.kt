package com.chouchou.ipredict.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface EventDateDao {
    @Query("SELECT * FROM event_dates ORDER BY date DESC")
    fun getAllEventDates(): LiveData<List<EventDateEntity>>

    @Query("SELECT * FROM event_dates ORDER BY date DESC LIMIT 1")
    fun getLatestEventDate(): LiveData<EventDateEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventDate(eventDate: EventDateEntity): Long

    @Delete
    suspend fun deleteEventDate(eventDate: EventDateEntity)

    @Query("DELETE FROM event_dates WHERE id = :id")
    suspend fun deleteEventDateById(id: Int)

    // 添加同步查询方法
    @Query("SELECT * FROM event_dates ORDER BY date DESC")
    suspend fun getAllEventDatesSync(): List<EventDateEntity>

    @Query("SELECT * FROM event_dates WHERE eventTypeId = :eventTypeId ORDER BY date DESC")
    fun getEventDatesByType(eventTypeId: Int): LiveData<List<EventDateEntity>>

    @Query("SELECT * FROM event_dates WHERE eventTypeId = :eventTypeId ORDER BY date DESC LIMIT 1")
    fun getLatestEventDateByType(eventTypeId: Int): LiveData<EventDateEntity?>

    @Query("DELETE FROM event_dates WHERE eventTypeId = :eventTypeId")
    suspend fun deleteEventDatesByType(eventTypeId: Int)

    @Query("SELECT * FROM event_dates WHERE eventTypeId = :eventTypeId ORDER BY date DESC")
    suspend fun getEventDatesByTypeSync(eventTypeId: Int): List<EventDateEntity>
}