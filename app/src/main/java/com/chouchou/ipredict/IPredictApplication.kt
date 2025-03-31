package com.chouchou.ipredict

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.viewModelScope
import com.chouchou.ipredict.data.db.AppDatabase
import com.chouchou.ipredict.data.db.EventTypeEntity
import com.chouchou.ipredict.data.repository.EventDateRepository
import com.chouchou.ipredict.data.repository.EventTypeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class IPredictApplication : Application() {

    // 应用级协程作用域
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // 懒加载数据库实例
    private val database by lazy { AppDatabase.getDatabase(this) }

    // 懒加载仓库实例
    val eventDateRepository by lazy { EventDateRepository(database.eventDateDao()) }
    val eventTypeRepository by lazy { EventTypeRepository(database.eventTypeDao()) }

    override fun onCreate() {
        super.onCreate()
        // 初始化默认数据
        initializeDefaultData()
        // 创建通知渠道
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // 仅在Android 8.0及以上版本需要创建通知渠道
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // 注册通知渠道
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "ipredict_reminder_channel"
    }

    private fun initializeDefaultData() {
        applicationScope.launch {
            // 检查是否已经有事件类型
            val existingEventTypes = database.eventTypeDao().getAllEventTypesSync()

            if (existingEventTypes.isEmpty()) {
                // 如果没有事件类型，则创建一个默认的"生理周期"事件类型并设为活动状态
                val defaultEventType = EventTypeEntity(
                    name = "重要事件",
                    isActive = true,
                    color = "#FF4081" // 粉色
                )
                database.eventTypeDao().insertEventType(defaultEventType)
            }
        }
    }
}