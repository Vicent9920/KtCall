package com.contact.ktcall.ui

import DialPadScreen
import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.blankj.utilcode.util.BarUtils
import com.blankj.utilcode.util.LogUtils
import com.contact.ktcall.R
import com.contact.ktcall.ui.screen.calling.CallingScreen
import com.contact.ktcall.ui.screen.calllog.CallLogScreen


// --- 1. 路由定义 (Route Definition) ---
// 使用 Sealed Class 定义所有页面，保证类型安全
sealed class Screen(val route: String, val title: String, val icon: Int?) {
    object DialPad : Screen("dialpad", "键盘", R.drawable.ic_bottom_dial)
    object CallLog : Screen("calllog", "最近记录", R.drawable.ic_bottom_log)
    object Contacts : Screen("contacts", "联系人", R.drawable.ic_bottom_person)

    // 通话页面不需要图标，因为它不显示在底部导航栏
    object InCall : Screen("incall/{number}", "通话中", null) {
        fun createRoute(number: String) = "incall/$number"
    }
}

// 定义底部导航栏显示的页面
val bottomNavItems = listOf(
    Screen.DialPad,
    Screen.CallLog,
    Screen.Contacts
)

// --- 2. 主入口 (Main Entry) ---
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BarUtils.setStatusBarLightMode(this,true)
        setContent {
            SamsungDialerTheme {
                DialerApp()
            }
        }

    }

    override fun onResume() {
        super.onResume()

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Activity.RESULT_CANCELED
        LogUtils.e("onActivityResult: requestCode=$requestCode, resultCode=$resultCode")
    }

    private fun checkDefaultDialer() {
        val telecomManager = getSystemService(TELECOM_SERVICE) as TelecomManager
        val isAlreadyDefault = packageName == telecomManager.defaultDialerPackage

        if (isAlreadyDefault) {
            // 已经是默认拨号器
            return
        }

        // 发起请求意图
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10 (API 29) 及以上使用 RoleManager
            val roleManager = getSystemService(Context.ROLE_SERVICE) as RoleManager
            roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER)
        } else {
            // Android 9 及以下使用 TelecomManager
            Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
            }
        }
        startActivityForResult(intent, 1001)
    }
}

@Composable
fun SamsungDialerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2ECC71), // 三星绿
            secondary = Color(0xFF34495E),
            background = Color.White,
            surface = Color(0xFFF8F9FA)
        ),
        content = content
    )
}

// --- 3. 应用骨架 (App Scaffold) ---
// --- DialerApp 修改 ---
@Composable
fun DialerApp() {
    val navController = rememberNavController()
    // 1. 控制底部导航栏显示的状态
    var isBottomBarVisible by remember { mutableStateOf(true) }

    Scaffold(
        // 2. 根据状态决定是否显示底部导航
        bottomBar = {
            // 使用 AnimatedVisibility 让底部栏消失得更自然（可选，也可以直接用 if）
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                DialerBottomNavigation(navController)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.DialPad.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.DialPad.route) {
                // 3. 将状态回调传递给 DialPadScreen
                DialPadScreen(
                    onCallClick = { number ->
                        navController.navigate(Screen.InCall.createRoute(number))
                    },
                    // 当输入框不为空时，隐藏底部栏
                    onSearchStateChanged = { isSearching ->
                        isBottomBarVisible = !isSearching
                    }
                )
            }
            composable(Screen.CallLog.route) {
                // 进入其他页面时确保底部栏显示
                LaunchedEffect(Unit) { isBottomBarVisible = true }
                CallLogScreen()// 假设你已定义
            }
            composable(Screen.Contacts.route) {
                LaunchedEffect(Unit) { isBottomBarVisible = true }
                ContactsScreen()
            }
            composable(Screen.InCall.route) { backStackEntry ->
                LaunchedEffect(Unit) { isBottomBarVisible = false } // 通话中肯定不显示
                val number = backStackEntry.arguments?.getString("number") ?: ""
                CallingScreen(number = number) { navController.popBackStack() }
            }
        }
    }
}

// --- 4. 组件实现 (Components) ---

@Composable
fun DialerBottomNavigation(navController: NavHostController) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        bottomNavItems.forEach { screen ->
            val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true

            NavigationBarItem(
                icon = {
                    Icon(
                        painter = painterResource(id = screen.icon!!),
                        contentDescription = null
                    )
                },
                label = { Text(screen.title) },
                selected = isSelected,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                ),
                onClick = {
                    navController.navigate(screen.route) {
                        // 避免堆栈过深，点击底部导航时弹出到起始页
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // 避免重复点击重新创建
                        launchSingleTop = true
                        // 恢复状态
                        restoreState = true
                    }
                }
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SamsungDialerTheme {
        DialerApp()
    }
}