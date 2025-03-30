package com.chouchou.ipredict.ui.dates

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.chouchou.ipredict.IPredictApplication
import com.chouchou.ipredict.data.EventDate
import com.chouchou.ipredict.data.repository.EventDateRepository
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

    private val TAG = "DatesViewModel"
    private val repository: EventDateRepository = (application as IPredictApplication).repository
    private val context: Context = application.applicationContext

    // 获取所有事件日期
    val eventDates: LiveData<List<EventDate>> = repository.allEventDates

    // 添加新的事件日期
    fun addEventDate(date: Date) {
        viewModelScope.launch {
            repository.insertEventDate(date)
        }
    }

    // 删除事件日期
    fun deleteEventDate(eventDate: EventDate) {
        viewModelScope.launch {
            repository.deleteEventDate(eventDate)
        }
    }

    // 导出数据为CSV文件并返回文件Uri
    suspend fun exportDataToCsv(): ExportResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "开始导出CSV数据")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timestampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "ipredict_export_${timestampFormat.format(Date())}.csv"

        try {
            // 获取当前所有数据
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
                        writer.append("序号,日期,间隔天数\n")

                        // 写入数据行
                        sortedDates.forEachIndexed { index, eventDate ->
                            val dayDiff = if (index > 0) {
                                val diffInMillis = eventDate.date.time - sortedDates[index - 1].date.time
                                (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
                            } else {
                                0
                            }

                            writer.append("${index + 1},${dateFormat.format(eventDate.date)},$dayDiff\n")
                        }
                        writer.flush()
                    }
                }

                Log.d(TAG, "文件写入成功，生成Content URI")

                // 使用FileProvider生成Uri - 确保authorities完全匹配AndroidManifest.xml中的配置
                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider", // 使用应用包名动态构建authorities
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
                                repository.insertEventDate(date)
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

    // 获取所有现有日期记录，用于去重
    private suspend fun getAllExistingDates(): List<EventDate> {
        return try {
            // 尝试使用同步方法获取
            repository.getAllEventDatesSync()
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