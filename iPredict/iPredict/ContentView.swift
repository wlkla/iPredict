import SwiftUI
import UserNotifications

struct ContentView: View {
    @StateObject private var reminderManager = ReminderManager()
    @State private var reminderText: String = "该处理你的事项了！"
    @State private var websiteURL: String = "https://www.apple.com"
    @State private var showingNextReminderTime = false
    
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: "bell.fill")
                .resizable()
                .scaledToFit()
                .frame(width: 60, height: 60)
                .foregroundColor(.blue)
            
            Text("iPredict 提醒应用")
                .font(.largeTitle)
                .fontWeight(.bold)
            
            Text("设置每天晚上9点的提醒，并在点击提醒后跳转到指定网站")
                .font(.body)
                .multilineTextAlignment(.center)
                .padding(.horizontal)
            
            Divider()
            
            VStack(alignment: .leading, spacing: 10) {
                Text("提醒内容:")
                    .font(.headline)
                
                TextField("输入提醒内容", text: $reminderText)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .padding(.bottom)
                
                Text("跳转网站URL:")
                    .font(.headline)
                
                TextField("输入网站URL", text: $websiteURL)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
            }
            .padding(.horizontal)
            
            HStack(spacing: 20) {
                Button(action: {
                    reminderManager.updateSettings(reminderText: reminderText, websiteURL: websiteURL)
                    reminderManager.scheduleReminder()
                    showingNextReminderTime = true
                }) {
                    Text("保存并设置提醒")
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(Color.blue)
                        .cornerRadius(10)
                }
                
                Button(action: {
                    reminderManager.updateSettings(reminderText: reminderText, websiteURL: websiteURL)
                    reminderManager.sendNotification()
                }) {
                    Text("测试提醒")
                        .fontWeight(.semibold)
                        .foregroundColor(.white)
                        .padding()
                        .frame(maxWidth: .infinity)
                        .background(Color.green)
                        .cornerRadius(10)
                }
            }
            .padding(.horizontal)
            
            if showingNextReminderTime {
                Text("下次提醒将在 \(reminderManager.nextReminderTimeString) 发送")
                    .font(.footnote)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding()
        .frame(minWidth: 400, minHeight: 500)
        .onAppear {
            reminderManager.requestNotificationPermission()
        }
    }
}

class ReminderManager: NSObject, ObservableObject, UNUserNotificationCenterDelegate {
    @Published var nextReminderTimeString: String = ""
    private var reminderText: String = "该处理你的事项了！"
    private var websiteURL: String = "https://www.apple.com"
    
    override init() {
        super.init()
        UNUserNotificationCenter.current().delegate = self
    }
    
    func updateSettings(reminderText: String, websiteURL: String) {
        self.reminderText = reminderText
        self.websiteURL = websiteURL
    }
    
    func requestNotificationPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            DispatchQueue.main.async {
                if granted {
                    print("通知权限已授予")
                    // 将应用注册为通知的接收者
                    NSApp.registerForRemoteNotifications()
                } else if let error = error {
                    print("通知权限错误: \(error)")
                    self.showNotificationSettingsAlert()
                } else {
                    print("用户拒绝了通知权限")
                    self.showNotificationSettingsAlert()
                }
            }
        }
    }
    
    func showNotificationSettingsAlert() {
        let alert = NSAlert()
        alert.messageText = "通知权限被禁用"
        alert.informativeText = "请在系统偏好设置>通知中允许iPredict发送通知"
        alert.alertStyle = .warning
        alert.addButton(withTitle: "打开系统设置")
        alert.addButton(withTitle: "稍后")
        
        let response = alert.runModal()
        if response == .alertFirstButtonReturn {
            // 打开系统通知设置
            if let url = URL(string: "x-apple.systempreferences:com.apple.preference.notifications") {
                NSWorkspace.shared.open(url)
            }
        }
    }
    
    func scheduleReminder() {
        // 取消所有现有通知
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
        
        // 获取当前日期组件
        let calendar = Calendar.current
        let now = Date()
        
        // 创建今天晚上9点的日期
        var components = calendar.dateComponents([.year, .month, .day], from: now)
        components.hour = 21
        components.minute = 0
        components.second = 0
        
        guard let todayAt9PM = calendar.date(from: components) else {
            print("无法创建日期对象")
            return
        }
        
        // 如果现在的时间已经过了今天晚上9点，则设置为明天晚上9点
        var targetDate = todayAt9PM
        if now > todayAt9PM {
            if let tomorrow = calendar.date(byAdding: .day, value: 1, to: todayAt9PM) {
                targetDate = tomorrow
            }
        }
        
        // 创建日期触发器
        let dateComponents = calendar.dateComponents([.hour, .minute], from: targetDate)
        let trigger = UNCalendarNotificationTrigger(dateMatching: dateComponents, repeats: true)
        
        // 创建通知内容
        let content = UNMutableNotificationContent()
        content.title = "提醒"
        content.body = reminderText
        content.sound = UNNotificationSound.default
        content.userInfo = ["url": websiteURL]
        
        // 创建通知请求
        let request = UNNotificationRequest(
            identifier: "dailyReminder",
            content: content,
            trigger: trigger
        )
        
        // 添加通知请求
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("设置通知错误: \(error)")
            } else {
                print("通知已设置")
            }
        }
        
        // 更新下次提醒时间显示
        let dateFormatter = DateFormatter()
        dateFormatter.dateFormat = "yyyy-MM-dd HH:mm"
        self.nextReminderTimeString = dateFormatter.string(from: targetDate)
    }
    
    func sendNotification() {
        let content = UNMutableNotificationContent()
        content.title = "提醒"
        content.body = reminderText
        content.sound = UNNotificationSound.default
        content.userInfo = ["url": websiteURL]
        
        let request = UNNotificationRequest(
            identifier: UUID().uuidString,
            content: content,
            trigger: nil
        )
        
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("发送通知错误: \(error)")
            } else {
                print("测试通知已发送")
            }
        }
    }
    
    // 处理通知点击
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        if let urlString = response.notification.request.content.userInfo["url"] as? String,
           let url = URL(string: urlString) {
            NSWorkspace.shared.open(url)
        }
        completionHandler()
    }
    
    // 确保通知可以在前台显示
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.alert, .sound])
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
