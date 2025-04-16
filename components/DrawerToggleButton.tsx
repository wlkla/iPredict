import { useDrawerStatus, DrawerActions } from '@react-navigation/drawer';
import { useNavigation } from 'expo-router';
import React from 'react';
import { TouchableOpacity, StyleSheet } from 'react-native';
import Animated, { useAnimatedStyle, withTiming } from 'react-native-reanimated';

import { IconSymbol } from '@/components/ui/IconSymbol';
import { Colors } from '@/constants/Colors';
import { useColorScheme } from '@/hooks/useColorScheme';

export function DrawerToggleButton() {
  const navigation = useNavigation();
  const isDrawerOpen = useDrawerStatus() === 'open';
  const colorScheme = useColorScheme() ?? 'light';
  const iconColor = Colors[colorScheme].icon;

  const toggleDrawer = () => {
    navigation.dispatch(DrawerActions.toggleDrawer());
  };

  const animatedStyle = useAnimatedStyle(() => {
    return {
      transform: [
        {
          rotate: withTiming(isDrawerOpen ? '180deg' : '0deg', { duration: 300 }),
        },
      ],
    };
  });

  return (
    <TouchableOpacity onPress={toggleDrawer} style={styles.button}>
      <Animated.View style={animatedStyle}>
        <IconSymbol name="menu" size={24} color={iconColor} />
      </Animated.View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  button: {
    padding: 8,
    marginLeft: 8,
  },
});
