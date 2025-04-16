import { StyleSheet, Platform, Dimensions } from 'react-native';
import React, { useEffect } from 'react';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withDelay,
  Easing,
} from 'react-native-reanimated';

import { ThemedText } from '@/components/ThemedText';
import { ThemedView } from '@/components/ThemedView';
import ParallaxScrollView from '@/components/ParallaxScrollView';
import { IconSymbol } from '@/components/ui/IconSymbol';
import { Colors } from '@/constants/Colors';
import { useColorScheme } from '@/hooks/useColorScheme';

const { width } = Dimensions.get('window');

// 模拟图表数据
const chartData = [
  { day: '周一', value: 30 },
  { day: '周二', value: 45 },
  { day: '周三', value: 28 },
  { day: '周四', value: 80 },
  { day: '周五', value: 60 },
  { day: '周六', value: 40 },
  { day: '周日', value: 25 },
];

// 最大值用于计算百分比高度
const maxValue = Math.max(...chartData.map(d => d.value));

export default function AnalyticsScreen() {
  const colorScheme = useColorScheme() ?? 'light';
  const iconColor = Colors[colorScheme].icon;
  const chartColor = Colors[colorScheme].tint;
  
  // 为每个柱状图创建动画值
  const barHeights = chartData.map(() => useSharedValue(0));
  
  useEffect(() => {
    // 按顺序为每个柱状图应用动画
    barHeights.forEach((height, index) => {
      height.value = withDelay(
        index * 100, // 延迟时间增加，创建连续动画效果
        withTiming(chartData[index].value / maxValue, {
          duration: 800,
          easing: Easing.bezier(0.25, 0.1, 0.25, 1),
        })
      );
    });
  }, []);
  
  // 为每个柱状图创建动画样式
  const barStyles = barHeights.map(height =>
    useAnimatedStyle(() => {
      return {
        height: `${height.value * 100}%`,
        opacity: withTiming(height.value > 0 ? 1 : 0),
      };
    })
  );

  return (
    <ParallaxScrollView
      headerBackgroundColor={{ light: '#E6F0FF', dark: '#1A253C' }}
      headerImage={
        <IconSymbol
          size={150}
          color={iconColor}
          name="analytics"
          style={styles.headerImage}
        />
      }>
      <ThemedView style={styles.titleContainer}>
        <ThemedText type="title">数据分析</ThemedText>
      </ThemedView>
      
      <ThemedView style={styles.analyticsContainer}>
        <ThemedText type="subtitle">每周活动统计</ThemedText>
        
        <ThemedView style={styles.chartCard}>
          <ThemedView style={styles.chartContainer}>
            {chartData.map((data, index) => (
              <ThemedView key={index} style={styles.barContainer}>
                <ThemedView style={styles.barWrapper}>
                  <Animated.View
                    style={[
                      styles.bar,
                      barStyles[index],
                      { backgroundColor: chartColor }
                    ]}
                  />
                </ThemedView>
                <ThemedText style={styles.barLabel}>{data.day}</ThemedText>
              </ThemedView>
            ))}
          </ThemedView>
        </ThemedView>
        
        <ThemedText type="subtitle" style={styles.statsTitle}>关键数据</ThemedText>
        
        <ThemedView style={styles.statsGrid}>
          <ThemedView style={styles.statCard}>
            <IconSymbol name="check" size={24} color={chartColor} />
            <ThemedText type="defaultSemiBold">已完成</ThemedText>
            <ThemedText type="title">45</ThemedText>
          </ThemedView>
          
          <ThemedView style={styles.statCard}>
            <IconSymbol name="timelapse" size={24} color={chartColor} />
            <ThemedText type="defaultSemiBold">进行中</ThemedText>
            <ThemedText type="title">12</ThemedText>
          </ThemedView>
          
          <ThemedView style={styles.statCard}>
            <IconSymbol name="trending_up" size={24} color={chartColor} />
            <ThemedText type="defaultSemiBold">完成率</ThemedText>
            <ThemedText type="title">78%</ThemedText>
          </ThemedView>
          
          <ThemedView style={styles.statCard}>
            <IconSymbol name="flag" size={24} color={chartColor} />
            <ThemedText type="defaultSemiBold">总计</ThemedText>
            <ThemedText type="title">57</ThemedText>
          </ThemedView>
        </ThemedView>
      </ThemedView>
    </ParallaxScrollView>
  );
}

const styles = StyleSheet.create({
  headerImage: {
    bottom: -60,
    right: -40,
    position: 'absolute',
    opacity: 0.6,
  },
  titleContainer: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 20,
  },
  analyticsContainer: {
    gap: 16,
  },
  chartCard: {
    padding: 16,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#DDDDDD',
    marginVertical: 10,
  },
  chartContainer: {
    height: 200,
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-end',
    paddingVertical: 10,
  },
  barContainer: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'flex-end',
    height: '100%',
  },
  barWrapper: {
    width: '60%',
    height: '90%',
    justifyContent: 'flex-end',
  },
  bar: {
    width: '100%',
    borderTopLeftRadius: 4,
    borderTopRightRadius: 4,
  },
  barLabel: {
    marginTop: 8,
    fontSize: 12,
  },
  statsTitle: {
    marginTop: 10,
  },
  statsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    gap: 12,
  },
  statCard: {
    width: '48%',
    padding: 16,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#DDDDDD',
    alignItems: 'center',
    marginBottom: 10,
  },
});
