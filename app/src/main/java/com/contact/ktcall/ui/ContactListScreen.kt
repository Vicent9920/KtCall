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
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
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

    // 将 dp 转换为 px
    val headerHeightPx = with(density) { headerHeightDp.toPx() }

    // 优化：动态获取 Toolbar 的真实高度，而不是写死 64dp
    // 初始化为一个合理的默认值，稍后通过 onGloballyPositioned 更新
    var toolbarHeightPx by remember { mutableStateOf(with(density) { 64.dp.toPx() }) }

    // Header 可折叠的总距离
    val collapseRangePx = headerHeightPx - toolbarHeightPx

    // Toolbar 图标的初始位移 (Header底部 - Toolbar高度)
    // 这样确保 Toolbar 初始位置的底部正好和 Header 的底部重合
    val maxToolbarOffsetPx = collapseRangePx

    // 计算滚动相关的状态 (使用 State 对象以确保依赖链正确)
    val scrollOffsetState = remember { derivedStateOf { listState.firstVisibleItemScrollOffset.toFloat() } }
    val firstVisibleIndexState = remember { derivedStateOf { listState.firstVisibleItemIndex } }

    // 计算折叠进度 (0.0 = 展开, 1.0 = 折叠)
    // 关键修复：添加 collapseRangePx 为 Key，确保当 Toolbar 高度测量更新时，此逻辑重新生成
    val collapseFractionState = remember(collapseRangePx) {
        derivedStateOf {
            if (firstVisibleIndexState.value > 0) 1f
            else (scrollOffsetState.value / collapseRangePx).coerceIn(0f, 1f)
        }
    }
    val collapseFraction by collapseFractionState

    // 状态：是否显示 Toolbar 背景和标题
    // 关键修复：直接依赖 collapseFractionState
    val showToolbarBackground by remember {
        derivedStateOf { collapseFractionState.value > 0.9f }
    }

    val targetAlpha = if (showToolbarBackground) 1f else 0f
    val toolbarAlpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = tween(durationMillis = 300),
        label = "ToolbarAlphaAnimation"
    )

    // Toolbar 标题的透明度动画
    // 修复：使用 Alpha 动画代替 AnimatedVisibility，防止布局高度变化导致的位置跳动
    val toolbarTitleAlpha by animateFloatAsState(
        targetValue = if (showToolbarBackground) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "ToolbarTitleAlpha"
    )

    // Toolbar 垂直位移：随 Header 滚动而上移
    // 关键修复：添加 maxToolbarOffsetPx 为 Key，修复因高度更新不同步导致的错位问题
    val toolbarTranslationY by remember(maxToolbarOffsetPx) {
        derivedStateOf {
            if (firstVisibleIndexState.value == 0) {
                (maxToolbarOffsetPx - scrollOffsetState.value).coerceAtLeast(0f)
            } else {
                0f
            }
        }
    }

    // Header 大标题透明度
    // 关键修复：依赖 collapseFractionState
    val bigTitleAlpha by remember {
        derivedStateOf {
            val fraction = collapseFractionState.value
            if (fraction > 0.7f) {
                1f - ((fraction - 0.7f) / 0.3f).coerceIn(0f, 1f)
            } else {
                1f
            }
        }
    }

    // FAB 控制逻辑
    var showFab by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress, firstVisibleIndexState.value) {
        if (firstVisibleIndexState.value == 0) {
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
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeightDp)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val maxWidthPx = with(density) { maxWidth.toPx() }

                    // 动态计算 Header 文本的变换
                    val textGraphicsModifier = Modifier.graphicsLayer {
                        alpha = bigTitleAlpha

                        // 1. 水平平移
                        translationX = -maxWidthPx * 0.35f * collapseFraction

                        // 2. 垂直平移
                        // 目标：从 Header 中心 移到 Toolbar 的中心位置。
                        translationY = collapseFraction * (headerHeightPx - toolbarHeightPx) * 0.5f

                        // 3. 缩放
                        val scale = 1f - (0.2f * collapseFraction)
                        scaleX = scale
                        scaleY = scale
                    }

                    // 大标题区域
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .then(textGraphicsModifier),
                        horizontalAlignment = Alignment.CenterHorizontally
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
                // 修复：移除 AnimatedVisibility，改用 graphicsLayer alpha
                // 确保 Toolbar 高度在整个生命周期内恒定，防止位置计算跳变
                Text(
                    text = "电话",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer { alpha = toolbarTitleAlpha }
                )
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
                // 优化：动态测量 Toolbar 真实高度
                .onGloballyPositioned { coordinates ->
                    // 仅当高度实质性变化时更新（忽略小于 1px 的浮点误差），避免抖动
                    if (abs(toolbarHeightPx - coordinates.size.height.toFloat()) > 1f) {
                        toolbarHeightPx = coordinates.size.height.toFloat()
                    }
                }
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
    var offsetX by remember { mutableStateOf(0f) }
    val screenWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val actionThreshold = screenWidth * 0.3f // 30%屏幕宽度作为阈值

    // 计算背景透明度 - 只有在侧滑时才显示，且有渐变效果
    val backgroundAlpha = when {
        offsetX < 0f -> (-offsetX / actionThreshold).coerceIn(0f, 1f) // 左滑显示呼叫按钮
        offsetX > 0f -> (offsetX / actionThreshold).coerceIn(0f, 1f)  // 右滑显示短信按钮
        else -> 0f
    }

    // 计算前景卡片的透明度 - 侧滑时逐渐变透明
    val cardAlpha = when {
        abs(offsetX) > actionThreshold -> 0.3f // 超过阈值时降低透明度
        abs(offsetX) > actionThreshold * 0.5f -> 0.7f // 中等侧滑时中等透明度
        abs(offsetX) > 0f -> (1f - abs(offsetX) / (actionThreshold * 0.5f) * 0.3f).coerceIn(0.7f, 1f) // 小幅侧滑时轻微变透明
        else -> 1f
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        // 根据拖拽距离决定最终状态和操作
                        when {
                            offsetX <= -actionThreshold -> {
                                // 左滑超过阈值 - 调用呼叫方法并恢复到初始状态
                                callContact(contact)
                                offsetX = 0f
                            }
                            offsetX >= actionThreshold -> {
                                // 右滑超过阈值 - 调用短信方法并恢复到初始状态
                                sendMessage(contact)
                                offsetX = 0f
                            }
                            else -> {
                                // 没有超过阈值 - 恢复到原始位置
                                offsetX = 0f
                            }
                        }
                    },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        // 允许完全滑动到屏幕宽度
                        val newOffset = offsetX + dragAmount
                        offsetX = newOffset.coerceIn(-screenWidth, screenWidth)
                    }
                )
            }
    ) {
        // 背景操作按钮 - 默认透明，只有侧滑时才渐变显示
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { alpha = backgroundAlpha }
        ) {
            // 左滑 - 呼叫按钮 (占据整个屏幕宽度)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp) // 固定高度，与卡片高度一致
                    .background(Color(0xFF4CAF50)) // 绿色背景
                    .clickable {
                        // 点击呼叫按钮执行操作并恢复
                        callContact(contact)
                        offsetX = 0f
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Call,
                        contentDescription = "呼叫",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "呼叫",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }

        // 右滑背景 - 当右滑时显示
        if (offsetX > 0f) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = backgroundAlpha }
            ) {
                // 右滑 - 短信按钮 (占据整个屏幕宽度)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp) // 固定高度，与卡片高度一致
                        .background(Color(0xFF2196F3)) // 蓝色背景
                        .clickable {
                            // 点击短信按钮执行操作并恢复
                            sendMessage(contact)
                            offsetX = 0f
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "短信",
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            Icons.Filled.Email,
                            contentDescription = "短信",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // 前景联系人卡片
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp) // 固定高度
                .offset { IntOffset(offsetX.toInt(), 0) }
                .graphicsLayer { alpha = cardAlpha } // 应用透明度渐变效果
                .clickable {
                    // 点击时恢复到原始位置
                    offsetX = 0f
                },
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            shape = MaterialTheme.shapes.medium,
            // 添加这一行：强制将背景色设为白色
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
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
    }
}

// 空方法 - 呼叫联系人
fun callContact(contact: Contact) {
    // TODO: 实现呼叫逻辑
    println("呼叫联系人: ${contact.name} - ${contact.phone}")
}

// 空方法 - 发送短信
fun sendMessage(contact: Contact) {
    // TODO: 实现发送短信逻辑
    println("发送短信给: ${contact.name} - ${contact.phone}")
}

// 辅助扩展函数
@Composable
fun ColorScheme.surfaceColorAtElevation(elevation: androidx.compose.ui.unit.Dp): Color {
    val alpha = ((4.5f * kotlin.math.ln(elevation.value + 1)) + 2f) / 100f
    return surfaceTint.copy(alpha = alpha).compositeOver(surface)
}