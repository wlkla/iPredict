import { Tabs } from 'expo-router';
import React from 'react';
import { Platform, TouchableOpacity, View, StyleSheet } from 'react-native';
import { useNavigation } from 'expo-router';

import { HapticTab } from '@/components/HapticTab';
import { IconSymbol } from '@/components/ui/IconSymbol';
import TabBarBackground from '@/components/ui/TabBarBackground';
import { Colors } from '@/constants/Colors';
import { useColorScheme } from '@/hooks/useColorScheme';
import { ThemedText } from '@/components/ThemedText';

export default function TabLayout() {
  const colorScheme = useColorScheme();
  const navigation = useNavigation();

  const openSettings = () => {
    navigation.navigate('settings' as never);
  };

  const openAbout = () => {
    navigation.navigate('about' as never);
  };

  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: Colors[colorScheme ?? 'light'].tint,
        headerShown: true,
        tabBarButton: HapticTab,
        tabBarBackground: TabBarBackground,
        tabBarStyle: Platform.select({
          ios: {
            // 使用透明背景在iOS上显示模糊效果
            position: 'absolute',
          },
          default: {},
        }),
        headerTitle: 'iPredict',
        headerTitleStyle: {
          fontWeight: 'bold',
        },
        headerLeft: () => (
          <TouchableOpacity style={styles.menuButton} onPress={openSettings}>
            <IconSymbol name="settings" size={24} color={Colors[colorScheme ?? 'light'].icon} />
          </TouchableOpacity>
        ),
        headerRight: () => (
          <TouchableOpacity style={styles.infoButton} onPress={openAbout}>
            <IconSymbol name="info" size={24} color={Colors[colorScheme ?? 'light'].icon} />
          </TouchableOpacity>
        ),
      }}>
      <Tabs.Screen
        name="countdown"
        options={{
          title: '倒计时',
          tabBarIcon: ({ color }) => <IconSymbol size={28} name="timer" color={color} />,
        }}
      />
      <Tabs.Screen
        name="date"
        options={{
          title: '日期',
          tabBarIcon: ({ color }) => <IconSymbol size={28} name="calendar" color={color} />,
        }}
      />
      <Tabs.Screen
        name="analytics"
        options={{
          title: '分析',
          tabBarIcon: ({ color }) => <IconSymbol size={28} name="analytics" color={color} />,
        }}
      />
    </Tabs>
  );
}

const styles = StyleSheet.create({
  menuButton: {
    padding: 8,
    marginLeft: 8,
  },
  infoButton: {
    padding: 8,
    marginRight: 8,
  },
});
