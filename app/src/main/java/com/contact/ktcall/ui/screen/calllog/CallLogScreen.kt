package com.contact.ktcall.ui.screen.calllog

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.contact.ktcall.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

// --- 1. 数据结构 (更新以支持详情显示) ---
enum class CallType(val label: String) {
    INCOMING("来电"),
    OUTGOING("呼出"),
    MISSED("未接"),
    REJECTED("拒接")
}

data class CallLogEntry(
    val id: Int,
    val name: String?,
    val nameOrNumber: String,
    val count: Int = 1,
    val type: CallType,
    val time: String,        // 列表显示的简略时间 (e.g., "18:02")
    val dateLabel: String,
    val duration: String = "", // 时长文本 (e.g., "0 分 34 秒")
)

// --- 2. 主界面 (CallLogScreen) ---
// 这部分核心逻辑没变，主要是增加了 Expanded 状态的管理

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CallLogScreen(viewModel: CallLogViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val rawData = uiState.items
    val groupedData = remember(rawData) { rawData.groupBy { it.dateLabel } }
    val callLogCount = rawData.size

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 默认展开 ID 为 3 (对应截图中的 157 5626 2958)
    var expandedItemId by remember { mutableIntStateOf(3) }

    // Header 动画相关变量
    val density = LocalDensity.current
    val headerHeightDp = 280.dp
    val headerHeightPx = with(density) { headerHeightDp.toPx() }
    var toolbarHeightPx by remember { mutableFloatStateOf(with(density) { 64.dp.toPx() }) }
    val collapseRangePx = headerHeightPx - toolbarHeightPx
    val maxToolbarOffsetPx = collapseRangePx
    val scrollOffsetState =
        remember { derivedStateOf { listState.firstVisibleItemScrollOffset.toFloat() } }
    val firstVisibleIndexState = remember { derivedStateOf { listState.firstVisibleItemIndex } }

    val collapseFractionState = remember(collapseRangePx) {
        derivedStateOf {
            if (firstVisibleIndexState.value > 0) 1f
            else (scrollOffsetState.value / collapseRangePx).coerceIn(0f, 1f)
        }
    }
    val collapseFraction by collapseFractionState

    val showToolbarBackground by remember { derivedStateOf { collapseFractionState.value > 0.9f } }
    val toolbarAlpha by animateFloatAsState(
        targetValue = if (showToolbarBackground) 1f else 0f,
        label = ""
    )
    val toolbarTitleAlpha by animateFloatAsState(
        targetValue = if (showToolbarBackground) 1f else 0f,
        label = ""
    )
    val toolbarTranslationY by remember(maxToolbarOffsetPx) {
        derivedStateOf {
            if (firstVisibleIndexState.value == 0) (maxToolbarOffsetPx - scrollOffsetState.value).coerceAtLeast(
                0f
            ) else 0f
        }
    }
    val bigTitleAlpha by remember {
        derivedStateOf {
            val fraction = collapseFractionState.value
            if (fraction > 0.7f) 1f - ((fraction - 0.7f) / 0.3f).coerceIn(0f, 1f) else 1f
        }
    }

    var showFab by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress, firstVisibleIndexState.value) {
        if (firstVisibleIndexState.value == 0) showFab = false
        else if (listState.isScrollInProgress) showFab = true
        else if (showFab) {
            delay(1000); showFab = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeightDp)
                        .background(Color(0xFFF2F2F7))
                ) {
                    val maxWidthPx =
                        with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
                    val textGraphicsModifier = Modifier.graphicsLayer {
                        alpha = bigTitleAlpha
                        translationX = -maxWidthPx * 0.35f * collapseFraction
                        translationY = collapseFraction * (headerHeightPx - toolbarHeightPx) * 0.5f
                        val scale = 1f - (0.2f * collapseFraction)
                        scaleX = scale
                        scaleY = scale
                    }
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .then(textGraphicsModifier),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "电话",
                            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                    }
                }
            }

            groupedData.forEach { (date, logs) ->
                // 1. 日期标题
                stickyHeader {
                    DateHeader(date)
                }

                // 2. 将同一天的所有记录放在一个 Item 里，包裹在一个 Card 中
                item {
                    GroupedCallLogCard(
                        logs = logs,
                        expandedItemId = expandedItemId,
                        onItemClick = { clickedId ->
                            expandedItemId = if (expandedItemId == clickedId) -1 else clickedId
                        }
                    )
                }
            }
        }

        // Toolbar
        TopAppBar(
            title = {
                Text(
                    text = "电话",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.graphicsLayer { alpha = toolbarTitleAlpha }
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFFF2F2F7).copy(alpha = toolbarAlpha),
                scrolledContainerColor = Color(0xFFF2F2F7).copy(alpha = toolbarAlpha)
            ),
            actions = {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "筛选"
                    )
                }
                IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "搜索") }
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "更多"
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onGloballyPositioned { coordinates ->
                    if (abs(toolbarHeightPx - coordinates.size.height.toFloat()) > 1f) toolbarHeightPx =
                        coordinates.size.height.toFloat()
                }
                .graphicsLayer { translationY = toolbarTranslationY }
        )

        // FAB
        Box(modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 32.dp)) {
            AnimatedVisibility(
                visible = showFab,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = { coroutineScope.launch { listState.animateScrollToItem(0) } },
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                }
            }
        }
    }
}

// --- 3. 核心组件：分组卡片 ---

@Composable
fun GroupedCallLogCard(
    logs: List<CallLogEntry>,
    expandedItemId: Int,
    onItemClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 2.dp) // 外边距，产生悬浮感
            .animateContentSize(animationSpec = tween(300)),
        shape = RoundedCornerShape(16.dp), // 整个分组的大圆角
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column {
            logs.forEachIndexed { index, entry ->
                val isExpanded = entry.id == expandedItemId

                // 渲染单个条目内容
                CallLogItemContent(
                    entry = entry,
                    isExpanded = isExpanded,
                    onClick = { onItemClick(entry.id) }
                )

                // 渲染分割线：如果不是列表的最后一项，则显示分割线
                // 且一般分割线会避开图标区域 (padding start)
                if (index < logs.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 52.dp, end = 16.dp), // 左侧避开图标
                        thickness = 0.5.dp,
                        color = Color(0xFFEFEFEF) // 极淡的灰色
                    )
                }
            }
        }
    }
}

// --- 4. 条目内容实现 (去除 Card 包装) ---

@Composable
private fun CallLogItemContent(
    entry: CallLogEntry,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    // 颜色定义
    val primaryTextColor = Color.Black
    val missedColor = Color(0xFFD32F2F)
    val subTextColor = Color(0xFF757575)

    val iconRes = when (entry.type) {
        CallType.INCOMING -> R.mipmap.ic_call_in
        CallType.OUTGOING -> R.mipmap.ic_call_out
        CallType.MISSED -> R.mipmap.ic_call_missed
        CallType.REJECTED -> R.mipmap.ic_call_rejected
    }
    val iconTint = if (entry.type == CallType.MISSED) missedColor else Color(0xFF757575)

    // 内容容器
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null // 去除波纹，保持干净
            ) { onClick() }
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 14.dp,
                bottom = if (isExpanded) 16.dp else 14.dp
            )
    ) {
        // === 模式 A: 收起状态 ===
        if (!isExpanded) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 图标
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(18.dp)) // 调整间距以匹配分割线缩进

                // 名字 + 次数
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.nameOrNumber,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (entry.type == CallType.MISSED) missedColor else primaryTextColor
                    )
                    if (entry.count > 1) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(${entry.count})",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryTextColor
                        )
                    }
                }


                // 时间
                Text(
                    text = entry.time,
                    fontSize = 13.sp,
                    color = subTextColor
                )
            }
        }

        // === 模式 B: 展开状态 ===
        else {
            // 1. 大号码
            Text(
                text = entry.nameOrNumber,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = primaryTextColor,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // 2. 状态行 + 录音图标
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.type.name + " , " + entry.duration,
                    fontSize = 14.sp,
                    color = subTextColor
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 3. 具体时间
            Text(
                text = entry.time,
                fontSize = 14.sp,
                color = subTextColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 4. 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                ActionCircleButton(Icons.Default.Call, Color(0xFF00C853))
                ActionCircleButton(Icons.Default.Message, Color(0xFF29B6F6))
                ActionCircleButton(Icons.Default.Videocam, Color(0xFF00C853))
                ActionCircleButton(Icons.Outlined.Info, Color(0xFF9E9E9E), isVector = true)
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (entry.name.isNullOrEmpty()) {
                // 5. 底部按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FooterButton(text = "+ 添加至联系人", modifier = Modifier.weight(1f))
                    FooterButton(text = "✎ 添加标签", modifier = Modifier.weight(1f))
                }
            }

        }
    }
}

// --- 辅助组件 (保持不变) ---

@Composable
fun ActionCircleButton(icon: ImageVector, color: Color, isVector: Boolean = true) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(color)
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun FooterButton(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F5F5))
            .clickable { },
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DateHeader(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF2F2F2))
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF757575))
    }
}