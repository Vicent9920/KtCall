package com.contact.ktcall

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.contact.ktcall.ui.ContactsScreen
import com.contact.ktcall.ui.contact.Contact3Screen


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
@Composable
fun DialerApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { DialerBottomNavigation(navController) }
    ) { innerPadding ->
        // 导航主机
        NavHost(
            navController = navController,
            startDestination = Screen.DialPad.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            // 页面 1: 拨号盘
            composable(Screen.DialPad.route) {
                DialPadScreen(
                    onCallClick = { number ->
                        navController.navigate(Screen.InCall.createRoute(number))
                    }
                )
            }
            // 页面 2: 通话记录
            composable(Screen.CallLog.route) {
                CallLogScreen {

                }
            }
            // 页面 3: 联系人
            composable(Screen.Contacts.route) {
                Contact3Screen()
            }
            // 页面 4: 通话中 (隐藏页面)
            composable(Screen.InCall.route) { backStackEntry ->
                val number = backStackEntry.arguments?.getString("number") ?: "未知号码"
                InCallPlaceholder(
                    number = number,
                    onEndCall = { navController.popBackStack() }
                )
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


// 定义按键数据结构
data class DialKey(val digit: String, val subText: String = "")

// 键盘数据源
val dialKeys = listOf(
    DialKey("1", "  "), DialKey("2", "ABC"), DialKey("3", "DEF"),
    DialKey("4", "GHI"), DialKey("5", "JKL"), DialKey("6", "MNO"),
    DialKey("7", "PQRS"), DialKey("8", "TUV"), DialKey("9", "WXYZ"),
    DialKey("*", ""), DialKey("0", "+"), DialKey("#", "")
)


@Composable
fun DialPadScreen(onCallClick: (String) -> Unit) {
    // 状态管理：当前输入的号码
    var phoneNumber by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 号码显示区域 (占据剩余空间，让内容垂直居中或偏下)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Text(
                text = phoneNumber,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = if (phoneNumber.length > 10) 32.sp else 48.sp // 动态字体大小
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }

        // 2. 键盘区域
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(horizontal = 24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(dialKeys) { key ->
                DialPadButton(
                    key = key,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) // 震动反馈
                        phoneNumber += key.digit
                    },
                    onLongClick = {
                        // 长按 0 输入 +
                        if (key.digit == "0") {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            phoneNumber += "+"
                        }
                    }
                )
            }
        }

        // 3. 底部操作栏 (拨号 & 删除)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .height(80.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 左侧占位 (为了让拨号键居中)
            Spacer(modifier = Modifier.size(64.dp))

            // 拨号按钮
            FilledIconButton(
                onClick = { onCallClick(phoneNumber) },
                modifier = Modifier.size(64.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    Icons.Default.Call,
                    contentDescription = "拨打",
                    modifier = Modifier.size(32.dp),
                    tint = Color.White
                )
            }

            // 删除按钮 (仅当有输入时显示)
            Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                if (phoneNumber.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            phoneNumber = phoneNumber.dropLast(1)
                        }
                    ) {
                        Icon(
                            painter = painterResource(id = R.mipmap.ic_delete),
                            contentDescription = "删除",
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 底部留白，适配 BottomNavigation
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialPadButton(
    key: DialKey,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // 简单的圆形按键布局
    Box(
        modifier = Modifier.run {
            clip(CircleShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(
                        bounded = true,
                        color = Color.LightGray
                    )
                )
                .padding(vertical = 8.dp)
        },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = key.digit,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Normal
            )
            if (key.subText.isNotEmpty()) {
                Text(
                    text = key.subText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            } else {
                Spacer(modifier = Modifier.height(12.dp)) // 保持高度一致
            }
        }
    }
}

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