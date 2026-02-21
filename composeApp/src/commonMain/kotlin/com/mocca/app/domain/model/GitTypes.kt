package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Git-related data models matching OpenCode server Git API.
 */

@Serializable
@Immutable
data class GitStatusResponse(
    val branch: String = "",
    val upstream: String? = null,
    val ahead: Int = 0,
    val behind: Int = 0,
    val staged: List<GitFileChange> = emptyList(),
    val unstaged: List<GitFileChange> = emptyList(),
    val untracked: List<String> = emptyList(),
    val conflicted: List<String> = emptyList(),
    val stashes: Int = 0,
    val clean: Boolean = true
) {
    val hasChanges: Boolean get() = !clean || staged.isNotEmpty() || unstaged.isNotEmpty() || untracked.isNotEmpty()
    val totalChanges: Int get() = staged.size + unstaged.size + untracked.size
}

@Serializable
@Immutable
data class GitFileChange(
    val path: String,
    val status: GitFileStatus,
    val oldPath: String? = null // For renames
)

@Serializable
enum class GitFileStatus {
    @SerialName("added")
    ADDED,
    @SerialName("modified")
    MODIFIED,
    @SerialName("deleted")
    DELETED,
    @SerialName("renamed")
    RENAMED,
    @SerialName("copied")
    COPIED,
    @SerialName("unmerged")
    UNMERGED,
    @SerialName("unknown")
    UNKNOWN
}

@Serializable
@Immutable
data class GitBranch(
    val name: String,
    val current: Boolean = false,
    val remote: Boolean = false,
    val upstream: String? = null,
    val ahead: Int = 0,
    val behind: Int = 0,
    @SerialName("lastCommit")
    val lastCommit: String? = null,
    @SerialName("lastCommitTime")
    val lastCommitTime: Long? = null
)

@Serializable
@Immutable
data class GitCommit(
    val hash: String,
    val shortHash: String = hash.take(7),
    val message: String,
    val author: String,
    val email: String? = null,
    val date: Long,
    val parents: List<String> = emptyList(),
    val refs: List<String> = emptyList()
) {
    val isInitialCommit: Boolean get() = parents.isEmpty()
    val isMergeCommit: Boolean get() = parents.size > 1
}

@Serializable
@Immutable
data class GitLog(
    val commits: List<GitCommit> = emptyList(),
    val total: Int = 0,
    val hasMore: Boolean = false
)

@Serializable
@Immutable
data class GitDiff(
    val files: List<GitDiffFile> = emptyList(),
    val additions: Int = 0,
    val deletions: Int = 0,
    val binary: Boolean = false
)

@Serializable
@Immutable
data class GitDiffFile(
    val path: String,
    val oldPath: String? = null,
    val status: GitFileStatus,
    val additions: Int = 0,
    val deletions: Int = 0,
    val binary: Boolean = false,
    val hunks: List<GitDiffHunk> = emptyList()
)

@Serializable
@Immutable
data class GitDiffHunk(
    val oldStart: Int,
    val oldLines: Int,
    val newStart: Int,
    val newLines: Int,
    val header: String = "",
    val lines: List<GitDiffLine> = emptyList()
)

@Serializable
@Immutable
data class GitDiffLine(
    val type: DiffLineType,
    val content: String,
    val oldLineNumber: Int? = null,
    val newLineNumber: Int? = null
)

@Serializable
enum class DiffLineType {
    @SerialName("context")
    CONTEXT,
    @SerialName("addition")
    ADDITION,
    @SerialName("deletion")
    DELETION,
    @SerialName("header")
    HEADER
}

// Request/Response types for Git operations

@Serializable
@Immutable
data class GitCommitRequest(
    val message: String,
    val files: List<String>? = null, // null = all staged
    val amend: Boolean = false
)

@Serializable
@Immutable
data class GitPushRequest(
    val remote: String = "origin",
    val branch: String? = null, // null = current branch
    val force: Boolean = false,
    @SerialName("setUpstream")
    val setUpstream: Boolean = false
)

@Serializable
@Immutable
data class GitPullRequest(
    val remote: String = "origin",
    val branch: String? = null,
    val rebase: Boolean = false
)

@Serializable
@Immutable
data class GitFetchRequest(
    val remote: String = "origin",
    val prune: Boolean = false,
    val all: Boolean = false
)

@Serializable
@Immutable
data class GitCheckoutRequest(
    val ref: String, // branch name, tag, or commit hash
    val create: Boolean = false, // -b flag
    val force: Boolean = false
)

@Serializable
@Immutable
data class GitStageRequest(
    val files: List<String>,
    val intent: Boolean = false // --intent-to-add
)

@Serializable
@Immutable
data class GitUnstageRequest(
    val files: List<String>
)

@Serializable
@Immutable
data class GitDiscardRequest(
    val files: List<String>
)

@Serializable
@Immutable
data class GitOperationResult(
    val success: Boolean,
    val message: String? = null,
    val error: String? = null
)

@Serializable
@Immutable
data class GitRemote(
    val name: String,
    val url: String,
    val fetchUrl: String? = null,
    val pushUrl: String? = null
)

@Serializable
@Immutable
data class GitStash(
    val index: Int,
    val message: String,
    val branch: String? = null,
    val date: Long? = null
)
