package com.contact.ktcall.ui

import DialPadScreen
import android.content.Intent
import android.os.Bundle
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
import com.contact.ktcall.R
import com.contact.ktcall.ui.screen.calling.InCallScreen
import com.contact.ktcall.ui.screen.calllog.CallLogScreen
import com.contact.ktcall.ui.screen.contact.ContactsScreen
import com.contact.ktcall.utils.callPhone


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
    private var hasCheckedDefaultDialer = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BarUtils.setStatusBarLightMode(this,true)
        setContent {
            SamsungDialerTheme {
                DialerApp()
            }
        }
        if (!hasCheckedDefaultDialer) {
            startActivity(Intent(this, DefaultDialerRequestActivity::class.java))
            hasCheckedDefaultDialer = true
        }
    }

    override fun onResume() {
        super.onResume()
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
                       number.callPhone()
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
                InCallScreen()
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