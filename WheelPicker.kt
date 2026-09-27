package com.aditya.wakey.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aditya.wakey.ui.theme.ClockStyle
import com.aditya.wakey.ui.theme.W

private val ITEM_H = 64.dp

/**
 * iOS/Alarmy style scroll wheel. Shows 3 rows; the middle row is the selection.
 * [loop] makes it endless (hours/minutes).
 */
@Composable
fun WheelPicker(
    values: List<String>,
    selected: Int,
    onSelected: (Int) -> Unit,
    loop: Boolean,
    width: Dp = 84.dp,
) {
    val count = values.size
    val total = if (loop) count * 1000 else count + 2
    val initialFirst = if (loop) count * 500 + selected - 1 else selected
    val state = rememberLazyListState(initialFirstVisibleItemIndex = initialFirst)
    val itemPx = with(LocalDensity.current) { ITEM_H.toPx() }

    val first by remember {
        derivedStateOf {
            state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
        }
    }
    val cb by rememberUpdatedState(onSelected)

    LaunchedEffect(first) {
        val v = if (loop) (first + 1) % count else first.coerceIn(0, count - 1)
        cb(v)
    }
    // Snap to the nearest row when the finger lets go.
    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress && state.firstVisibleItemScrollOffset != 0) {
            state.animateScrollToItem(first)
        }
    }

    Box(Modifier.width(width).height(ITEM_H * 3), contentAlignment = Alignment.Center) {
        LazyColumn(
            state = state,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(width).height(ITEM_H * 3),
        ) {
            items(total) { i ->
                val label = when {
                    loop -> values[i % count]
                    i == 0 || i == total - 1 -> ""
                    else -> values[i - 1]
                }
                val isSel = i == first + 1
                Box(Modifier.height(ITEM_H).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        label,
                        style = ClockStyle,
                        fontSize = if (isSel) 42.sp else 30.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSel) Color.White else W.Text2.copy(alpha = 0.55f),
                    )
                }
            }
        }
        // fade top and bottom rows
        Box(
            Modifier.width(width).height(ITEM_H * 3).background(
                Brush.verticalGradient(
                    0f to W.Bg, 0.3f to Color.Transparent, 0.7f to Color.Transparent, 1f to W.Bg,
                ),
            ),
        )
    }
}
