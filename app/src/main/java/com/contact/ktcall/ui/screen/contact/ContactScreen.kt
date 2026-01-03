package com.contact.ktcall.ui.screen.contact

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.blankj.utilcode.util.LogUtils
import com.contact.ktcall.core.data.ContactData
import com.contact.ktcall.utils.LoadingState
import com.contact.ktcall.utils.getSortKey
import com.github.promeg.pinyinhelper.Pinyin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

// --- 1. 数据模型 ---

data class Contact(
    val id: Long,
    val name: String,
    val phone: String?,
    val color: Color,
    val firstChar: String,
    val isFavorite: Boolean = false
)

// 将 ContactData 转换为 UI 使用的 Contact
fun ContactData.toContact(color: Color): Contact {
    val displayName = this.name ?: "未知联系人"
    // 修复引用问题，调用 ContactDataProcessor 内部的方法或者直接逻辑
    val firstChar = ContactDataProcessor.getPinyinChar(displayName)
    return Contact(
        id = this.id,
        name = displayName,
        phone = this.number ?: "",
        color = color,
        firstChar = firstChar,
        isFavorite = this.isFavorite
    )
}

sealed class ListItem {
    data class TopFunction(val title: String, val icon: ImageVector, val color: Color) : ListItem()
    data class SectionHeader(val char: String) : ListItem()
    data class ContactItem(val data: Contact) : ListItem()
}

// 定义项目在分组中的位置，用于决定圆角形状
enum class ItemPosition {
    Top, Middle, Bottom, Single
}

object ContactDataProcessor {
    private val topFunctions = listOf(
        ListItem.TopFunction("我的个人资料", Icons.Default.Person, Color(0xFFAB47BC)),
        ListItem.TopFunction("添加收藏的联系人", Icons.Default.Star, Color(0xFFFFA726)),
        ListItem.TopFunction("群组", Icons.Default.Group, Color(0xFFBDBDBD))
    )

    private val colors = listOf(
        Color(0xFFE57373), Color(0xFFF06292), Color(0xFFBA68C8), Color(0xFF9575CD),
        Color(0xFF7986CB), Color(0xFF64B5F6), Color(0xFF4FC3F7), Color(0xFF4DB6AC),
        Color(0xFFAED581), Color(0xFFFFD54F), Color(0xFFFFB74D), Color(0xFFA1887F)
    )

    fun getPinyinChar(name: String): String {
        if (name.isEmpty()) return "#"
        val firstChar = name[0]

        return when {
            // 中文字符：转换为拼音并取首字母大写
            Pinyin.isChinese(firstChar) -> {
                Pinyin.toPinyin(firstChar).first().toString()
            }
            // 英文字母：转为大写
            firstChar.isLetter() -> {
                firstChar.uppercaseChar().toString()
            }
            // 数字或其它符号：返回 #
            else -> "#"
        }
    }

    // 将 ContactData 列表转换为分组的 ListItem 列表
    fun getGroupedListFromContactData(contactDataList: List<ContactData>): List<ListItem> {
        val result = mutableListOf<ListItem>()
        result.addAll(topFunctions)

        if (contactDataList.isNotEmpty()) {
            // 转换 ContactData 为 Contact，并分配颜色
            val contacts = contactDataList.mapIndexed { index, contactData ->
                contactData.toContact(colors[index % colors.size])
            }

            // 按首字母分组
            contacts.groupBy { it.firstChar.getSortKey() }.forEach { (char, list) ->
                result.add(ListItem.SectionHeader(char))
                list.forEach { contact -> result.add(ListItem.ContactItem(contact)) }
            }
        }

        return result
    }
}

// --- 2. 主屏幕 ---

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(viewModel: ContactsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val listItems = remember(uiState.items) {
        ContactDataProcessor.getGroupedListFromContactData(uiState.items)
    }
    val contactCount = uiState.items.size
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // *** 关键状态：当前展开的联系人 ID ***
    // 互斥逻辑：同一时间只能有一个 ID 被赋值
    var expandedContactId by remember { mutableStateOf<Long?>(null) }

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

    LaunchedEffect(Unit) {
        viewModel.uiState.collect {
            if (it.loadingState == LoadingState.SUCCESS){
                it.items.take(5).forEachIndexed { index, data ->
                    LogUtils.e("ContactData: $data")
                    if (index == 0){
                        viewModel.queryContactNumber(data)
                    }
                }
            }
        }
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
                        // 判断当前项是否展开
                        val isExpanded = expandedContactId == item.data.id

                        // 联系人行（带侧滑、圆角、边距、手风琴动画）
                        SwipeableContactRow(
                            contact = item.data,
                            position = position,
                            showDivider = showDivider,
                            isExpanded = isExpanded,
                            onItemClick = {
                                // 切换展开状态：如果点的是当前已展开的，则收起(null)；否则展开新的
                                expandedContactId = if (isExpanded) null else item.data.id
                            }
                        )
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

// *** 核心修改组件：支持侧滑 + 点击展开动画 ***
@Composable
fun SwipeableContactRow(
    contact: Contact,
    position: ItemPosition,
    showDivider: Boolean,
    isExpanded: Boolean,
    onItemClick: () -> Unit
) {
    // 侧滑偏移量
    var offsetX by remember { mutableFloatStateOf(0f) }
    val screenWidth = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val actionThreshold = screenWidth * 0.3f
    val shape = getShapeForPosition(position)

    // 如果变成展开状态，强制复位侧滑（防止侧滑开着的时候点了展开，导致UI错位）
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            offsetX = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(shape)
            // *** 关键修改：禁用侧滑逻辑 ***
            // 1. 将 isExpanded 作为 key，状态变化时重新构建输入处理
            .pointerInput(isExpanded) {
                // 2. 只有在【未展开】时才检测手势
                if (!isExpanded) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                offsetX <= -actionThreshold -> {
                                    callContact(contact)
                                    offsetX = 0f
                                }
                                offsetX >= actionThreshold -> {
                                    sendMessage(contact)
                                    offsetX = 0f
                                }
                                else -> offsetX = 0f
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount).coerceIn(-screenWidth, screenWidth)
                        }
                    )
                }
            }
    ) {
        // 1. 侧滑背景层 (Swipe Layer)
        // 仅在未展开且有偏移时显示，或者为了动画平滑始终保留但被前景遮挡
        if (!isExpanded) {
            Row(modifier = Modifier.matchParentSize()) {
                // 左滑背景 (绿色 - 呼叫)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF4CAF50)),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    if (offsetX < 0) { // 优化性能：只有滑动时才渲染内部图标
                        Row(modifier = Modifier.padding(end = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("呼叫", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Call, contentDescription = null, tint = Color.White)
                        }
                    }
                }
                // 右滑背景 (蓝色 - 短信)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color(0xFF2196F3)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (offsetX > 0) {
                        Row(modifier = Modifier.padding(start = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Email, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("短信", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 2. 前景白色内容 (Layer 2)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.toInt(), 0) }
                .background(Color.White)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    // 如果有侧滑偏移，点击则是复位；否则是执行点击回调（展开/收起）
                    if (offsetX != 0f) offsetX = 0f else onItemClick()
                }
        ) {
            Column(
                modifier = Modifier.animateContentSize() // 高度动画
            ) {
                // 2.1 头部信息 (始终显示，高度 72dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
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
                            text = contact.name.first().toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
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

                // 2.2 展开后的详细区域 (根据截图调整布局)
                if (isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            // 左侧缩进对齐名字 (16dp margin + 40dp avatar + 16dp spacer = 72dp)
                            .padding(start = 72.dp, end = 24.dp, bottom = 24.dp)
                    ) {
                        // 手机号：加粗，黑色
                        val displayPhone = if (contact.phone.isNullOrEmpty()) "暂无号码" else contact.phone
                        Text(
                            text = "手机 $displayPhone",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold, // 截图显示号码较粗
                                fontSize = 16.sp
                            ),
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(20.dp)) // 增加间距，让布局更像截图那样宽松

                        // 操作按钮组
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween // 两端对齐，中间自动分配间距
                        ) {
                            // 呼叫 (绿)
                            ActionButton(
                                icon = Icons.Default.Call,
                                backgroundColor = Color(0xFF4CAF50),
                                onClick = { callContact(contact) }
                            )
                            // 短信 (蓝)
                            ActionButton(
                                icon = Icons.Filled.ChatBubble, // 实心气泡
                                backgroundColor = Color(0xFF2196F3),
                                onClick = { sendMessage(contact) }
                            )
                            // 视频 (绿 - 截图是摄像机)
                            ActionButton(
                                icon = Icons.Default.Videocam,
                                backgroundColor = Color(0xFF4CAF50),
                                onClick = { /* Video Call */ }
                            )
                            // 信息 (灰 - 截图是 i)
                            ActionButton(
                                icon = Icons.Default.Info, // 或者 Icons.Outlined.Info
                                backgroundColor = Color(0xFF9E9E9E), // 灰色
                                onClick = { /* Info */ }
                            )
                        }
                    }
                }

                // 2.3 分割线
                if (showDivider) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 72.dp),
                        color = Color(0xFFE0E0E0),
                        thickness = 0.5.dp
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = backgroundColor,
        modifier = Modifier.size(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                // 图标大小保持标准或微调
                modifier = Modifier.size(24.dp)
            )
        }
    }
}



// Mock actions
fun callContact(contact: Contact) { println("Call ${contact.name}") }
fun sendMessage(contact: Contact) { println("Msg ${contact.name}") }