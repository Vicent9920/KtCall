package com.contact.ktcall.ui.screen.coordinator

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity

/**
 * CollapsingToolbar状态管理类
 *
 * 核心原理：
 * 1. 通过NestedScrollConnection拦截子组件的滚动事件
 * 2. 优先消费Toolbar折叠所需的滚动量
 * 3. 剩余滚动量传递给子组件（如LazyColumn）
 *
 * 这与CoordinatorLayout的Behavior机制完全对应：
 * - onPreScroll = AppBarLayout.Behavior的onNestedPreScroll
 * - onPostScroll = AppBarLayout.Behavior的onNestedScroll
 * - onPreFling = 处理快速滑动的惯性
 *
 * @param maxHeight Toolbar最大高度（展开状态）
 * @param minHeight Toolbar最小高度（折叠状态）
 * @param initialOffset 初始偏移量
 */
@Stable
class CollapsingToolbarState(
    val maxHeight: Float,
    val minHeight: Float,
    initialOffset: Float = 0f
) {
    /**
     * 当前滚动偏移量
     * - 0 = 完全展开
     * - (maxHeight - minHeight) = 完全折叠
     */
    var scrollOffset by mutableFloatStateOf(initialOffset)
        private set

    /**
     * 可折叠的总范围
     */
    val scrollRange: Float
        get() = maxHeight - minHeight

    /**
     * 当前Toolbar高度
     */
    val currentHeight: Float
        get() = maxHeight - scrollOffset

    /**
     * 折叠进度 [0f, 1f]
     * 0f = 完全展开
     * 1f = 完全折叠
     */
    val progress: Float
        get() = if (scrollRange > 0f) {
            (scrollOffset / scrollRange).coerceIn(0f, 1f)
        } else 0f

    /**
     * 是否完全折叠
     */
    val isCollapsed: Boolean
        get() = scrollOffset >= scrollRange

    /**
     * 是否完全展开
     */
    val isExpanded: Boolean
        get() = scrollOffset <= 0f

    /**
     * NestedScrollConnection实现
     *
     * 关键点：
     * 1. onPreScroll：在子组件滚动之前，先消费Toolbar需要的滚动量
     *    - 向上滑动（available.y < 0）：折叠Toolbar
     *    - 向下滑动（available.y > 0）：展开Toolbar（仅当列表在顶部时）
     *
     * 2. onPostScroll：子组件滚动完成后，处理剩余的滚动量
     *    - 用于处理边界情况，如列表滚动到顶部后继续展开Toolbar
     */
    val nestedScrollConnection = object : NestedScrollConnection {

        /**
         * 在子组件消费滚动之前调用
         *
         * @param available 可用的滚动量
         * @param source 滚动来源（拖动/Fling）
         * @return 本层消费的滚动量
         */
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            val delta = available.y

            // 向上滑动（delta < 0）：优先折叠Toolbar
            if (delta < 0) {
                val consumed = consumeScrollOffset(-delta)
                return if (consumed != 0f) {
                    Offset(0f, -consumed)
                } else {
                    Offset.Zero
                }
            }

            return Offset.Zero
        }

        /**
         * 在子组件消费滚动之后调用
         *
         * @param consumed 子组件已消费的滚动量
         * @param available 剩余可用的滚动量
         * @param source 滚动来源
         * @return 本层消费的剩余滚动量
         */
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource
        ): Offset {
            val delta = available.y

            // 向下滑动（delta > 0）：展开Toolbar
            // 只有当子组件不再消费滚动时（列表已在顶部），才展开Toolbar
            if (delta > 0) {
                val consumed = consumeScrollOffset(-delta)
                return if (consumed != 0f) {
                    Offset(0f, -consumed)
                } else {
                    Offset.Zero
                }
            }

            return Offset.Zero
        }

        /**
         * 处理Fling开始前的预处理
         * 可用于在快速滑动时提供更平滑的体验
         */
        override suspend fun onPreFling(available: Velocity): Velocity {
            return Velocity.Zero
        }

        /**
         * 处理Fling结束后的剩余速度
         */
        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
            return Velocity.Zero
        }
    }

    /**
     * 消费滚动偏移量
     *
     * @param delta 要消费的滚动量（正值=折叠，负值=展开）
     * @return 实际消费的滚动量
     */
    private fun consumeScrollOffset(delta: Float): Float {
        val oldOffset = scrollOffset
        val newOffset = (scrollOffset + delta).coerceIn(0f, scrollRange)
        scrollOffset = newOffset
        return newOffset - oldOffset
    }

    /**
     * 手动设置滚动偏移
     */
    fun snapTo(offset: Float) {
        scrollOffset = offset.coerceIn(0f, scrollRange)
    }

    /**
     * 展开Toolbar
     */
    fun expand() {
        scrollOffset = 0f
    }

    /**
     * 折叠Toolbar
     */
    fun collapse() {
        scrollOffset = scrollRange
    }

    companion object {
        /**
         * 用于状态保存和恢复的Saver
         */
        fun Saver(maxHeight: Float, minHeight: Float): Saver<CollapsingToolbarState, Float> = Saver(
            save = { it.scrollOffset },
            restore = { CollapsingToolbarState(maxHeight, minHeight, it) }
        )
    }
}

/**
 * 创建并记住CollapsingToolbarState
 *
 * @param maxHeightPx Toolbar最大高度（像素）
 * @param minHeightPx Toolbar最小高度（像素）
 */
@Composable
fun rememberCollapsingToolbarState(
    maxHeightPx: Float,
    minHeightPx: Float
): CollapsingToolbarState {
    return rememberSaveable(
        saver = CollapsingToolbarState.Saver(maxHeightPx, minHeightPx)
    ) {
        CollapsingToolbarState(maxHeightPx, minHeightPx)
    }
}
