package com.contact.ktcall.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

    val contacts = remember { MockData.getContacts(46) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // 尺寸定义
    val headerHeight = 320.dp
    val toolbarHeight = 64.dp

    val headerHeightPx = with(density) { headerHeight.toPx() }
    val toolbarHeightPx = with(density) { toolbarHeight.toPx() }
    val collapseRangePx = headerHeightPx - toolbarHeightPx

    // 滚动进度
    val scrollOffset by remember {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) collapseRangePx
            else listState.firstVisibleItemScrollOffset.toFloat()
        }
    }

    val collapseFraction by remember {
        derivedStateOf {
            (scrollOffset / collapseRangePx).coerceIn(0f, 1f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {

        // ---------------- 列表 ----------------
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            // 底部 padding 防止遮挡，顶部不需要 padding 因为要显示在 header 下面
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {

            // ---------- Header (大标题区域) ----------
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeight),
                    contentAlignment = Alignment.Center
                ) {

                    // 大标题只做 alpha 渐隐
                    val titleAlpha =
                        if (collapseFraction < 0.6f) 1f
                        else 1f - ((collapseFraction - 0.6f) / 0.4f)

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer {
                            alpha = titleAlpha.coerceIn(0f, 1f)
                        }
                    ) {
                        Text(
                            text = "电话",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "${contacts.size} 个有手机号码的联系人",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ---------- 联系人列表 ----------
            items(contacts, key = { it.id }) { contact ->
                ContactItem(contact)
                Divider(
                    modifier = Modifier.padding(start = 72.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }

        // ---------------- Toolbar（顶部浮动栏） ----------------

        // 计算 Toolbar 背景的透明度。
        // 逻辑：前 30% 滚动是全透明的，超过 30% 后背景慢慢显现，直到变成实体色。
        val toolbarContainerAlpha by remember {
            derivedStateOf {
                if (collapseFraction < 0.3f) 0f
                else ((collapseFraction - 0.3f) / 0.7f).coerceIn(0f, 1f)
            }
        }

        TopAppBar(
            title = {
                // 小标题：完全折叠时才显示
                AnimatedVisibility(
                    visible = collapseFraction >= 0.95f,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Text(
                        text = "电话",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
            },
            actions = {
                // Action 图标：始终显示，不受背景透明度影响
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = {}) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                // 关键修改：只改变 containerColor 的 alpha，图标颜色保持不变
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = toolbarContainerAlpha),
                scrolledContainerColor = MaterialTheme.colorScheme.surface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            ),
            modifier = Modifier
                .align(Alignment.TopCenter)
            // 移除了 translationY 和 alpha 变换，使其固定在顶部且可见
        )
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