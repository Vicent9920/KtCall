package com.contact.ktcall.ui.contact

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// 常量定义
val HEADER_HEIGHT = 420.dp
val TOOLBAR_HEIGHT = 100.dp

@Composable
fun Contact3Screen() {
    val density = LocalDensity.current

    // Header 最大上移距离 (例如 -320px)
    val maxUpOffsetPx = with(density) { -(HEADER_HEIGHT - TOOLBAR_HEIGHT).toPx() }
    val minUpOffsetPx = 0f

    // Header 当前偏移量
    var headerOffsetPx by remember { mutableStateOf(0f) }

    // 核心：嵌套滚动逻辑
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {

            // 1. 拦截滑动前：处理【上滑折叠】逻辑
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y

                // 只有当手指【向上】滑动 (delta < 0) 时，Header 才需要主动响应
                if (delta < 0) {
                    val newOffset = headerOffsetPx + delta
                    headerOffsetPx = newOffset.coerceIn(maxUpOffsetPx, minUpOffsetPx)
                    // 关键点：这里虽然改变了 Header，但我们返回 Zero。
                    // 这样列表也会收到这个滑动事件，列表内容就会随着 Header 上移而上滚，视觉上同步。
                    return Offset.Zero
                }

                // 如果是【向下】滑动，这里什么都不做，让列表先去滚。
                return Offset.Zero
            }

            // 2. 列表滑动后：处理【下滑展开】逻辑
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y

                // 如果是【向下】滑动 (delta > 0)，且列表已经滑不动了（available > 0），
                // 说明列表到顶了，这时候我们才拉下 Header
                if (delta > 0) {
                    val oldOffset = headerOffsetPx
                    val newOffset = (oldOffset + delta).coerceIn(maxUpOffsetPx, minUpOffsetPx)
                    headerOffsetPx = newOffset
                    // 告诉系统我们消费了多少，防止过度滑动效果
                    return Offset(0f, newOffset - oldOffset)
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF6F6F6))
            .nestedScroll(nestedScrollConnection)
    ) {

        // --- 1. 列表区域 ---
        LazyColumn(
            // 留出头部空间
            contentPadding = PaddingValues(top = HEADER_HEIGHT),
            modifier = Modifier.fillMaxSize()
        ) {
            items(50) { index ->
                ListItem(
                    headlineContent = { Text("Contact Person $index") },
                    leadingContent = {
                        Box(modifier = Modifier.size(40.dp).background(Color.LightGray, CircleShape))
                    }
                )
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
            }
        }

        // --- 2. Header 区域 ---
        CoordinatorHeader(
            offsetPx = headerOffsetPx,
            maxOffsetPx = maxUpOffsetPx
        )

        // --- 3. FAB ---
        if (headerOffsetPx < maxUpOffsetPx / 2) {
            FloatingActionButton(
                onClick = {},
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp),
                containerColor = Color.White
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
            }
        }
    }
}

@Composable
fun CoordinatorHeader(
    offsetPx: Float,
    maxOffsetPx: Float
) {
    // 0f = 展开, maxOffsetPx = 折叠
    val progress = 1f - (offsetPx / maxOffsetPx)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HEADER_HEIGHT)
            .offset { IntOffset(x = 0, y = offsetPx.roundToInt()) } // 位移实现动画
            .background(Color(0xFFF6F6F6))
    ) {

        // 中间大标题
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .graphicsLayer {
                    alpha = (progress - 0.5f).coerceIn(0f, 1f) * 2
                    translationY = -offsetPx * 0.5f // 视差
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Contact Phone",
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "128 contacts",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }

        // 底部 Toolbar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(TOOLBAR_HEIGHT)
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(modifier = Modifier.size(24.dp).background(Color.White, CircleShape))
                Text("All", fontSize = 12.sp)
            }

            Text(
                text = "Contact Phone",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .weight(1f)
                    .graphicsLayer {
                        alpha = if (progress < 0.2f) (0.2f - progress) * 5 else 0f
                    }
            )

            Icon(Icons.Default.Add, null, Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.Search, null, Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Default.MoreVert, null, Modifier.size(32.dp))
        }
    }
}