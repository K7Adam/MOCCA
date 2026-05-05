package com.mocca.app.util

import com.mocca.app.domain.model.Message
import com.mocca.app.domain.model.MessagePart
import com.mocca.app.domain.model.MessageRole

/**
 * Utility for exporting chat sessions to Markdown format.
 */
object ChatExporter {
    fun exportSessionToMarkdown(
        sessionTitle: String,
        messages: List<Message>
    ): String {
        val builder = StringBuilder()
        
        builder.appendLine("# $sessionTitle")
        builder.appendLine()
        
        // For each message
        for (message in messages) {
            val roleHeader = when (message.role) {
                MessageRole.USER -> "## User"
                MessageRole.ASSISTANT -> "## Assistant"
                MessageRole.SYSTEM -> "## System"
            }
            builder.appendLine(roleHeader)
            builder.appendLine()
            
            // Process each part
            for (part in message.parts) {
                when (part) {
                    is MessagePart.Text -> {
                        builder.appendLine(part.text)
                        builder.appendLine()
                    }
                    is MessagePart.ToolInvocation -> {
                        builder.appendLine("```")
                        builder.appendLine("Tool: ${part.name}")
                        builder.appendLine("Input: ${part.input}")
                        builder.appendLine("```")
                        builder.appendLine()
                    }
                    is MessagePart.ToolResult -> {
                        builder.appendLine("```")
                        builder.appendLine("Result: ${part.result}")
                        builder.appendLine("```")
                        builder.appendLine()
                    }
                    is MessagePart.Thinking -> {
                        // Include as collapsed blockquote
                        builder.appendLine("> Thinking...")
                        part.content.lines().forEach { line ->
                            builder.appendLine("> $line")
                        }
                        builder.appendLine()
                    }
                    is MessagePart.File -> {
                        builder.appendLine("📎 ${part.filename ?: "file"}")
                        builder.appendLine()
                    }
                    is MessagePart.Reasoning -> {
                        // Include as blockquote
                        part.content.lines().forEach { line ->
                            builder.appendLine("> $line")
                        }
                        builder.appendLine()
                    }
                    is MessagePart.SubTask -> {
                        // Skip sub-tasks
                    }
                }
            }
            
            // Timestamp
            if (message.createdAt > 0) {
                val formattedTime = TimeFormatter.formatDateTime(message.createdAt)
                builder.appendLine("_Sent: " + formattedTime + "_")
                builder.appendLine()
            }
        }
        
        return builder.toString()
    }
}
