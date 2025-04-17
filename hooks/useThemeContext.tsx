import React, { createContext, useState, useContext, useEffect } from 'react';
import { useColorScheme as useDeviceColorScheme } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

// 主题模式类型
type ThemeMode = 'light' | 'dark' | 'system';

// 主题上下文属性
interface ThemeContextProps {
  themeMode: ThemeMode;
  isDarkMode: boolean;
  setThemeMode: (mode: ThemeMode) => void;
}

// 创建主题上下文
const ThemeContext = createContext<ThemeContextProps | undefined>(undefined);

// 存储键
const THEME_STORAGE_KEY = 'ipredict_theme_mode';

// 主题提供者组件
export function ThemeProvider({ children }: { children: React.ReactNode }) {
  // 获取设备颜色方案
  const deviceColorScheme = useDeviceColorScheme();
  
  // 主题模式状态
  const [themeMode, setThemeModeState] = useState<ThemeMode>('system');
  
  // 初始化时从存储加载主题设置
  useEffect(() => {
    (async () => {
      try {
        const storedTheme = await AsyncStorage.getItem(THEME_STORAGE_KEY);
        if (storedTheme) {
          setThemeModeState(storedTheme as ThemeMode);
        }
      } catch (error) {
        console.error('Failed to load theme setting:', error);
      }
    })();
  }, []);
  
  // 计算当前是否为深色模式
  const isDarkMode =
    themeMode === 'system'
      ? deviceColorScheme === 'dark'
      : themeMode === 'dark';
  
  // 设置主题模式并保存到存储
  const setThemeMode = async (mode: ThemeMode) => {
    setThemeModeState(mode);
    try {
      await AsyncStorage.setItem(THEME_STORAGE_KEY, mode);
    } catch (error) {
      console.error('Failed to save theme setting:', error);
    }
  };
  
  // 提供主题上下文
  return (
    <ThemeContext.Provider
      value={{
        themeMode,
        isDarkMode,
        setThemeMode,
      }}
    >
      {children}
    </ThemeContext.Provider>
  );
}

// 使用主题的钩子
export function useThemeContext() {
  const context = useContext(ThemeContext);
  if (context === undefined) {
    throw new Error('useThemeContext must be used within a ThemeProvider');
  }
  return context;
}

// 覆盖原始的useColorScheme钩子，使其使用我们的主题上下文
export function useColorScheme() {
  const { isDarkMode } = useThemeContext();
  return isDarkMode ? 'dark' : 'light';
}
