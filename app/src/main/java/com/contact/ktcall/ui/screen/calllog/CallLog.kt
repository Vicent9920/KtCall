package com.contact.ktcall.ui.screen.calllog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.compose.ui.coordinator.CollapsingHeader
import com.example.compose.ui.coordinator.CoordinatorLayout
import com.example.compose.ui.coordinator.ScrollToTopFab
import com.example.compose.ui.coordinator.rememberCollapsingToolbarState
import com.example.compose.ui.coordinator.rememberScrollToTopFabVisibility
import kotlinx.coroutines.launch

/**
 * 通话记录页面 - 仿照 `ContactListScreen` 的折叠头部 + 列表展示
 */
@Composable
fun CallLogScreen() {
    val density = LocalDensity.current
    val maxHeight = 360.dp
    val minHeight = 56.dp
    val maxHeightPx = with(density) { maxHeight.toPx() }
    val minHeightPx = with(density) { minHeight.toPx() }

    val toolbarState = rememberCollapsingToolbarState(maxHeightPx, minHeightPx)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop = rememberScrollToTopFabVisibility(listState, showThreshold = 4)

    // 模拟通话记录数据
    val callLogs = remember { generateMockCallLogs() }

    CoordinatorLayout(
        modifier = Modifier.fillMaxSize(),
        maxHeaderHeight = maxHeight,
        minHeaderHeight = minHeight,
        toolbarState = toolbarState,
        header = { progress, currentHeight ->
            CallLogHeader(
                progress = progress,
                currentHeight = currentHeight,
                maxHeight = maxHeight,
                entryCount = callLogs.size
            )
        },
        fab = { progress ->
            ScrollToTopFab(
                visible = showScrollToTop,
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                        toolbarState.expand()
                    }
                }
            )
        },
        content = { topPadding ->
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(items = callLogs, key = { it.id }) { entry ->
                    CallLogItem(entry = entry)
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.CallLogHeader(
    progress: Float,
    currentHeight: androidx.compose.ui.unit.Dp,
    maxHeight: androidx.compose.ui.unit.Dp,
    entryCount: Int
) {
    val primaryColor = Color(0xFF1E88E5)

    CollapsingHeader(
        progress = progress,
        currentHeight = currentHeight,
        maxHeight = maxHeight,
        backgroundColor = primaryColor,
        parallaxMultiplier = 0.5f,
        expandedContent = {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "电话",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "$entryCount 条通话记录",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        },
        collapsedContent = {
            var isFilterActive by remember { mutableStateOf(false) }
            TopAppBar(
                title = {
                    Text(
                        text = if (isFilterActive) "筛选通话" else "最近记录",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { isFilterActive = !isFilterActive }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "筛选/更多",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: 添加操作 */ }) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "语音",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    )
}

@Composable
private fun CallLogItem(entry: CallLogEntry) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .clickable { /* 点击项：打开详情或回拨 */ },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(entry.avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.nameOrNumber,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.typeLabel,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.time,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "录音",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

data class CallLogEntry(
    val id: Int,
    val nameOrNumber: String,
    val typeLabel: String,
    val time: String,
    val avatarColor: Color
)

private fun generateMockCallLogs(): List<CallLogEntry> {
    val numbers = listOf(
        "13 421 412 7641",
        "157 5626 2958",
        "186 2817 1501",
        "177 8167 7121",
        "028 8577 4138",
        "010 8888 9999",
        "138 0013 8000",
        "021 6666 7777",
        "400 800 9000",
        "185 1234 5678"
    )

    val types = listOf("已接", "未接", "已拨出")
    val colors = listOf(
        Color(0xFF4CAF50),
        Color(0xFFE91E63),
        Color(0xFF03DAC5),
        Color(0xFFFF9800),
        Color(0xFF2196F3)
    )

    return numbers.mapIndexed { index, number ->
        CallLogEntry(
            id = index,
            nameOrNumber = number,
            typeLabel = types[index % types.size],
            time = when (index % 5) {
                0 -> "22:44"
                1 -> "18:58"
                2 -> "17:01"
                3 -> "14:32"
                else -> "14:08"
            },
            avatarColor = colors[index % colors.size]
        )
    }
}