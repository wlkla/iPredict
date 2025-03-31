package com.chouchou.ipredict

import android.graphics.drawable.GradientDrawable
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.chouchou.ipredict.data.EventType
import com.chouchou.ipredict.databinding.ActivityMainBinding
import com.chouchou.ipredict.databinding.DialogAddEventTypeBinding
import com.chouchou.ipredict.ui.event.EventTypeViewModel
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController
    private lateinit var eventTypeViewModel: EventTypeViewModel

    // 添加一个变量来跟踪当前页面ID
    private var currentFragmentId = R.id.countdownFragment

    // 预定义颜色列表
    private val colorOptions = listOf(
        "#000000", // 黑色
        "#FF5252", // 红色
        "#FF4081", // 粉色
        "#E040FB", // 紫色
        "#7C4DFF", // 深紫色
        "#536DFE", // 靛蓝
        "#448AFF", // 蓝色
        "#40C4FF", // 浅蓝
        "#18FFFF", // 青色
        "#64FFDA", // 蓝绿色
        "#69F0AE", // 绿色
        "#B2FF59", // 浅绿色
        "#EEFF41", // 酸橙色
        "#FFFF00", // 黄色
        "#FFD740", // 琥珀色
        "#FFAB40", // 橙色
        "#FF6E40"  // 深橙色
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPrefs = getSharedPreferences("ipredict_prefs", Context.MODE_PRIVATE)
        val savedThemeMode = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedThemeMode)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化ViewModel
        eventTypeViewModel = ViewModelProvider(this).get(EventTypeViewModel::class.java)

        // 设置抽屉布局
        drawerLayout = binding.drawerLayout
        drawerToggle = ActionBarDrawerToggle(
            this, drawerLayout,
            R.string.nav_drawer_open, R.string.nav_drawer_close
        )
        drawerLayout.addDrawerListener(drawerToggle)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

        // 获取NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment_activity_main) as NavHostFragment
        navController = navHostFragment.navController

        // 设置顶部标题栏将显示的页面
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.countdownFragment, R.id.datesFragment, R.id.analysisFragment
            ),
            drawerLayout // 添加抽屉布局作为参数
        )

        setupActionBarWithNavController(navController, appBarConfiguration)

        // 设置底部导航项的点击监听
        binding.navView.setOnItemSelectedListener { item ->
            navigateToDestination(item.itemId)
            true
        }

        // 保持与底部导航同步
        navController.addOnDestinationChangedListener { _, destination, _ ->
            currentFragmentId = destination.id
            binding.navView.menu.findItem(destination.id)?.isChecked = true
        }

        // 设置抽屉菜单
        setupDrawerMenu()

        // 观察事件类型变化
        observeEventTypes()
    }

    // 观察事件类型变化并更新菜单
    private fun observeEventTypes() {
        eventTypeViewModel.allEventTypes.observe(this) { eventTypes ->
            updateEventTypesMenu(eventTypes)
        }
    }

    // 更新事件类型菜单
    private fun updateEventTypesMenu(eventTypes: List<EventType>) {
        val navMenu = binding.navDrawer.menu

        // 清除现有的事件类型项
        val itemsToRemove = ArrayList<Int>()
        for (i in 0 until navMenu.size()) {
            val item = navMenu.getItem(i)
            if (item.itemId >= EVENT_TYPE_ID_START && item.itemId < EVENT_TYPE_ID_START + 1000) {
                itemsToRemove.add(item.itemId)
            }
        }
        // 移除收集的项目
        for (itemId in itemsToRemove) {
            navMenu.removeItem(itemId)
        }

        // 获取事件类型标题位置
        val headerPosition = findMenuItemPosition(navMenu, R.id.nav_events_header)
        val addEventPosition = findMenuItemPosition(navMenu, R.id.nav_add_event)

        if (headerPosition != -1 && addEventPosition != -1) {
            // 添加事件类型菜单项
            var index = headerPosition + 1

            // 创建子菜单组（如果需要）
            // 获取 events_header 项
            val eventsHeader = navMenu.findItem(R.id.nav_events_header)

            // 为每个事件类型添加菜单项
            // 在 MainActivity.kt 的 updateEventTypesMenu 方法中添加

            for (eventType in eventTypes) {
                val itemId = EVENT_TYPE_ID_START + eventType.id
                val menuItem = navMenu.add(R.id.nav_group_events, itemId, index, eventType.name)

                // 尝试从事件类型的颜色创建圆形图标
                try {
                    val circleDrawable = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.parseColor(eventType.color))
                        setSize(24, 24) // 适当的大小
                    }
                    menuItem.setIcon(circleDrawable)
                } catch (e: Exception) {
                    // 如果颜色解析失败，使用默认图标
                    menuItem.setIcon(R.drawable.ic_calendar)
                }

                // 如果是当前活动项，则设置为选中状态
                menuItem.isChecked = eventType.isActive

                // 为菜单项添加标识，用于后续查找
                menuItem.actionView = View(this).apply {
                    id = View.generateViewId()
                    tag = eventType.id // 保存事件类型ID作为标签
                    isClickable = true

                    // 设置长按监听器
                    // 修改长按监听器代码
                    setOnLongClickListener {
                        val typeId = it.tag as Int
                        val eventType = eventTypeViewModel.allEventTypes.value?.find { type -> type.id == typeId }
                        if (eventType != null) {
                            showEventTypeOptionsMenu(it, eventType)
                        }
                        true
                    }
                }

                index++
            }
        }

        // 设置事件类型菜单项的点击监听
        binding.navDrawer.setNavigationItemSelectedListener { menuItem ->
            when {
                menuItem.itemId == R.id.nav_theme_light -> {
                    setThemeMode(AppCompatDelegate.MODE_NIGHT_NO)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                menuItem.itemId == R.id.nav_theme_dark -> {
                    setThemeMode(AppCompatDelegate.MODE_NIGHT_YES)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                menuItem.itemId == R.id.nav_theme_system -> {
                    setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                menuItem.itemId == R.id.nav_add_event -> {
                    showAddEventTypeDialog()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                menuItem.itemId >= EVENT_TYPE_ID_START && menuItem.itemId < EVENT_TYPE_ID_START + 1000 -> {
                    val eventTypeId = menuItem.itemId - EVENT_TYPE_ID_START
                    activateEventType(eventTypeId)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                menuItem.itemId == R.id.nav_theme_group -> {
                    // 返回true表示已处理此事件
                    true
                }
                else -> false
            }
        }
    }

    // 查找菜单项的位置
    private fun findMenuItemPosition(menu: android.view.Menu, itemId: Int): Int {
        for (i in 0 until menu.size()) {
            if (menu.getItem(i).itemId == itemId) {
                return i
            }
        }
        return -1
    }

    // 激活事件类型
    private fun activateEventType(eventTypeId: Int) {
        eventTypeViewModel.setActiveEventType(eventTypeId)
        // 刷新当前页面数据
        refreshCurrentFragment()
    }

    // 刷新当前Fragment
    private fun refreshCurrentFragment() {
        val navOptions = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .build()
        navController.navigate(currentFragmentId, null, navOptions)
    }

    // 显示添加事件类型对话框
    private fun showAddEventTypeDialog() {
        val dialogBinding = DialogAddEventTypeBinding.inflate(LayoutInflater.from(this))
        var selectedColor = colorOptions[0]

        // 设置颜色选项
        val colorOptionsLayout = dialogBinding.colorOptions
        colorOptionsLayout.removeAllViews()

        for (color in colorOptions) {
            val colorView = LayoutInflater.from(this).inflate(R.layout.item_color_option, colorOptionsLayout, false)
            val colorCircle = colorView.findViewById<View>(R.id.color_circle)
            val checkMark = colorView.findViewById<ImageView>(R.id.check_mark)

            try {
                colorCircle.setBackgroundColor(Color.parseColor(color))
            } catch (e: Exception) {
                colorCircle.setBackgroundColor(Color.BLACK)
            }

            if (color == selectedColor) {
                checkMark.visibility = View.VISIBLE
            }

            colorView.setOnClickListener {
                // 更新所有颜色选项的选中状态
                for (i in 0 until colorOptionsLayout.childCount) {
                    val child = colorOptionsLayout.getChildAt(i)
                    child.findViewById<ImageView>(R.id.check_mark).visibility = View.GONE
                }

                checkMark.visibility = View.VISIBLE
                selectedColor = color
            }

            colorOptionsLayout.addView(colorView)
        }

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = dialogBinding.editEventName.text.toString().trim()
                if (name.isNotEmpty()) {
                    eventTypeViewModel.addEventType(name, selectedColor)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // 在 MainActivity 类中添加这个方法

    private fun showDeleteEventTypeDialog(typeId: Int, typeName: String) {
        // 不允许删除最后一个事件类型
        eventTypeViewModel.allEventTypes.value?.let { types ->
            if (types.size <= 1) {
                Snackbar.make(
                    binding.root,
                    "无法删除唯一的事件类型",
                    Snackbar.LENGTH_LONG
                ).show()
                return
            }
        }

        // 显示确认对话框
        AlertDialog.Builder(this)
            .setTitle("删除事件类型")
            .setMessage("确定要删除事件类型\"$typeName\"吗？这将删除所有相关记录。")
            .setPositiveButton("删除") { _, _ ->
                // 执行删除操作
                deleteEventType(typeId)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 显示事件类型操作菜单
    private fun showEventTypeOptionsMenu(view: View, eventType: EventType) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.event_type_options, popup.menu)

        // 如果只有一个事件类型，禁用删除选项
        eventTypeViewModel.allEventTypes.value?.let { types ->
            if (types.size <= 1) {
                popup.menu.findItem(R.id.action_delete_event_type).isEnabled = false
            }
        }

        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit_event_type -> {
                    showEditEventTypeDialog(eventType)
                    true
                }
                R.id.action_delete_event_type -> {
                    showDeleteEventTypeDialog(eventType.id, eventType.name)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // 显示编辑事件类型对话框
    private fun showEditEventTypeDialog(eventType: EventType) {
        val dialogBinding = DialogAddEventTypeBinding.inflate(layoutInflater)
        var selectedColor = eventType.color

        // 设置原有的事件类型名称
        dialogBinding.editEventName.setText(eventType.name)
        dialogBinding.dialogTitle.text = getString(R.string.edit_event_type)

        // 设置颜色选项
        val colorOptionsLayout = dialogBinding.colorOptions
        colorOptionsLayout.removeAllViews()

        for (color in colorOptions) {
            val colorView = LayoutInflater.from(this).inflate(R.layout.item_color_option, colorOptionsLayout, false)
            val colorCircle = colorView.findViewById<View>(R.id.color_circle)
            val checkMark = colorView.findViewById<ImageView>(R.id.check_mark)

            try {
                colorCircle.setBackgroundColor(Color.parseColor(color))
            } catch (e: Exception) {
                colorCircle.setBackgroundColor(Color.BLACK)
            }

            // 选中当前事件类型使用的颜色
            if (color == selectedColor) {
                checkMark.visibility = View.VISIBLE
            }

            colorView.setOnClickListener {
                // 更新所有颜色选项的选中状态
                for (i in 0 until colorOptionsLayout.childCount) {
                    val child = colorOptionsLayout.getChildAt(i)
                    child.findViewById<ImageView>(R.id.check_mark).visibility = View.GONE
                }

                checkMark.visibility = View.VISIBLE
                selectedColor = color
            }

            colorOptionsLayout.addView(colorView)
        }

        AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val name = dialogBinding.editEventName.text.toString().trim()
                if (name.isNotEmpty()) {
                    // 更新事件类型
                    val updatedEventType = EventType(eventType.id, name, eventType.isActive, selectedColor)
                    eventTypeViewModel.updateEventType(updatedEventType)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // 删除事件类型的方法
    private fun deleteEventType(typeId: Int) {
        // 查找要删除的事件类型
        eventTypeViewModel.allEventTypes.value?.find { it.id == typeId }?.let { eventType ->
            // 删除事件类型
            eventTypeViewModel.deleteEventType(eventType)

            // 如果删除的是当前活动的事件类型，则选择另一个事件类型作为活动
            if (eventType.isActive) {
                eventTypeViewModel.allEventTypes.value?.firstOrNull { it.id != typeId }?.let {
                    eventTypeViewModel.setActiveEventType(it.id)
                }
            }

            // 显示成功消息
            Snackbar.make(
                binding.root,
                "已删除事件类型: ${eventType.name}",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    // 根据目标页面和当前页面关系来决定导航动画
    private fun navigateToDestination(destinationId: Int) {
        // 如果目标页面就是当前页面，不进行导航
        if (destinationId == currentFragmentId) {
            return
        }

        // 根据目标页面和当前页面的相对位置，确定导航动画
        val currentIndex = getFragmentIndex(currentFragmentId)
        val destinationIndex = getFragmentIndex(destinationId)

        val navBuilder = NavOptions.Builder()

        if (destinationIndex > currentIndex) {
            // 向右移动：当前页面淡出并左移，新页面从右侧滑入
            navBuilder.setEnterAnim(R.anim.slide_right_enter)
                .setExitAnim(R.anim.slide_right_exit)
                .setPopEnterAnim(R.anim.slide_left_enter)
                .setPopExitAnim(R.anim.slide_left_exit)
        } else {
            // 向左移动：当前页面淡出并右移，新页面从左侧滑入
            navBuilder.setEnterAnim(R.anim.slide_left_enter)
                .setExitAnim(R.anim.slide_left_exit)
                .setPopEnterAnim(R.anim.slide_right_enter)
                .setPopExitAnim(R.anim.slide_right_exit)
        }

        // 执行导航
        navController.navigate(destinationId, null, navBuilder.build())
    }

    // 获取Fragment在导航栏中的索引位置
    private fun getFragmentIndex(fragmentId: Int): Int {
        return when (fragmentId) {
            R.id.countdownFragment -> 0
            R.id.datesFragment -> 1
            R.id.analysisFragment -> 2
            else -> -1
        }
    }

    private fun setThemeMode(mode: Int) {
        // 记录当前抽屉状态
        val wasDrawerOpen = drawerLayout.isDrawerOpen(GravityCompat.START)

        // 保存主题设置
        val sharedPrefs = getSharedPreferences("ipredict_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putInt("theme_mode", mode).apply()

        // 设置标志，防止Activity重建
        delegate.localNightMode = mode
        AppCompatDelegate.setDefaultNightMode(mode)

        // 更新选中的菜单项
        updateSelectedThemeMenuItem()

        // 如果抽屉之前是打开的，保持它打开
        if (wasDrawerOpen) {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        // 显示提示消息
        val messageResId = when(mode) {
            AppCompatDelegate.MODE_NIGHT_YES -> R.string.theme_dark
            AppCompatDelegate.MODE_NIGHT_NO -> R.string.theme_light
            else -> R.string.theme_system
        }
        Snackbar.make(binding.root, messageResId, Snackbar.LENGTH_SHORT).show()
    }

    private fun setupDrawerMenu() {
        // 根据当前主题模式选中相应的菜单项
        updateSelectedThemeMenuItem()
    }

    private fun updateSelectedThemeMenuItem() {
        val navDrawer: NavigationView = binding.navDrawer
        val menu = navDrawer.menu

        // 根据当前主题模式选中相应的菜单项
        when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_NO ->
                menu.findItem(R.id.nav_theme_light)?.isChecked = true
            AppCompatDelegate.MODE_NIGHT_YES ->
                menu.findItem(R.id.nav_theme_dark)?.isChecked = true
            else ->
                menu.findItem(R.id.nav_theme_system)?.isChecked = true
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        // 同步抽屉状态
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onSupportNavigateUp(): Boolean {
        // 处理导航按钮点击
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            drawerLayout.openDrawer(GravityCompat.START)
        }
        return true
    }

    override fun onBackPressed() {
        // 处理返回按钮
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        // 事件类型菜单项ID起始值
        private const val EVENT_TYPE_ID_START = 10000
    }
}