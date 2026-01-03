package com.example.compose.ui.demo

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contact.ktcall.ui.screen.coordinator.CollapsingHeader
import com.contact.ktcall.ui.screen.coordinator.CoordinatorLayout
import com.contact.ktcall.ui.screen.coordinator.ScrollToTopFab
import com.contact.ktcall.ui.screen.coordinator.rememberCollapsingToolbarState
import com.contact.ktcall.ui.screen.coordinator.rememberScrollToTopFabVisibility
import kotlinx.coroutines.launch

/**
 * 联系人列表页面 - 完整演示
 *
 * 功能对照：
 * ┌─────────────────────────────────────────────────────────────────────┐
 * │ XML布局                              │ Compose实现                   │
 * ├─────────────────────────────────────────────────────────────────────┤
 * │ CoordinatorLayout                     │ CoordinatorLayout             │
 * │ ├── AppBarLayout (420dp)              │ header参数                    │
 * │ │   └── CollapsingToolbarLayout       │ CollapsingHeader              │
 * │ │       ├── LinearLayout (大标题)     │ expandedContent               │
 * │ │       └── Toolbar (操作栏)          │ collapsedContent              │
 * │ ├── RecyclerView                      │ content (LazyColumn)          │
 * │ │   layout_behavior=scroll            │ NestedScrollConnection        │
 * │ └── FloatingActionButton              │ fab参数 + AnimatedVisibility  │
 * │     layout_behavior=hide              │ rememberScrollToTopFabVisible │
 * └─────────────────────────────────────────────────────────────────────┘
 *
 * 滚动联动行为：
 * 1. 向上滑动 → AppBar折叠 → Toolbar固定 → 继续滚动列表
 * 2. 向下滑动 → 列表滚动到顶部 → AppBar展开
 * 3. 滚动一定距离后 → 显示"回到顶部"FAB
 */
@Composable
fun ContactListScreen() {
    val density = LocalDensity.current
    val maxHeight = 420.dp
    val minHeight = 56.dp
    val maxHeightPx = with(density) { maxHeight.toPx() }
    val minHeightPx = with(density) { minHeight.toPx() }

    // CollapsingToolbar状态
    val toolbarState = rememberCollapsingToolbarState(maxHeightPx, minHeightPx)

    // 列表状态（用于FAB和滚动控制）
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // FAB可见性
    val showScrollToTop = rememberScrollToTopFabVisibility(listState, showThreshold = 3)

    // 模拟联系人数据
    val contacts = remember { generateMockContacts() }

    CoordinatorLayout(
        modifier = Modifier.fillMaxSize(),
        maxHeaderHeight = maxHeight,
        minHeaderHeight = minHeight,
        toolbarState = toolbarState,
        header = { progress, currentHeight ->
            ContactListHeader(
                progress = progress,
                currentHeight = currentHeight,
                maxHeight = maxHeight,
                contactCount = contacts.size
            )
        },
        fab = { progress ->
            // 滚动到顶部FAB
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
            // 联系人列表
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = contacts,
                    key = { it.id }
                ) { contact ->
                    ContactItem(contact = contact)
                }
            }
        }
    )
}

/**
 * 联系人列表Header
 *
 * 展开状态：显示大标题 + 联系人数量
 * 折叠状态：显示Toolbar（全选/标题/添加/搜索/更多）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.ContactListHeader(
    progress: Float,
    currentHeight: androidx.compose.ui.unit.Dp,
    maxHeight: androidx.compose.ui.unit.Dp,
    contactCount: Int
) {
    val primaryColor = Color(0xFF6200EE)

    CollapsingHeader(
        progress = progress,
        currentHeight = currentHeight,
        maxHeight = maxHeight,
        backgroundColor = primaryColor,
        parallaxMultiplier = 0.5f,
        expandedContent = {
            // 展开时的内容：大标题 + 联系人数量
            ExpandedHeaderContent(contactCount = contactCount)
        },
        collapsedContent = {
            // 折叠时的内容：Toolbar
            CollapsedToolbar()
        }
    )
}

/**
 * 展开状态的Header内容
 */
@Composable
private fun BoxScope.ExpandedHeaderContent(contactCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 大图标
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 大标题
        Text(
            text = "联系人",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 联系人数量
        Text(
            text = "$contactCount 位联系人",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

/**
 * 折叠状态的Toolbar
 *
 * 包含：全选 | 标题 | 添加 | 搜索 | 更多
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.CollapsedToolbar() {
    var isSelectMode by remember { mutableStateOf(false) }

    TopAppBar(
        title = {
            Text(
                text = if (isSelectMode) "选择联系人" else "联系人",
                color = Color.White
            )
        },
        navigationIcon = {
            IconButton(onClick = { isSelectMode = !isSelectMode }) {
                Icon(
                    imageVector = if (isSelectMode)
                        Icons.Default.CheckBox
                    else
                        Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = "全选",
                    tint = Color.White
                )
            }
        },
        actions = {
            IconButton(onClick = { /* 添加联系人 */ }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加",
                    tint = Color.White
                )
            }
            IconButton(onClick = { /* 搜索 */ }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "搜索",
                    tint = Color.White
                )
            }
            IconButton(onClick = { /* 更多选项 */ }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "更多",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        ),
        modifier = Modifier.align(Alignment.BottomCenter)
    )
}

/**
 * 联系人列表项
 */
@Composable
private fun ContactItem(contact: Contact) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { /* 点击联系人 */ },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 头像
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(contact.avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.first().toString(),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // 联系人信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contact.phone,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * 联系人数据类
 */
data class Contact(
    val id: Int,
    val name: String,
    val phone: String,
    val avatarColor: Color
)

/**
 * 生成模拟联系人数据
 */
private fun generateMockContacts(): List<Contact> {
    val names = listOf(
        "张三", "李四", "王五", "赵六", "钱七",
        "孙八", "周九", "吴十", "郑一", "王二",
        "冯三", "陈四", "楚五", "魏六", "蒋七",
        "沈八", "韩九", "杨十", "朱一", "秦二",
        "许三", "何四", "吕五", "施六", "张七",
        "孔八", "曹九", "严十", "华一", "金二"
    )

    val colors = listOf(
        Color(0xFF6200EE),
        Color(0xFF03DAC5),
        Color(0xFFFF5722),
        Color(0xFF2196F3),
        Color(0xFF4CAF50),
        Color(0xFFE91E63),
        Color(0xFF9C27B0),
        Color(0xFF00BCD4)
    )

    return names.mapIndexed { index, name ->
        Contact(
            id = index,
            name = name,
            phone = "1${(38..89).random()}${(10000000..99999999).random()}",
            avatarColor = colors[index % colors.size]
        )
    }
}
