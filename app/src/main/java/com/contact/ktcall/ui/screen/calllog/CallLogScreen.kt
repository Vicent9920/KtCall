package com.contact.ktcall.ui.screen.calllog

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blankj.utilcode.util.LogUtils
import com.contact.ktcall.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

// --- 1. 定义数据结构 ---

enum class CallType {
    INCOMING, // 呼入
    OUTGOING, // 呼出
    MISSED,    // 未接
    REJECTED // 拒接
}

data class CallLogEntry(
    val id: Int,
    val nameOrNumber: String,
    val count: Int = 1,      // 通话次数，用于显示 (2)
    val type: CallType,      // 类型
    val time: String,
    val dateLabel: String,    // 分组依据：今天、昨天、2025年...
    val duration: String = ""
)

// --- 2. 主界面 ---

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CallLogScreen(viewModel: CallLogViewModel = viewModel()) {
    // 获取ViewModel状态
    val uiState by viewModel.uiState.collectAsState()
    val rawData = uiState.items
    val groupedData = remember(rawData) { rawData.groupBy { it.dateLabel } }
    val entryCount = rawData.size
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

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
            .background(Color.White)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
        ) {
            item {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeightDp)
                        .background(Color.White)
                ) {
                    val maxWidthPx = with(density) { maxWidth.toPx() }
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
                        // 默认位置没有居中
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
                // 2. 日期分组头部 (Sticky Header)
                stickyHeader {
                    DateHeader(date)
                }

                var index = 0
                // 3. 列表项
                items(items = logs, key = { it.id }) { entry ->
                    CallLogItem(entry = entry)
                    if (index < 5) {
                        LogUtils.e("entry: $entry")
                    }
                    index++
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
                containerColor = Color.White.copy(alpha = toolbarAlpha),
                scrolledContainerColor = Color.White.copy(alpha = toolbarAlpha)
            ),
            actions = {
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "FilterList"
                    )
                }
                IconButton(onClick = {}) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "search"
                    )
                }
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

// --- 3. 组件实现 ---

/**
 * 列表项：完全依照视频样式复刻
 * 图标 + 粗体名字(次数) + 录音 + 时间
 */
@Composable
private fun CallLogItem(entry: CallLogEntry) {
    // 样式逻辑：未接电话显示红色
    val isMissed = entry.type == CallType.MISSED
    val primaryColor = if (isMissed) Color(0xFFD32F2F) else Color.Black
    val iconColor = if (isMissed) Color(0xFFD32F2F) else Color.Gray

    // 图标逻辑
    val iconVector = when (entry.type) {
        CallType.INCOMING -> R.mipmap.ic_call_in// ↙
        CallType.OUTGOING -> R.mipmap.ic_call_out     // ↗
        CallType.MISSED -> R.mipmap.ic_call_missed     // 折线
        else -> R.mipmap.ic_call_rejected
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* 点击事件 */ }
            .padding(horizontal = 20.dp, vertical = 14.dp), // 增加间距
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 左侧小图标
        Icon(
            painter = painterResource(id = iconVector),
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        // 2. 名字与次数
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = entry.nameOrNumber,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold, // 粗体
                color = primaryColor
            )

            // 如果次数大于1，显示 (N)
            if (entry.count > 1) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "(${entry.count})",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
            }
        }

        // 3. 录音图标 (灰色，小)
//        Icon(
//            imageVector = Icons.Outlined.Mic,
//            contentDescription = "录音",
//            tint = Color.LightGray,
//            modifier = Modifier.size(14.dp)
//        )

        Spacer(modifier = Modifier.width(12.dp))

        // 4. 时间
        Text(
            text = entry.time,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}

/**
 * 日期分组标题
 */
@Composable
private fun DateHeader(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF7F7F7)) // 浅灰背景
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
    }
}

