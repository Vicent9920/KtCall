package com.contact.ktcall.ui.screen.contact

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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

// --- 1. 数据模型 ---

data class Contact(
    val id: Int, val name: String, val phone: String, val color: Color, val firstChar: String
)

sealed class ListItem {
    data class TopFunction(val title: String, val icon: ImageVector, val color: Color) : ListItem()
    data class SectionHeader(val char: String) : ListItem()
    data class ContactItem(val data: Contact) : ListItem()
}

// 定义项目在分组中的位置，用于决定圆角形状
enum class ItemPosition {
    Top, Middle, Bottom, Single
}

object MockData {
    private val topFunctions = listOf(
        ListItem.TopFunction("我的个人资料", Icons.Default.Person, Color(0xFFAB47BC)),
        ListItem.TopFunction("添加收藏的联系人", Icons.Default.Star, Color(0xFFFFA726)),
        ListItem.TopFunction("群组", Icons.Default.Group, Color(0xFFBDBDBD))
    )

    private val firstNames = listOf("赵", "钱", "孙", "李", "周", "吴", "郑", "王", "冯", "陈", "蒋", "沈", "韩", "杨")
    private val lastNames = listOf("伟", "芳", "娜", "敏", "静", "秀英", "丽", "强", "磊", "洋", "艳", "勇", "军", "杰", "娟", "涛")
    private val colors = listOf(
        Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8), Color(0xFF9575CD),
        Color(0xFF7986CB), Color(0xFF64B5F6), Color(0xFF4FC3F7), Color(0xFF4DB6AC),
        Color(0xFFAED581), Color(0xFFFFD54F), Color(0xFFFFB74D), Color(0xFFA1887F)
    )

    private fun getPinyinChar(name: String): String {
        return when (name.first().toString()) {
            "赵", "郑", "周", "朱", "张" -> "Z"
            "钱", "强", "秦" -> "Q"
            "孙", "沈" -> "S"
            "李", "雷", "刘" -> "L"
            "吴", "王", "卫", "魏" -> "W"
            "冯", "方" -> "F"
            "陈", "褚" -> "C"
            "蒋", "姜" -> "J"
            "韩", "何" -> "H"
            "杨", "严" -> "Y"
            else -> "#"
        }
    }

    fun getGroupedList(count: Int = 100): List<ListItem> {
        val contacts = List(count) { id ->
            val name = "${firstNames.random()}${lastNames.random()}"
            Contact(
                id = id,
                name = name,
                phone = "13${Random.nextInt(0, 9)} ${Random.nextInt(1000, 9999)} ${Random.nextInt(1000, 9999)}",
                color = colors.random(),
                firstChar = getPinyinChar(name)
            )
        }.sortedBy { it.firstChar }

        val result = mutableListOf<ListItem>()
        result.addAll(topFunctions)
        contacts.groupBy { it.firstChar }.forEach { (char, list) ->
            result.add(ListItem.SectionHeader(char))
            list.forEach { contact -> result.add(ListItem.ContactItem(contact)) }
        }
        return result
    }
}

// --- 2. 主屏幕 ---

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen() {
    val listItems = remember { MockData.getGroupedList(100) }
    val contactCount = remember { listItems.count { it is ListItem.ContactItem } }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Header 动画相关变量
    val density = LocalDensity.current
    val headerHeightDp = 320.dp
    val headerHeightPx = with(density) { headerHeightDp.toPx() }
    var toolbarHeightPx by remember { mutableFloatStateOf(with(density) { 64.dp.toPx() }) }
    val collapseRangePx = headerHeightPx - toolbarHeightPx
    val maxToolbarOffsetPx = collapseRangePx
    val scrollOffsetState = remember { derivedStateOf { listState.firstVisibleItemScrollOffset.toFloat() } }
    val firstVisibleIndexState = remember { derivedStateOf { listState.firstVisibleItemIndex } }

    val collapseFractionState = remember(collapseRangePx) {
        derivedStateOf {
            if (firstVisibleIndexState.value > 0) 1f
            else (scrollOffsetState.value / collapseRangePx).coerceIn(0f, 1f)
        }
    }
    val collapseFraction by collapseFractionState

    val showToolbarBackground by remember { derivedStateOf { collapseFractionState.value > 0.9f } }
    val toolbarAlpha by animateFloatAsState(targetValue = if (showToolbarBackground) 1f else 0f, label = "")
    val toolbarTitleAlpha by animateFloatAsState(targetValue = if (showToolbarBackground) 1f else 0f, label = "")
    val toolbarTranslationY by remember(maxToolbarOffsetPx) {
        derivedStateOf {
            if (firstVisibleIndexState.value == 0) (maxToolbarOffsetPx - scrollOffsetState.value).coerceAtLeast(0f) else 0f
        }
    }
    val bigTitleAlpha by remember {
        derivedStateOf {
            val fraction = collapseFractionState.value
            if (fraction > 0.7f) 1f - ((fraction - 0.7f) / 0.3f).coerceIn(0f, 1f) else 1f
        }
    }

    // 悬浮字母胶囊
    val currentSectionChar by remember {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            if (index == 0) return@derivedStateOf null
            val listIndex = index - 1
            if (listIndex >= 0 && listIndex < listItems.size) {
                when (val item = listItems[listIndex]) {
                    is ListItem.SectionHeader -> item.char
                    is ListItem.ContactItem -> item.data.firstChar
                    else -> null
                }
            } else null
        }
    }
    val showFloatingPill by remember { derivedStateOf { listState.isScrollInProgress && currentSectionChar != null } }

    var showFab by remember { mutableStateOf(false) }
    LaunchedEffect(listState.isScrollInProgress, firstVisibleIndexState.value) {
        if (firstVisibleIndexState.value == 0) showFab = false
        else if (listState.isScrollInProgress) showFab = true
        else if (showFab) { delay(1000); showFab = false }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // 关键：背景色设为浅灰色，为了显示出白色卡片的圆角和边距
            .background(Color(0xFFF2F2F7))
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(headerHeightDp)
                        .background(Color(0xFFF2F2F7)) // Header 背景也跟随整体
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
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$contactCount 位联系人",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.Gray
                        )
                    }
                }
            }

            itemsIndexed(listItems) { index, item ->
                // 计算当前项的位置（Top, Middle, Bottom, Single）以决定圆角
                val position = remember(index, listItems) {
                    getItemPosition(listItems, index)
                }

                // 计算是否需要显示分割线（最后一行不显示）
                val showDivider = position != ItemPosition.Bottom && position != ItemPosition.Single

                when (item) {
                    is ListItem.TopFunction -> {
                        // 顶部功能项（复刻视频：使用圆角卡片样式）
                        TopFunctionRow(item, ItemPosition.Single)
                        if (index != 2){
                            TopFunctionHeaderRow()
                        }
                    }
                    is ListItem.SectionHeader -> {
                        // 字母头（保持灰色背景）
                        SectionHeaderRow(item.char)
                    }
                    is ListItem.ContactItem -> {
                        // 联系人行（带侧滑、圆角、边距）
                        SwipeableContactRow(item.data, position, showDivider)
                    }
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
                containerColor = Color(0xFFF2F2F7).copy(alpha = toolbarAlpha), // Toolbar 也是灰色背景
                scrolledContainerColor = Color(0xFFF2F2F7).copy(alpha = toolbarAlpha)
            ),
            actions = {
                IconButton(onClick = {}) { Icon(Icons.Default.Search, contentDescription = "搜索") }
                IconButton(onClick = {}) { Icon(Icons.Default.Add, contentDescription = "添加") }
                IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, contentDescription = "更多") }
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .onGloballyPositioned { coordinates ->
                    if (abs(toolbarHeightPx - coordinates.size.height.toFloat()) > 1f) toolbarHeightPx = coordinates.size.height.toFloat()
                }
                .graphicsLayer { translationY = toolbarTranslationY }
        )

        // 悬浮胶囊
        AnimatedVisibility(
            visible = showFloatingPill,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = with(LocalDensity.current) { (toolbarHeightPx.toDp() + 16.dp) })
        ) {
            currentSectionChar?.let { char ->
                Surface(
                    color = Color(0xFF424242).copy(alpha = 0.9f),
                    shape = RoundedCornerShape(50),
                    shadowElevation = 4.dp
                ) {
                    Box(modifier = Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                        Text(text = char, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // FAB
        Box(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)) {
            AnimatedVisibility(visible = showFab, enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
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

// --- 3. 辅助逻辑 ---

// 判断 Item 在当前组的位置
fun getItemPosition(list: List<ListItem>, index: Int): ItemPosition {
    val current = list[index]

    // Header 总是没有特殊位置（或者你可以视为独立的，不需要圆角处理逻辑，因为它自身是扁平的）
    if (current is ListItem.SectionHeader) return ItemPosition.Middle

    val prev = list.getOrNull(index - 1)
    val next = list.getOrNull(index + 1)

    // 判断上一项是否同组：如果是 null 或者是 Header，说明当前项是组头
    val isTop = prev == null || prev is ListItem.SectionHeader

    // 判断下一项是否同组：如果是 null 或者是 Header，说明当前项是组尾
    val isBottom = next == null || next is ListItem.SectionHeader

    return when {
        isTop && isBottom -> ItemPosition.Single
        isTop -> ItemPosition.Top
        isBottom -> ItemPosition.Bottom
        else -> ItemPosition.Middle
    }
}

// 获取圆角 Shape
fun getShapeForPosition(position: ItemPosition): Shape {
    val radius = 12.dp // 圆角大小
    return when (position) {
        ItemPosition.Top -> RoundedCornerShape(topStart = radius, topEnd = radius)
        ItemPosition.Bottom -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
        ItemPosition.Single -> RoundedCornerShape(radius)
        ItemPosition.Middle -> RoundedCornerShape(0.dp)
    }
}

// --- 4. 组件实现 ---

@Composable
fun TopFunctionRow(item: ListItem.TopFunction, position: ItemPosition) {
    val shape = getShapeForPosition(position)

    // 外层容器：负责边距和背景色
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // 左右边距
            .clip(shape)
            .background(Color.White)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp) // 高度调整为 72dp
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(item.color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(item.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }

        }
    }
}

@Composable
fun SectionHeaderRow(char: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Header 不需要圆角，且背景是透明或灰色
            .padding(horizontal = 16.dp, vertical = 8.dp) // Header 文字本身也有边距
    ) {
        Text(
            text = char,
            style = MaterialTheme.typography.labelLarge,
            color = Color.Gray,
            fontWeight = FontWeight.Bold
        )
    }
}
@Composable
fun TopFunctionHeaderRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Header 不需要圆角，且背景是透明或灰色
            .padding(horizontal = 16.dp, vertical = 8.dp) // Header 文字本身也有边距
    ) {
    }
}

@Composable
fun SwipeableContactRow(contact: Contact, position: ItemPosition, showDivider: Boolean) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val screenWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val actionThreshold = screenWidth * 0.3f
    val shape = getShapeForPosition(position)

    // 整个 Row 都有左右边距
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp) // 关键：左右留白
            .height(72.dp) // 关键：高度增加到 72dp
            // 应用圆角 Clip，确保侧滑背景和白色前景都被裁切为圆角
            .clip(shape)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(onDragEnd = {
                    when {
                        offsetX <= -actionThreshold -> { callContact(contact); offsetX = 0f }
                        offsetX >= actionThreshold -> { sendMessage(contact); offsetX = 0f }
                        else -> offsetX = 0f
                    }
                }, onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    offsetX = (offsetX + dragAmount).coerceIn(-screenWidth, screenWidth)
                })
            }
    ) {
        // 1. 侧滑背景 (Layer 1)
        // 必须填满父容器，因为父容器已经 Clip 过了，所以这里不需要再 Clip
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // 左滑显示的背景（绿色）
            if (offsetX < 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(modifier = Modifier.padding(end = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("呼叫", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                    }
                }
            }
            // 右滑显示的背景（蓝色）
            if (offsetX > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF2196F3)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(modifier = Modifier.padding(start = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Email, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("短信", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 2. 前景白色内容 (Layer 2)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.toInt(), 0) }
                .background(Color.White)
                .clickable { offsetX = 0f }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(contact.color),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = contact.name.takeLast(1), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = contact.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                        color = Color.Black,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 3. 分割线
            // 分割线必须在 offset 的 Box 内部，这样侧滑时分割线会随白色卡片一起移动
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 72.dp), // 16+40+16
                    color = Color(0xFFE0E0E0),
                    thickness = 0.5.dp
                )
            }
        }
    }
}

// Mock actions
fun callContact(contact: Contact) { println("Call ${contact.name}") }
fun sendMessage(contact: Contact) { println("Msg ${contact.name}") }