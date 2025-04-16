// This file is a fallback for using MaterialIcons on Android and web.

import MaterialIcons from '@expo/vector-icons/MaterialIcons';
import { SymbolWeight } from 'expo-symbols';
import React from 'react';
import { OpaqueColorValue, StyleProp, ViewStyle } from 'react-native';

// Add your SFSymbol to MaterialIcons mappings here.
const MAPPING = {
  // See MaterialIcons here: https://icons.expo.fyi
  // See SF Symbols in the SF Symbols app on Mac.
  'house.fill': 'home',
  'paperplane.fill': 'send',
  'chevron.left.forwardslash.chevron.right': 'code',
  'chevron.right': 'chevron-right',
  // 已有图标映射
  'timer': 'timer',
  'calendar': 'event',
  'analytics': 'analytics',
  'menu': 'menu',
  'home': 'home',
  'settings': 'settings',
  'info': 'info',
  'notifications': 'notifications',
  'dark_mode': 'dark_mode',
  'volume_up': 'volume_up',
  'vibration': 'vibration',
  'cleaning_services': 'cleaning_services',
  'cloud_download': 'cloud_download',
  'delete': 'delete',
  'star': 'star',
  'feedback': 'feedback',
  'email': 'email',
  'public': 'public',
  'check': 'check',
  'timelapse': 'timelapse',
  'trending_up': 'trending_up',
  'flag': 'flag',
  'chevron_right': 'chevron_right',
  // 新增图标映射
  'add': 'add',
} as Partial<
  Record<
    import('expo-symbols').SymbolViewProps['name'],
    React.ComponentProps<typeof MaterialIcons>['name']
  >
>;

export type IconSymbolName = keyof typeof MAPPING;

/**
 * An icon component that uses native SFSymbols on iOS, and MaterialIcons on Android and web. This ensures a consistent look across platforms, and optimal resource usage.
 *
 * Icon `name`s are based on SFSymbols and require manual mapping to MaterialIcons.
 */
export function IconSymbol({
  name,
  size = 24,
  color,
  style,
}: {
  name: IconSymbolName;
  size?: number;
  color: string | OpaqueColorValue;
  style?: StyleProp<ViewStyle>;
  weight?: SymbolWeight;
}) {
  return <MaterialIcons color={color} size={size} name={MAPPING[name]} style={style} />;
}
