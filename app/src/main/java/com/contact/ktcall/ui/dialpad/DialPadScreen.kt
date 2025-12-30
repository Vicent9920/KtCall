import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.Divider
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// --- 1. 常量定义 (复刻三星绿) ---
val SamsungGreen = Color(0xFF00C853) // 更鲜艳的绿色
val DividerColor = Color(0xFFE0E0E0)
val TextPrimary = Color(0xFF212121)
val TextSecondary = Color(0xFF757575)

// --- 2. 数据模型 ---
data class Contact(
    val id: String,
    val phoneNumber: String,
    val secondaryInfo: String,
    val avatarColor: Color
)

fun getDummyContacts(): List<Contact> {
    val colors = listOf(
        Color(0xFF9FA8DA), Color(0xFFEF9A9A), Color(0xFFFFCC80),
        Color(0xFFA5D6A7), Color(0xFFCE93D8)
    )
    val list = mutableListOf<Contact>()
    // 模拟视频中的大量数据
    val prefixes = listOf("11111111", "11111755", "11111333", "11152541", "12211452")
    for (i in 0..50) {
        val prefix = prefixes[i % prefixes.size]
        val suffix = (1000..9999).random()
        list.add(
            Contact(
                id = i.toString(),
                phoneNumber = "$prefix$suffix",
                secondaryInfo = "00:15  101 193 1000..9999", // 模拟视频中的详细信息格式
                avatarColor = colors[i % colors.size]
            )
        )
    }
    return list
}

// --- 主界面 DialPadScreen ---
@Composable
fun DialPadScreen(
    onCallClick: (String) -> Unit,
    onSearchStateChanged: (Boolean) -> Unit
) {
    var inputNumber by remember { mutableStateOf("") }
    var isDialPadExpanded by remember { mutableStateOf(true) }

    val allContacts = remember { getDummyContacts() }

    val searchResults = remember(inputNumber) {
        if (inputNumber.isEmpty()) emptyList()
        else allContacts.filter { it.phoneNumber.contains(inputNumber) }
    }

    LaunchedEffect(inputNumber) {
        val isSearching = inputNumber.isNotEmpty()
        onSearchStateChanged(isSearching)
        if (isSearching) {
            isDialPadExpanded = true
        }
    }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isDialPadExpanded && source == NestedScrollSource.Drag) {
                    isDialPadExpanded = false
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // --- 层级 A: 列表与顶部栏 ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 76.dp)
                // [修复顶部距离核心]：
                // 添加 statusBarsPadding，让内容自动避让状态栏高度。
                // 同时也处理 displayCutout (刘海屏区域)，防止内容被刘海遮挡。
//                .windowInsetsPadding(WindowInsets.statusBars.union(WindowInsets.displayCutout))
        ) {

            // 顶部工具栏
            AnimatedVisibility(
                visible = inputNumber.isNotEmpty(),
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                TopActionToolbar()
            }

            // 搜索列表
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .nestedScroll(nestedScrollConnection),
                // 底部 Padding 策略：
                // 展开时：留出键盘高度 (约420dp)
                // 收起时：留出 FAB 高度 + 导航栏高度 (约100dp)
                contentPadding = PaddingValues(bottom = if (isDialPadExpanded) 420.dp else 0.dp)
            ) {
                items(searchResults) { contact ->
                    SearchContactItem(contact, inputNumber)
                }
            }
        }

        // --- 层级 B: 拨号键盘面板 ---
        AnimatedVisibility(
            visible = isDialPadExpanded,
            // 调整动画 Spec，让其更跟手
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .background(Color.White)
                    // [修复底部距离核心]：
                    // navigationBarsPadding 确保内容不被手势横条遮挡
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                InputDisplayArea(number = inputNumber)

                DialPadGrid(
                    onDigitClick = { digit ->
                        if (inputNumber.length < 50) inputNumber += digit
                    }
                )

                ActionRow(
                    inputNumber = inputNumber,
                    onCallClick = { if (inputNumber.isNotEmpty()) onCallClick(inputNumber) },
                    onDeleteClick = { if (inputNumber.isNotEmpty()) inputNumber = inputNumber.dropLast(1) },
                    onDeleteLongClick = { inputNumber = "" }
                )

                // [视觉优化]：在 NavigationBar 上方再增加一点点空白，避免按钮贴底太紧
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // --- 层级 C: FAB ---
        AnimatedVisibility(
            visible = !isDialPadExpanded,
            enter = scaleIn(animationSpec = tween(200, delayMillis = 100)) + fadeIn(),
            exit = scaleOut(animationSpec = tween(200)) + fadeOut(),
            modifier = Modifier
                // 改为右下角 (根据 Image_146a85.png 来看，有些三星版本是在右下角，有些是底部居中，这里按照您的录屏代码保持右下角，或者根据截图2改为底部居中)
                // 如果按照 Image_14698a.png (底部居中绿色按钮)，请使用 BottomCenter
                // 这里我按照截图2 (绿色小键盘按钮在底部中间) 进行调整：
                .align(Alignment.BottomCenter)
                .navigationBarsPadding() // 避让手势条
        ) {
            FloatingActionButton(
                onClick = { isDialPadExpanded = true },
                containerColor = SamsungGreen,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Dialpad, // 保持图标不变
                    contentDescription = "Open Keypad"
                )
            }
        }
    }
}

// --- 4. 组件细节实现 ---

@Composable
fun TopActionToolbar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 视频中顶部比较干净，这里保留功能按钮但调淡颜色
//        IconButton(onClick = { }) { Icon(Icons.Default.Task, contentDescription = null, tint = TextSecondary) }
        IconButton(onClick = { }) { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) }
        IconButton(onClick = { }) { Icon(Icons.Default.MoreVert, contentDescription = null, tint = TextSecondary) }
    }
}

@Composable
fun SearchContactItem(contact: Contact, highlight: String) {
    // 文本高亮逻辑
    val annotatedString = buildAnnotatedString {
        val str = contact.phoneNumber
        val index = str.indexOf(highlight)
        if (index >= 0) {
            append(str.substring(0, index))
            withStyle(style = SpanStyle(color = SamsungGreen, fontWeight = FontWeight.Bold)) {
                append(str.substring(index, index + highlight.length))
            }
            append(str.substring(index + highlight.length))
        } else {
            append(str)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp) // 增加一点高度，更像三星列表
            .clickable { /* 点击逻辑 */ }
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(contact.avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            // 使用高亮文本
            Text(
                text = annotatedString,
                style = TextStyle(fontSize = 18.sp, color = TextPrimary)
            )
            Text(
                text = contact.secondaryInfo,
                style = TextStyle(fontSize = 13.sp, color = TextSecondary),
                maxLines = 1
            )
        }
    }
}

@Composable
fun InputDisplayArea(number: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp) // 稍微加高
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                // 点击输入框通常不会有反应，或者光标控制
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            style = TextStyle(
                fontSize = 34.sp, // 大字体
                color = TextPrimary,
                textAlign = TextAlign.Center
            ),
            maxLines = 1
        )
    }
}

@Composable
fun DialPadGrid(onDigitClick: (String) -> Unit) {
    val keys = listOf(
        "1" to "", "2" to "ABC", "3" to "DEF",
        "4" to "GHI", "5" to "JKL", "6" to "MNO",
        "7" to "PQRS", "8" to "TUV", "9" to "WXYZ",
        "*" to "", "0" to "+", "#" to ""
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 36.dp) // 两侧边距调整
    ) {
        for (i in 0 until 4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (j in 0 until 3) {
                    val index = i * 3 + j
                    val (num, sub) = keys[index]
                    DialButton(
                        number = num,
                        subText = sub,
                        modifier = Modifier.weight(1f),
                        onClick = { onDigitClick(num) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp)) // 行间距
        }
    }
}

@Composable
fun DialButton(
    number: String,
    subText: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(64.dp) // 按钮高度
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = Color.LightGray), // 灰色涟漪
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = number,
                style = TextStyle(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Normal, // 三星的数字不是粗体
                    color = TextPrimary
                )
            )
            if (subText.isNotEmpty() || number == "0") {
                Text(
                    text = subText,
                    style = TextStyle(
                        fontSize = 11.sp, // 字母更小
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    ),
                    modifier = Modifier.offset(y = (-2).dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActionRow(
    inputNumber: String,
    onCallClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDeleteLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = 48.dp), // 操作栏内缩
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 左侧留白或视频通话按钮
        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            if (inputNumber.isEmpty()) {
                // 没数字时显示视频通话或其他
            } else {
                Icon(
                    imageVector = Icons.Default.VideoCall,
                    contentDescription = "Video",
                    tint = SamsungGreen,
                    modifier = Modifier.size(30.dp)
                )
            }
        }

        // 呼叫按钮 (大绿圆)
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(SamsungGreen)
                .clickable { onCallClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = "Call",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        // 删除按钮
        Box(
            modifier = Modifier.size(56.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = inputNumber.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = "Delete",
                    tint = TextSecondary, // 灰色删除键
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(bounded = false),
                            onClick = onDeleteClick,
                            onLongClick = onDeleteLongClick
                        )
                )
            }
        }
    }
}