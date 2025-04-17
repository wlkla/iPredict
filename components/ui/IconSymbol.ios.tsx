import { SymbolView, SymbolViewProps, SymbolWeight } from 'expo-symbols';
import { StyleProp, ViewStyle } from 'react-native';

// iOS平台上的SF Symbols映射
const SF_SYMBOLS_MAPPING = {
  // 系统图标名称映射
  'pie_chart': 'chart.pie.fill',
  'timer': 'timer',
  'calendar': 'calendar',
  'analytics': 'chart.bar.fill',
  'menu': 'line.3.horizontal',
  'home': 'house.fill',
  'settings': 'gearshape.fill',
  'info': 'info.circle.fill',
  'notifications': 'bell.fill',
  'dark_mode': 'moon.fill',
  'volume_up': 'speaker.wave.3.fill',
  'vibration': 'waveform.path',
  'cleaning_services': 'trash.fill',
  'cloud_download': 'arrow.down.circle.fill',
  'delete': 'trash.slash.fill',
  'star': 'star.fill',
  'feedback': 'text.bubble.fill',
  'email': 'envelope.fill',
  'public': 'globe',
  'check': 'checkmark.circle.fill',
  'timelapse': 'clock.fill',
  'trending_up': 'chart.line.uptrend.xyaxis',
  'flag': 'flag.fill',
  'chevron_right': 'chevron.right',
  // 新增图标映射
  'add': 'plus.circle.fill',
};

export function IconSymbol({
  name,
  size = 24,
  color,
  style,
  weight = 'regular',
}: {
  name: keyof typeof SF_SYMBOLS_MAPPING;
  size?: number;
  color: string;
  style?: StyleProp<ViewStyle>;
  weight?: SymbolWeight;
}) {
  // 将Material图标名称转换为SF Symbols名称
  const sfSymbolName = SF_SYMBOLS_MAPPING[name] as SymbolViewProps['name'];
  
  return (
    <SymbolView
      weight={weight}
      tintColor={color}
      resizeMode="scaleAspectFit"
      name={sfSymbolName}
      style={[
        {
          width: size,
          height: size,
        },
        style,
      ]}
    />
  );
}
