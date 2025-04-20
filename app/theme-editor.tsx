import { StyleSheet, ScrollView, TouchableOpacity, View, Alert } from 'react-native';
import React, { useState, useEffect } from 'react';
import { Stack, useRouter } from 'expo-router';
import { LinearGradient } from 'expo-linear-gradient';
import ColorPicker from 'react-native-wheel-color-picker';

import { ThemedText } from '@/components/ThemedText';
import { ThemedView } from '@/components/ThemedView';
import { GradientPresets, CurrentGradients } from '@/constants/Gradients';
import { saveCustomTheme } from '@/services/ThemeService';
import { useColorScheme } from '@/hooks/useColorScheme';

export default function ThemeEditorScreen() {
  const colorScheme = useColorScheme();
  const router = useRouter();
  const [customTheme, setCustomTheme] = useState(JSON.parse(JSON.stringify(CurrentGradients)));
  const [currentPage, setCurrentPage] = useState('countdown');
  const [currentMode, setCurrentMode] = useState(colorScheme);
  const [currentColor, setCurrentColor] = useState(customTheme[currentPage][currentMode].colors[0]);
  const [editingIndex, setEditingIndex] = useState(0);
  
  // 保存主题
  const saveTheme = async () => {
    try {
      const success = await saveCustomTheme(customTheme);
      if (success) {
        Alert.alert('成功', '自定义主题已保存');
        router.back();
      } else {
        Alert.alert('错误', '保存主题失败');
      }
    } catch (error) {
      console.error('保存主题错误:', error);
      Alert.alert('错误', '保存主题时发生错误');
    }
  };
  
  // 更新颜色
  const updateColor = (color: string) => {
    setCurrentColor(color);
    const updatedTheme = { ...customTheme };
    updatedTheme[currentPage][currentMode].colors[editingIndex] = color;
    setCustomTheme(updatedTheme);
  };
  
  // 渲染颜色预览
  const renderColorPreview = () => {
    const colors = customTheme[currentPage][currentMode].colors;
    const start = customTheme[currentPage][currentMode].start;
    const end = customTheme[currentPage][currentMode].end;
    
    return (
      <View style={styles.previewContainer}>
        <ThemedText style={styles.previewTitle}>
          {currentPage.charAt(0).toUpperCase() + currentPage.slice(1)} - {currentMode === 'light' ? '浅色模式' : '深色模式'}
        </ThemedText>
        <LinearGradient
          colors={colors}
          start={start}
          end={end}
          style={styles.gradientPreview}
        />
        <View style={styles.colorButtons}>
          {colors.map((color, index) => (
            <TouchableOpacity
              key={index}
              style={[
                styles.colorButton,
                { backgroundColor: color },
                editingIndex === index && styles.selectedColorButton
              ]}
              onPress={() => {
                setEditingIndex(index);
                setCurrentColor(color);
              }}
            />
          ))}
        </View>
      </View>
    );
  };
  
  return (
    <>
      <Stack.Screen
        options={{
          title: '主题编辑器',
          headerTitleStyle: { fontWeight: 'bold' },
          headerRight: () => (
            <TouchableOpacity onPress={saveTheme} style={{ marginRight: 16 }}>
              <ThemedText style={{ color: '#0a7ea4' }}>保存</ThemedText>
            </TouchableOpacity>
          ),
        }}
      />
      <ScrollView style={styles.container}>
        <ThemedView style={styles.tabContainer}>
          <TouchableOpacity
            style={[styles.tab, currentPage === 'countdown' && styles.activeTab]}
            onPress={() => setCurrentPage('countdown')}
          >
            <ThemedText>倒计时</ThemedText>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.tab, currentPage === 'date' && styles.activeTab]}
            onPress={() => setCurrentPage('date')}
          >
            <ThemedText>日期</ThemedText>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.tab, currentPage === 'analytics' && styles.activeTab]}
            onPress={() => setCurrentPage('analytics')}
          >
            <ThemedText>分析</ThemedText>
          </TouchableOpacity>
        </ThemedView>
        
        <ThemedView style={styles.modeContainer}>
          <TouchableOpacity
            style={[styles.modeTab, currentMode === 'light' && styles.activeModeTab]}
            onPress={() => setCurrentMode('light')}
          >
            <ThemedText>浅色模式</ThemedText>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.modeTab, currentMode === 'dark' && styles.activeModeTab]}
            onPress={() => setCurrentMode('dark')}
          >
            <ThemedText>深色模式</ThemedText>
          </TouchableOpacity>
        </ThemedView>
        
        {renderColorPreview()}
        
        <ThemedView style={styles.colorPickerContainer}>
          <ColorPicker
            color={currentColor}
            onColorChange={updateColor}
            thumbSize={30}
            sliderSize={20}
            noSnap={true}
            row={false}
            swatchesLast={true}
            discrete={false}
          />
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
  tabContainer: {
    flexDirection: 'row',
    marginBottom: 16,
    borderRadius: 8,
    overflow: 'hidden',
  },
  tab: {
    flex: 1,
    paddingVertical: 12,
    alignItems: 'center',
    backgroundColor: '#f0f0f0',
  },
  activeTab: {
    backgroundColor: '#0a7ea4',
  },
  modeContainer: {
    flexDirection: 'row',
    marginBottom: 24,
    borderRadius: 8,
    overflow: 'hidden',
  },
  modeTab: {
    flex: 1,
    paddingVertical: 12,
    alignItems: 'center',
    backgroundColor: '#f0f0f0',
  },
  activeModeTab: {
    backgroundColor: '#0a7ea4',
  },
  previewContainer: {
    marginBottom: 24,
  },
  previewTitle: {
    marginBottom: 8,
    fontWeight: 'bold',
    fontSize: 16,
  },
  gradientPreview: {
    height: 100,
    borderRadius: 8,
    marginBottom: 16,
  },
  colorButtons: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 16,
  },
  colorButton: {
    width: 40,
    height: 40,
    borderRadius: 20,
    borderWidth: 2,
    borderColor: '#ddd',
  },
  selectedColorButton: {
    borderColor: '#0a7ea4',
    borderWidth: 3,
  },
  colorPickerContainer: {
    height: 300,
    marginBottom: 40,
  },
});
