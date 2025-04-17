import { StyleSheet, Switch, ScrollView, TouchableOpacity, Alert, Platform, View } from 'react-native';
import React, { useState, useEffect } from 'react';
import { Stack } from 'expo-router';
import * as Haptics from 'expo-haptics';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as FileSystem from 'expo-file-system';
import * as DocumentPicker from 'expo-document-picker';
import * as Sharing from 'expo-sharing';

import { ThemedText } from '@/components/ThemedText';
import { ThemedView } from '@/components/ThemedView';
import { IconSymbol } from '@/components/ui/IconSymbol';
import { Colors } from '@/constants/Colors';
import { useThemeContext } from '@/hooks/useThemeContext';
import { useColorScheme } from '@/hooks/useColorScheme';

// 本地存储键名（与 date.tsx 一致）
const STORAGE_KEY = 'ipredict_date_records';

// 加密密钥（实际应用中应该使用更安全的方式存储）
const ENCRYPTION_KEY = 'ipredict_secure_key_2025';

export default function SettingsScreen() {
  const colorScheme = useColorScheme();
  const { themeMode, setThemeMode } = useThemeContext();
  
  const iconColor = Colors[colorScheme].icon;
  const tintColor = Colors[colorScheme].tint;
  
  const [notificationsEnabled, setNotificationsEnabled] = useState(true);
  const [soundEnabled, setSoundEnabled] = useState(true);
  const [vibrationEnabled, setVibrationEnabled] = useState(true);
  const [darkModeEnabled, setDarkModeEnabled] = useState(colorScheme === 'dark');
  
  // 当系统主题变化时更新开关状态
  useEffect(() => {
    setDarkModeEnabled(colorScheme === 'dark');
  }, [colorScheme]);
  
  const toggleSwitch = (setter: React.Dispatch<React.SetStateAction<boolean>>, value: boolean) => {
    if (Platform.OS === 'ios' && vibrationEnabled) {
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    }
    setter(value);
  };
  
  // 切换深色模式
  const toggleDarkMode = (value: boolean) => {
    if (Platform.OS === 'ios' && vibrationEnabled) {
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Light);
    }
    setDarkModeEnabled(value);
    setThemeMode(value ? 'dark' : 'light');
  };

  // 使用简单的Base64编码作为"加密"（在实际应用中，应使用更安全的加密方法）
  const encryptData = async (data: string): Promise<string> => {
    try {
      // 简单的密钥添加
      const modifiedData = data.split('').map((char, index) => {
        // 使用密钥的字符和位置来修改原始数据
        const keyChar = ENCRYPTION_KEY[index % ENCRYPTION_KEY.length];
        return String.fromCharCode(char.charCodeAt(0) ^ keyChar.charCodeAt(0));
      }).join('');
      
      // 转换为 Base64
      return btoa(modifiedData);
    } catch (error) {
      console.error('加密失败:', error);
      throw error;
    }
  };

  // 解密数据
  const decryptData = async (encryptedData: string): Promise<string> => {
    try {
      // 从Base64解码
      const decodedData = atob(encryptedData);
      
      // 还原数据
      const originalData = decodedData.split('').map((char, index) => {
        // 使用相同的方法还原原始数据
        const keyChar = ENCRYPTION_KEY[index % ENCRYPTION_KEY.length];
        return String.fromCharCode(char.charCodeAt(0) ^ keyChar.charCodeAt(0));
      }).join('');
      
      return originalData;
    } catch (error) {
      console.error('解密失败:', error);
      throw error;
    }
  };

  // 导出数据
  const exportData = async () => {
    if (Platform.OS === 'ios' && vibrationEnabled) {
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    }
    
    try {
      // 获取存储的记录
      const storedRecords = await AsyncStorage.getItem(STORAGE_KEY);
      
      if (!storedRecords) {
        Alert.alert('提示', '没有数据可导出');
        return;
      }
      
      // 创建元数据
      const metadata = {
        app: 'iPredict',
        version: '1.0.0',
        exportDate: new Date().toISOString(),
        dataType: 'date_records'
      };
      
      // 组合数据
      const dataToExport = JSON.stringify({
        metadata,
        records: JSON.parse(storedRecords)
      });
      
      // 加密数据
      const encryptedData = await encryptData(dataToExport);
      
      // 创建临时文件
      const fileName = `ipredict_export_${new Date().getTime()}.ipredict`;
      const filePath = `${FileSystem.cacheDirectory}${fileName}`;
      
      // 写入文件
      await FileSystem.writeAsStringAsync(filePath, encryptedData);
      
      // 分享文件
      if (await Sharing.isAvailableAsync()) {
        await Sharing.shareAsync(filePath, {
          mimeType: 'application/octet-stream',
          dialogTitle: '导出iPredict数据',
          UTI: 'public.data' // iOS文件类型标识符
        });
      } else {
        Alert.alert('错误', '您的设备不支持文件分享功能');
      }
    } catch (error) {
      console.error('导出数据失败:', error);
      Alert.alert('导出失败', '导出数据时发生错误');
    }
  };

  // 导入数据
  const importData = async () => {
    if (Platform.OS === 'ios' && vibrationEnabled) {
      Haptics.impactAsync(Haptics.ImpactFeedbackStyle.Medium);
    }
    
    try {
      // 选择文件
      const result = await DocumentPicker.getDocumentAsync({
        type: 'application/octet-stream',
        copyToCacheDirectory: true
      });
      
      if (result.canceled) {
        return;
      }
      
      const file = result.assets[0];
      
      // 检查文件扩展名
      if (!file.name.endsWith('.ipredict')) {
        Alert.alert('错误', '请选择有效的.ipredict文件');
        return;
      }
      
      // 读取文件内容
      const fileContent = await FileSystem.readAsStringAsync(file.uri);
      
      // 解密数据
      const decryptedData = await decryptData(fileContent);
      
      try {
        // 解析解密后的数据
        const parsedData = JSON.parse(decryptedData);
        
        // 验证数据格式
        if (!parsedData.metadata || !parsedData.records || parsedData.metadata.app !== 'iPredict') {
          throw new Error('无效的数据格式');
        }
        
        // 确认导入
        Alert.alert(
          '确认导入',
          '导入将覆盖当前的所有数据，是否继续？',
          [
            {
              text: '取消',
              style: 'cancel'
            },
            {
              text: '确定',
              onPress: async () => {
                // 存储导入的记录
                await AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(parsedData.records));
                Alert.alert('成功', '数据导入成功');
              }
            }
          ]
        );
      } catch (parseError) {
        Alert.alert('错误', '无效的数据文件格式');
      }
    } catch (error) {
      console.error('导入数据失败:', error);
      Alert.alert('导入失败', '导入数据时发生错误');
    }
  };

  // 重置所有数据
  const resetAllData = () => {
    if (Platform.OS === 'ios' && vibrationEnabled) {
      Haptics.notificationAsync(Haptics.NotificationFeedbackType.Warning);
    }
    
    // 显示警告弹窗
    Alert.alert(
      '警告',
      '重置将删除所有数据，此操作无法撤销，是否继续？',
      [
        {
          text: '取消',
          style: 'cancel'
        },
        {
          text: '删除',
          style: 'destructive',
          onPress: async () => {
            try {
              // 清除存储的记录
              await AsyncStorage.removeItem(STORAGE_KEY);
              Alert.alert('成功', '所有数据已重置');
            } catch (error) {
              console.error('重置数据失败:', error);
              Alert.alert('错误', '重置数据时发生错误');
            }
          }
        }
      ]
    );
  };
  
  // 添加系统主题选择器
  const showThemeSelector = () => {
    Alert.alert(
      '选择主题',
      '请选择应用主题模式',
      [
        {
          text: '浅色',
          onPress: () => setThemeMode('light')
        },
        {
          text: '深色',
          onPress: () => setThemeMode('dark')
        },
        {
          text: '跟随系统',
          onPress: () => setThemeMode('system')
        },
        {
          text: '取消',
          style: 'cancel'
        }
      ]
    );
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
            
            
            <TouchableOpacity
              style={styles.buttonItem}
              onPress={showThemeSelector}
            >
              <ThemedView style={styles.settingInfo}>
                <IconSymbol name="public" size={24} color={iconColor} />
                <View style={{ flex: 1 }}>
                  <ThemedText style={styles.settingText}>主题设置</ThemedText>
                  <ThemedText style={styles.settingSubtext}>
                    {themeMode === 'system' ? '跟随系统' : themeMode === 'dark' ? '深色' : '浅色'}
                  </ThemedText>
                </View>
              </ThemedView>
              <IconSymbol name="chevron_right" size={24} color={iconColor} />
            </TouchableOpacity>
          </ThemedView>
          
          <ThemedView style={styles.section}>
            <ThemedText type="subtitle">数据管理</ThemedText>
            
            <TouchableOpacity
              style={styles.buttonItem}
              onPress={exportData}
            >
              <ThemedView style={styles.settingInfo}>
                <IconSymbol name="cloud_up" size={24} color={iconColor} />
                <ThemedText style={styles.settingText}>导出数据</ThemedText>
              </ThemedView>
              <IconSymbol name="chevron_right" size={24} color={iconColor} />
            </TouchableOpacity>
            
            <TouchableOpacity
              style={styles.buttonItem}
              onPress={importData}
            >
              <ThemedView style={styles.settingInfo}>
                <IconSymbol name="cloud_download" size={24} color={iconColor} />
                <ThemedText style={styles.settingText}>导入数据</ThemedText>
              </ThemedView>
              <IconSymbol name="chevron_right" size={24} color={iconColor} />
            </TouchableOpacity>
            
            <TouchableOpacity
              style={[styles.buttonItem, styles.dangerButton]}
              onPress={resetAllData}
            >
              <ThemedView style={styles.settingInfo}>
                <IconSymbol name="delete" size={24} color="#FF3B30" />
                <ThemedText style={[styles.settingText, styles.dangerText]}>重置所有数据</ThemedText>
              </ThemedView>
              <IconSymbol name="chevron_right" size={24} color="#FF3B30" />
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
    flex: 1,
  },
  settingText: {
    fontSize: 16,
  },
  settingSubtext: {
    fontSize: 12,
    opacity: 0.7,
    marginTop: 2,
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
