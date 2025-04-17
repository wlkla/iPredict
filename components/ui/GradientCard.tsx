import React from 'react';
import { View, StyleSheet, ViewStyle } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Gradients } from '@/constants/Gradients';
import { useColorScheme } from '@/hooks/useColorScheme';

interface GradientCardProps {
  children: React.ReactNode;
  type?: 'primary' | 'warning' | 'neutral';
  style?: ViewStyle;
  gradientOpacity?: number;
}

export function GradientCard({
  children,
  type = 'primary',
  style,
  gradientOpacity = 0.1
}: GradientCardProps) {
  const colorScheme = useColorScheme() ?? 'light';
  const isDark = colorScheme === 'dark';
  
  const gradient = React.useMemo(() => {
    if (isDark) {
      switch (type) {
        case 'warning': return Gradients.warningDark;
        case 'neutral': return Gradients.neutral;
        default: return Gradients.primaryDark;
      }
    } else {
      switch (type) {
        case 'warning': return Gradients.warning;
        case 'neutral': return Gradients.neutral;
        default: return Gradients.primary;
      }
    }
  }, [type, isDark]);

  // 调整渐变色的透明度
  const gradientColors = gradient.colors.map(color => {
    // 如果色值是十六进制，转换为带透明度的rgba
    if (color.startsWith('#')) {
      const r = parseInt(color.slice(1, 3), 16);
      const g = parseInt(color.slice(3, 5), 16);
      const b = parseInt(color.slice(5, 7), 16);
      return `rgba(${r}, ${g}, ${b}, ${gradientOpacity})`;
    }
    return color;
  });

  return (
    <View style={[styles.cardContainer, style]}>
      <LinearGradient
        colors={gradientColors}
        start={gradient.start}
        end={gradient.end}
        style={styles.gradient}
      />
      <View style={styles.contentContainer}>
        {children}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  cardContainer: {
    borderRadius: 12,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: 'rgba(0, 0, 0, 0.1)',
  },
  gradient: {
    position: 'absolute',
    left: 0,
    right: 0,
    top: 0,
    bottom: 0,
  },
  contentContainer: {
    padding: 16,
  },
});
