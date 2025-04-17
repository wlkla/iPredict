import { ScrollView } from 'react-native';
import { StyleSheet, TouchableOpacity, Dimensions, View } from 'react-native';
import React, { useState, useEffect, useCallback } from 'react';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withSequence,
  withRepeat,
  Easing,
  SharedValue,
  interpolateColor,
} from 'react-native-reanimated';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { useFocusEffect } from '@react-navigation/native';
import { LinearGradient } from 'expo-linear-gradient';
import { BlurView } from 'expo-blur';

import { ThemedText } from '@/components/ThemedText';
import { ThemedView } from '@/components/ThemedView';
import { IconSymbol } from '@/components/ui/IconSymbol';
import { Colors } from '@/constants/Colors';
import { useColorScheme } from '@/hooks/useColorScheme';
import ParallaxScrollView from '@/components/ParallaxScrollView';

// 屏幕尺寸
const { width, height } = Dimensions.get('window');

// 本地存储键名（与 date.tsx 一致）
const STORAGE_KEY = 'ipredict_date_records';

// 定义日期记录的接口
interface DateRecord {
  id: string;
  date: Date;
  daysSinceLastRecord: number | null;
}

export default function CountdownScreen() {
  const colorScheme = useColorScheme() ?? 'light';
  const tintColor = Colors[colorScheme].tint;
  const iconColor = Colors[colorScheme].icon;
  
  // 状态管理
  const [dateRecords, setDateRecords] = useState<DateRecord[]>([]);
  const [lastDate, setLastDate] = useState<Date | null>(null);
  const [expectedDate, setExpectedDate] = useState<Date | null>(null);
  const [daysLeft, setDaysLeft] = useState<number>(0);
  const [isOverdue, setIsOverdue] = useState<boolean>(false);
  const [daysPassed, setDaysPassed] = useState<number>(0);
  const [averageInterval, setAverageInterval] = useState<number>(30);
  
  // 动画值
  const progressValue = useSharedValue(0);
  const animatedOpacity = useSharedValue(0);
  const buttonScale = useSharedValue(1);
  const pulseAnimation = useSharedValue(1);
  
  // 脉冲动画效果
  useEffect(() => {
    pulseAnimation.value = withRepeat(
      withSequence(
        withTiming(1.2, { duration: 800 }),
        withTiming(1, { duration: 800 })
      ),
      -1, // 无限重复
      true // 反向
    );
  }, []);

  const pulseAnimatedStyle = useAnimatedStyle(() => {
    return {
      transform: [{ scale: pulseAnimation.value }],
    };
  });
  
  // 加载数据
  useFocusEffect(
    useCallback(() => {
      loadRecordsFromStorage();
      return () => {};
    }, [])
  );
  
  // 从 AsyncStorage 加载数据
  const loadRecordsFromStorage = async () => {
    try {
      const storedRecords = await AsyncStorage.getItem(STORAGE_KEY);
      
      if (storedRecords) {
        // 解析存储的 JSON 字符串，并转换日期字符串为 Date 对象
        const parsedRecords: DateRecord[] = JSON.parse(storedRecords).map((record: any) => ({
          ...record,
          date: new Date(record.date)
        }));
        
        // 按日期降序排序，最近的日期在前
        parsedRecords.sort((a, b) => b.date.getTime() - a.date.getTime());
        
        setDateRecords(parsedRecords);
        calculateCountdown(parsedRecords);
      } else {
        setDateRecords([]);
        resetCountdown();
      }
    } catch (error) {
      console.error('Error loading records from storage:', error);
      resetCountdown();
    }
  };
  
  // 重置倒计时状态
  const resetCountdown = () => {
    setLastDate(null);
    setExpectedDate(null);
    setDaysLeft(0);
    setIsOverdue(false);
    setDaysPassed(0);
    setAverageInterval(30);
  };
  
  // 计算倒计时
  const calculateCountdown = (records: DateRecord[]) => {
    if (records.length === 0) {
      resetCountdown();
      return;
    }
    
    // 最后一次记录
    const lastRecord = records[0];
    const last = new Date(lastRecord.date);
    
    // 计算平均间隔
    let totalIntervals = 0;
    let intervalCount = 0;
    
    records.forEach(record => {
      if (record.daysSinceLastRecord) {
        totalIntervals += record.daysSinceLastRecord;
        intervalCount++;
      }
    });
    
    const avgInterval = intervalCount > 0
      ? Math.round(totalIntervals / intervalCount)
      : (lastRecord.daysSinceLastRecord || 30);
    
    // 预期下一次日期
    const expected = new Date(last);
    expected.setDate(expected.getDate() + avgInterval);
    
    // 计算天数差异
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    const diffTime = expected.getTime() - today.getTime();
    const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
    
    // 是否超时
    const overdue = diffDays < 0;
    
    // 距离上次的天数 - 计算从最后一次记录到今天的天数
    const passedTime = today.getTime() - last.getTime();
    const passed = Math.ceil(passedTime / (1000 * 60 * 60 * 24));
    
    // 更新状态
    setLastDate(last);
    setExpectedDate(expected);
    setDaysLeft(Math.abs(diffDays));
    setIsOverdue(overdue);
    setDaysPassed(passed);  // 这个值是距离上次记录的天数
    setAverageInterval(avgInterval);
    
    // 更新进度动画
    const progress = overdue ? 1 : Math.min(1, passed / avgInterval);
    progressValue.value = withTiming(progress, {
      duration: 1500,
      easing: Easing.bezierFn(0.16, 1, 0.3, 1),
    });
    
    // 淡入动画
    animatedOpacity.value = withTiming(1, { duration: 800 });
  };
  
  // 添加今天的记录
  const addTodayRecord = async () => {
    buttonScale.value = withSequence(
      withTiming(0.9, { duration: 100 }),
      withTiming(1, { duration: 200 })
    );
    
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    
    // 检查今天是否已添加
    const isTodayRecorded = dateRecords.some(record => {
      const recordDate = new Date(record.date);
      return (
        recordDate.getFullYear() === today.getFullYear() &&
        recordDate.getMonth() === today.getMonth() &&
        recordDate.getDate() === today.getDate()
      );
    });
    
    if (isTodayRecorded) {
      // 如果今天已添加，则不做任何操作
      return;
    }
    
    // 创建新记录
    const newRecord: DateRecord = {
      id: Date.now().toString(),
      date: today,
      daysSinceLastRecord: dateRecords.length > 0
        ? Math.round((today.getTime() - new Date(dateRecords[0].date).getTime()) / (1000 * 60 * 60 * 24))
        : null
    };
    
    const updatedRecords = [newRecord, ...dateRecords];
    
    try {
      await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(updatedRecords));
      setDateRecords(updatedRecords);
      calculateCountdown(updatedRecords);
    } catch (error) {
      console.error('Error saving record:', error);
    }
  };
  
  // 动画样式
  const containerAnimatedStyle = useAnimatedStyle(() => {
    return {
      opacity: animatedOpacity.value,
      transform: [{ translateY: (1 - animatedOpacity.value) * 20 }],
    };
  });
  
  const progressRingStyle = useAnimatedStyle(() => {
    return {
      transform: [
        { rotateZ: `${progressValue.value * 360}deg` },
      ],
    };
  });
  
  const buttonAnimatedStyle = useAnimatedStyle(() => {
    return {
      transform: [{ scale: buttonScale.value }],
    };
  });
  
  // 格式化日期
  const formatDate = (date: Date | null): string => {
    if (!date) return "-";
    return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`;
  };
  
  // 渲染进度圆环
  const ProgressCircle = ({ progress, isOverdue }: {
    progress: SharedValue<number>,
    isOverdue: boolean
  }) => {
    const circleSize = width * 0.6;
    const strokeWidth = circleSize * 0.03;
    const innerCircleSize = circleSize - strokeWidth * 2;
    
    // 渐变颜色
    const gradientColors = isOverdue
      ? ['#FF5F6D', '#FF9966']
      : ['#00C9FF', '#92FE9D'];
    
    // 动画样式
    const fillAnimatedStyle = useAnimatedStyle(() => {
      const circleColor = interpolateColor(
        progress.value,
        [0, 0.5, 1],
        ['rgba(255, 255, 255, 0.1)', 'rgba(255, 255, 255, 0.15)', 'rgba(255, 255, 255, 0.2)']
      );
      
      return {
        backgroundColor: circleColor,
      };
    });
    
    return (
      <View style={[styles.circleContainer, { width: circleSize, height: circleSize }]}>
        {/* 背景圆 */}
        <Animated.View style={[
          styles.circleBackground,
          { width: innerCircleSize, height: innerCircleSize, borderRadius: innerCircleSize / 2 },
          fillAnimatedStyle
        ]} />
        
        {/* 进度条 */}
        <View style={[styles.progressMask, { width: circleSize, height: circleSize }]}>
          <Animated.View style={[
            styles.progressIndicator,
            { width: circleSize, height: circleSize },
            progressRingStyle
          ]}>
            <LinearGradient
              colors={gradientColors}
              style={[
                styles.progressGradient,
                {
                  width: strokeWidth,
                  height: circleSize / 2,
                  borderRadius: strokeWidth / 2,
                  top: 0,
                  right: circleSize / 2 - strokeWidth / 2,
                }
              ]}
            />
          </Animated.View>
        </View>
        
        {/* 数字显示 - 根据是否超时显示不同的数字 */}
        <View style={styles.daysTextContainer}>
          <ThemedText style={[styles.daysNumber, { lineHeight: 60 }]}>
            {isOverdue ? daysPassed : daysLeft}
          </ThemedText>
          <ThemedText style={styles.daysUnit}>天
          </ThemedText>
        </View>
      </View>
    );
  };
  
  // 渲染信息卡片
  const InfoCard = ({ title, value }: { title: string, value: string }) => (
    <View style={styles.infoCard}>
      <BlurView intensity={25} tint={colorScheme} style={styles.cardBlur}>
        <ThemedText style={styles.cardTitle}>{title}</ThemedText>
        <ThemedText style={styles.cardValue}>{value}</ThemedText>
      </BlurView>
    </View>
  );
  
  return (
    <ParallaxScrollView
      headerBackgroundColor={{ light: '#F0F8FF', dark: '#1A2C38' }}
      headerImage={
        <Animated.View style={[styles.iconContainer, pulseAnimatedStyle]}>
          <IconSymbol
            size={140}
            color={iconColor}
            name="timer"
            style={styles.headerImage}
          />
        </Animated.View>
      }>
      <ThemedView style={styles.titleContainer}>
        <ThemedText type="title">倒计时</ThemedText>
      </ThemedView>

      {/* 主内容 */}
      <Animated.View style={[styles.content, containerAnimatedStyle]}>
        {/* 状态文本 */}
        <ThemedText style={styles.statusText}>
          {dateRecords.length === 0
            ? '开始记录您的重要日期'
            : (isOverdue ? '已超出预期时间' : '距离预期时间还有')}
        </ThemedText>
        
        {/* 进度圆环或空状态 */}
        {dateRecords.length === 0 ? (
          <View style={styles.emptyState}>
            <IconSymbol name="calendar" size={80} color={tintColor} />
            <ThemedText style={styles.emptyText}>
              点击下方按钮添加您的第一条记录
            </ThemedText>
          </View>
        ) : (
          <ProgressCircle
            progress={progressValue}
            isOverdue={isOverdue}
          />
        )}
        
        {/* 信息卡片 */}
        {dateRecords.length > 0 && (
          <View style={styles.infoGrid}>
            <InfoCard
              title="上次记录"
              value={formatDate(lastDate)}
            />
            <InfoCard
              title="预期下次"
              value={formatDate(expectedDate)}
            />
            <InfoCard
              title="已经过去"
              value={`${daysPassed}天`}
            />
            <InfoCard
              title="平均间隔"
              value={`${averageInterval}天`}
            />
          </View>
        )}
        
        {/* 添加按钮 */}
        <Animated.View style={[styles.buttonContainer, buttonAnimatedStyle]}>
          <TouchableOpacity
            style={styles.addButton}
            activeOpacity={0.8}
            onPress={addTodayRecord}
          >
            <LinearGradient
              colors={isOverdue ? ['#FF5F6D', '#FF9966'] : ['#00C9FF', '#92FE9D']}
              start={{ x: 0, y: 0 }}
              end={{ x: 1, y: 0 }}
              style={styles.buttonGradient}
            >
              <ThemedText style={styles.buttonText}>
                {dateRecords.length === 0 ? '开始记录' : '添加今天'}
              </ThemedText>
            </LinearGradient>
          </TouchableOpacity>
        </Animated.View>
      </Animated.View>
    </ParallaxScrollView>
  );
}

const styles = StyleSheet.create({
  iconContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
    height: '100%',
  },
  headerImage: {
    opacity: 0.8,
  },
  container: {
    flex: 1,
  },
  header: {
    height: 60,
    justifyContent: 'center',
    alignItems: 'center',
    borderBottomWidth: 1,
    borderBottomColor: 'rgba(0, 0, 0, 0.1)',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
  },
  content: {
    flex: 1,
    paddingHorizontal: 20,
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingTop: 0,
    paddingBottom: 30,
  },
  statusText: {
    fontSize: 20,
    fontWeight: '500',
    textAlign: 'center',
  },
  emptyState: {
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: 40,
  },
  scrollContainer: {
    flexGrow: 1,
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 20,
    paddingTop: 40,
    paddingBottom: 30,
  },
  emptyText: {
    fontSize: 16,
    textAlign: 'center',
    marginTop: 20,
    opacity: 0.7,
    maxWidth: 250,
  },
  titleContainer: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 20,
  },
  countdownContainer: {
    gap: 16,
  },
  eventCard: {
    padding: 16,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#DDDDDD',
    marginBottom: 10,
  },
  circleContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    marginVertical: 20,
  },
  circleBackground: {
    position: 'absolute',
    borderWidth: 1,
    borderColor: 'rgba(0, 0, 0, 0.05)',
  },
  progressMask: {
    position: 'absolute',
    overflow: 'hidden',
  },
  progressIndicator: {
    position: 'absolute',
  },
  progressGradient: {
    position: 'absolute',
  },
  daysTextContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'row',
    position: 'relative',
    zIndex: 10,
  },
  daysNumber: {
    fontSize: 50,
    fontWeight: 'bold',
    textAlign: 'center',
    includeFontPadding: false, // 解决数字显示不完整问题
    padding: 0,
    margin: 0,
    height: 60, // 确保文字高度固定
  },
  daysUnit: {
    fontSize: 20,
    fontWeight: '500',
    alignSelf: 'flex-end',
    marginBottom: 10,
  },
  infoGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    width: '100%',
  },
  infoCard: {
    width: '49%',
    height: 90,
    marginBottom: 15,
    borderRadius: 16,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: 'rgba(0, 0, 0, 0.1)',
  },
  cardBlur: {
    flex: 1,
    padding: 15,
    justifyContent: 'space-between',
  },
  cardTitle: {
    fontSize: 14,
    opacity: 0.7,
  },
  cardValue: {
    fontSize: 15,
    fontWeight: '600',
  },
  buttonContainer: {
    marginTop: 20,
    width: '100%',
    paddingBottom: 30,
  },
  addButton: {
    height: 56,
    borderRadius: 28,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.2,
    shadowRadius: 8,
    elevation: 4,
  },
  buttonGradient: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: 'bold',
  },
});
