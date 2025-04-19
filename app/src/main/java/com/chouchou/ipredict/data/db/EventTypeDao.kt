package com.chouchou.ipredict.data.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface EventTypeDao {
    @Query("SELECT * FROM event_types ORDER BY name ASC")
    fun getAllEventTypes(): LiveData<List<EventTypeEntity>>

    @Query("SELECT * FROM event_types WHERE isActive = 1 LIMIT 1")
    fun getActiveEventType(): LiveData<EventTypeEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventType(eventType: EventTypeEntity): Long

    @Update
    suspend fun updateEventType(eventType: EventTypeEntity)

    @Delete
    suspend fun deleteEventType(eventType: EventTypeEntity)

    @Query("UPDATE event_types SET isActive = 0")
    suspend fun clearActiveEventTypes()

    @Query("UPDATE event_types SET isActive = 1 WHERE id = :id")
    suspend fun setActiveEventType(id: Int)

    @Query("SELECT * FROM event_types ORDER BY name ASC")
    suspend fun getAllEventTypesSync(): List<EventTypeEntity>
}