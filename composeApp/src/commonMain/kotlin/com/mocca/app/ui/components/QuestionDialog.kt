package com.mocca.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mocca.app.domain.model.QuestionRequest
import com.mocca.app.ui.theme.TerminalColors
import com.mocca.app.ui.theme.TerminalSpacing
import com.mocca.app.ui.theme.TerminalTypography

/**
 * Dialog for handling interactive questions from OpenCode.
 * Supports single/multiple choice and free text input.
 */
@Composable
fun QuestionDialog(
    request: QuestionRequest,
    onAnswer: (List<List<String>>) -> Unit,
    onReject: () -> Unit
) {
    // Track answers for each question. 
    // Each element in this list corresponds to a question in request.questions
    // and contains the list of selected values (or text input) for that question.
    val answers = remember(request) {
        mutableStateListOf<List<String>>().apply {
            repeat(request.questions.size) { add(emptyList()) }
        }
    }
    
    // Track free text inputs separately to avoid focus/recomposition issues
    val textInputs = remember(request) {
        mutableStateListOf<String>().apply {
            repeat(request.questions.size) { add("") }
        }
    }

    // Check if all required questions have at least one answer (if they are required - usually yes)
    // For now we assume all questions require at least one answer if options exist.
    // If no options (text input), allow empty? Usually yes, unless specified.
    val canSubmit = request.questions.indices.all { index ->
        val question = request.questions[index]
        if (question.options.isNotEmpty()) {
            answers[index].isNotEmpty()
        } else {
            // Text input - non-empty? OpenCode might allow empty strings.
            // Let's assume non-empty for now to be safe, or allow it.
            // Actually, textInputs[index] is the source of truth for text fields.
            textInputs[index].isNotBlank() 
        }
    }

    AlertDialog(
        onDismissRequest = onReject,
        icon = {
            Icon(
                @Suppress("DEPRECATION") Icons.Default.Help,
                contentDescription = null,
                tint = TerminalColors.white
            )
        },
        title = {
            Text(
                text = "OpenCode needs input",
                style = TerminalTypography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = TerminalSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(TerminalSpacing.lg)
            ) {
                request.questions.forEachIndexed { index, question ->
                    val isMultiple = question.multiple
                    
                    Column(verticalArrangement = Arrangement.spacedBy(TerminalSpacing.sm)) {
                        // Header/Question text
                        if (question.header.isNotBlank()) {
                            Text(
                                text = question.header,
                                style = TerminalTypography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = question.question,
                            style = TerminalTypography.bodyMedium
                        )
                        
                        // Options or Text Field
                        if (question.options.isNotEmpty()) {
                            Surface(
                                shape = RectangleShape,
                                color = TerminalColors.surfaceVariant.copy(alpha = 0.3f),
                                border = BorderStroke(
                                    1.dp, 
                                    TerminalColors.border
                                )
                            ) {
                                Column {
                                    question.options.forEach { option ->
                                        val isSelected = answers[index].contains(option.label)
                                        
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .toggleable(
                                                    value = isSelected,
                                                    role = if (isMultiple) Role.Checkbox else Role.RadioButton,
                                                    onValueChange = { selected ->
                                                        if (isMultiple) {
                                                            val current = answers[index].toMutableList()
                                                            if (selected) current.add(option.label) else current.remove(option.label)
                                                            answers[index] = current
                                                        } else {
                                                            answers[index] = if (selected) listOf(option.label) else emptyList()
                                                        }
                                                    }
                                                )
                                                .padding(horizontal = TerminalSpacing.md, vertical = TerminalSpacing.sm),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (isMultiple) {
                                                Checkbox(
                                                    checked = isSelected,
                                                    onCheckedChange = null // Handled by toggleable
                                                )
                                            } else {
                                                RadioButton(
                                                    selected = isSelected,
                                                    onClick = null // Handled by toggleable
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(TerminalSpacing.md))
                                            Column {
                                                Text(
                                                    text = option.label,
                                                    style = TerminalTypography.bodyMedium
                                                )
                                                if (option.description.isNotBlank()) {
                                                    Text(
                                                        text = option.description,
                                                        style = TerminalTypography.bodySmall,
                                                        color = TerminalColors.grey
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Text Input
                            OutlinedTextField(
                                value = textInputs[index],
                                onValueChange = { 
                                    textInputs[index] = it
                                    // Update answers list too so we can submit
                                    answers[index] = listOf(it)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { 
                                    Text(
                                        text = "Enter your answer",
                                        style = TerminalTypography.bodyMedium
                                    ) 
                                },
                                shape = RectangleShape
                            )
                        }
                    }
                    
                    if (index < request.questions.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = TerminalSpacing.sm),
                            color = TerminalColors.border
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // Sync text inputs to answers just in case (already done in onValueChange but safety first)
                    // For text inputs, answers[index] should be listOf(text)
                    onAnswer(answers.toList())
                },
                enabled = canSubmit,
                shape = RectangleShape
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(TerminalSpacing.sm))
                Text(
                    text = "Submit",
                    style = TerminalTypography.labelMedium
                )
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onReject,
                shape = RectangleShape,
                border = BorderStroke(1.dp, TerminalColors.border)
            ) {
                Icon(Icons.Default.Close, contentDescription = null)
                Spacer(modifier = Modifier.width(TerminalSpacing.sm))
                Text(
                    text = "Reject",
                    style = TerminalTypography.labelMedium
                )
            }
        },
        shape = RectangleShape,
        containerColor = TerminalColors.surface,
        titleContentColor = TerminalColors.white,
        textContentColor = TerminalColors.whiteDim,
        iconContentColor = TerminalColors.white
    )
}
