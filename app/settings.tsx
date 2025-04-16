import { StyleSheet, Switch, ScrollView, TouchableOpacity } from 'react-native';
import React, { useState } from 'react';
import { Stack } from 'expo-router';
import * as Haptics from 'expo-haptics';

import { ThemedText } from '@/components/ThemedText';
import { ThemedView } from '@/components/ThemedView';
import { IconSymbol } from '@/components/ui/IconSymbol';
import { Colors } from '@/constants/Colors';
import { useColorScheme } from '@/hooks/useColorScheme';

export default function SettingsScreen() {
  const colorScheme = useColorScheme() ?? 'light';
  const iconColor = Colors[colorScheme].icon;
  const tintColor = Colors[colorScheme].tint;
  
  const [notificationsEnabled, setNotificationsEnabled] = useState(true);
  const [darkModeEnabled, setDarkModeEnabled] = useState(colorScheme === 'dark');
  const [soundEnabled, setSoundEnabled] = useState(true);
  const [vibrationEnabled, setVibrationEnabled] = useState(true);
  
  const toggleSwitch = (setter: React.Dispatch<React.SetStateAction<boolean>>, value: boolean) => {
    if (process.env.EXPO_OS === 'ios' && vibrationEnabled) {
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    }
    setter(value);
  };

  return (
    <>
      <Stack.Screen
        options={{
          title: '设置',
          headerTitleStyle: {
            fontWeight: 'bold',
          },
        }}
      />
      <ScrollView>
        <ThemedView style={styles.container}>
          <ThemedView style={styles.section}>
            <ThemedText type="subtitle">应用设置</ThemedText>
            
            <ThemedView style={styles.settingItem}>
              <ThemedView style={styles.settingInfo}>
                <IconSymbol name="notifications" size={24} color={iconColor} />
                <ThemedText style={styles.settingText}>通知</ThemedText>
              </ThemedView>
              <Switch
                trackColor={{ false: '#767577', true: tintColor }}
                thumbColor="#f4f3f4"
                ios_backgroundColor="#3e3e3e"
                onValueChange={(value) => toggleSwitch(setNotificationsEnabled, value)}
                value={notificationsEnabled}
              />
            </ThemedView>
            
            <ThemedView style={styles.settingItem}>
              <ThemedView style={styles.settingInfo}>
                <IconSymbol name="dark_mode" size={24} color={iconColor} />
                <ThemedText style={styles.settingText}>深色模式</ThemedText>
              </ThemedView>
              <Switch
                trackColor={{ false: '#767577', true: tintColor }}
                thumbColor="#f4f3f4"
                ios_backgroundColor="#3e3e3e"
                onValueChange={(value) => toggleSwitch(setDarkModeEnabled, value)}
                value={darkModeEnabled}
              />
            </ThemedView>
            
            <ThemedView style={styles.settingItem}>
              <ThemedView style={styles.settingInfo}>
                <IconSymbol name="volume_up" size={24} color={iconColor} />
                <ThemedText style={styles.settingText}>声音</ThemedText>
              </ThemedView>
              <Switch
                trackColor={{ false: '#767577', true: tintColor }}
                thumbColor="#f4f3f4"
                ios_backgroundColor="#3e3e3e"
                onValueChange={(value) => toggleSwitch(setSoundEnabled, value)}
                value={soundEnabled}
              />
            </ThemedView>
            
            <ThemedView style={styles.settingItem}>
              <ThemedView style={styles.settingInfo}>
                <IconSymbol name="vibration" size={24} color={iconColor} />
                <ThemedText style={styles.settingText}>震动</ThemedText>
              </ThemedView>
              <Switch
                trackColor={{ false: '#767577', true: tintColor }}
                thumbColor="#f4f3f4"
                ios_backgroundColor="#3e3e3e"
                onValueChange={(value) => toggleSwitch(setVibrationEnabled, value)}
                value={vibrationEnabled}
              />
            </ThemedView>
          </ThemedView>
          
          <ThemedView style={styles.section}>
            <ThemedText type="subtitle">数据管理</ThemedText>
            
            <TouchableOpacity
              style={styles.buttonItem}
              onPress={() => {
                if (process.env.EXPO_OS === 'ios' && vibrationEnabled) {
                  Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
                }
                // 这里添加清除缓存的功能
                console.log('清除缓存');
              }}
            >
              <ThemedView style={styles.settingInfo}>
                <IconSymbol name="cleaning_services" size={24} color={iconColor} />
                <ThemedText style={styles.settingText}>清除缓存</ThemedText>
              </ThemedView>
              <IconSymbol name="chevron_right" size={24} color={iconColor} />
            </TouchableOpacity>
            
            <TouchableOpacity
              style={styles.buttonItem}
              onPress={() => {
                if (process.env.EXPO_OS === 'ios' && vibrationEnabled) {
                  Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
                }
                // 这里添加数据导出功能
                console.log('导出数据');
              }}
            >
              <ThemedView style={styles.settingInfo}>
                <IconSymbol name="cloud_download" size={24} color={iconColor} />
                <ThemedText style={styles.settingText}>导出数据</ThemedText>
              </ThemedView>
              <IconSymbol name="chevron_right" size={24} color={iconColor} />
            </TouchableOpacity>
            
            <TouchableOpacity
              style={[styles.buttonItem, styles.dangerButton]}
              onPress={() => {
                if (process.env.EXPO_OS === 'ios' && vibrationEnabled) {
                  Haptics.notificationAsync(Haptics.NotificationFeedbackType.Warning);
                }
                // 这里添加重置数据功能
                console.log('重置所有数据');
              }}
            >
              <ThemedView style={styles.settingInfo}>
                <IconSymbol name="delete" size={24} color="#FF3B30" />
                <ThemedText style={[styles.settingText, styles.dangerText]}>重置所有数据</ThemedText>
              </ThemedView>
              <IconSymbol name="chevron_right" size={24} color="#FF3B30" />
            </TouchableOpacity>
          </ThemedView>
          
          <ThemedView style={styles.section}>
            <ThemedText type="subtitle">其他</ThemedText>
            
            <TouchableOpacity
              style={styles.buttonItem}
              onPress={() => {
                if (process.env.EXPO_OS === 'ios' && vibrationEnabled) {
                  Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
                }
                // 这里添加评分功能
                console.log('给我们评分');
              }}
            >
              <ThemedView style={styles.settingInfo}>
                <IconSymbol name="star" size={24} color={iconColor} />
                <ThemedText style={styles.settingText}>给我们评分</ThemedText>
              </ThemedView>
              <IconSymbol name="chevron_right" size={24} color={iconColor} />
            </TouchableOpacity>
            
            <TouchableOpacity
              style={styles.buttonItem}
              onPress={() => {
                if (process.env.EXPO_OS === 'ios' && vibrationEnabled) {
                  Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
                }
                // 这里添加反馈功能
                console.log('问题反馈');
              }}
            >
              <ThemedView style={styles.settingInfo}>
                <IconSymbol name="feedback" size={24} color={iconColor} />
                <ThemedText style={styles.settingText}>问题反馈</ThemedText>
              </ThemedView>
              <IconSymbol name="chevron_right" size={24} color={iconColor} />
            </TouchableOpacity>
          </ThemedView>
          
          <ThemedText style={styles.version}>版本 1.0.0</ThemedText>
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
  section: {
    marginBottom: 24,
    gap: 8,
  },
  settingItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#CCCCCC',
  },
  buttonItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#CCCCCC',
  },
  settingInfo: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  settingText: {
    fontSize: 16,
  },
  dangerButton: {
    marginTop: 8,
  },
  dangerText: {
    color: '#FF3B30',
  },
  version: {
    textAlign: 'center',
    marginTop: 20,
    marginBottom: 40,
    opacity: 0.6,
  },
});
