package com.chouchou.ipredict

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.chouchou.ipredict.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var drawerToggle: ActionBarDrawerToggle
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController: NavController

    // 添加一个变量来跟踪当前页面ID
    private var currentFragmentId = R.id.countdownFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPrefs = getSharedPreferences("ipredict_prefs", Context.MODE_PRIVATE)
        val savedThemeMode = sharedPrefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(savedThemeMode)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
        val navDrawer: NavigationView = binding.navDrawer

        // 设置菜单项选中时的处理逻辑
        navDrawer.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_theme_light -> {
                    setThemeMode(AppCompatDelegate.MODE_NIGHT_NO)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_theme_dark -> {
                    setThemeMode(AppCompatDelegate.MODE_NIGHT_YES)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_theme_system -> {
                    setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                // 父菜单项不需要特殊处理，由系统自动展开/折叠
                R.id.nav_theme_group -> {
                    // 返回true表示已处理此事件
                    true
                }
                else -> false
            }
        }

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
}