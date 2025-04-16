import { StyleSheet, Platform } from 'react-native';
import React, { useState } from 'react';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  Easing,
} from 'react-native-reanimated';

import { ThemedText } from '@/components/ThemedText';
import { ThemedView } from '@/components/ThemedView';
import ParallaxScrollView from '@/components/ParallaxScrollView';
import { IconSymbol } from '@/components/ui/IconSymbol';
import { Colors } from '@/constants/Colors';
import { useColorScheme } from '@/hooks/useColorScheme';

export default function DateScreen() {
  const colorScheme = useColorScheme() ?? 'light';
  const iconColor = Colors[colorScheme].icon;
  
  const [currentDate] = useState(new Date());
  const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'];
  const months = ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月'];
  
  // 动画控制值
  const rotateValue = useSharedValue(0);
  
  // 按月日历视图的动画样式
  const calendarAnimatedStyle = useAnimatedStyle(() => {
    return {
      transform: [
        {
          rotateY: `${rotateValue.value}deg`
        }
      ],
      opacity: withTiming(rotateValue.value > 90 ? 0 : 1, { duration: 150 })
    };
  });
  
  // 执行翻转动画
  const flipCalendar = () => {
    rotateValue.value = withTiming(180, {
      duration: 800,
      easing: Easing.bezier(0.25, 1, 0.5, 1),
    }, () => {
      rotateValue.value = 0;
    });
  };

  return (
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
        <ThemedText type="title">日期</ThemedText>
      </ThemedView>
      
      <ThemedView style={styles.dateContainer}>
        <ThemedText type="subtitle">今天是</ThemedText>
        <ThemedText type="title">
          {currentDate.getFullYear()}年{currentDate.getMonth() + 1}月{currentDate.getDate()}日
        </ThemedText>
        <ThemedText type="defaultSemiBold">{days[currentDate.getDay()]}</ThemedText>
        
        <Animated.View style={[styles.calendarCard, calendarAnimatedStyle]}>
          <ThemedText type="subtitle" style={styles.monthTitle}>
            {months[currentDate.getMonth()]}
          </ThemedText>
          
          <ThemedView style={styles.calendarGrid}>
            {/* 日历星期表头 */}
            <ThemedView style={styles.weekdayRow}>
              {days.map((day, index) => (
                <ThemedText key={index} style={styles.weekdayText}>
                  {day.substring(0, 1)}
                </ThemedText>
              ))}
            </ThemedView>
            
            {/* 示例日历内容 - 实际应用中应该根据月份生成 */}
            <ThemedView style={styles.calendarDays}>
              {Array(31).fill(0).map((_, i) => (
                <ThemedView
                  key={i}
                  style={[
                    styles.dayCell,
                    currentDate.getDate() === i + 1 ? styles.currentDay : null
                  ]}
                >
                  <ThemedText
                    style={currentDate.getDate() === i + 1 ? styles.currentDayText : null}
                  >
                    {i + 1}
                  </ThemedText>
                </ThemedView>
              ))}
            </ThemedView>
          </ThemedView>
        </Animated.View>
      </ThemedView>
    </ParallaxScrollView>
  );
}

const styles = StyleSheet.create({
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
    alignItems: 'center',
  },
  calendarCard: {
    width: '100%',
    padding: 16,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#DDDDDD',
    marginTop: 20,
    backfaceVisibility: 'hidden',
  },
  monthTitle: {
    textAlign: 'center',
    marginBottom: 15,
  },
  calendarGrid: {
    width: '100%',
  },
  weekdayRow: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginBottom: 10,
  },
  weekdayText: {
    fontWeight: 'bold',
  },
  calendarDays: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  dayCell: {
    width: '14.28%',
    height: 35,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 5,
  },
  currentDay: {
    backgroundColor: Colors.light.tint,
    borderRadius: 20,
  },
  currentDayText: {
    color: 'white',
    fontWeight: 'bold',
  },
});
