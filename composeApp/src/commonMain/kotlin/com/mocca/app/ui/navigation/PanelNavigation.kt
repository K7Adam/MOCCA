package com.mocca.app.ui.navigation

import androidx.compose.foundation.pager.PagerState

/**
 * Semantic enum representing which of the 3 main panels is active.
 *
 * Maps to HorizontalPager page indices:
 *   LEFT_OPEN  -> page 0 (Sessions / Context History)
 *   CENTER     -> page 1 (Chat)          <-- initial page
 *   RIGHT_OPEN -> page 2 (Tools / Dashboard)
 */
enum class PanelState {
    CENTER,
    LEFT_OPEN,
    RIGHT_OPEN
}

/** Converts a [PanelState] to its corresponding HorizontalPager page index. */
fun PanelState.toPageIndex(): Int = when (this) {
    PanelState.LEFT_OPEN  -> 0
    PanelState.CENTER     -> 1
    PanelState.RIGHT_OPEN -> 2
}

/** Converts a HorizontalPager page index back to its [PanelState]. */
fun Int.toPanelState(): PanelState = when (this) {
    0    -> PanelState.LEFT_OPEN
    2    -> PanelState.RIGHT_OPEN
    else -> PanelState.CENTER
}

/**
 * Computes the bottom-nav drag progress from [PagerState] in real-time.
 *
 * Maps page position to the 0.0–1.0 range used by [PersistentNavRow]:
 *   page 0 (Sessions) = 1.0f
 *   page 1 (Chat)     = 0.5f
 *   page 2 (Tools)    = 0.0f
 *
 * This value changes continuously during a swipe gesture, enabling the
 * nav indicator to track finger position live without triggering recomposition
 * in the caller (use inside [derivedStateOf] or [graphicsLayer]).
 */
fun PagerState.toDragProgress(): Float {
    val pageWithOffset = currentPage + currentPageOffsetFraction
    return (1f - pageWithOffset / 2f).coerceIn(0f, 1f)
}
