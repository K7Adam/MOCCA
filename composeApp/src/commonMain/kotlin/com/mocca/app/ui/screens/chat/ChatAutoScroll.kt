package com.mocca.app.ui.screens.chat

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch

/**
 * Scroll direction state for dock auto-hide functionality.
 */
enum class ScrollDirection {
    UP,      // Scrolling up (reading older messages) - hide dock
    DOWN,    // Scrolling down (reading newer messages) - show dock
    IDLE     // Not scrolling - show dock
}

/**
 * State holder for auto-scroll behavior.
 * Tracks whether user is at bottom and manages auto-scroll decisions.
 */
data class AutoScrollState(
    val isAtBottom: Boolean = true,
    val shouldAutoScroll: Boolean = true,
    val userHasScrolledUp: Boolean = false
)

/**
 * Threshold (in items) to determine if user is "at bottom" in reverse layout.
 * With reverseLayout=true, index 0 is the bottom (newest messages).
 */
private const val BOTTOM_THRESHOLD = 2

/**
 * Creates and manages auto-scroll state for a LazyColumn with reverseLayout.
 *
 * With reverseLayout = true:
 * - Index 0 is the BOTTOM (newest messages)
 * - Higher indices are at the TOP (older messages)
 *
 * @param listState The LazyListState to track
 * @param enabled Whether auto-scroll is enabled
 * @return AutoScrollState indicating current scroll position state
 */
@Composable
fun rememberAutoScrollState(
    listState: LazyListState,
    enabled: Boolean = true
): AutoScrollState {
    val isAtBottom by remember(listState) {
        derivedStateOf {
            if (!enabled) true
            else listState.firstVisibleItemIndex <= BOTTOM_THRESHOLD
        }
    }

    val userHasScrolledUp by remember(listState) {
        derivedStateOf {
            if (!enabled) false
            else {
                listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 50
            }
        }
    }

    val shouldAutoScroll by remember(listState) {
        derivedStateOf {
            if (!enabled) false
            else isAtBottom
        }
    }

    return remember(isAtBottom, shouldAutoScroll, userHasScrolledUp) {
        AutoScrollState(
            isAtBottom = isAtBottom,
            shouldAutoScroll = shouldAutoScroll,
            userHasScrolledUp = userHasScrolledUp
        )
    }
}

/**
 * Auto-scroll effect that smoothly scrolls to bottom when new content arrives.
 * Respects user scroll position - won't auto-scroll if user is reading history.
 *
 * @param listState The LazyListState to control
 * @param messageCount Current message count (triggers scroll when changed)
 * @param streamingText Current streaming text (triggers scroll when non-empty)
 * @param autoScrollState Current auto-scroll state
 * @param enabled Whether auto-scroll is enabled
 */
@Composable
fun AutoScrollEffect(
    listState: LazyListState,
    messageCount: Int,
    streamingText: String,
    autoScrollState: AutoScrollState,
    enabled: Boolean = true
) {
    val coroutineScope = rememberCoroutineScope()

    var lastMessageCount by remember { mutableStateOf(messageCount) }
    var wasStreaming by remember { mutableStateOf(false) }

    LaunchedEffect(messageCount, streamingText, enabled) {
        if (!enabled) return@LaunchedEffect

        val messagesChanged = messageCount != lastMessageCount
        val startedStreaming = streamingText.isNotEmpty() && !wasStreaming
        val isStreaming = streamingText.isNotEmpty()

        val shouldScroll = autoScrollState.shouldAutoScroll && (
                messagesChanged ||
                        startedStreaming ||
                        (isStreaming && autoScrollState.isAtBottom)
                )

        if (shouldScroll) {
            coroutineScope.launch {
                listState.animateScrollToItem(
                    index = 0,
                    scrollOffset = 0
                )
            }
        }

        lastMessageCount = messageCount
        wasStreaming = streamingText.isNotEmpty()
    }
}

/**
 * Extension function to smoothly scroll to the bottom of a reverse-layout LazyColumn.
 *
 * For long distances, snap close to the latest content first, then animate the
 * final approach so the interaction stays responsive instead of animating
 * through a large portion of the list.
 */
suspend fun LazyListState.animateScrollToBottom() {
    val currentIndex = firstVisibleItemIndex
    val longDistanceThreshold = 40
    val animatedApproachIndex = 6

    if (currentIndex > longDistanceThreshold) {
        scrollToItem(index = animatedApproachIndex, scrollOffset = 0)
    }

    animateScrollToItem(index = 0, scrollOffset = 0)
}

/**
 * Extension function to instantly scroll to the bottom.
 */
suspend fun LazyListState.scrollToBottom() {
    scrollToItem(index = 0, scrollOffset = 0)
}
