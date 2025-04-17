import { LinearGradient } from 'expo-linear-gradient';
import { StyleSheet, TouchableOpacity, Alert, Platform, View } from 'react-native';
import React, { useState, useEffect, useCallback } from 'react';
import { Swipeable } from 'react-native-gesture-handler';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  runOnJS,
  FadeIn,
  FadeOut,
} from 'react-native-reanimated';
import DateTimePicker from '@react-native-community/datetimepicker';
import { SvgXml } from 'react-native-svg';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useFocusEffect } from '@react-navigation/native'; // 导入useFocusEffect

import { ThemedText } from '@/components/ThemedText';
import { ThemedView } from '@/components/ThemedView';
import ParallaxScrollView from '@/components/ParallaxScrollView';
import { IconSymbol } from '@/components/ui/IconSymbol';
import { Colors } from '@/constants/Colors';
import { useColorScheme } from '@/hooks/useColorScheme';

// 本地存储键名
const STORAGE_KEY = 'ipredict_date_records';

// 定义日期记录的接口
interface DateRecord {
  id: string;
  date: Date;
  daysSinceLastRecord: number | null; // null 表示首次记录
}

export default function DateScreen() {
  const colorScheme = useColorScheme() ?? 'light';
  const iconColor = Colors[colorScheme].icon;
  const tintColor = Colors[colorScheme].tint;
  
  // 用于存储日期记录的状态
  const [dateRecords, setDateRecords] = useState<DateRecord[]>([]);
  
  // 日期选择模态框状态
  const [datePickerVisible, setDatePickerVisible] = useState(false);
  const [selectedDate, setSelectedDate] = useState(new Date());
  
  // SVG图标
  const noDataSvg = `<svg viewBox="0 0 500 500" xmlns="http://www.w3.org/2000/svg">
    <!-- 日历背景 -->
    <rect x="100" y="100" width="300" height="300" rx="20" ry="20" fill="#f0f0f0" stroke="#cccccc" stroke-width="4"/>
    
    <!-- 日历顶部 -->
    <rect x="100" y="100" width="300" height="50" rx="20" ry="20" fill="#0a7ea4"/>
    <rect x="100" y="130" width="300" height="20" fill="#0a7ea4"/>
    
    <!-- 日历格子 -->
    <line x1="166" y1="180" x2="166" y2="400" stroke="#dddddd" stroke-width="2"/>
    <line x1="232" y1="180" x2="232" y2="400" stroke="#dddddd" stroke-width="2"/>
    <line x1="298" y1="180" x2="298" y2="400" stroke="#dddddd" stroke-width="2"/>
    <line x1="364" y1="180" x2="364" y2="400" stroke="#dddddd" stroke-width="2"/>
    
    <line x1="100" y1="235" x2="400" y2="235" stroke="#dddddd" stroke-width="2"/>
    <line x1="100" y1="290" x2="400" y2="290" stroke="#dddddd" stroke-width="2"/>
    <line x1="100" y1="345" x2="400" y2="345" stroke="#dddddd" stroke-width="2"/>
    
    <!-- 叹号标志 -->
    <circle cx="250" cy="260" r="50" fill="#ffcc00" opacity="0.8"/>
    <rect x="245" y="230" width="10" height="40" rx="5" ry="5" fill="white"/>
    <circle cx="250" cy="285" r="5" fill="white"/>
    
    <!-- 提示箭头 -->
    <path d="M320 350 L350 380 L380 350" stroke="#0a7ea4" stroke-width="6" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>`;
  
  // 使用 useFocusEffect 替代 useEffect，确保每次页面获得焦点时都重新加载数据
  useFocusEffect(
    useCallback(() => {
      // 当页面获得焦点时调用
      loadRecordsFromStorage();
      
      // 返回的函数在页面失去焦点时调用（清理函数）
      return () => {
        // 这里可以进行一些清理操作（如果需要）
      };
    }, []) // 空依赖数组意味着只有在页面获得/失去焦点时才会重新执行
  );
  
  // 加载本地存储数据
  const loadRecordsFromStorage = async () => {
    try {
      const storedRecords = await AsyncStorage.getItem(STORAGE_KEY);
      
      if (storedRecords) {
        // 解析存储的 JSON 字符串，并转换日期字符串为 Date 对象
        const parsedRecords: DateRecord[] = JSON.parse(storedRecords).map((record: any) => ({
          ...record,
          date: new Date(record.date)
        }));
        
        // 按日期降序排序，直接使用 date 对象的 getTime 方法
        parsedRecords.sort((a, b) => b.date.getTime() - a.date.getTime());
        
        setDateRecords(parsedRecords);
      } else {
        setDateRecords([]);
      }
    } catch (error) {
      console.error('Error loading records from storage:', error);
    }
  };
  
  // 保存记录到本地存储
  const saveRecordsToStorage = async (records: DateRecord[]) => {
    try {
      await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(records));
    } catch (error) {
      console.error('Error saving records to storage:', error);
    }
  };
  
  // 更新所有记录的天数差
  const updateRecordIntervals = (records: DateRecord[]) => {
    if (records.length === 0) return records;
    
    // 按日期降序排序（最近的日期在前）
    const sortedRecords = [...records].sort((a, b) => b.date.getTime() - a.date.getTime());
    
    // 更新每条记录的天数差
    const updatedRecords = sortedRecords.map((record, index) => {
      if (index === sortedRecords.length - 1) {
        // 最后一条记录（最早的记录）应该是首次记录
        return { ...record, daysSinceLastRecord: null };
      } else {
        // 计算与下一条记录的天数差
        const currentDate = new Date(record.date);
        const nextDate = new Date(sortedRecords[index + 1].date);
        
        // 计算天数差（向下取整）
        const diffTime = Math.abs(currentDate.getTime() - nextDate.getTime());
        const diffDays = Math.floor(diffTime / (24 * 60 * 60 * 1000));
        
        return { ...record, daysSinceLastRecord: diffDays };
      }
    });
    
    return updatedRecords;
  };
  
  // 格式化日期为字符串
  const formatDate = (date: Date): string => {
    return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
  };
  
  // 显示日期选择器
  const showDatePicker = () => {
    setSelectedDate(new Date());
    setDatePickerVisible(true);
  };
  
  // 处理日期选择
  const handleDateChange = (event: any, date?: Date) => {
    setDatePickerVisible(Platform.OS === 'ios'); // 在 iOS 上保持打开，Android 上自动关闭
    
    if (date) {
      setSelectedDate(date);
      
      if (Platform.OS === 'android') {
        // 在 Android 上，选择日期后立即添加记录
        addNewRecord(date);
      }
    }
  };
  
  // 确认选择的日期（iOS）
  const confirmIOSDate = () => {
    setDatePickerVisible(false);
    addNewRecord(selectedDate);
  };
  
  // 关闭日期选择器
  const closeDatePicker = () => {
    setDatePickerVisible(false);
  };
  
  // 处理添加新日期记录
  const addNewRecord = (newDate: Date) => {
    // 检查是否已存在相同日期的记录
    const isSameDayExists = dateRecords.some(record => {
      const recordDate = new Date(record.date);
      return (
        recordDate.getFullYear() === newDate.getFullYear() &&
        recordDate.getMonth() === newDate.getMonth() &&
        recordDate.getDate() === newDate.getDate()
      );
    });
    
    // 如果已存在相同日期的记录，直接返回不添加
    if (isSameDayExists) {
      Alert.alert(
        "提示",
        "该日期已有记录，无需重复添加",
        [{ text: "确定", style: "default" }]
      );
      return;
    }
    
    // 创建新记录
    const newRecord: DateRecord = {
      id: Date.now().toString(), // 使用当前时间戳作为ID
      date: newDate,
      daysSinceLastRecord: 0 // 临时值，将在 updateRecordIntervals 中更新
    };
    
    // 更新状态，将新记录添加到数组
    const updatedRecords = updateRecordIntervals([newRecord, ...dateRecords]);
    setDateRecords(updatedRecords);
    
    // 保存到本地存储
    saveRecordsToStorage(updatedRecords);
  };
  
  // 处理删除记录
  const handleDeleteRecord = (id: string) => {
    Alert.alert(
      "确认删除",
      "确定要删除这条记录吗？",
      [
        {
          text: "取消",
          style: "cancel"
        },
        {
          text: "确定",
          onPress: () => {
            // 删除指定的记录
            const updatedRecords = dateRecords.filter(record => record.id !== id);
            
            // 更新所有记录的天数差
            const processedRecords = updateRecordIntervals(updatedRecords);
            setDateRecords(processedRecords);
            
            // 保存到本地存储
            saveRecordsToStorage(processedRecords);
          }
        }
      ]
    );
  };
  
  // 渲染列表项右侧的删除按钮
  const renderRightActions = (id: string, progress: any) => {
    return (
      <TouchableOpacity
        style={[styles.deleteAction]}
        onPress={() => handleDeleteRecord(id)}
      >
        <IconSymbol name="delete" size={24} color="#fff" />
      </TouchableOpacity>
    );
  };

  return (
    <ThemedView style={styles.container}>
          <ParallaxScrollView
            headerHeight={180}
            headerGradient={{
              light: {
                colors: ['#FFA726', '#FFCC80'],
                start: { x: 0, y: 0 },
                end: { x: 1, y: 1 }
              },
              dark: {
                colors: ['#E65100', '#EF6C00'],
                start: { x: 0, y: 0 },
                end: { x: 1, y: 1 }
              }
            }}
            headerImage={
              <View style={styles.headerImageContainer}>
                <IconSymbol
                  size={130}
                  color={iconColor}
                  name="calendar"
                  style={styles.headerImage}
                />
              </View>
            }>
        <ThemedView style={styles.titleContainer}>
          <ThemedText type="title">记录日期</ThemedText>
        </ThemedView>
        
        <ThemedView style={styles.dateContainer}>
          
          {dateRecords.length === 0 ? (
            <ThemedView style={styles.emptyContainer}>
              <SvgXml
                xml={noDataSvg}
                width={200}
                height={200}
                style={styles.emptyImage}
              />
              <ThemedText style={styles.emptyText}>暂无记录，点击右下角按钮添加</ThemedText>
            </ThemedView>
          ) : (
            dateRecords.map((record) => (
              <Animated.View
                key={record.id}
                entering={FadeIn.duration(300)}
                exiting={FadeOut.duration(300)}
              >
                <Swipeable
                  renderRightActions={(progress) => renderRightActions(record.id, progress)}
                >
                  <ThemedView style={styles.dateItem}>
                    <ThemedView>
                      <ThemedText type="defaultSemiBold">{formatDate(record.date)}</ThemedText>
                      <ThemedText style={styles.intervalText}>
                        {record.daysSinceLastRecord === null
                          ? '首次记录'
                          : `距上次记录: ${record.daysSinceLastRecord}天`}
                      </ThemedText>
                    </ThemedView>
                  </ThemedView>
                </Swipeable>
              </Animated.View>
            ))
          )}
        </ThemedView>
      </ParallaxScrollView>
      
      {/* 添加按钮 */}
          <Animated.View style={styles.addButtonContainer}>
            <TouchableOpacity
              style={styles.addButton}
              onPress={showDatePicker}
              activeOpacity={0.7}
            >
              {/* 替换纯色背景为渐变色背景 */}
              <LinearGradient
                colors={colorScheme === 'dark' ? ['#E65100', '#EF6C00'] : ['#FFA726', '#FFCC80']}
                start={{ x: 0, y: 0 }}
                end={{ x: 1, y: 1 }}
                style={styles.buttonGradient}
              >
                <IconSymbol name="add" size={28} color="#FFFFFF" />
              </LinearGradient>
            </TouchableOpacity>
          </Animated.View>

      {/* 日期选择器 - 移到视图层级最外层，确保显示在最上方 */}
          {datePickerVisible && (
            <ThemedView style={styles.datePickerOverlay}>
              <ThemedView style={styles.datePickerContainer}>
                <ThemedText type="subtitle" style={styles.datePickerTitle}>选择日期</ThemedText>
                
                <DateTimePicker
                  value={selectedDate}
                  mode="date"
                  display={Platform.OS === 'ios' ? 'spinner' : 'default'}
                  onChange={handleDateChange}
                  maximumDate={new Date()} // 限制只能选择今天及以前的日期
                  style={styles.datePicker}
                />
                
                {Platform.OS === 'ios' && (
                  <ThemedView style={styles.buttonContainer}>
                    <TouchableOpacity
                      style={[styles.cancelButton]}
                      onPress={closeDatePicker}
                    >
                      <ThemedText style={styles.cancelButtonText}>取消</ThemedText>
                    </TouchableOpacity>
                    
                    <TouchableOpacity
                      style={[styles.confirmButton]}
                      onPress={confirmIOSDate}
                    >
                      {/* 替换纯色为渐变色 */}
                      <LinearGradient
                        colors={colorScheme === 'dark' ? ['#E65100', '#EF6C00'] : ['#FFA726', '#FFCC80']}
                        start={{ x: 0, y: 0 }}
                        end={{ x: 1, y: 1 }}
                        style={StyleSheet.absoluteFill}
                      />
                      <ThemedText style={styles.confirmButtonText}>确定</ThemedText>
                    </TouchableOpacity>
                  </ThemedView>
                )}
              </ThemedView>
            </ThemedView>
      )}
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
    headerImageContainer: {
      position: 'absolute',
      width: '100%',
      height: '100%',
      alignItems: 'center',
      justifyContent: 'center',
    },
    headerImage: {
      opacity: 0.7,
    },
  titleContainer: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 20,
  },
  dateContainer: {
    gap: 16,
    paddingBottom: 80, // 添加底部内边距，确保内容不被浮动按钮遮挡
  },
  emptyContainer: {
    padding: 20,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 0, // 将顶部间距移除
  },
  emptyImage: {
    marginBottom: 16, // 减少与文字的间距
  },
  emptyText: {
    fontSize: 16,
    opacity: 0.7,
    textAlign: 'center',
  },
  dateItem: {
    padding: 16,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#DDDDDD',
    marginBottom: 10,
    marginRight: 10, // 添加右侧外边距，与删除按钮保持间距
  },
  intervalText: {
    marginTop: 4,
    fontSize: 14,
    opacity: 0.6,
  },
  deleteAction: {
    backgroundColor: '#FF3B30',
    justifyContent: 'center',
    alignItems: 'center',
    width: 70, // 调整删除按钮宽度
    borderRadius: 12,
    marginBottom: 10,
    marginLeft: 10, // 添加左侧外边距，与日期项目保持间距
  },
  addButtonContainer: {
    position: 'absolute',
    bottom: 100, // 提高按钮位置，避免被底部导航栏遮挡
    right: 30,
    zIndex: 999, // 确保按钮容器在最上层
  },
    addButton: {
      width: 60,
      height: 60,
      borderRadius: 30,
      justifyContent: 'center',
      alignItems: 'center',
      elevation: 6,
      shadowColor: '#000',
      shadowOffset: { width: 0, height: 4 },
      shadowOpacity: 0.3,
      shadowRadius: 5,
      overflow: 'hidden', // 确保渐变不超出圆形边界
    },
    buttonGradient: {
      width: '100%',
      height: '100%',
      justifyContent: 'center',
      alignItems: 'center',
    },
  datePickerOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 2000, // 提高z-index确保在最上层
  },
  datePickerContainer: {
    width: 300,
    padding: 20,
    borderRadius: 15,
    backgroundColor: '#FFFFFF',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    elevation: 10, // 提高elevation值
    position: 'absolute', // 使用绝对定位
    top: '50%', // 居中显示
    left: '50%',
    transform: [{ translateX: -150 }, { translateY: -150 }], // 水平和垂直居中
  },
  datePickerTitle: {
    marginBottom: 20,
    fontSize: 20,
  },
  datePicker: {
    width: 260,
    height: 200,
  },
  buttonContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    width: '100%',
    marginTop: 20,
  },
  cancelButton: {
    flex: 1,
    marginRight: 10,
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: '#F2F2F2',
  },
    confirmButton: {
      flex: 1,
      marginLeft: 10,
      paddingVertical: 12,
      borderRadius: 8,
      alignItems: 'center',
      justifyContent: 'center',
      overflow: 'hidden', // 确保渐变不超出按钮边界
      position: 'relative', // 确保绝对定位的渐变背景正确显示
    },
  cancelButtonText: {
    color: '#333333',
  },
  confirmButtonText: {
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
});
