import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';

// 通知存储键
const NOTIFICATION_IDS_KEY = 'ipredict_notification_ids';

// 设置通知处理器
Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: true,
  }),
});

// 初始化通知权限
export async function initNotifications() {
  // 检查是否已获取权限
  const { status: existingStatus } = await Notifications.getPermissionsAsync();
  let finalStatus = existingStatus;
  
  // 如果还没有权限，请求权限
  if (existingStatus !== 'granted') {
    const { status } = await Notifications.requestPermissionsAsync();
    finalStatus = status;
  }
  
  // 如果用户拒绝授予权限，返回false
  if (finalStatus !== 'granted') {
    return false;
  }
  
  // 在iOS上设置通知类别
  if (Platform.OS === 'ios') {
    Notifications.setNotificationCategoryAsync('countdown', [
      {
        identifier: 'view',
        buttonTitle: '查看详情',
        options: {
          opensAppToForeground: true,
        },
      },
    ]);
  }
  
  return true;
}

// 保存通知ID到AsyncStorage
async function saveNotificationId(key: string, id: string) {
  try {
    const existingIdsStr = await AsyncStorage.getItem(NOTIFICATION_IDS_KEY);
    let notificationIds: Record<string, string> = {};
    
    if (existingIdsStr) {
      notificationIds = JSON.parse(existingIdsStr);
    }
    
    notificationIds[key] = id;
    await AsyncStorage.setItem(NOTIFICATION_IDS_KEY, JSON.stringify(notificationIds));
  } catch (error) {
    console.error('保存通知ID失败:', error);
  }
}

// 获取通知ID
async function getNotificationId(key: string): Promise<string | null> {
  try {
    const existingIdsStr = await AsyncStorage.getItem(NOTIFICATION_IDS_KEY);
    
    if (existingIdsStr) {
      const notificationIds = JSON.parse(existingIdsStr);
      return notificationIds[key] || null;
    }
    
    return null;
  } catch (error) {
    console.error('获取通知ID失败:', error);
    return null;
  }
}

// 取消通知
export async function cancelNotification(key: string) {
  try {
    const notificationId = await getNotificationId(key);
    
    if (notificationId) {
      await Notifications.cancelScheduledNotificationAsync(notificationId);
      
      // 从存储中移除通知ID
      const existingIdsStr = await AsyncStorage.getItem(NOTIFICATION_IDS_KEY);
      
      if (existingIdsStr) {
        const notificationIds = JSON.parse(existingIdsStr);
        delete notificationIds[key];
        await AsyncStorage.setItem(NOTIFICATION_IDS_KEY, JSON.stringify(notificationIds));
      }
    }
  } catch (error) {
    console.error('取消通知失败:', error);
  }
}

// 取消所有通知
export async function cancelAllNotifications() {
  try {
    await Notifications.cancelAllScheduledNotificationsAsync();
    await AsyncStorage.removeItem(NOTIFICATION_IDS_KEY);
  } catch (error) {
    console.error('取消所有通知失败:', error);
  }
}

// 设置倒计时提醒
export async function scheduleCountdownNotification(
  eventId: string,
  targetDate: Date,
  daysBefore: number = 1
) {
  try {
    // 确保已初始化
    const hasPermission = await initNotifications();
    if (!hasPermission) {
      console.log('没有通知权限，无法设置通知');
      return false;
    }
    
    // 取消可能已存在的提醒
    await cancelNotification(`${eventId}_day_before`);
    await cancelNotification(`${eventId}_day_of`);
    
    // 计算前一天的日期（早上8点）
    const dayBeforeDate = new Date(targetDate);
    dayBeforeDate.setDate(dayBeforeDate.getDate() - daysBefore);
    dayBeforeDate.setHours(8, 0, 0, 0);
    
    // 计算当天的日期（早上8点）
    const dayOfDate = new Date(targetDate);
    dayOfDate.setHours(8, 0, 0, 0);
    
    // 如果日期已经过了，不设置通知
    const now = new Date();
    
    // 设置前一天的通知
    if (dayBeforeDate > now) {
      const dayBeforeId = await Notifications.scheduleNotificationAsync({
        content: {
          title: '预期日期提醒',
          body: `您设置的预期日期将在明天到达，请注意安排您的行程。`,
          sound: true,
          priority: Notifications.AndroidNotificationPriority.HIGH,
          categoryIdentifier: 'countdown',
        },
        trigger: {
          date: dayBeforeDate,
        },
      });
      
      // 保存通知ID
      await saveNotificationId(`${eventId}_day_before`, dayBeforeId);
    }
    
    // 设置当天的通知
    if (dayOfDate > now) {
      const dayOfId = await Notifications.scheduleNotificationAsync({
        content: {
          title: '预期日期已到',
          body: `今天是您设置的预期日期，请及时处理相关事项。`,
          sound: true,
          priority: Notifications.AndroidNotificationPriority.HIGH,
          categoryIdentifier: 'countdown',
        },
        trigger: {
          date: dayOfDate,
        },
      });
      
      // 保存通知ID
      await saveNotificationId(`${eventId}_day_of`, dayOfId);
    }
    
    return true;
  } catch (error) {
    console.error('设置倒计时通知失败:', error);
    return false;
  }
}

// 更新所有倒计时通知
export async function updateAllCountdownNotifications(records: any[]) {
  try {
    // 确保已初始化
    const hasPermission = await initNotifications();
    if (!hasPermission) {
      console.log('没有通知权限，无法更新通知');
      return false;
    }
    
    // 取消所有已存在的通知
    await cancelAllNotifications();
    
    // 处理记录为空的情况
    if (!records || records.length === 0) {
      return true;
    }
    
    // 按日期升序排序记录
    const sortedRecords = [...records].sort((a, b) => {
      const dateA = new Date(a.date);
      const dateB = new Date(b.date);
      return dateA.getTime() - dateB.getTime();
    });
    
    // 计算平均间隔
    let totalIntervals = 0;
    let intervalCount = 0;
    
    sortedRecords.forEach(record => {
      if (record.daysSinceLastRecord) {
        totalIntervals += record.daysSinceLastRecord;
        intervalCount++;
      }
    });
    
    const avgInterval = intervalCount > 0
      ? Math.round(totalIntervals / intervalCount)
      : 30; // 默认30天
    
    // 获取最近一条记录
    const lastRecord = sortedRecords[sortedRecords.length - 1];
    const lastDate = new Date(lastRecord.date);
    
    // 计算预期日期
    const expectedDate = new Date(lastDate);
    expectedDate.setDate(expectedDate.getDate() + avgInterval);
    
    // 为预期日期设置通知
    await scheduleCountdownNotification(lastRecord.id, expectedDate);
    
    return true;
  } catch (error) {
    console.error('更新所有倒计时通知失败:', error);
    return false;
  }
}
