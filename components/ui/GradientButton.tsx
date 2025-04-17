import React from 'react';
import { TouchableOpacity, Text, StyleSheet, ViewStyle, TextStyle } from 'react-native';
import { LinearGradient } from 'expo-linear-gradient';
import { Gradients } from '@/constants/Gradients';
import { useColorScheme } from '@/hooks/useColorScheme';

interface GradientButtonProps {
  title: string;
  onPress: () => void;
  type?: 'primary' | 'warning' | 'neutral';
  containerStyle?: ViewStyle;
  textStyle?: TextStyle;
  iconLeft?: React.ReactNode;
  iconRight?: React.ReactNode;
  disabled?: boolean;
}

export function GradientButton({
  title,
  onPress,
  type = 'primary',
  containerStyle,
  textStyle,
  iconLeft,
  iconRight,
  disabled = false
}: GradientButtonProps) {
  const colorScheme = useColorScheme() ?? 'light';
  const isDark = colorScheme === 'dark';
  
  const gradient = React.useMemo(() => {
    if (disabled) return Gradients.neutral;
    
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
  }, [type, disabled, isDark]);

  return (
    <TouchableOpacity
      style={[styles.button, containerStyle]}
      onPress={onPress}
      disabled={disabled}
      activeOpacity={0.8}
    >
      <LinearGradient
        colors={gradient.colors}
        start={gradient.start}
        end={gradient.end}
        style={styles.gradient}
      >
        {iconLeft}
        <Text style={[styles.buttonText, textStyle]}>{title}</Text>
        {iconRight}
      </LinearGradient>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  button: {
    height: 56,
    borderRadius: 28,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.2,
    shadowRadius: 8,
    elevation: 4,
  },
  gradient: {
    flex: 1,
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 16,
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: 'bold',
    textAlign: 'center',
  },
});
