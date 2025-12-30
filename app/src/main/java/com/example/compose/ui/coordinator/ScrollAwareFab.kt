package com.example.compose.ui.coordinator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.dp

/**
 * FAB滚动感知状态
 *
 * 实现原理：
 * 监听滚动事件，当用户向上滑动时隐藏FAB，向下滑动时显示FAB
 * 这对应XML中的 app:layout_behavior="com.google.android.material.behavior.HideBottomViewOnScrollBehavior"
 *
 * @param scrollThreshold 触发显示/隐藏的滚动阈值
 */
@Stable
class FabScrollState(
    private val scrollThreshold: Float = 50f
) {
    /**
     * FAB是否可见
     */
    var isVisible by mutableFloatStateOf(1f)
        private set

    /**
     * 累计滚动量
     */
    private var accumulatedScroll = 0f

    /**
     * 滚动方向：1 = 向下（显示FAB），-1 = 向上（隐藏FAB）
     */
    private var scrollDirection = 0

    /**
     * NestedScrollConnection实现
     * 监听滚动方向来控制FAB显示/隐藏
     */
    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y

            // 判断滚动方向变化
            val newDirection = when {
                delta > 0 -> 1  // 向下滑动（手指下滑，内容上移）
                delta < 0 -> -1 // 向上滑动（手指上滑，内容下移）
                else -> scrollDirection
            }

            // 方向改变时重置累计值
            if (newDirection != scrollDirection) {
                accumulatedScroll = 0f
                scrollDirection = newDirection
            }

            // 累计滚动量
            accumulatedScroll += kotlin.math.abs(delta)

            // 达到阈值时更新可见性
            if (accumulatedScroll >= scrollThreshold) {
                isVisible = if (scrollDirection > 0) 1f else 0f
            }

            return Offset.Zero // 不消费滚动，让其传递给子组件
        }
    }

    /**
     * 强制显示FAB
     */
    fun show() {
        isVisible = 1f
    }

    /**
     * 强制隐藏FAB
     */
    fun hide() {
        isVisible = 0f
    }
}

/**
 * 创建并记住FabScrollState
 */
@Composable
fun rememberFabScrollState(
    scrollThreshold: Float = 50f
): FabScrollState {
    return remember { FabScrollState(scrollThreshold) }
}

/**
 * 根据LazyListState判断是否应该显示"滚动到顶部"FAB
 *
 * 规则：
 * 1. 当列表滚动超过一定距离时显示
 * 2. 当列表在顶部时隐藏
 *
 * @param listState LazyColumn的状态
 * @param showThreshold 显示FAB的滚动item阈值
 */
@Composable
fun rememberScrollToTopFabVisibility(
    listState: LazyListState,
    showThreshold: Int = 2
): Boolean {
    val showButton by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex >= showThreshold ||
                    (listState.firstVisibleItemIndex > 0 && listState.firstVisibleItemScrollOffset > 0)
        }
    }
    return showButton
}

/**
 * 滚动到顶部FAB组件
 *
 * 特性：
 * 1. 当列表滚动一定距离后自动显示
 * 2. 点击后滚动到列表顶部
 * 3. 带有动画效果的显示/隐藏
 *
 * 这对应XML中常见的FAB + Behavior模式
 */
@Composable
fun BoxScope.ScrollToTopFab(
    modifier: Modifier = Modifier,
    visible: Boolean,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp),
        enter = fadeIn(animationSpec = tween(300)) +
                scaleIn(animationSpec = tween(300)) +
                slideInVertically(
                    animationSpec = tween(300),
                    initialOffsetY = { it / 2 }
                ),
        exit = fadeOut(animationSpec = tween(300)) +
                scaleOut(animationSpec = tween(300)) +
                slideOutVertically(
                    animationSpec = tween(300),
                    targetOffsetY = { it / 2 }
                )
    ) {
        FloatingActionButton(
            onClick = onClick
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Scroll to top"
            )
        }
    }
}

/**
 * 组合多个NestedScrollConnection
 *
 * 用于同时处理多个滚动联动效果，如：
 * 1. CollapsingToolbar折叠
 * 2. FAB显示/隐藏
 *
 * 这类似于CoordinatorLayout可以有多个Behavior同时工作
 */
@Composable
fun rememberCombinedNestedScrollConnection(
    vararg connections: NestedScrollConnection
): NestedScrollConnection {
    return remember(connections) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                var remaining = available
                for (connection in connections) {
                    val consumed = connection.onPreScroll(remaining, source)
                    remaining = Offset(
                        remaining.x - consumed.x,
                        remaining.y - consumed.y
                    )
                }
                return Offset(
                    available.x - remaining.x,
                    available.y - remaining.y
                )
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                var remaining = available
                for (connection in connections) {
                    val postConsumed = connection.onPostScroll(consumed, remaining, source)
                    remaining = Offset(
                        remaining.x - postConsumed.x,
                        remaining.y - postConsumed.y
                    )
                }
                return Offset(
                    available.x - remaining.x,
                    available.y - remaining.y
                )
            }
        }
    }
}
