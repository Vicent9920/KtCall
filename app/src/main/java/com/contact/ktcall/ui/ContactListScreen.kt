package com.contact.ktcall.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlin.random.Random

// --- 数据模型保持不变 ---
data class Contact(
    val id: Int,
    val name: String,
    val phone: String,
    val color: Color
)

object MockData {
    // ... (保持原有的 MockData 代码不变)
    // 为了节省篇幅，这里假设 MockData.getContacts() 依然可用
    private val firstNames = listOf("赵", "钱", "孙", "李", "周", "吴", "郑", "王")
    private val lastNames = listOf("伟", "芳", "娜", "敏", "静", "秀英", "丽", "强")
    private val colors = listOf(Color(0xFFE57373), Color(0xFF64B5F6), Color(0xFFAED581), Color(0xFFFFB74D))

    fun getContacts(count: Int = 46): List<Contact> {
        return List(count) { id ->
            Contact(id, "${firstNames.random()}${lastNames.random()}", "138 0000 0000", colors.random())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen() {
    val contacts = remember { MockData.getContacts(46) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // 定义尺寸
    val headerHeight = 420.dp
    val toolbarHeight = 64.dp // 标准 Toolbar 高度

    val headerHeightPx = with(density) { headerHeight.toPx() }
    val toolbarHeightPx = with(density) { toolbarHeight.toPx() }
    // 折叠范围：列表滚动多少距离后，Toolbar 应该完全显示并吸顶
    val collapseRangePx = headerHeightPx - toolbarHeightPx

    // 1. 获取状态栏高度 (用于位移计算)
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val statusBarHeightPx = with(density) { statusBarPadding.toPx() }

    // 滚动计算
    val scrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) collapseRangePx
            else listState.firstVisibleItemScrollOffset.toFloat()
        }
    }

    val collapseFraction by remember {
        derivedStateOf { (scrollOffset / collapseRangePx).coerceIn(0f, 1f) }
    }

    // 优化透明度计算：让大标题消失得慢一点，小标题出现得快一点，避免中间出现真空期
    val headerContentAlpha by remember {
        // 0.8f 开始快速消失，保留更长时间的显示
        derivedStateOf { (1f - (collapseFraction / 0.8f)).coerceIn(0f, 1f) }
    }

    val backgroundColor = Color(0xFFF2F2F2)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // 白色圆角卡片背景
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = headerHeight - 24.dp),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = Color.White,
            tonalElevation = 2.dp
        ) {}

        // ---------------- 列表层 ----------------
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Header 区域
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight),
                    contentAlignment = Alignment.Center
                ) {
                    // 大标题与描述
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .graphicsLayer { alpha = headerContentAlpha } // 绑定优化后的透明度
                            .padding(bottom = 40.dp)
                    ) {
                        Text(
                            text = "电话",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${contacts.size} 个有手机号码的联系人",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                    }

                    // Header 内的操作按钮
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 32.dp) // 保持 32dp 避免被卡片遮挡
                            .graphicsLayer { alpha = headerContentAlpha },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {}) { Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black) }
                        IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Black) }
                        IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Black) }
                    }
                }
            }

            // 联系人列表
            itemsIndexed(contacts, key = { _, contact -> contact.id }) { index, contact ->
                val shape = when {
                    contacts.size == 1 -> RoundedCornerShape(24.dp)
                    index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    index == contacts.lastIndex -> RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
                    else -> RectangleShape
                }
                val showDivider = index != contacts.lastIndex
                ContactItem(contact = contact, shape = shape, showDivider = showDivider)
            }
        }

        // ---------------- Toolbar 层 (重点修复) ----------------

        // 计算 Toolbar 的位移
        // 这里的逻辑是：开始时让 Toolbar 处于下方，随着滚动向上移动，最终停在顶部 (0的位置)
        val toolbarTranslationY by remember {
            derivedStateOf {
                lerp(
                    start = collapseRangePx - statusBarHeightPx, // 减去状态栏高度，因为 TopAppBar 内部包含状态栏高度
                    stop = 0f,
                    fraction = collapseFraction
                )
            }
        }

        // Toolbar 背景透明度
        val toolbarContainerAlpha by remember {
            derivedStateOf {
                if (collapseFraction > 0.85f) {
                    ((collapseFraction - 0.85f) / 0.15f).coerceIn(0f, 1f)
                } else {
                    0f
                }
            }
        }

        // 判断是否完全折叠 (用于显示阴影)
        val isCollapsed by remember { derivedStateOf { collapseFraction > 0.99f } }

        TopAppBar(
            title = {
                // 优化：提前一点显示小标题 (0.7f)，实现淡入淡出的交叉过渡，更自然
                AnimatedVisibility(
                    visible = collapseFraction >= 0.7f,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        "电话",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            },
            actions = {
                // Actions 也跟随小标题一起显隐
                AnimatedVisibility(
                    visible = collapseFraction >= 0.7f,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row {
                        IconButton(onClick = {}) { Icon(Icons.Default.Add, contentDescription = null) }
                        IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = null) }
                        IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = null) }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                // 确保背景是纯白（带透明度控制），而不是由系统混色
                containerColor = Color.White.copy(alpha = toolbarContainerAlpha),
                scrolledContainerColor = Color.White,
                actionIconContentColor = Color.Black,
                titleContentColor = Color.Black
            ),
            // ★★★ 修复重点 1：移除 modifier 上的 statusBarsPadding() ★★★
            // TopAppBar 默认的 windowInsets 已经包含了 statusBars，重复添加会导致高度异常
            windowInsets = WindowInsets.statusBars,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer {
                    translationY = toolbarTranslationY
                    // ★★★ 修复重点 2：添加阴影 ★★★
                    // 只有完全折叠时才显示阴影，区分白色 Toolbar 和白色列表
                    shadowElevation = if (isCollapsed) 4.dp.toPx() else 0f
                }
        )

        // FAB
        val showFab by remember { derivedStateOf { listState.firstVisibleItemIndex > 3 } }
        val coroutineScope = rememberCoroutineScope()

        AnimatedVisibility(
            visible = showFab,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 20.dp)
        ) {
            FloatingActionButton(
                onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(Icons.Default.Add, contentDescription = "回到顶部")
            }
        }
    }
}

// --- 3. 修改后的 Item 组件 ---

@Composable
fun ContactItem(
    contact: Contact,
    shape: Shape,
    showDivider: Boolean
) {
    // 整个 Row 包裹在 Surface 或 Box 中来处理背景
    // 注意：padding(horizontal = 16.dp) 加在最外层，实现左右留白
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // 左右留白，形成卡片效果
            .clip(shape) // 裁剪圆角
            .background(Color.White) // 白色背景
            .clickable { /* 点击事件 */ }
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp), // 内部内容间距
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(contact.color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contact.name.take(1),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = contact.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }

            // 手动绘制分割线，为了不顶到最左边，通常左侧会留白
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 78.dp, end = 20.dp), // start = 头像宽度 + 间距
                    thickness = 0.5.dp,
                    color = Color.LightGray.copy(alpha = 0.5f)
                )
            }
        }
    }
}