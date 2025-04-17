import { StyleSheet, ScrollView, Image, Linking } from 'react-native';
import React from 'react';
import { Stack } from 'expo-router';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  withRepeat,
  withTiming,
  Easing,
} from 'react-native-reanimated';

import { ThemedText } from '@/components/ThemedText';
import { ThemedView } from '@/components/ThemedView';
import { IconSymbol } from '@/components/ui/IconSymbol';
import { Colors } from '@/constants/Colors';
import { useColorScheme } from '@/hooks/useColorScheme';
import { ExternalLink } from '@/components/ExternalLink';

export default function AboutScreen() {
  const colorScheme = useColorScheme() ?? 'light';
  const tintColor = Colors[colorScheme].tint;
  
  // 旋转动画
  const rotation = useSharedValue(0);
  
  React.useEffect(() => {
    rotation.value = withRepeat(
      withTiming(360, {
        duration: 6000,
        easing: Easing.linear,
      }),
      -1, // 无限重复
      false // 不反向
    );
  }, []);
  
  const animatedStyle = useAnimatedStyle(() => {
    return {
      transform: [{ rotateZ: `${rotation.value}deg` }],
    };
  });

  return (
    <>
      <Stack.Screen
        options={{
          title: '关于',
          headerTitleStyle: {
            fontWeight: 'bold',
          },
        }}
      />
      <ScrollView>
        <ThemedView style={styles.container}>
          <ThemedView style={styles.logoContainer}>
            <Animated.View style={animatedStyle}>
              <Image
                source={require('@/assets/images/react-logo.png')}
                style={styles.logo}
              />
            </Animated.View>
            <ThemedText type="title" style={styles.appName}>iPredict</ThemedText>
            <ThemedText style={styles.tagline}>智能日程管理与预测</ThemedText>
          </ThemedView>
          
          <ThemedView style={styles.section}>
            <ThemedText type="subtitle">应用介绍</ThemedText>
            <ThemedText style={styles.description}>
              iPredict 是一款集日程管理、时间预测和数据分析于一体的综合应用。
              通过智能分析，帮助用户更好地规划时间、跟踪目标完成情况，并提供
              直观的数据分析，让您的时间管理更高效。
            </ThemedText>
          </ThemedView>
          
          <ThemedView style={styles.section}>
            <ThemedText type="subtitle">功能特点</ThemedText>
            
            <ThemedView style={styles.featureItem}>
              <IconSymbol name="timer" size={24} color={tintColor} />
              <ThemedView style={styles.featureTextContainer}>
                <ThemedText type="defaultSemiBold">智能倒计时</ThemedText>
                <ThemedText>跟踪重要事件和事件的剩余时间</ThemedText>
              </ThemedView>
            </ThemedView>
            
            <ThemedView style={styles.featureItem}>
              <IconSymbol name="calendar" size={24} color={tintColor} />
              <ThemedView style={styles.featureTextContainer}>
                <ThemedText type="defaultSemiBold">日期管理</ThemedText>
                <ThemedText>直观的列表，让您一目了然地查看历史记录</ThemedText>
              </ThemedView>
            </ThemedView>
            
            <ThemedView style={styles.featureItem}>
              <IconSymbol name="analytics" size={24} color={tintColor} />
              <ThemedView style={styles.featureTextContainer}>
                <ThemedText type="defaultSemiBold">数据分析</ThemedText>
                <ThemedText>详细的统计和图表，帮助您了解事件周期情况</ThemedText>
              </ThemedView>
            </ThemedView>
            
            <ThemedView style={styles.featureItem}>
              <IconSymbol name="notifications" size={24} color={tintColor} />
              <ThemedView style={styles.featureTextContainer}>
                <ThemedText type="defaultSemiBold">智能提醒</ThemedText>
                <ThemedText>根据事件倒计时自动发送提醒</ThemedText>
              </ThemedView>
            </ThemedView>
          </ThemedView>
          
          <ThemedView style={styles.section}>
            <ThemedText type="subtitle">联系我们</ThemedText>
            
            <ThemedView style={styles.contactItem}>
              <IconSymbol name="email" size={24} color={tintColor} />
              <ExternalLink href="mailto:onebuaaer@gmail.com" style={styles.link}>
                <ThemedText type="link">onebuaaer@gmail.com</ThemedText>
              </ExternalLink>
            </ThemedView>
          </ThemedView>
          
          <ThemedView style={styles.section}>
            <ThemedText type="subtitle">隐私政策</ThemedText>
            <ThemedText style={styles.privacyText}>
              我们尊重您的隐私。iPredict 应用收集的所有数据仅用于提供和改进服务。
              我们不会与第三方共享您的个人信息。
            </ThemedText>
          </ThemedView>
          
          <ThemedText style={styles.copyright}>
            © 2025 iPredict. 保留所有权利。
          </ThemedText>
        </ThemedView>
      </ScrollView>
    </>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
  },
  logoContainer: {
    alignItems: 'center',
    marginVertical: 24,
  },
  logo: {
    width: 120,
    height: 120,
    marginBottom: 16,
  },
  appName: {
    marginBottom: 8,
  },
  tagline: {
    opacity: 0.7,
    fontSize: 16,
  },
  section: {
    marginBottom: 24,
  },
  description: {
    marginTop: 8,
    lineHeight: 22,
  },
  featureItem: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginTop: 12,
    gap: 16,
  },
  featureTextContainer: {
    flex: 1,
  },
  contactItem: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 12,
    gap: 16,
  },
  link: {
    flex: 1,
  },
  privacyText: {
    marginTop: 8,
    marginBottom: 12,
    lineHeight: 22,
  },
  privacyLink: {
    marginTop: 8,
  },
  copyright: {
    textAlign: 'center',
    marginTop: 20,
    marginBottom: 40,
    opacity: 0.6,
  },
});
