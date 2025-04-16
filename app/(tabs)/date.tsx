import { StyleSheet, TouchableOpacity, Alert } from 'react-native';
import React, { useState, useEffect } from 'react';
import { Swipeable } from 'react-native-gesture-handler';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  runOnJS,
  FadeIn,
  FadeOut,
} from 'react-native-reanimated';

import { ThemedText } from '@/components/ThemedText';
import { ThemedView } from '@/components/ThemedView';
import ParallaxScrollView from '@/components/ParallaxScrollView';
import { IconSymbol } from '@/components/ui/IconSymbol';
// 注意：需要确保 IconSymbol 组件映射表中包含 'add' 图标
import { Colors } from '@/constants/Colors';
import { useColorScheme } from '@/hooks/useColorScheme';

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
  
  // 加载模拟数据
  useEffect(() => {
    // 模拟从数据库加载数据
    const loadData = () => {
      const today = new Date();
      const mockData: DateRecord[] = [
        {
          id: '1',
          date: today,
          daysSinceLastRecord: 0 // 修正为0天，因为是同一天的记录
        },
        {
          id: '2',
          date: new Date(today.getTime() - 5 * 24 * 60 * 60 * 1000), // 5天前
          daysSinceLastRecord: 10
        },
        {
          id: '3',
          date: new Date(today.getTime() - 15 * 24 * 60 * 60 * 1000), // 15天前
          daysSinceLastRecord: null // 首次记录
        }
      ];
      
      // 按日期降序排序（最近的日期在前）
      mockData.sort((a, b) => b.date.getTime() - a.date.getTime());
      
      setDateRecords(mockData);
    };
    
    loadData();
  }, []);
  
  // 格式化日期为字符串
  const formatDate = (date: Date): string => {
    return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
  };
  
  // 显示日期选择器
  const showDatePicker = () => {
    // 显示自定义日期选择模态框
    setDatePickerVisible(true);
  };
  
  // 自定义日期选择方法
  const handleDateSelect = (date: Date) => {
    setDatePickerVisible(false);
    addNewRecord(date);
  };
  
  // 关闭日期选择器
  const closeDatePicker = () => {
    setDatePickerVisible(false);
  };
  
  // 处理添加新日期记录
  const addNewRecord = (selectedDate: Date) => {
    let daysSinceLastRecord = 0;
    
    // 如果已有记录，计算与最近记录的天数差
    if (dateRecords.length > 0) {
      const lastDate = dateRecords[0].date; // 最近的记录（已按日期降序排序）
      
      // 计算天数差（向上取整）
      const diffTime = Math.abs(selectedDate.getTime() - lastDate.getTime());
      
      // 使用向下取整并检查是否为同一天
      if (
        selectedDate.getFullYear() === lastDate.getFullYear() &&
        selectedDate.getMonth() === lastDate.getMonth() &&
        selectedDate.getDate() === lastDate.getDate()
      ) {
        daysSinceLastRecord = 0; // 如果是同一天，则设为0天
      } else {
        // 否则计算天数差（按照日期计算，不考虑时分秒）
        const oneDayMs = 24 * 60 * 60 * 1000;
        daysSinceLastRecord = Math.floor(diffTime / oneDayMs);
      }
    }
    
    // 创建新记录
    const newRecord: DateRecord = {
      id: Date.now().toString(), // 使用当前时间戳作为ID
      date: selectedDate,
      daysSinceLastRecord
    };
    
    // 更新状态，将新记录添加到数组开头
    setDateRecords([newRecord, ...dateRecords]);
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
            
            // 更新第一条记录的daysSinceLastRecord（如果存在）
            if (updatedRecords.length > 0 && updatedRecords.length < dateRecords.length) {
              // 如果删除的是第一条记录，需要更新新的第一条记录
              const deletedRecord = dateRecords.find(record => record.id === id);
              const deletedIndex = dateRecords.findIndex(record => record.id === id);
              
              // 如果删除的是第一条记录，且还有其他记录
              if (deletedIndex === 0 && updatedRecords.length > 0) {
                const nextRecord = updatedRecords[0];
                const nextNextRecord = updatedRecords[1]; // 可能不存在
                
                if (nextNextRecord) {
                  // 检查是否是同一天
                  if (
                    nextRecord.date.getFullYear() === nextNextRecord.date.getFullYear() &&
                    nextRecord.date.getMonth() === nextNextRecord.date.getMonth() &&
                    nextRecord.date.getDate() === nextNextRecord.date.getDate()
                  ) {
                    updatedRecords[0] = {
                      ...nextRecord,
                      daysSinceLastRecord: 0 // 同一天设置为0
                    };
                  } else {
                    // 否则计算天数差
                    const oneDayMs = 24 * 60 * 60 * 1000;
                    const diffTime = Math.abs(nextRecord.date.getTime() - nextNextRecord.date.getTime());
                    const diffDays = Math.floor(diffTime / oneDayMs);
                    
                    updatedRecords[0] = {
                      ...nextRecord,
                      daysSinceLastRecord: diffDays
                    };
                  }
                } else {
                  // 如果只剩一条记录，将其设为首次记录
                  updatedRecords[0] = {
                    ...nextRecord,
                    daysSinceLastRecord: null
                  };
                }
              }
            }
            
            setDateRecords(updatedRecords);
          }
        }
      ]
    );
  };
  
  // 渲染列表项右侧的删除按钮
  const renderRightActions = (id: string, progress: any) => {
    // 使用列表项的高度来动态计算删除按钮高度
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
        headerBackgroundColor={{ light: '#FFF5E6', dark: '#352E21' }}
        headerImage={
          <IconSymbol
            size={140}
            color={iconColor}
            name="calendar"
            style={styles.headerImage}
          />
        }>
        <ThemedView style={styles.titleContainer}>
          <ThemedText type="title">记录日期</ThemedText>
        </ThemedView>
        
        <ThemedView style={styles.dateContainer}>
          <ThemedText type="subtitle">所有记录</ThemedText>
          
          {dateRecords.length === 0 ? (
            <ThemedView style={styles.emptyContainer}>
              <ThemedText>暂无记录，点击右下角按钮添加</ThemedText>
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
                          : record.daysSinceLastRecord === 0
                            ? '今日已有记录'
                            : `距上次记录: ${record.daysSinceLastRecord}天`}
                      </ThemedText>
                    </ThemedView>
                  </ThemedView>
                </Swipeable>
              </Animated.View>
            ))
          )}
        </ThemedView>
        
        {/* 日期选择面板 - 当datePickerVisible为true时显示 */}
        {datePickerVisible && (
          <ThemedView style={styles.datePickerOverlay}>
            <ThemedView style={styles.datePickerContainer}>
              <ThemedText type="subtitle" style={styles.datePickerTitle}>选择日期</ThemedText>
              
              <ThemedView style={styles.calendarButtonsContainer}>
                {/* 今天按钮 */}
                <TouchableOpacity
                  style={[styles.dateButton, { backgroundColor: tintColor }]}
                  onPress={() => handleDateSelect(new Date())}
                >
                  <ThemedText style={styles.dateButtonText}>今天</ThemedText>
                </TouchableOpacity>
                
                {/* 昨天按钮 */}
                <TouchableOpacity
                  style={[styles.dateButton, { backgroundColor: tintColor }]}
                  onPress={() => {
                    const yesterday = new Date();
                    yesterday.setDate(yesterday.getDate() - 1);
                    handleDateSelect(yesterday);
                  }}
                >
                  <ThemedText style={styles.dateButtonText}>昨天</ThemedText>
                </TouchableOpacity>
                
                {/* 前天按钮 */}
                <TouchableOpacity
                  style={[styles.dateButton, { backgroundColor: tintColor }]}
                  onPress={() => {
                    const dayBeforeYesterday = new Date();
                    dayBeforeYesterday.setDate(dayBeforeYesterday.getDate() - 2);
                    handleDateSelect(dayBeforeYesterday);
                  }}
                >
                  <ThemedText style={styles.dateButtonText}>前天</ThemedText>
                </TouchableOpacity>
              </ThemedView>
              
              <ThemedView style={styles.calendarButtonsContainer}>
                {/* 上周按钮 */}
                <TouchableOpacity
                  style={[styles.dateButton, { backgroundColor: tintColor }]}
                  onPress={() => {
                    const lastWeek = new Date();
                    lastWeek.setDate(lastWeek.getDate() - 7);
                    handleDateSelect(lastWeek);
                  }}
                >
                  <ThemedText style={styles.dateButtonText}>一周前</ThemedText>
                </TouchableOpacity>
                
                {/* 上月按钮 */}
                <TouchableOpacity
                  style={[styles.dateButton, { backgroundColor: tintColor }]}
                  onPress={() => {
                    const lastMonth = new Date();
                    lastMonth.setMonth(lastMonth.getMonth() - 1);
                    handleDateSelect(lastMonth);
                  }}
                >
                  <ThemedText style={styles.dateButtonText}>一月前</ThemedText>
                </TouchableOpacity>
              </ThemedView>
              
              <TouchableOpacity
                style={styles.cancelButton}
                onPress={closeDatePicker}
              >
                <ThemedText style={styles.cancelButtonText}>取消</ThemedText>
              </TouchableOpacity>
            </ThemedView>
          </ThemedView>
        )}
      </ParallaxScrollView>
      
      {/* 添加按钮 - 将其放置在视图层次结构的最外层，确保它总是可见 */}
      <Animated.View style={styles.addButtonContainer}>
        <TouchableOpacity
          style={[styles.addButton, { backgroundColor: tintColor }]}
          onPress={showDatePicker}
          activeOpacity={0.7}
        >
          <IconSymbol name="add" size={28} color="#FFFFFF" />
        </TouchableOpacity>
      </Animated.View>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  headerImage: {
    bottom: -50,
    right: -30,
    position: 'absolute',
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
    elevation: 6, // 增加阴影效果
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3,
    shadowRadius: 5,
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
    zIndex: 1000,
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
    elevation: 5,
  },
  datePickerTitle: {
    marginBottom: 20,
    fontSize: 20,
  },
  calendarButtonsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    width: '100%',
    marginBottom: 15,
  },
  dateButton: {
    flex: 1,
    marginHorizontal: 5,
    paddingVertical: 12,
    borderRadius: 8,
    alignItems: 'center',
    justifyContent: 'center',
  },
  dateButtonText: {
    color: '#FFFFFF',
    fontWeight: 'bold',
  },
  cancelButton: {
    marginTop: 10,
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 8,
    backgroundColor: '#F2F2F2',
  },
  cancelButtonText: {
    color: '#333333',
  },
});
