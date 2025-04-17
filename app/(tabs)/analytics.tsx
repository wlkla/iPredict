import { StyleSheet, Platform, Dimensions, TouchableOpacity, View } from 'react-native';
import React, { useEffect, useState, useCallback } from 'react';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withDelay,
  Easing,
} from 'react-native-reanimated';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Svg, Circle, Path, G, Text as SvgText, Line } from 'react-native-svg';
import { useFocusEffect } from '@react-navigation/native';

import { ThemedText } from '@/components/ThemedText';
import { ThemedView } from '@/components/ThemedView';
import ParallaxScrollView from '@/components/ParallaxScrollView';
import { IconSymbol } from '@/components/ui/IconSymbol';
import { Colors } from '@/constants/Colors';
import { useColorScheme } from '@/hooks/useColorScheme';

const { width } = Dimensions.get('window');

// 本地存储键名（与 date.tsx 一致）
const STORAGE_KEY = 'ipredict_date_records';

// 定义日期记录的接口
interface DateRecord {
  id: string;
  date: Date;
  daysSinceLastRecord: number | null; // null 表示首次记录
}

// 图表类型
type ChartType = 'line' | 'bar' | 'pie';

// 频率数据结构
interface FrequencyData {
  interval: number; // 间隔天数
  count: number;    // 出现次数
}

export default function AnalyticsScreen() {
  const colorScheme = useColorScheme() ?? 'light';
  const iconColor = Colors[colorScheme].icon;
  const chartColor = Colors[colorScheme].tint;
  
  // 状态管理
  const [dateRecords, setDateRecords] = useState<DateRecord[]>([]);
  const [chartType, setChartType] = useState<ChartType>('line'); // 默认显示柱状图
  const [intervalData, setIntervalData] = useState<number[]>([]);
  const [intervalFrequencyData, setIntervalFrequencyData] = useState<FrequencyData[]>([]);
  const [hasData, setHasData] = useState<boolean>(false);
  
  // 使用 useFocusEffect 来确保每次页面获取焦点时重新加载数据
  useFocusEffect(
    useCallback(() => {
      loadRecordsFromStorage();
      return () => {};
    }, [])
  );
  
  // 数据处理
  useEffect(() => {
    if (dateRecords.length > 0) {
      // 计算时间间隔数据
      const intervals = dateRecords
        .filter(record => record.daysSinceLastRecord !== null)
        .map(record => record.daysSinceLastRecord as number);
      
      setIntervalData(intervals);
      
      // 计算间隔频率数据
      const frequencyMap = new Map<number, number>();
      
      intervals.forEach(interval => {
        if (frequencyMap.has(interval)) {
          frequencyMap.set(interval, frequencyMap.get(interval)! + 1);
        } else {
          frequencyMap.set(interval, 1);
        }
      });
      
      // 转换为数组并按间隔天数排序
      const frequencyArray = Array.from(frequencyMap, ([interval, count]) => ({ interval, count }));
      frequencyArray.sort((a, b) => a.interval - b.interval);
      
      setIntervalFrequencyData(frequencyArray);
      setHasData(intervals.length > 0);
    } else {
      setHasData(false);
    }
  }, [dateRecords]);
  
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
  
    const LineChart = () => {
      if (intervalData.length === 0) return null;

      const chartWidth = width - 64; // 减去内边距
      const chartHeight = 200;
      const padding = 30;
      const graphWidth = chartWidth - padding * 2;
      const graphHeight = chartHeight - padding * 2;

      const maxValue = Math.max(...intervalData, 1); // 至少为1，防止除以0

      const averageValue = intervalData.reduce((sum, val) => sum + val, 0) / intervalData.length;
      const averageY = padding + graphHeight - (averageValue / maxValue) * graphHeight;

      // 计算点的坐标
      const points = intervalData.map((value, index) => {
        const x = padding + (index * (graphWidth / (intervalData.length - 1 || 1)));
        const y = padding + graphHeight - (value / maxValue) * graphHeight;
        return { x, y };
      });

      // 构建路径
      let pathD = `M ${points[0].x} ${points[0].y}`;
      points.slice(1).forEach(point => {
        pathD += ` L ${point.x} ${point.y}`;
      });

      return (
        <Svg width={chartWidth} height={chartHeight}>
          {/* Y轴 */}
          <Line
            x1={padding}
            y1={padding}
            x2={padding}
            y2={chartHeight - padding}
            stroke={colorScheme === 'dark' ? '#555' : '#ddd'}
            strokeWidth="1"
          />

          {/* X轴 */}
          <Line
            x1={padding}
            y1={chartHeight - padding}
            x2={chartWidth - padding}
            y2={chartHeight - padding}
            stroke={colorScheme === 'dark' ? '#555' : '#ddd'}
            strokeWidth="1"
          />

          {/* 折线图路径 */}
          <Path
            d={pathD}
            fill="none"
            stroke={chartColor}
            strokeWidth="2"
          />

          {/* 平均值横线 */}
          <Line
            x1={padding}
            y1={averageY}
            x2={chartWidth - padding}
            y2={averageY}
            stroke="red"
            strokeDasharray="4 2"
            strokeWidth="1"
          />
          <SvgText
            x={chartWidth - padding}
            y={averageY - 4}
            fontSize="10"
            fill="red"
            textAnchor="end"
          >
            {averageValue.toFixed(1)}
          </SvgText>

          {/* 数据点 */}
          {points.map((point, index) => (
            <Circle
              key={index}
              cx={point.x}
              cy={point.y}
              r={4}
              fill={chartColor}
            />
          ))}

          {/* X轴标签 */}
          {points.map((point, index) => (
            <SvgText
              key={index}
              x={point.x}
              y={chartHeight - 10}
              fontSize="10"
              fill={colorScheme === 'dark' ? '#ccc' : '#666'}
              textAnchor="middle"
            >
              {index + 1}
            </SvgText>
          ))}

          {/* Y轴最大值标签 */}
          <SvgText
            x={padding - 5}
            y={padding + 5}
            fontSize="10"
            fill={colorScheme === 'dark' ? '#ccc' : '#666'}
            textAnchor="end"
          >
            {maxValue}
          </SvgText>

          {/* Y轴0值标签 */}
          <SvgText
            x={padding - 5}
            y={chartHeight - padding + 5}
            fontSize="10"
            fill={colorScheme === 'dark' ? '#ccc' : '#666'}
            textAnchor="end"
          >
            0
          </SvgText>
        </Svg>
      );
    };

  // 柱状图组件 - 间隔天数频率分布
  const BarChart = () => {
    if (intervalFrequencyData.length === 0) return null;
    
    const chartWidth = width - 64; // 减去内边距
    const chartHeight = 200;
    const padding = 30;
    const graphWidth = chartWidth - padding * 2;
    const graphHeight = chartHeight - padding * 2;
    
    const maxValue = Math.max(...intervalFrequencyData.map(d => d.count), 1); // 至少为1，防止除以0
    const barWidth = (graphWidth / intervalFrequencyData.length) * 0.7; // 留30%的间隙
    
    return (
      <Svg width={chartWidth} height={chartHeight}>
        {/* Y轴 */}
        <Line
          x1={padding}
          y1={padding}
          x2={padding}
          y2={chartHeight - padding}
          stroke={colorScheme === 'dark' ? '#555' : '#ddd'}
          strokeWidth="1"
        />
        
        {/* X轴 */}
        <Line
          x1={padding}
          y1={chartHeight - padding}
          x2={chartWidth - padding}
          y2={chartHeight - padding}
          stroke={colorScheme === 'dark' ? '#555' : '#ddd'}
          strokeWidth="1"
        />
        
        {/* 柱状 */}
        {intervalFrequencyData.map((data, index) => {
          const barHeight = (data.count / maxValue) * graphHeight;
          const x = padding + index * (graphWidth / intervalFrequencyData.length) + (graphWidth / intervalFrequencyData.length - barWidth) / 2;
          const y = chartHeight - padding - barHeight;
          
          return (
            <G key={index}>
              <Path
                d={`M ${x} ${chartHeight - padding} V ${y} H ${x + barWidth} V ${chartHeight - padding} Z`}
                fill={chartColor}
              />
              
              {/* 数值标签 */}
              <SvgText
                x={x + barWidth / 2}
                y={y - 5}
                fontSize="10"
                fill={colorScheme === 'dark' ? '#ccc' : '#666'}
                textAnchor="middle"
              >
                {data.count}
              </SvgText>
              
              {/* X轴标签 */}
              <SvgText
                x={x + barWidth / 2}
                y={chartHeight - 5}
                fontSize="10"
                fill={colorScheme === 'dark' ? '#ccc' : '#666'}
                textAnchor="middle"
              >
                {data.interval}
              </SvgText>
            </G>
          );
        })}
        
        {/* Y轴最大值标签 */}
        <SvgText
          x={padding - 5}
          y={padding + 5}
          fontSize="10"
          fill={colorScheme === 'dark' ? '#ccc' : '#666'}
          textAnchor="end"
        >
          {maxValue}
        </SvgText>
        
        {/* Y轴0值标签 */}
        <SvgText
          x={padding - 5}
          y={chartHeight - padding + 5}
          fontSize="10"
          fill={colorScheme === 'dark' ? '#ccc' : '#666'}
          textAnchor="end"
        >
          0
        </SvgText>
      </Svg>
    );
  };
  
    // 饼图组件 - 间隔天数频率分布
    const PieChart = () => {
      if (intervalFrequencyData.length === 0) return null;
      
      const chartSize = Math.min(width - 64, 200); // 减去内边距
      const radius = chartSize / 2 - 2; // 减去边距
      const centerX = chartSize / 2;
      const centerY = chartSize / 2;
      
      const total = intervalFrequencyData.reduce((sum, data) => sum + data.count, 0);
      
      // 计算每个扇形
      let startAngle = 0;
      const sectors = intervalFrequencyData.map((data, index) => {
        const percentage = data.count / total;
        const endAngle = startAngle + percentage * 2 * Math.PI;
        
        const x1 = centerX + radius * Math.cos(startAngle);
        const y1 = centerY + radius * Math.sin(startAngle);
        const x2 = centerX + radius * Math.cos(endAngle);
        const y2 = centerY + radius * Math.sin(endAngle);
        
        const largeArcFlag = endAngle - startAngle > Math.PI ? 1 : 0;
        
        // 标签位置
        const labelAngle = startAngle + (endAngle - startAngle) / 2;
        const labelRadius = radius * 0.5; // 标签在半径70%处
        const labelX = centerX + labelRadius * Math.cos(labelAngle);
        const labelY = centerY + labelRadius * Math.sin(labelAngle);
        
        // 饼图颜色
        const colors = [
          chartColor,
          '#FF7043',
          '#66BB6A',
          '#42A5F5',
          '#AB47BC',
          '#FFA726',
        ];
        const color = colors[index % colors.length];
        
        const result = {
          path: `M ${centerX} ${centerY} L ${x1} ${y1} A ${radius} ${radius} 0 ${largeArcFlag} 1 ${x2} ${y2} Z`,
          color: color,
          labelX,
          labelY,
          interval: data.interval,
          count: data.count,
          percentage
        };
        
        startAngle = endAngle;
        return result;
      });
      
      return (
        <ThemedView style={styles.pieChartContainer}>
          <Svg width={chartSize} height={chartSize}>
            {sectors.map((sector, index) => (
              <G key={index}>
                <Path
                  d={sector.path}
                  fill={sector.color}
                />
                <SvgText
                  x={sector.labelX}
                  y={sector.labelY}
                  fontSize="11"
                  fontWeight="bold"
                  fill="#fff"
                  textAnchor="middle"
                >
                  {`${sector.interval}(${(sector.percentage * 100).toFixed(1)}%)`}
                </SvgText>
              </G>
            ))}
          </Svg>
        </ThemedView>
      );
    };

  // 图表选择器
  const ChartSelector = () => {
    return (
      <ThemedView style={styles.chartSelector}>
        <TouchableOpacity
          style={[styles.chartButton, chartType === 'line' && styles.chartButtonActive]}
          onPress={() => setChartType('line')}
        >
          <IconSymbol name="trending_up" size={20} color={chartType === 'line' ? '#fff' : iconColor} />
        </TouchableOpacity>
        
        <TouchableOpacity
          style={[styles.chartButton, chartType === 'bar' && styles.chartButtonActive]}
          onPress={() => setChartType('bar')}
        >
          <IconSymbol name="analytics" size={20} color={chartType === 'bar' ? '#fff' : iconColor} />
        </TouchableOpacity>
        
        <TouchableOpacity
          style={[styles.chartButton, chartType === 'pie' && styles.chartButtonActive]}
          onPress={() => setChartType('pie')}
        >
          <IconSymbol name="pie_chart" size={20} color={chartType === 'pie' ? '#fff' : iconColor} />
        </TouchableOpacity>
      </ThemedView>
    );
  };
  
  // 动画状态
  const barHeights = [0, 1, 2, 3, 4, 5, 6].map(() => useSharedValue(0));
  
  useEffect(() => {
    // 按顺序为每个柱状图应用动画
    barHeights.forEach((height, index) => {
      height.value = withDelay(
        index * 100, // 延迟时间增加，创建连续动画效果
        withTiming(1, {
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
        opacity: withTiming(height.value > 0 ? 1 : 0),
      };
    })
  );

  // 根据数据情况选择显示内容
  const renderChartContent = () => {
    if (!hasData) {
      return (
        <ThemedView style={styles.noDataContainer}>
          <IconSymbol
            size={80}
            color={iconColor}
            name="analytics"
            style={styles.noDataIcon}
          />
          <ThemedText style={styles.noDataText}>
            暂无数据可供分析，请先在"日期"标签页中添加记录。
          </ThemedText>
        </ThemedView>
      );
    }
    
    return (
      <>
        <ThemedView style={styles.chartCard}>
          <ThemedView style={styles.chartHeader}>
            <ThemedText type="defaultSemiBold">
              {chartType === 'line' ? '间隔天数趋势' :
               chartType === 'bar' ? '间隔天数频率分布' : '间隔天数比例分布'}
            </ThemedText>
            <ChartSelector />
          </ThemedView>
          
          <ThemedView style={styles.chartContainer}>
            <Animated.View style={barStyles[0]}>
              {chartType === 'line' ? <LineChart /> :
               chartType === 'bar' ? <BarChart /> :
               <PieChart />}
            </Animated.View>
          </ThemedView>
        </ThemedView>
        
        <ThemedText type="subtitle" style={styles.statsTitle}>数据统计</ThemedText>
        
        <ThemedView style={styles.statsGrid}>
          <ThemedView style={styles.statCard}>
            <IconSymbol name="check" size={24} color={chartColor} />
            <ThemedText type="defaultSemiBold">总记录</ThemedText>
            <ThemedText type="subtitle">{dateRecords.length}条</ThemedText>
          </ThemedView>
          
          <ThemedView style={styles.statCard}>
            <IconSymbol name="calendar" size={24} color={chartColor} />
            <ThemedText type="defaultSemiBold">记录月份</ThemedText>
            <ThemedText type="subtitle">
              {(() => {
                const months = new Set();
                dateRecords.forEach(record => {
                  const date = new Date(record.date);
                  months.add(`${date.getFullYear()}-${date.getMonth() + 1}`);
                });
                return months.size;
              })()}
            </ThemedText>
          </ThemedView>
          
          <ThemedView style={styles.statCard}>
            <IconSymbol name="timelapse" size={24} color={chartColor} />
            <ThemedText type="defaultSemiBold">平均间隔</ThemedText>
            <ThemedText type="subtitle">
              {intervalData.length > 0
                ? Math.round(intervalData.reduce((sum, val) => sum + val, 0) / intervalData.length)
                : 0}天
            </ThemedText>
          </ThemedView>
          
          <ThemedView style={styles.statCard}>
            <IconSymbol name="flag" size={24} color={chartColor} />
            <ThemedText type="defaultSemiBold">最近记录</ThemedText>
            <ThemedText type="subtitle">
              {dateRecords.length > 0
                ? `${new Date(dateRecords[0].date).getMonth() + 1}月${new Date(dateRecords[0].date).getDate()}日`
                : '-'}
            </ThemedText>
          </ThemedView>
        </ThemedView>
      </>
    );
  };

  return (
    <ParallaxScrollView
      headerBackgroundColor={{ light: '#E6F0FF', dark: '#1A253C' }}
      headerHeight={180}
      headerImage={
        <View style={styles.headerImageContainer}>
          <IconSymbol
            size={130}
            color={iconColor}
            name="analytics"
            style={styles.headerImage}
          />
        </View>
      }>
      <ThemedView style={styles.titleContainer}>
        <ThemedText type="title">数据分析</ThemedText>
      </ThemedView>
      
      <ThemedView style={styles.analyticsContainer}>
        {renderChartContent()}
      </ThemedView>
    </ParallaxScrollView>
  );
}

const styles = StyleSheet.create({
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
  chartHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  chartSelector: {
    flexDirection: 'row',
    backgroundColor: '#f0f0f0',
    borderRadius: 20,
    padding: 4,
  },
  chartButton: {
    width: 36,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
    marginHorizontal: 2,
  },
  chartButtonActive: {
    backgroundColor: '#0a7ea4',
  },
  pieChartContainer: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  legendContainer: {
    marginTop: 20,
    width: '100%',
  },
  legendItem: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  legendColor: {
    width: 16,
    height: 16,
    borderRadius: 8,
    marginRight: 8,
  },
  legendText: {
    fontSize: 12,
  },
  chartContainer: {
    height: 200,
    justifyContent: 'center',
    alignItems: 'center',
    paddingVertical: 10,
  },
  noDataContainer: {
    padding: 20,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 20,
    marginBottom: 20,
  },
  noDataIcon: {
    opacity: 0.5,
    marginBottom: 20,
  },
  noDataText: {
    fontSize: 16,
    opacity: 0.7,
    textAlign: 'center',
    lineHeight: 24,
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
