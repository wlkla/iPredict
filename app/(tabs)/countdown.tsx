import { StyleSheet, Platform } from 'react-native';
import React, { useState, useEffect } from 'react';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withTiming,
  withRepeat,
  withSequence,
} from 'react-native-reanimated';

import { ThemedText } from '@/components/ThemedText';
import { ThemedView } from '@/components/ThemedView';
import ParallaxScrollView from '@/components/ParallaxScrollView';
import { IconSymbol } from '@/components/ui/IconSymbol';
import { Colors } from '@/constants/Colors';
import { useColorScheme } from '@/hooks/useColorScheme';

export default function CountdownScreen() {
  const colorScheme = useColorScheme() ?? 'light';
  const iconColor = Colors[colorScheme].icon;
  
  const pulseAnimation = useSharedValue(1);

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

  const animatedStyle = useAnimatedStyle(() => {
    return {
      transform: [{ scale: pulseAnimation.value }],
    };
  });

  return (
    <ParallaxScrollView
      headerBackgroundColor={{ light: '#F0F8FF', dark: '#1A2C38' }}
      headerImage={
        <Animated.View style={[styles.iconContainer, animatedStyle]}>
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
      
      <ThemedView style={styles.countdownContainer}>
        <ThemedText type="subtitle">即将到来的事件</ThemedText>
        <ThemedView style={styles.eventCard}>
          <ThemedText type="defaultSemiBold">项目发布</ThemedText>
          <ThemedText>剩余时间: 14天 8小时 30分钟</ThemedText>
        </ThemedView>
        
        <ThemedView style={styles.eventCard}>
          <ThemedText type="defaultSemiBold">团队会议</ThemedText>
          <ThemedText>剩余时间: 2天 3小时 15分钟</ThemedText>
        </ThemedView>
        
        <ThemedView style={styles.eventCard}>
          <ThemedText type="defaultSemiBold">截止日期</ThemedText>
          <ThemedText>剩余时间: 7天 12小时 45分钟</ThemedText>
        </ThemedView>
      </ThemedView>
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
});
