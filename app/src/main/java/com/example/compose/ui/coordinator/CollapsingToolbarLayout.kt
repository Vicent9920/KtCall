package com.example.compose.ui.coordinator

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 模拟CoordinatorLayout + CollapsingToolbarLayout的Compose实现
 *
 * 设计原理对照：
 * ┌─────────────────────────────────────────────────────────────┐
 * │ XML (CoordinatorLayout)          │ Compose (本实现)          │
 * ├─────────────────────────────────────────────────────────────┤
 * │ CoordinatorLayout                 │ CoordinatorLayout        │
 * │ AppBarLayout                      │ CollapsingHeader         │
 * │ CollapsingToolbarLayout           │ (header参数)             │
 * │ app:layout_behavior               │ NestedScrollConnection   │
 * │ app:layout_scrollFlags            │ CollapsingToolbarState   │
 * │ FloatingActionButton              │ fab参数                  │
 * │ RecyclerView                      │ content (LazyColumn)     │
 * └─────────────────────────────────────────────────────────────┘
 *
 * 滚动事件流程：
 * 1. 用户触摸屏幕产生滚动
 * 2. NestedScrollConnection.onPreScroll 首先拦截
 * 3. 根据滚动方向决定消费量（折叠/展开Toolbar）
 * 4. 剩余滚动量传递给content（LazyColumn）
 * 5. content滚动完成后，onPostScroll处理边界情况
 */
@Composable
fun CoordinatorLayout(
    modifier: Modifier = Modifier,
    maxHeaderHeight: Dp = 420.dp,
    minHeaderHeight: Dp = 56.dp,
    toolbarState: CollapsingToolbarState? = null,
    header: @Composable BoxScope.(progress: Float, currentHeight: Dp) -> Unit,
    fab: (@Composable BoxScope.(progress: Float) -> Unit)? = null,
    content: @Composable BoxScope.(topPadding: Dp) -> Unit
) {
    val density = LocalDensity.current
    val maxHeightPx = with(density) { maxHeaderHeight.toPx() }
    val minHeightPx = with(density) { minHeaderHeight.toPx() }

    // 使用传入的state或创建新的
    val state = toolbarState ?: rememberCollapsingToolbarState(maxHeightPx, minHeightPx)

    // 当前Header高度（Dp）
    val currentHeaderHeight = with(density) { state.currentHeight.toDp() }

    Box(
        modifier = modifier
            .fillMaxSize()
            // 关键：将NestedScrollConnection附加到容器
            // 这相当于CoordinatorLayout设置了默认的Behavior
            .nestedScroll(state.nestedScrollConnection)
    ) {
        // Content区域（相当于RecyclerView with layout_behavior）
        // 需要添加顶部padding避免被Header遮挡
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = currentHeaderHeight)
        ) {
            content(currentHeaderHeight)
        }

        // Header区域（相当于AppBarLayout + CollapsingToolbarLayout）
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(currentHeaderHeight)
                .align(Alignment.TopCenter)
        ) {
            header(state.progress, currentHeaderHeight)
        }

        // FAB区域
        fab?.invoke(this, state.progress)
    }
}

/**
 * 折叠Header的具体实现
 *
 * 包含两层：
 * 1. expandedContent: 展开时显示的内容（大标题等），随折叠渐隐
 * 2. collapsedContent: 折叠时显示的内容（Toolbar），随折叠渐显
 *
 * 这对应XML中的：
 * - CollapsingToolbarLayout的title
 * - Toolbar
 *
 * 视差效果实现原理：
 * parallaxMultiplier 控制内容滚动速度与容器滚动速度的比率
 * - 0f: 内容固定不动（pin效果）
 * - 0.5f: 内容以容器一半速度滚动（视差效果）
 * - 1f: 内容与容器同速滚动（正常滚动）
 */
@Composable
fun CollapsingHeader(
    modifier: Modifier = Modifier,
    progress: Float,
    currentHeight: Dp,
    maxHeight: Dp,
    backgroundColor: Color = Color(0xFF6200EE),
    parallaxMultiplier: Float = 0.5f,
    expandedContent: @Composable BoxScope.() -> Unit,
    collapsedContent: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val maxHeightPx = with(density) { maxHeight.toPx() }
    val currentHeightPx = with(density) { currentHeight.toPx() }

    // 视差偏移计算
    val parallaxOffset = (maxHeightPx - currentHeightPx) * parallaxMultiplier

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(currentHeight)
            .background(backgroundColor)
    ) {
        // 展开内容（带视差效果）
        // 随着折叠进度增加而渐隐
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // 视差位移
                    translationY = -parallaxOffset
                }
                .alpha(1f - progress),
            content = expandedContent
        )

        // 折叠内容（固定在顶部）
        // 随着折叠进度增加而渐显
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(progress),
            content = collapsedContent
        )
    }
}

/**
 * 带Pin效果的Toolbar
 * 模拟 app:layout_collapseMode="pin" 效果
 *
 * Pin效果：Toolbar始终固定在顶部，不随Header滚动
 */
@Composable
fun PinnedToolbar(
    modifier: Modifier = Modifier,
    progress: Float,
    toolbarHeight: Dp = 56.dp,
    content: @Composable BoxScope.(progress: Float) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(toolbarHeight),
        content = { content(progress) }
    )
}

/**
 * 可折叠的标题文本效果
 *
 * 模拟CollapsingToolbarLayout的title折叠动画：
 * - 展开时：大标题显示在中央
 * - 折叠时：标题缩小并移动到Toolbar位置
 *
 * @param progress 折叠进度 [0f, 1f]
 * @param expandedScale 展开时的缩放比例
 * @param collapsedScale 折叠时的缩放比例
 */
@Composable
fun CollapsingTitle(
    modifier: Modifier = Modifier,
    progress: Float,
    expandedOffsetY: Dp = 0.dp,
    collapsedOffsetY: Dp = 0.dp,
    expandedScale: Float = 1.5f,
    collapsedScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current

    // 动画过渡的缩放值
    val scale by animateFloatAsState(
        targetValue = expandedScale + (collapsedScale - expandedScale) * progress,
        animationSpec = tween(durationMillis = 0), // 即时响应，无延迟
        label = "scale"
    )

    // 动画过渡的Y偏移
    val offsetY = with(density) {
        (expandedOffsetY.toPx() + (collapsedOffsetY.toPx() - expandedOffsetY.toPx()) * progress).roundToInt()
    }

    Box(
        modifier = modifier
            .offset { IntOffset(0, offsetY) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        content()
    }
}
