package com.mocca.app.ui.screens.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mocca.app.ui.navigation.PanelProgressHolder
import com.mocca.app.ui.navigation.PanelState
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.LocalAppPerformance

@Composable
internal fun MainScreenPanelHost(
    panels: List<MainPanelDefinition>,
    panelState: PanelState,
    onPanelStateChange: (PanelState) -> Unit,
    progressHolder: PanelProgressHolder,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    val centerPanel = MainScreenPanelRegistry.getByPanelState(panels, PanelState.CENTER)
        ?: panels.first { it.placement == MainPanelPlacement.CENTER }
    val activePanel = MainScreenPanelRegistry.getByPanelState(panels, panelState) ?: centerPanel

    val targetProgress = when (panelState) {
        PanelState.RIGHT_OPEN -> 0f
        PanelState.CENTER -> 0.5f
        PanelState.LEFT_OPEN -> 1f
    }
    val performance = LocalAppPerformance.current
    val panelProgress = if (performance.useHeavyNavigationMotion) {
        val animatedProgress by animateFloatAsState(
            targetValue = targetProgress,
            animationSpec = MaterialTheme.motionScheme.defaultEffectsSpec(),
            label = "mainScreenPanelProgress"
        )
        animatedProgress
    } else {
        targetProgress
    }

    LaunchedEffect(panelProgress) {
        progressHolder.updateProgress(panelProgress)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        centerPanel.content()

        if (activePanel.id != centerPanel.id) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppColors.scrim.copy(alpha = 0.42f))
                    .clickable { onPanelStateChange(PanelState.CENTER) }
            )

            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(max = 420.dp)
                    .align(
                        when (activePanel.placement) {
                            MainPanelPlacement.LEFT -> Alignment.CenterStart
                            MainPanelPlacement.CENTER -> Alignment.Center
                            MainPanelPlacement.RIGHT -> Alignment.CenterEnd
                        }
                    )
                    .padding(horizontal = AppSpacing.xs)
            ) {
                activePanel.content()
            }
        }
    }
}
