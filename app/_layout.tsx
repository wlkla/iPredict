import { initTheme } from '@/services/ThemeService';
import { DarkTheme, DefaultTheme, ThemeProvider as NavigationThemeProvider } from '@react-navigation/native';
import { useFonts } from 'expo-font';
import { Stack } from 'expo-router';
import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import { useEffect } from 'react';
import 'react-native-reanimated';
import { GestureHandlerRootView } from 'react-native-gesture-handler';

import { ThemeProvider, useColorScheme } from '@/hooks/useThemeContext';

// Prevent the splash screen from auto-hiding before asset loading is complete.
SplashScreen.preventAutoHideAsync();

// 内部布局组件
function InnerLayout() {
  const colorScheme = useColorScheme();
  const [loaded] = useFonts({
    SpaceMono: require('../assets/fonts/SpaceMono-Regular.ttf'),
  });

    useEffect(() => {
      const initializeApp = async () => {
        // 初始化主题
        await initTheme();
        
        if (loaded) {
          SplashScreen.hideAsync();
        }
      };
      
      initializeApp();
    }, [loaded]);

  if (!loaded) {
    return null;
  }

  return (
    <NavigationThemeProvider value={colorScheme === 'dark' ? DarkTheme : DefaultTheme}>
          <Stack>
            <Stack.Screen name="(tabs)" options={{ headerShown: false }} />
            <Stack.Screen name="settings" options={{ title: '设置' }} />
            <Stack.Screen name="theme-editor" options={{ title: '主题编辑器' }} />
            <Stack.Screen name="about" options={{ title: '关于' }} />
            <Stack.Screen name="+not-found" />
          </Stack>
      <StatusBar style="auto" />
    </NavigationThemeProvider>
  );
}

// 根布局组件
export default function RootLayout() {
  return (
    <GestureHandlerRootView style={{ flex: 1 }}>
      <ThemeProvider>
        <InnerLayout />
      </ThemeProvider>
    </GestureHandlerRootView>
  );
}
