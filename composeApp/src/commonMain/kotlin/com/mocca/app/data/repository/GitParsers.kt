package com.mocca.app.data.repository

import com.mocca.app.domain.model.*

/**
 * Parsers for raw Git command output.
 */
object GitParsers {

    fun parseStatus(output: String): GitStatusResponse {
        var branch = "HEAD"
        var upstream: String? = null
        var ahead = 0
        var behind = 0
        val staged = mutableListOf<GitFileChange>()
        val unstaged = mutableListOf<GitFileChange>()
        val untracked = mutableListOf<String>()
        val conflicted = mutableListOf<String>()

        output.lineSequence().forEach { line ->
            when {
                line.startsWith("# branch.head") -> {
                    branch = line.removePrefix("# branch.head ").trim()
                }
                line.startsWith("# branch.upstream") -> {
                    upstream = line.removePrefix("# branch.upstream ").trim()
                }
                line.startsWith("# branch.ab") -> {
                    val parts = line.removePrefix("# branch.ab ").trim().split(" ")
                    if (parts.size >= 2) {
                        ahead = parts[0].replace("+", "").toIntOrNull() ?: 0
                        behind = parts[1].replace("-", "").toIntOrNull() ?: 0
                    }
                }
                line.startsWith("1 ") -> {
                    val parts = line.split(" ")
                    if (parts.size >= 9) {
                        val xy = parts[1]
                        val path = parts.drop(8).joinToString(" ")
                        val x = xy[0]
                        val y = xy[1]

                        if (x != '.') staged.add(GitFileChange(path, mapStatus(x)))
                        if (y != '.') unstaged.add(GitFileChange(path, mapStatus(y)))
                    }
                }
                line.startsWith("2 ") -> {
                    val parts = line.split(" ")
                    if (parts.size >= 9) {
                        val xy = parts[1]
                        val path = parts.drop(8).joinToString(" ")
                        val x = xy[0]
                        val y = xy[1]

                        if (x != '.') staged.add(GitFileChange(path, mapStatus(x)))
                        if (y != '.') unstaged.add(GitFileChange(path, mapStatus(y)))
                    }
                }
                line.startsWith("? ") -> {
                    untracked.add(line.removePrefix("? ").trim())
                }
                line.startsWith("u ") -> {
                    val parts = line.split(" ")
                    if (parts.size >= 11) {
                        val path = parts.drop(10).joinToString(" ")
                        conflicted.add(path)
                    }
                }
            }
        }

        return GitStatusResponse(
            branch = branch,
            upstream = upstream,
            ahead = ahead,
            behind = behind,
            staged = staged,
            unstaged = unstaged,
            untracked = untracked,
            conflicted = conflicted,
            clean = staged.isEmpty() && unstaged.isEmpty() && untracked.isEmpty()
        )
    }

    private fun mapStatus(char: Char): GitFileStatus = when (char) {
        'M' -> GitFileStatus.MODIFIED
        'A' -> GitFileStatus.ADDED
        'D' -> GitFileStatus.DELETED
        'R' -> GitFileStatus.RENAMED
        'C' -> GitFileStatus.COPIED
        'U' -> GitFileStatus.UNMERGED
        else -> GitFileStatus.UNKNOWN
    }

    fun parseBranches(output: String): List<GitBranch> {
        val branches = mutableListOf<GitBranch>()
        output.lineSequence().forEach { line ->
            if (line.isBlank()) return@forEach
            
            val isCurrent = line.startsWith("*")
            val cleanLine = line.removePrefix("*").trim()
            val parts = cleanLine.split(Regex("\\s+"), limit = 2)
            if (parts.isEmpty()) return@forEach
            
            val name = parts[0]
            var upstream: String? = null
            var ahead = 0
            var behind = 0
            
            val bracketStart = cleanLine.indexOf('[')
            val bracketEnd = cleanLine.indexOf(']')
            if (bracketStart != -1 && bracketEnd != -1) {
                val meta = cleanLine.substring(bracketStart + 1, bracketEnd)
                val metaParts = meta.split(": ")
                upstream = metaParts[0]
                
                if (metaParts.size > 1) {
                    val flowParts = metaParts[1].split(", ")
                    flowParts.forEach { part ->
                        when {
                            part.startsWith("ahead") -> ahead = part.split(" ")[1].toIntOrNull() ?: 0
                            part.startsWith("behind") -> behind = part.split(" ")[1].toIntOrNull() ?: 0
                        }
                    }
                }
            }

            branches.add(GitBranch(
                name = name,
                current = isCurrent,
                upstream = upstream,
                ahead = ahead,
                behind = behind,
                remote = name.startsWith("remotes/")
            ))
        }
        return branches
    }

    fun parseLog(output: String): GitLog {
        val commits = output.lineSequence()
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                val parts = line.split("|")
                if (parts.size < 6) return@mapNotNull null
                
                GitCommit(
                    hash = parts[0],
                    shortHash = parts[1],
                    message = parts[2],
                    author = parts[3],
                    email = parts[4],
                    date = parts[5].toLongOrNull()?.times(1000) ?: 0L,
                    parents = if (parts.size > 6) parts[6].split(" ").filter { it.isNotBlank() } else emptyList()
                )
            }
            .toList()
            
        return GitLog(commits = commits)
    }

    fun parseRemotes(output: String): List<GitRemote> {
        val remoteMap = mutableMapOf<String, GitRemote>()
        
        output.lineSequence().forEach { line ->
            if (line.isBlank()) return@forEach
            val parts = line.split(Regex("\\s+"))
            if (parts.size < 3) return@forEach
            
            val name = parts[0]
            val url = parts[1]
            val type = parts[2]
            
            val current = remoteMap.getOrPut(name) { GitRemote(name, url) }
            if (type == "(push)") {
                remoteMap[name] = current.copy(pushUrl = url)
            } else {
                remoteMap[name] = current.copy(fetchUrl = url)
            }
        }
        
        return remoteMap.values.toList()
    }

    fun parseDiff(output: String): GitDiff {
        val files = mutableListOf<GitDiffFile>()
        var currentPath: String? = null
        var currentOldPath: String? = null
        var currentHunks = mutableListOf<GitDiffHunk>()
        var currentLines = mutableListOf<GitDiffLine>()
        var currentHunkHeader = ""
        var oldStart = 0
        var oldLines = 0
        var newStart = 0
        var newLines = 0
        
        // Use a lambda for finalize to avoid named local function issue
        val finalizeFile = {
            if (currentPath != null) {
                if (currentLines.isNotEmpty() || currentHunkHeader.isNotEmpty()) {
                    currentHunks.add(GitDiffHunk(oldStart, oldLines, newStart, newLines, currentHunkHeader, ArrayList(currentLines)))
                }
                
                files.add(GitDiffFile(
                    path = currentPath!!,
                    oldPath = currentOldPath,
                    status = GitFileStatus.MODIFIED,
                    hunks = ArrayList(currentHunks),
                    additions = currentHunks.sumOf { h -> h.lines.count { it.type == DiffLineType.ADDITION } },
                    deletions = currentHunks.sumOf { h -> h.lines.count { it.type == DiffLineType.DELETION } }
                ))
                currentHunks.clear()
                currentLines.clear()
            }
        }

        output.lineSequence().forEach { line ->
            when {
                line.startsWith("diff --git") -> {
                    finalizeFile()
                    val parts = line.split(" ")
                    if (parts.size >= 4) {
                        currentOldPath = parts[2].removePrefix("a/")
                        currentPath = parts[3].removePrefix("b/")
                    }
                }
                line.startsWith("@@") -> {
                    if (currentLines.isNotEmpty()) {
                        currentHunks.add(GitDiffHunk(oldStart, oldLines, newStart, newLines, currentHunkHeader, ArrayList(currentLines)))
                        currentLines.clear()
                    }
                    
                    currentHunkHeader = line
                    val rangeParts = line.split(" ")
                    if (rangeParts.size >= 3) {
                        val oldRange = rangeParts[1].removePrefix("-").split(",")
                        oldStart = oldRange[0].toIntOrNull() ?: 0
                        oldLines = if (oldRange.size > 1) oldRange[1].toIntOrNull() ?: 0 else 1
                        
                        val newRange = rangeParts[2].removePrefix("+").split(",")
                        newStart = newRange[0].toIntOrNull() ?: 0
                        newLines = if (newRange.size > 1) newRange[1].toIntOrNull() ?: 0 else 1
                    }
                }
                line.startsWith("+") && !line.startsWith("+++") -> {
                    currentLines.add(GitDiffLine(DiffLineType.ADDITION, line.substring(1)))
                }
                line.startsWith("-") && !line.startsWith("---") -> {
                    currentLines.add(GitDiffLine(DiffLineType.DELETION, line.substring(1)))
                }
                line.startsWith(" ") -> {
                    currentLines.add(GitDiffLine(DiffLineType.CONTEXT, line.substring(1)))
                }
            }
        }
        finalizeFile()
        
        return GitDiff(files = files)
    }
}
