package com.mocca.app.ui.components.modern

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.ui.theme.AppColors
import com.mocca.app.ui.theme.AppShapes
import com.mocca.app.ui.theme.AppSpacing
import com.mocca.app.ui.theme.AppTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMessagePartDialog(
    part: MessagePart,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val initialText = when (part) {
        is MessagePart.Text -> part.text
        is MessagePart.Reasoning -> part.content
        else -> ""
    }
    var text by remember { mutableStateOf(initialText) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(AppColors.surfaceElevated, AppShapes.dialog)
                .border(AppSpacing.borderThin, AppColors.border, AppShapes.dialog)
                .padding(AppSpacing.lg)
        ) {
            Text(
                text = "EDIT PART",
                style = AppTypography.labelLarge,
                color = AppColors.accent,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(AppSpacing.md))
            MoccaInput(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                singleLine = false
            )
            Spacer(modifier = Modifier.height(AppSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                MoccaOutlinedButton(
                    text = "CANCEL",
                    onClick = onDismiss,
                    height = AppSpacing.buttonHeightCompact
                )
                Spacer(modifier = Modifier.width(AppSpacing.sm))
                MoccaButton(
                    text = "SAVE",
                    onClick = { onConfirm(text) },
                    height = AppSpacing.buttonHeightCompact
                )
            }
        }
    }
}
