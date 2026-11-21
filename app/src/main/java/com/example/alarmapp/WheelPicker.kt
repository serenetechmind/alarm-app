package com.example.alarmapp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A scroll wheel that snaps to one value. onSelected fires when scrolling settles. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    labels: List<String>,
    initialIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    visibleItems: Int = 5,
    itemHeight: Dp = 52.dp,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    val latest by rememberUpdatedState(onSelected)

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (!scrolling) latest(listState.firstVisibleItemIndex.coerceIn(0, labels.lastIndex))
        }
    }

    Box(modifier.height(itemHeight * visibleItems), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(itemHeight)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, RoundedCornerShape(16.dp))
        )
        LazyColumn(
            state = listState,
            flingBehavior = fling,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItems / 2)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(labels.size) { i ->
                Box(Modifier.fillMaxWidth().height(itemHeight), contentAlignment = Alignment.Center) {
                    Text(
                        labels[i],
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
