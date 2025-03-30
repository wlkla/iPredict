package com.chouchou.ipredict

import android.app.Application
import com.chouchou.ipredict.data.db.AppDatabase
import com.chouchou.ipredict.data.repository.EventDateRepository

class IPredictApplication : Application() {

    // 懒加载数据库实例
    private val database by lazy { AppDatabase.getDatabase(this) }

    // 懒加载仓库实例
    val repository by lazy { EventDateRepository(database.eventDateDao()) }
}