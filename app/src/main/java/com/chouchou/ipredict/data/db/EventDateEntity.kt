package com.chouchou.ipredict.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.Date

@Entity(tableName = "event_dates")
@TypeConverters(DateConverter::class)
data class EventDateEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: Date,
    val note: String = "", // 可选的备注字段
    val eventTypeId: Int = 1 // 默认为1，关联到事件类型
)