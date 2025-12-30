package com.contact.ktcall

import DialPadScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.contact.ktcall.ui.ContactsScreen


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
                CallLogScreen {} // 假设你已定义
            }
            composable(Screen.Contacts.route) {
                LaunchedEffect(Unit) { isBottomBarVisible = true }
                ContactsScreen()
            }
            composable(Screen.InCall.route) { backStackEntry ->
                LaunchedEffect(Unit) { isBottomBarVisible = false } // 通话中肯定不显示
                val number = backStackEntry.arguments?.getString("number") ?: ""
                InCallPlaceholder(number = number) { navController.popBackStack() }
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

// --- 5. 页面占位符 (Placeholders) ---


// 5.1 数据模型
enum class CallType { INCOMING, OUTGOING, MISSED, REJECTED }

data class CallLogEntry(
    val id: String,
    val name: String?,
    val number: String,
    val type: CallType,
    val time: String,
    val date: String
)

// 5.2 模拟数据
val mockCallLogs = listOf(
    CallLogEntry("1", "快递小哥", "13800138000", CallType.MISSED, "10:30", "今天"),
    CallLogEntry("2", "爸爸", "13912345678", CallType.INCOMING, "09:15", "今天"),
    CallLogEntry("3", null, "021-12345678", CallType.OUTGOING, "昨天", "昨天"),
    CallLogEntry("4", "Boss", "18888888888", CallType.INCOMING, "昨天", "昨天"),
    CallLogEntry("5", "诈骗电话", "400-800-8888", CallType.MISSED, "周一", "周一"),
    CallLogEntry("6", "外卖", "15000000000", CallType.INCOMING, "周一", "周一"),
    CallLogEntry("7", "妈妈", "13700000000", CallType.OUTGOING, "周日", "周日"),
)

// 5.3 页面实现
@Composable
fun CallLogScreen(onItemClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 顶部标题区域
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "最近记录",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // 列表区域
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(mockCallLogs) { log ->
                CallLogItemRow(log = log, onClick = { onItemClick(log.number) })
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp), // 三星风格：分割线不通栏
                    thickness = 0.5.dp,
                    color = Color.LightGray.copy(alpha = 0.2f)
                )
            }
        }
    }
}

// 5.4 列表项组件
@Composable
fun CallLogItemRow(log: CallLogEntry, onClick: () -> Unit) {
    val isMissed = log.type == CallType.MISSED
    val textColor = if (isMissed) Color(0xFFFF4444) else Color.Black

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 头像
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isMissed) Color(0xFFFF4444).copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = if (isMissed) Color(0xFFFF4444) else MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 中间信息
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = log.name ?: log.number,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 通话类型图标
                val typeIcon = when (log.type) {
                    CallType.INCOMING -> R.mipmap.ic_call_in
                    CallType.OUTGOING -> R.mipmap.ic_call_out
                    CallType.MISSED -> R.mipmap.ic_call_missed
                    CallType.REJECTED -> R.mipmap.ic_call_rejected
                }
                Icon(
                    painterResource(typeIcon),
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                // 如果有名字，这里显示号码，否则显示归属地(模拟)
                Text(
                    text = if (log.name != null) "手机" else "上海",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }

        // 右侧时间
        Text(
            text = log.time,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Composable
fun SimplePlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineLarge, color = Color.LightGray)
    }
}

@Composable
fun InCallPlaceholder(number: String, onEndCall: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF2C3E50)), // 深色背景模拟通话界面
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.White
            )
            Spacer(Modifier.height(16.dp))
            Text(number, style = MaterialTheme.typography.headlineLarge, color = Color.White)
            Text(
                "正在通话 00:05",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            IconButton(onClick = {}) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_mute),
                    null,
                    tint = Color.White
                )
            }
            // 挂断按钮
            FloatingActionButton(
                onClick = onEndCall,
                containerColor = Color.Red,
                shape = CircleShape
            ) {
                Image(painterResource(id = R.mipmap.ic_hangup), contentDescription = "挂断")
            }
            IconButton(onClick = {}) {
                Icon(
                    painterResource(id = R.drawable.ic_speak),
                    null,
                    tint = Color.White
                )
            }
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