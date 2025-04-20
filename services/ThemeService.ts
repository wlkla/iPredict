import AsyncStorage from '@react-native-async-storage/async-storage';
import { GradientPresets, CurrentGradients } from '@/constants/Gradients';

// 存储键
const THEME_PRESET_KEY = 'ipredict_theme_preset';
const CUSTOM_THEME_KEY = 'ipredict_custom_theme';

// 获取当前主题预设名称
export async function getCurrentThemePresetName(): Promise<string> {
  try {
    const presetName = await AsyncStorage.getItem(THEME_PRESET_KEY);
    return presetName || 'default';
  } catch (error) {
    console.error('加载主题预设失败:', error);
    return 'default';
  }
}

// 设置主题预设
export async function setThemePreset(presetName: string): Promise<boolean> {
  try {
    if (presetName === 'custom') {
      // 如果选择自定义，加载自定义主题
      const customTheme = await AsyncStorage.getItem(CUSTOM_THEME_KEY);
      if (customTheme) {
        CurrentGradients = JSON.parse(customTheme);
      }
    } else if (GradientPresets[presetName]) {
      // 如果是预设主题，直接应用
      CurrentGradients = GradientPresets[presetName];
    } else {
      // 如果预设不存在，使用默认
      CurrentGradients = GradientPresets.default;
      presetName = 'default';
    }
    
    // 保存设置
    await AsyncStorage.setItem(THEME_PRESET_KEY, presetName);
    return true;
  } catch (error) {
    console.error('设置主题预设失败:', error);
    return false;
  }
}

// 保存自定义主题
export async function saveCustomTheme(customTheme: any): Promise<boolean> {
  try {
    await AsyncStorage.setItem(CUSTOM_THEME_KEY, JSON.stringify(customTheme));
    CurrentGradients = customTheme;
    // 同时设置当前主题为自定义
    await AsyncStorage.setItem(THEME_PRESET_KEY, 'custom');
    return true;
  } catch (error) {
    console.error('保存自定义主题失败:', error);
    return false;
  }
}

// 初始化主题
export async function initTheme(): Promise<void> {
  try {
    const presetName = await getCurrentThemePresetName();
    await setThemePreset(presetName);
  } catch (error) {
    console.error('初始化主题失败:', error);
  }
}
