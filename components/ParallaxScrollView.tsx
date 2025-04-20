import type { PropsWithChildren, ReactElement } from 'react';
import { StyleSheet } from 'react-native';
import Animated, {
  interpolate,
  useAnimatedRef,
  useAnimatedStyle,
  useScrollViewOffset,
} from 'react-native-reanimated';
import { LinearGradient } from 'expo-linear-gradient';

import { ThemedView } from '@/components/ThemedView';
import { useBottomTabOverflow } from '@/components/ui/TabBarBackground';
import { useColorScheme } from '@/hooks/useColorScheme';
import { CurrentGradients } from '@/constants/Gradients';

const DEFAULT_HEADER_HEIGHT = 250;

type Props = PropsWithChildren<{
  headerImage: ReactElement;
  headerBackgroundColor?: { dark: string; light: string };
  headerGradient?: {
    light: {
      colors: string[];
      start: { x: number; y: number };
      end: { x: number; y: number };
    };
    dark: {
      colors: string[];
      start: { x: number; y: number };
      end: { x: number; y: number };
    };
  };
  headerHeight?: number;
}>;

export default function ParallaxScrollView({
  children,
  headerImage,
  headerBackgroundColor,
  headerGradient,
  headerHeight = DEFAULT_HEADER_HEIGHT,
}: Props) {
  const colorScheme = useColorScheme() ?? 'light';
  const scrollRef = useAnimatedRef<Animated.ScrollView>();
  const scrollOffset = useScrollViewOffset(scrollRef);
  const bottom = useBottomTabOverflow();
  
  // 默认渐变配置
  const defaultGradient = {
    light: CurrentGradients.countdown.light,
    dark: CurrentGradients.countdown.dark,
  };
  
  // 使用传入的渐变或默认渐变
  const gradient = headerGradient || defaultGradient;
  
  const headerAnimatedStyle = useAnimatedStyle(() => {
    return {
      transform: [
        {
          translateY: interpolate(
            scrollOffset.value,
            [-headerHeight, 0, headerHeight],
            [-headerHeight / 2, 0, headerHeight * 0.75]
          ),
        },
        {
          scale: interpolate(scrollOffset.value, [-headerHeight, 0, headerHeight], [2, 1, 1]),
        },
      ],
    };
  });

  return (
    <ThemedView style={styles.container}>
      <Animated.ScrollView
        ref={scrollRef}
        scrollEventThrottle={16}
        scrollIndicatorInsets={{ bottom }}
        contentContainerStyle={{ paddingBottom: bottom }}>
        <Animated.View
          style={[
            styles.header,
            {
              height: headerHeight,
            },
            headerAnimatedStyle,
          ]}>
          {/* 使用渐变背景替代纯色背景 */}
          <LinearGradient
            colors={gradient[colorScheme].colors}
            start={gradient[colorScheme].start}
            end={gradient[colorScheme].end}
            style={StyleSheet.absoluteFill}
          />
          {headerImage}
        </Animated.View>
        <ThemedView style={styles.content}>{children}</ThemedView>
      </Animated.ScrollView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  header: {
    overflow: 'hidden',
  },
  content: {
    flex: 1,
    padding: 32,
    gap: 16,
    overflow: 'hidden',
  },
});
