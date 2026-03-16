package com.mocca.app.ui.components.modern

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.adaptive.*
import androidx.compose.material3.adaptive.layout.*
import androidx.compose.material3.adaptive.navigation.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mocca.app.ui.theme.MoccaTheme

/**
 * A highly adaptive scaffold for MOCCA, powered by Material 3 Adaptive.
 * 
 * Automatically switches between:
 * - Single pane (Mobile)
 * - List-Detail (Tablets/Foldables)
 * - Supporting Pane (Large screens)
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MoccaAdaptiveScaffold(
    navigator: ThreePaneScaffoldNavigator<*>,
    modifier: Modifier = Modifier,
    startPane: @Composable ThreePaneScaffoldScope.() -> Unit,
    centerPane: @Composable ThreePaneScaffoldScope.() -> Unit,
    endPane: @Composable ThreePaneScaffoldScope.() -> Unit,
) {
    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane(modifier = Modifier.fillMaxSize()) {
                startPane()
            }
        },
        detailPane = {
            AnimatedPane(modifier = Modifier.fillMaxSize()) {
                centerPane()
            }
        },
        extraPane = {
            AnimatedPane(modifier = Modifier.fillMaxSize()) {
                endPane()
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * A single adaptive pane wrapper with consistent styling and motion.
 */
@Composable
fun MoccaAdaptivePane(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(MoccaTheme.spacing.md)
    ) {
        content()
    }
}
