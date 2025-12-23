package com.contact.ktcall.ui

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.unit.lerp
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

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen() {
    val contacts = remember { MockData.getContacts(100) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 定义尺寸常量
    val density = LocalDensity.current
    val headerHeightDp = 320.dp
    val toolbarHeightDp = 64.dp

    val headerHeightPx = with(density) { headerHeightDp.toPx() }
    val toolbarHeightPx = with(density) { toolbarHeightDp.toPx() }

    // Header 可折叠的总距离
    val collapseRangePx = headerHeightPx - toolbarHeightPx

    // Toolbar 图标的初始位移 (Header底部 - Toolbar高度)
    val maxToolbarOffsetPx = collapseRangePx

    // 计算滚动相关的状态
    val scrollOffset = remember { derivedStateOf { listState.firstVisibleItemScrollOffset.toFloat() } }
    val firstVisibleIndex = remember { derivedStateOf { listState.firstVisibleItemIndex } }

    // 计算折叠进度 (0.0 = 展开, 1.0 = 折叠)
    val collapseFraction by remember {
        derivedStateOf {
            if (firstVisibleIndex.value > 0) 1f
            else (scrollOffset.value / collapseRangePx).coerceIn(0f, 1f)
        }
    }

    // 状态：是否显示 Toolbar 背景和标题
    // 修改判断逻辑：当折叠进度接近完成时才显示 Toolbar 标题，让过渡更自然
    val showToolbarBackground by remember {
        derivedStateOf { collapseFraction > 0.9f }
    }

    val targetAlpha = if (showToolbarBackground) 1f else 0f
    val toolbarAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 300),
        label = "ToolbarAlphaAnimation"
    )

    // Toolbar 垂直位移：随 Header 滚动而上移
    val toolbarTranslationY by remember {
        derivedStateOf {
            if (firstVisibleIndex.value == 0) {
                (maxToolbarOffsetPx - scrollOffset.value).coerceAtLeast(0f)
            } else {
                0f
            }
        }
    }

    // Header 大标题透明度：在最后阶段才快速消失，以便和 Toolbar 标题衔接
    val bigTitleAlpha by remember {
        derivedStateOf {
            // 0.0 -> 0.7 保持不透明，0.7 -> 1.0 快速变透明
            if (collapseFraction > 0.7f) {
                1f - ((collapseFraction - 0.7f) / 0.3f).coerceIn(0f, 1f)
            } else {
                1f
            }
        }
    }

    // FAB 控制逻辑
    var showFab by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress, firstVisibleIndex.value) {
        if (firstVisibleIndex.value == 0) {
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // 1. 列表内容
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // --- 头部区域 ---
            item {
                BoxWithConstraints( // 使用 BoxWithConstraints 获取宽度以计算平移
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeightDp)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val maxWidthPx = with(density) { maxWidth.toPx() }

                    // 动态计算 Header 文本的变换
                    val textGraphicsModifier = Modifier.graphicsLayer {
                        alpha = bigTitleAlpha

                        // 1. 水平平移 (TranslationX)
                        // 目标：从中心 (0) 移到左侧。
                        // 假设文本块大概占宽度的 40% (估算)，要移到左边 padding 24dp 的位置
                        // 向左移动距离约为：屏幕宽度的一半 - 左边距 - 文本一半宽(估算)
                        // 这里使用一个经验值系数 0.35f * maxWidthPx 来模拟移动到左侧
                        translationX = -maxWidthPx * 0.35f * collapseFraction

                        // 2. 垂直平移 (TranslationY)
                        // 目标：从 Header 中心 移到 Toolbar 的中心位置。
                        // 随着 Box 向上滚动，内容会随之上升。我们需要补偿一定的 Y 值让它看起来"走得慢一点"或者"停在 Toolbar 区域"
                        // 这里的计算让文本在滚动过程中稍微向下偏移，抵消一部分向上滚动的距离，从而对齐到 Toolbar 高度
                        translationY = collapseFraction * (headerHeightPx - toolbarHeightPx) * 0.5f

                        // 3. 缩放 (Scale)
                        // 从 1.0 缩小到 0.8，模拟变成 Toolbar 标题的大小
                        val scale = 1f - (0.2f * collapseFraction)
                        scaleX = scale
                        scaleY = scale
                    }

                    // 大标题区域
                    // 初始状态：居中 (Alignment.Center)
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center) // 关键：初始完全居中
                            .then(textGraphicsModifier), // 应用动态变换
                        horizontalAlignment = Alignment.CenterHorizontally // 文字内容水平居中
                    ) {
                        Text(
                            text = "电话",
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

        // 2. 固定的 Toolbar
        TopAppBar(
            title = {
                // 当 Header 几乎完全折叠后，显示 Toolbar 上的标题
                androidx.compose.animation.AnimatedVisibility(
                    visible = showToolbarBackground,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "电话",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
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
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = toolbarAlpha),
                scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = toolbarAlpha),
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                navigationIconContentColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .graphicsLayer { translationY = toolbarTranslationY }
        )

        // 3. FloatingActionButton
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

// 辅助扩展函数
@Composable
fun ColorScheme.surfaceColorAtElevation(elevation: androidx.compose.ui.unit.Dp): Color {
    val alpha = ((4.5f * kotlin.math.ln(elevation.value + 1)) + 2f) / 100f
    return surfaceTint.copy(alpha = alpha).compositeOver(surface)
}