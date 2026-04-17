package com.mocca.app.util

/**
 * Unified diff parsing utility for raw diff text (fenced or plain git diff format).
 *
 * This operates on raw diff strings — typically from LLM output or clipboard content —
 * and is separate from the structured [com.mocca.app.domain.model.GitDiff] types
 * that represent parsed server responses.
 */
object DiffParser {

    // ── Data structures ──────────────────────────────────────────────

    /** Classification of a single line inside a diff block. */
    enum class DiffLineKind {
        ADDITION, DELETION, HUNK, META, NEUTRAL
    }

    /** High-level action for an entire file chunk. */
    enum class DiffAction {
        EDITED, ADDED, DELETED, RENAMED
    }

    /** A single file's parsed diff chunk. */
    data class DiffChunk(
        val id: String,
        val path: String,
        val action: DiffAction,
        val additions: Int,
        val deletions: Int,
        val diffCode: String
    )

    /** A classified line within a diff block. */
    data class DiffLine(
        val content: String,
        val kind: DiffLineKind
    )

    // ── Public API ───────────────────────────────────────────────────

    /**
     * Returns true if [text] likely contains a unified diff.
     * Fast detection only — no full parsing.
     */
    fun looksLikeDiff(text: String): Boolean {
        return text.contains("diff --git") ||
                text.contains("```diff") ||
                text.contains("@@ ")
    }

    /**
     * Parse [text] into per-file [DiffChunk]s.
     * Handles fenced (```diff ... ```) and raw unified diff formats.
     */
    fun parseDiffChunks(text: String): List<DiffChunk> {
        if (text.isBlank()) return emptyList()

        val stripped = stripFencedBlock(text)
        val fileChunks = splitUnifiedDiffByFile(stripped)
        if (fileChunks.isEmpty()) return emptyList()

        return fileChunks.mapNotNull { chunk -> parseSingleChunk(chunk) }
    }

    /**
     * Classify a single diff line into its visual kind.
     */
    fun classifyDiffLine(line: String): DiffLineKind {
        return when {
            line.startsWith("@@") -> DiffLineKind.HUNK
            line.startsWith("+") && !line.startsWith("+++") -> DiffLineKind.ADDITION
            line.startsWith("-") && !line.startsWith("---") -> DiffLineKind.DELETION
            line.startsWith("diff ") ||
                    line.startsWith("--- ") ||
                    line.startsWith("+++ ") ||
                    line.startsWith("index ") ||
                    line.startsWith("Binary ") -> DiffLineKind.META
            else -> DiffLineKind.NEUTRAL
        }
    }

    /**
     * Split a raw unified diff into individual file sections.
     * Each section retains its `diff --git` header line.
     */
    fun splitUnifiedDiffByFile(text: String): List<String> {
        val marker = "diff --git"
        val chunks = mutableListOf<String>()
        var start = 0

        while (start < text.length) {
            val idx = text.indexOf(marker, start)
            if (idx == -1) break

            // Find the next `diff --git` boundary
            val next = text.indexOf(marker, idx + marker.length)
            val end = if (next == -1) text.length else next

            chunks.add(text.substring(idx, end).trimEnd())
            start = end
        }

        return chunks
    }

    /**
     * Infer the file action from the old/new paths in the diff header.
     */
    fun inferDiffAction(oldPath: String, newPath: String): DiffAction {
        return when {
            oldPath.contains("/dev/null") -> DiffAction.ADDED
            newPath.contains("/dev/null") -> DiffAction.DELETED
            oldPath != newPath -> DiffAction.RENAMED
            else -> DiffAction.EDITED
        }
    }

    // ── Internal helpers ─────────────────────────────────────────────

    private val FENCED_DIFF_REGEX = Regex("""```diff\s*\n([\s\S]*?)```""", RegexOption.MULTILINE)

    /**
     * If [text] is wrapped in a fenced diff block, extract the inner content.
     * Otherwise return [text] unchanged.
     */
    private fun stripFencedBlock(text: String): String {
        val match = FENCED_DIFF_REGEX.find(text)
        return match?.groupValues?.get(1)?.trim()?.ifBlank { text } ?: text
    }

    private val OLD_PATH_REGEX = Regex("^--- (?:a/)?(.+)$", RegexOption.MULTILINE)
    private val NEW_PATH_REGEX = Regex("^\\+\\+\\+ (?:b/)?(.+)$", RegexOption.MULTILINE)

    private fun parseSingleChunk(chunk: String): DiffChunk? {
        val oldPath = OLD_PATH_REGEX.find(chunk)?.groupValues?.get(1)?.trim() ?: return null
        val newPath = NEW_PATH_REGEX.find(chunk)?.groupValues?.get(1)?.trim() ?: return null

        val path = when {
            newPath != "/dev/null" -> newPath
            oldPath != "/dev/null" -> oldPath
            else -> return null
        }

        val action = inferDiffAction(oldPath, newPath)

        val lines = chunk.lines()
        var additions = 0
        var deletions = 0
        for (line in lines) {
            when {
                line.startsWith("+") && !line.startsWith("+++") -> additions++
                line.startsWith("-") && !line.startsWith("---") -> deletions++
            }
        }

        val id = path.replace('/', '_')

        return DiffChunk(
            id = id,
            path = path,
            action = action,
            additions = additions,
            deletions = deletions,
            diffCode = chunk
        )
    }
}
