package com.chouchou.ipredict

import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
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

    override fun onCreate(savedInstanceState: Bundle?) {
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
        val navController = navHostFragment.navController

        // 设置顶部标题栏将显示的页面
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.countdownFragment, R.id.datesFragment, R.id.analysisFragment
            ),
            drawerLayout // 添加抽屉布局作为参数
        )

        setupActionBarWithNavController(navController, appBarConfiguration)

        // 创建导航动画
        val navBuilder = NavOptions.Builder()
            .setEnterAnim(R.anim.nav_enter_anim)
            .setExitAnim(R.anim.nav_exit_anim)
            .setPopEnterAnim(R.anim.nav_pop_enter_anim)
            .setPopExitAnim(R.anim.nav_pop_exit_anim)
            .build()

        // 设置底部导航项的点击监听
        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.countdownFragment -> navController.navigate(R.id.countdownFragment, null, navBuilder)
                R.id.datesFragment -> navController.navigate(R.id.datesFragment, null, navBuilder)
                R.id.analysisFragment -> navController.navigate(R.id.analysisFragment, null, navBuilder)
            }
            true
        }

        // 保持与底部导航同步
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.navView.menu.findItem(destination.id)?.isChecked = true
        }

        // 设置抽屉菜单
        setupDrawerMenu()
    }

    private fun setThemeMode(mode: Int) {
        // 记录当前抽屉状态
        val wasDrawerOpen = drawerLayout.isDrawerOpen(GravityCompat.START)

        // 设置标志，防止Activity重建
        delegate.localNightMode = mode

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

        navDrawer.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_theme_light -> {
                    setThemeMode(AppCompatDelegate.MODE_NIGHT_NO)
                    Snackbar.make(binding.root, R.string.theme_light, Snackbar.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_theme_dark -> {
                    setThemeMode(AppCompatDelegate.MODE_NIGHT_YES)
                    Snackbar.make(binding.root, R.string.theme_dark, Snackbar.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                R.id.nav_theme_system -> {
                    setThemeMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    Snackbar.make(binding.root, R.string.theme_system, Snackbar.LENGTH_SHORT).show()
                    drawerLayout.closeDrawer(GravityCompat.START)
                    true
                }
                else -> false
            }
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