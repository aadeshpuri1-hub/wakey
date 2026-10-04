package app.upwake.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.upwake.ui.theme.ClockStyle
import app.upwake.ui.theme.W

val WHEEL_ITEM_H = 60.dp

/**
 * Alarmy/iOS style scroll wheel: 3 rows visible, middle row is the selection,
 * native snap fling and a haptic tick on every step.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    values: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit,
    loop: Boolean,
    width: Dp = 84.dp,
    bg: Color = W.Card,
) {
    val count = values.size
    val total = if (loop) count * 1000 else count + 2
    val initialFirst = if (loop) count * 500 + selected - 1 else selected
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialFirst)
    val fling = rememberSnapFlingBehavior(lazyListState = state)
    val itemPx = with(LocalDensity.current) { WHEEL_ITEM_H.toPx() }
    val haptics = LocalHapticFeedback.current

    val first by remember {
        derivedStateOf {
            state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
        }
    }
    val cb by rememberUpdatedState(onSelected)
    val lastFirst = remember { intArrayOf(initialFirst) }

    LaunchedEffect(first) {
        if (first != lastFirst[0]) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastFirst[0] = first
        }
        cb(if (loop) (first + 1) % count else first.coerceIn(0, count - 1))
    }

    Box(Modifier.width(width).height(WHEEL_ITEM_H * 3), contentAlignment = Alignment.Center) {
        LazyColumn(
            state = state,
            flingBehavior = fling,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(width).height(WHEEL_ITEM_H * 3),
        ) {
            items(total) { i ->
                val label = when {
                    loop -> values[i % count]
                    i == 0 || i == total - 1 -> ""
                    else -> values[i - 1]
                }
                val isSel = i == first + 1
                Box(Modifier.height(WHEEL_ITEM_H).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        style = ClockStyle,
                        fontSize = if (isSel) 40.sp else 30.sp,
                        color = if (isSel) W.Text else W.Text3,
                    )
                }
            }
        }
        Box(
            Modifier.width(width).height(WHEEL_ITEM_H * 3).background(
                Brush.verticalGradient(
                    0f to bg, 0.28f to Color.Transparent, 0.72f to Color.Transparent, 1f to bg,
                ),
            ),
        )
    }
}
