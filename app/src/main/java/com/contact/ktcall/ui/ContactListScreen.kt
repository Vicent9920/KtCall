package com.contact.ktcall.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState // 修改：引入 Float 动画
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- 1. 数据模型与 Mock 数据生成 ---

data class Contact(
    val id: Int,
    val name: String,
    val phone: String,
    val color: Color
)

object MockData {
    private val firstNames = listOf("赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈", "褚", "卫", "蒋", "沈", "韩", "杨")
    private val lastNames = listOf("伟", "芳", "娜", "敏", "静", "秀英", "丽", "强", "磊", "洋", "艳", "勇", "军", "杰", "娟", "涛")
    private val colors = listOf(
        Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8), Color(0xFF9575CD),
        Color(0xFF7986CB), Color(0xFF64B5F6), Color(0xFF4FC3F7), Color(0xFF4DD0E1),
        Color(0xFF4DB6AC), Color(0xFF81C784), Color(0xFFAED581), Color(0xFFFFD54F),
        Color(0xFFFFB74D), Color(0xFFFF8A65), Color(0xFFA1887F), Color(0xFF90A4AE)
    )

    fun getContacts(count: Int = 50): List<Contact> {
        return List(count) { id ->
            Contact(
                id = id,
                name = "${firstNames.random()}${lastNames.random()}",
                phone = "13${Random.nextInt(0, 9)} ${Random.nextInt(1000, 9999)} ${Random.nextInt(1000, 9999)}",
                color = colors.random()
            )
        }
    }
}

// --- 2. 主屏幕 Composable ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen() {
    val contacts = remember { MockData.getContacts(100) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 定义尺寸常量
    val density = LocalDensity.current
    val headerHeightDp = 320.dp // 模拟 XML 中的 420dp 高头部
    val toolbarHeightDp = 64.dp

    // 将 dp 转换为 px 用于计算
    val headerHeightPx = with(density) { headerHeightDp.toPx() }
    val toolbarHeightPx = with(density) { toolbarHeightDp.toPx() }

    // 计算滚动相关的状态
    val showToolbarBackground by remember {
        derivedStateOf {
            val firstVisibleIndex = listState.firstVisibleItemIndex
            val firstVisibleOffset = listState.firstVisibleItemScrollOffset

            // 如果 Header (Index 0) 已经滚过了一定距离（保留 Toolbar 高度），或者已经滚到了后面的 item
            firstVisibleIndex > 0 || firstVisibleOffset > (headerHeightPx - toolbarHeightPx)
        }
    }

    // 修复：不使用 animateColorAsState，改为使用 animateFloatAsState 控制 Alpha。
    // 避免 Color.Transparent (0x00000000) 到 Surface (0xFFFFFFFF) 插值过程中产生灰色/黑色中间值。
    val targetAlpha = if (showToolbarBackground) 1f else 0f
    val toolbarAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 300),
        label = "ToolbarAlphaAnimation"
    )

    // 计算大标题的透明度：向上滚时逐渐消失
    val bigTitleAlpha by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) 0f
            else {
                val offset = listState.firstVisibleItemScrollOffset
                // 当滚动距离超过一半高度时开始变透明
                val progress = (offset / (headerHeightPx * 0.8f)).coerceIn(0f, 1f)
                1f - progress
            }
        }
    }

    // FAB 控制逻辑
    var showFab by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress, listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex == 0) {
            showFab = false
        } else if (listState.isScrollInProgress) {
            showFab = true
        } else {
            if (showFab) {
                delay(1000)
                showFab = false
            }
        }
    }

    // 修复：给根 Box 添加背景色，防止透明区域透出 Window 的黑色背景导致闪烁
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 1. 列表内容 (Header 作为第一个 Item)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp) // 底部留白给 FAB
        ) {
            // --- 模拟 CollapsingToolbarLayout 的头部区域 ---
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeightDp)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // 大标题区域 (Gravity Bottom)
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 24.dp, bottom = 24.dp)
                            .graphicsLayer { alpha = bigTitleAlpha } // 随滚动淡出
                    ) {
                        Text(
                            text = "联系人",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${contacts.size} 位联系人",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // --- 联系人列表 ---
            items(contacts, key = { it.id }) { contact ->
                ContactItem(contact)
                Divider(
                    modifier = Modifier.padding(start = 72.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp
                )
            }
        }

        // 2. 固定的 Toolbar (Pin 效果)
        // 始终覆盖在最上层
        TopAppBar(
            title = {
                // 当 Header 滚出去后，显示 Toolbar 上的小标题
                androidx.compose.animation.AnimatedVisibility(
                    visible = showToolbarBackground,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "联系人",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            // 修复：移除 navigationIcon 块（全选按钮）
            actions = {
                IconButton(onClick = { /* 搜索 */ }) {
                    Icon(Icons.Default.Search, contentDescription = "搜索")
                }
                IconButton(onClick = { /* 添加 */ }) {
                    Icon(Icons.Default.Add, contentDescription = "添加")
                }
                IconButton(onClick = { /* 更多 */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "更多")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                // 修复：始终使用 Surface 基色，仅对 Alpha 通道应用动画
                // 这样避免了 RGB 从 0 (黑) 变到 255 (白) 过程中的灰色状态
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = toolbarAlpha),
                scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = toolbarAlpha),
                // 确保按钮在透明背景下也清晰可见
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // 3. FloatingActionButton (底部居中)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        ) {
            AnimatedVisibility(
                visible = showFab,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch { listState.animateScrollToItem(0) }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "回到顶部")
                }
            }
        }
    }
}

// --- 3. 列表项组件 ---

@Composable
fun ContactItem(contact: Contact) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* 点击事件 */ }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
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
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = contact.phone,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 辅助扩展函数：获取 Elevation 颜色
@Composable
fun ColorScheme.surfaceColorAtElevation(elevation: androidx.compose.ui.unit.Dp): Color {
    val alpha = ((4.5f * kotlin.math.ln(elevation.value + 1)) + 2f) / 100f
    return surfaceTint.copy(alpha = alpha).compositeOver(surface)
}