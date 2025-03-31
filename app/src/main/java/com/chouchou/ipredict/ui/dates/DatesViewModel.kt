package com.chouchou.ipredict.ui.dates

import com.chouchou.ipredict.data.db.EventTypeEntity
import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.chouchou.ipredict.IPredictApplication
import com.chouchou.ipredict.data.EventDate
import com.chouchou.ipredict.data.EventType
import com.chouchou.ipredict.data.repository.EventDateRepository
import com.chouchou.ipredict.data.repository.EventTypeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DatesViewModel(application: Application) : AndroidViewModel(application) {
    // 预定义颜色列表
    private val colorOptions = listOf(
        "#000000", // 黑色
        "#FF5252", // 红色
        "#FF4081", // 粉色
        "#E040FB", // 紫色
        "#7C4DFF", // 深紫色
        "#536DFE", // 靛蓝
        "#448AFF", // 蓝色
        "#40C4FF", // 浅蓝
        "#18FFFF", // 青色
        "#64FFDA", // 蓝绿色
        "#69F0AE", // 绿色
        "#B2FF59", // 浅绿色
        "#EEFF41", // 酸橙色
        "#FFFF00", // 黄色
        "#FFD740", // 琥珀色
        "#FFAB40", // 橙色
        "#FF6E40"  // 深橙色
    )

    private val TAG = "DatesViewModel"
    private val eventDateRepository: EventDateRepository = (application as IPredictApplication).eventDateRepository
    private val eventTypeRepository: EventTypeRepository = (application as IPredictApplication).eventTypeRepository
    private val context: Context = application.applicationContext

    // 当前活动的事件类型
    val activeEventType: LiveData<EventType?> = eventTypeRepository.activeEventType

    // 当前活动事件类型的所有日期记录
    val eventDates: LiveData<List<EventDate>> = activeEventType.switchMap { eventType ->
        eventType?.let {
            eventDateRepository.getEventDatesByType(it.id)
        } ?: eventDateRepository.allEventDates
    }

    // 添加新的事件日期
    fun addEventDate(date: Date) {
        val eventTypeId = activeEventType.value?.id ?: 1
        viewModelScope.launch {
            eventDateRepository.insertEventDate(date, eventTypeId)
        }
    }

    // 删除事件日期
    fun deleteEventDate(eventDate: EventDate) {
        viewModelScope.launch {
            eventDateRepository.deleteEventDate(eventDate)
        }
    }

    // 导出数据为CSV文件并返回文件Uri
    suspend fun exportDataToCsv(): ExportResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "开始导出CSV数据")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "ipredict_export_${timestampFormat.format(Date())}.csv"

        try {
            // 获取当前活动事件类型ID
            val eventTypeId = activeEventType.value?.id ?: 1

            // 获取当前活动事件类型的数据
            val allDates = eventDates.value
            Log.d(TAG, "获取到数据：${allDates?.size ?: 0}条记录")

            if (allDates.isNullOrEmpty()) {
                Log.w(TAG, "没有数据可导出")
                return@withContext ExportResult(false, "没有数据可导出")
            }

            // 按日期排序（从早到晚）
            val sortedDates = allDates.sortedBy { it.date }
            Log.d(TAG, "数据已排序，准备写入文件")

            // 创建文件 - 使用filesDir而不是cacheDir
            val file = File(context.filesDir, fileName)
            Log.d(TAG, "创建文件：${file.absolutePath}")

            try {
                FileOutputStream(file).use { fos ->
                    OutputStreamWriter(fos).use { writer ->
                        // 写入CSV头
                        writer.append("序号,日期,间隔天数,事件类型ID\n")

                        // 写入数据行
                        sortedDates.forEachIndexed { index, eventDate ->
                            val dayDiff = if (index > 0) {
                                val diffInMillis = eventDate.date.time - sortedDates[index - 1].date.time
                                (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
                            } else {
                                0
                            }

                            writer.append("${index + 1},${dateFormat.format(eventDate.date)},$dayDiff,$eventTypeId\n")
                        }
                        writer.flush()
                    }
                }

                Log.d(TAG, "文件写入成功，生成Content URI")

                // 使用FileProvider生成Uri
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                Log.d(TAG, "生成的Content URI: $contentUri")
                return@withContext ExportResult(true, null, contentUri)

            } catch (e: Exception) {
                Log.e(TAG, "文件写入或URI生成失败", e)
                return@withContext ExportResult(false, "文件创建失败: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "导出过程中发生错误", e)
            return@withContext ExportResult(false, "导出失败: ${e.message}")
        }
    }

    // 导出结果数据类
    data class ExportResult(
        val success: Boolean,
        val errorMessage: String? = null,
        val fileUri: Uri? = null
    )

    // 导入CSV数据，添加去重功能
    suspend fun importDataFromCsv(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            // 获取当前活动事件类型ID
            val currentEventTypeId = activeEventType.value?.id ?: 1

            // 首先获取已有的所有日期，用于去重
            val existingDates = getAllExistingDates()
            Log.d(TAG, "已有日期记录: ${existingDates.size}条")

            // 创建一个HashSet用于快速查询日期是否存在
            val existingDateSet = existingDates.map {
                dateFormat.format(it.date)
            }.toHashSet()

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                var isFirstLine = true
                var importedCount = 0
                var duplicateCount = 0
                var errorCount = 0

                while (reader.readLine().also { line = it } != null) {
                    if (isFirstLine) {
                        isFirstLine = false
                        continue // 跳过表头
                    }

                    val parts = line?.split(",") ?: continue
                    if (parts.size >= 2) {
                        try {
                            val dateStr = parts[1].trim()

                            // 检查日期是否已存在
                            if (existingDateSet.contains(dateStr)) {
                                Log.d(TAG, "日期已存在: $dateStr")
                                duplicateCount++
                                continue // 跳过重复日期
                            }

                            val date = dateFormat.parse(dateStr)
                            if (date != null) {
                                // 获取事件类型ID（如果CSV中有提供）
                                val eventTypeId = if (parts.size >= 4) {
                                    try {
                                        parts[3].trim().toInt()
                                    } catch (e: NumberFormatException) {
                                        currentEventTypeId
                                    }
                                } else {
                                    currentEventTypeId
                                }

                                // 添加导入的日期到指定事件类型
                                eventDateRepository.insertEventDate(date, eventTypeId)
                                importedCount++

                                // 将新导入的日期添加到去重集合中
                                existingDateSet.add(dateStr)
                                Log.d(TAG, "成功导入日期: $dateStr")
                            } else {
                                errorCount++
                                Log.d(TAG, "日期解析失败: $dateStr")
                            }
                        } catch (e: ParseException) {
                            errorCount++
                            Log.e(TAG, "日期格式错误: ${parts[1]}", e)
                        }
                    }
                }

                ImportResult(importedCount, errorCount, duplicateCount = duplicateCount)
            } ?: ImportResult(0, 0, errorMessage = "无法读取文件")
        } catch (e: Exception) {
            Log.e(TAG, "导入过程中发生错误", e)
            ImportResult(0, 0, errorMessage = e.message ?: "导入过程中发生错误")
        }
    }

    suspend fun exportAllEventTypesData(): ExportResult = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "ipredict_all_data_${timestampFormat.format(Date())}.csv"

        try {
            // 获取所有事件类型
            val eventTypes = (getApplication() as IPredictApplication).eventTypeRepository.allEventTypes.value ?: emptyList()

            if (eventTypes.isEmpty()) {
                return@withContext ExportResult(false, "没有事件类型可导出")
            }

            // 创建文件
            val file = File(context.filesDir, fileName)

            try {
                FileOutputStream(file).use { fos ->
                    OutputStreamWriter(fos).use { writer ->
                        // 写入CSV头
                        writer.append("事件类型ID,事件类型名称,日期,间隔天数\n")

                        // 遍历每个事件类型
                        for (eventType in eventTypes) {
                            // 获取该事件类型的所有日期
                            val dates = (getApplication() as IPredictApplication).eventDateRepository
                                .getEventDatesByTypeSync(eventType.id)
                                .sortedBy { it.date }

                            // 写入该事件类型的数据
                            dates.forEachIndexed { index, eventDate ->
                                val dayDiff = if (index > 0) {
                                    val diffInMillis = eventDate.date.time - dates[index - 1].date.time
                                    (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
                                } else {
                                    0
                                }

                                writer.append("${eventType.id},${eventType.name},${dateFormat.format(eventDate.date)},$dayDiff\n")
                            }
                        }
                        writer.flush()
                    }
                }

                // 生成Content URI
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                return@withContext ExportResult(true, null, contentUri)
            } catch (e: Exception) {
                return@withContext ExportResult(false, "文件创建失败: ${e.message}")
            }
        } catch (e: Exception) {
            return@withContext ExportResult(false, "导出失败: ${e.message}")
        }
    }

    suspend fun importAllEventTypesData(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val eventTypeRepository = (getApplication() as IPredictApplication).eventTypeRepository

            // 首先获取已有的所有日期，用于去重
            val existingDates = getAllExistingDates()
            val existingDateMap = mutableMapOf<Int, MutableSet<String>>()

            // 按事件类型ID进行分组
            for (date in existingDates) {
                val eventTypeId = 1 // 默认事件类型ID，需要从数据库获取
                val dateStr = dateFormat.format(date.date)

                existingDateMap.getOrPut(eventTypeId) { mutableSetOf() }.add(dateStr)
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                var line: String?
                var isFirstLine = true
                var importedCount = 0
                var duplicateCount = 0
                var errorCount = 0
                var newEventTypesCount = 0

                // 获取现有事件类型
                val existingEventTypes = eventTypeRepository.getAllEventTypesSync()

                // 创建事件类型名称到ID的映射，用于快速查找
                val eventTypeNameToIdMap = existingEventTypes.associateBy { it.name }

                // 创建一个缓存来存储导入过程中创建的新事件类型
                val newEventTypeMap = mutableMapOf<String, Int>()

                while (reader.readLine().also { line = it } != null) {
                    if (isFirstLine) {
                        isFirstLine = false
                        continue // 跳过表头
                    }

                    val parts = line?.split(",") ?: continue
                    if (parts.size >= 3) {
                        try {
                            // 格式: 事件类型ID,事件类型名称,日期,间隔天数
                            val eventTypeName = parts[1].trim()
                            val dateStr = parts[2].trim()

                            // 首先查找现有的事件类型映射
                            var eventTypeId = eventTypeNameToIdMap[eventTypeName]?.id

                            // 然后查找导入过程中创建的事件类型
                            if (eventTypeId == null) {
                                eventTypeId = newEventTypeMap[eventTypeName]
                            }

                            // 如果仍未找到，创建新事件类型
                            if (eventTypeId == null) {
                                // 创建新事件类型
                                val newEventType = EventTypeEntity(
                                    name = eventTypeName,
                                    isActive = false,
                                    color = colorOptions.random() // 随机选择一个颜色
                                )
                                val insertedId = eventTypeRepository.insertEventType(newEventType).toInt()
                                eventTypeId = insertedId
                                newEventTypesCount++

                                // 将新事件类型添加到缓存
                                newEventTypeMap[eventTypeName] = insertedId

                                // 更新重复检查映射
                                existingDateMap[eventTypeId] = mutableSetOf()
                            }

                            // 检查日期是否已存在于该事件类型中
                            val eventDates = existingDateMap.getOrPut(eventTypeId) { mutableSetOf() }
                            if (eventDates.contains(dateStr)) {
                                duplicateCount++
                                continue
                            }

                            val date = dateFormat.parse(dateStr)
                            if (date != null) {
                                // 添加日期记录
                                eventDateRepository.insertEventDate(date, eventTypeId)
                                importedCount++

                                // 更新重复检查映射
                                eventDates.add(dateStr)
                            } else {
                                errorCount++
                            }
                        } catch (e: Exception) {
                            errorCount++
                            Log.e(TAG, "导入行错误: ${line}", e)
                        }
                    }
                }

                ImportResult(
                    importedCount,
                    errorCount,
                    duplicateCount,
                    "成功导入 $importedCount 条记录，新增 $newEventTypesCount 个事件类型"
                )
            } ?: ImportResult(0, 0, errorMessage = "无法读取文件")
        } catch (e: Exception) {
            Log.e(TAG, "导入过程中发生错误", e)
            ImportResult(0, 0, errorMessage = e.message ?: "导入过程中发生错误")
        }
    }

    // 获取所有现有日期记录，用于去重
    private suspend fun getAllExistingDates(): List<EventDate> {
        return try {
            // 尝试使用同步方法获取
            eventDateRepository.getAllEventDatesSync()
        } catch (e: Exception) {
            // 如果没有同步方法，则使用LiveData的当前值
            eventDates.value ?: emptyList()
        }
    }

    // 更新 ImportResult 数据类，添加重复记录数
    data class ImportResult(
        val successCount: Int = 0,
        val errorCount: Int = 0,
        val duplicateCount: Int = 0,
        val errorMessage: String? = null
    )
}