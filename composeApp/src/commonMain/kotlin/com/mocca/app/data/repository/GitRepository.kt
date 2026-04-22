package com.mocca.app.data.repository

import com.mocca.app.bridge.client.NativeCliUnavailableException
import com.mocca.app.bridge.client.requestPayload
import com.mocca.app.bridge.client.requireClient
import com.mocca.app.bridge.connection.BridgeConnectionManager
import com.mocca.app.bridge.opencode.BridgeResponseException
import com.mocca.app.bridge.protocol.BridgeConfirmation
import com.mocca.app.data.local.LocalCache
import com.mocca.app.domain.model.DiffLineType
import com.mocca.app.domain.model.FileDiff
import com.mocca.app.domain.model.GitBranch
import com.mocca.app.domain.model.GitDiff
import com.mocca.app.domain.model.GitLog
import com.mocca.app.domain.model.GitOperationResult
import com.mocca.app.domain.model.GitRemote
import com.mocca.app.domain.model.GitStash
import com.mocca.app.domain.model.GitStatusResponse
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.VcsInfo
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement

class GitRepository(
    private val bridgeConnectionManager: BridgeConnectionManager,
    private val localCache: LocalCache,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    companion object {
        private const val TAG = "GitRepository"
    }

    fun getVcsInfo(): Flow<Resource<VcsInfo>> = flow {
        emit(Resource.Loading())
        when (val status = fetchStatus()) {
            is Resource.Success -> emit(Resource.Success(VcsInfo(branch = status.data.branch)))
            is Resource.Error -> emit(Resource.Error(status.message, cause = status.cause))
            is Resource.Loading -> emit(Resource.Loading(status.data?.let { VcsInfo(it.branch) }))
        }
    }.flowOn(Dispatchers.Default)

    fun getStatus(sessionId: String? = null): Flow<Resource<GitStatusResponse>> = flow {
        emit(Resource.Loading(localCache.getGitStatus()))
        when (val status = fetchStatus()) {
            is Resource.Success -> {
                localCache.saveGitStatus(status.data)
                emit(Resource.Success(status.data))
            }
            is Resource.Error -> emit(Resource.Error(status.message, localCache.getGitStatus(), status.cause))
            is Resource.Loading -> emit(status)
        }
    }.flowOn(Dispatchers.Default)

    fun getBranches(sessionId: String? = null): Flow<Resource<List<GitBranch>>> =
        bridgeFlow("git.branches") { request<List<GitBranch>>("git", "branches") }

    fun getLog(sessionId: String? = null, branch: String? = null, count: Int = 50, skip: Int = 0): Flow<Resource<GitLog>> =
        bridgeFlow("git.log") {
            request(
                ns = "git",
                action = "log",
                payload = GitLogPayload(branch = branch, count = count, skip = skip)
            )
        }

    fun getRemotes(sessionId: String? = null): Flow<Resource<List<GitRemote>>> =
        bridgeFlow("git.remotes") { request<List<GitRemote>>("git", "remotes") }

    fun getTags(sessionId: String? = null): Flow<Resource<List<String>>> =
        bridgeFlow("git.tags") { request<List<String>>("git", "tags") }

    fun getStashes(sessionId: String? = null): Flow<Resource<List<GitStash>>> =
        bridgeFlow("git.stashes") { request<List<GitStash>>("git", "stashes") }

    fun getDiff(path: String, staged: Boolean): Flow<Resource<GitDiff>> =
        bridgeFlow("git.diff") {
            request(
                ns = "git",
                action = "diff",
                payload = GitDiffPayload(path = path, staged = staged)
            )
        }

    fun getSessionDiffs(sessionId: String): Flow<Resource<List<FileDiff>>> = flow {
        emit(Resource.Error("Session diffs were replaced by native CLI git.diff"))
    }

    suspend fun stage(files: List<String>): Result<GitOperationResult> =
        write("git.stage") { request("git", "stage", GitFilesPayload(files)) }

    suspend fun stage(sessionId: String, files: List<String>): Result<GitOperationResult> = stage(files)

    suspend fun unstage(files: List<String>): Result<GitOperationResult> =
        write("git.unstage") { request("git", "unstage", GitFilesPayload(files)) }

    suspend fun unstage(sessionId: String, files: List<String>): Result<GitOperationResult> = unstage(files)

    suspend fun discard(files: List<String>, confirmation: BridgeConfirmation? = null): Result<GitOperationResult> =
        write("git.discard") { request("git", "discard", GitFilesPayload(files = files, confirmation = confirmation)) }

    suspend fun discard(sessionId: String, files: List<String>): Result<GitOperationResult> = discard(files)

    suspend fun commit(message: String, amend: Boolean = false): Result<GitOperationResult> =
        write("git.commit") { request("git", "commit", GitCommitPayload(message, amend)) }

    suspend fun commit(sessionId: String, message: String, amend: Boolean = false): Result<GitOperationResult> =
        commit(message, amend)

    suspend fun push(
        remote: String = "origin",
        branch: String? = null,
        force: Boolean = false,
        setUpstream: Boolean = false,
        confirmation: BridgeConfirmation? = null
    ): Result<GitOperationResult> =
        write("git.push") { request("git", "push", GitPushPayload(remote, branch, force, setUpstream, confirmation)) }

    suspend fun push(sessionId: String, remote: String = "origin", branch: String? = null, force: Boolean = false, setUpstream: Boolean = false): Result<GitOperationResult> =
        push(remote, branch, force, setUpstream)

    suspend fun pull(remote: String = "origin", branch: String? = null, rebase: Boolean = false): Result<GitOperationResult> =
        write("git.pull") { request("git", "pull", GitPullPayload(remote, branch, rebase)) }

    suspend fun pull(sessionId: String, remote: String = "origin", branch: String? = null, rebase: Boolean = false): Result<GitOperationResult> =
        pull(remote, branch, rebase)

    suspend fun fetch(remote: String = "origin", prune: Boolean = false, all: Boolean = false): Result<GitOperationResult> =
        write("git.fetch") { request("git", "fetch", GitFetchPayload(remote, prune, all)) }

    suspend fun fetch(sessionId: String, remote: String = "origin", prune: Boolean = false, all: Boolean = false): Result<GitOperationResult> =
        fetch(remote, prune, all)

    suspend fun checkout(ref: String, create: Boolean = false, force: Boolean = false, confirmation: BridgeConfirmation? = null): Result<GitOperationResult> =
        write("git.checkout") { request("git", "checkout", GitCheckoutPayload(ref, create, force, confirmation)) }

    suspend fun checkout(sessionId: String, ref: String, create: Boolean = false, force: Boolean = false): Result<GitOperationResult> =
        checkout(ref, create, force)

    suspend fun createStash(message: String?): Result<GitOperationResult> =
        write("git.stashCreate") { request("git", "stashCreate", GitStashCreatePayload(message)) }

    suspend fun createStash(sessionId: String, message: String?): Result<GitOperationResult> = createStash(message)

    suspend fun popStash(index: Int): Result<GitOperationResult> =
        write("git.stashApply") { request("git", "stashApply", GitStashApplyPayload(index, pop = true)) }

    suspend fun popStash(sessionId: String, index: Int): Result<GitOperationResult> = popStash(index)

    suspend fun applyStash(index: Int): Result<GitOperationResult> =
        write("git.stashApply") { request("git", "stashApply", GitStashApplyPayload(index, pop = false)) }

    suspend fun applyStash(sessionId: String, index: Int): Result<GitOperationResult> = applyStash(index)

    suspend fun dropStash(index: Int, confirmation: BridgeConfirmation? = null): Result<GitOperationResult> =
        write("git.stashDrop") { request("git", "stashDrop", GitIndexPayload(index, confirmation)) }

    suspend fun dropStash(sessionId: String, index: Int): Result<GitOperationResult> = dropStash(index)

    suspend fun merge(branch: String): Result<GitOperationResult> =
        write("git.merge") { request("git", "merge", GitBranchPayload(branch)) }

    suspend fun merge(sessionId: String, branch: String): Result<GitOperationResult> = merge(branch)

    suspend fun rebase(branch: String): Result<GitOperationResult> =
        write("git.rebase") { request("git", "rebase", GitBranchPayload(branch)) }

    suspend fun rebase(sessionId: String, branch: String): Result<GitOperationResult> = rebase(branch)

    suspend fun addRemote(name: String, url: String): Result<GitOperationResult> =
        write("git.remoteAdd") { request("git", "remoteAdd", GitRemotePayload(name, url)) }

    suspend fun addRemote(sessionId: String, name: String, url: String): Result<GitOperationResult> = addRemote(name, url)

    suspend fun removeRemote(name: String, confirmation: BridgeConfirmation? = null): Result<GitOperationResult> =
        write("git.remoteRemove") { request("git", "remoteRemove", GitRemoteRemovePayload(name, confirmation)) }

    suspend fun removeRemote(sessionId: String, name: String): Result<GitOperationResult> = removeRemote(name)

    suspend fun createTag(name: String, message: String?): Result<GitOperationResult> =
        write("git.tagCreate") { request("git", "tagCreate", GitTagPayload(name, message)) }

    suspend fun createTag(sessionId: String, name: String, message: String?): Result<GitOperationResult> = createTag(name, message)

    suspend fun deleteTag(name: String, confirmation: BridgeConfirmation? = null): Result<GitOperationResult> =
        write("git.tagDelete") { request("git", "tagDelete", GitTagDeletePayload(name, confirmation)) }

    suspend fun deleteTag(sessionId: String, name: String): Result<GitOperationResult> = deleteTag(name)

    suspend fun refreshStatus(): Result<VcsInfo> {
        return when (val status = fetchStatus()) {
            is Resource.Success -> {
                localCache.saveGitStatus(status.data)
                Result.success(VcsInfo(status.data.branch))
            }
            is Resource.Error -> Result.failure(status.cause ?: IllegalStateException(status.message))
            is Resource.Loading -> Result.success(VcsInfo(status.data?.branch.orEmpty()))
        }
    }

    suspend fun refresh() {
        when (val status = fetchStatus()) {
            is Resource.Success -> localCache.saveGitStatus(status.data)
            is Resource.Error -> throw status.cause ?: IllegalStateException(status.message)
            is Resource.Loading -> Unit
        }
    }

    private suspend fun fetchStatus(): Resource<GitStatusResponse> {
        return try {
            Resource.Success(request("git", "status"))
        } catch (error: Exception) {
            Napier.w("$TAG: git.status failed through MOCCA CLI: ${error.message}")
            Resource.Error(error.toResourceMessage("Failed to get git status"), cause = error)
        }
    }

    private fun <T> bridgeFlow(feature: String, block: suspend () -> T): Flow<Resource<T>> = flow {
        emit(Resource.Loading())
        try {
            emit(Resource.Success(block()))
        } catch (error: Exception) {
            Napier.e("$TAG: $feature failed through MOCCA CLI", error)
            emit(Resource.Error(error.toResourceMessage("Operation failed"), cause = error))
        }
    }.flowOn(Dispatchers.Default)

    private suspend fun write(feature: String, block: suspend () -> GitOperationResult): Result<GitOperationResult> {
        return try {
            Result.success(block())
        } catch (error: Exception) {
            Napier.e("$TAG: $feature failed through MOCCA CLI", error)
            Result.failure(error)
        }
    }

    private suspend inline fun <reified T> request(ns: String, action: String): T {
        val client = bridgeConnectionManager.requireClient("$ns.$action")
        return client.requestPayload(ns = ns, action = action, json = json)
    }

    private suspend inline fun <reified T, reified P> request(ns: String, action: String, payload: P): T {
        val client = bridgeConnectionManager.requireClient("$ns.$action")
        return client.requestPayload(
            ns = ns,
            action = action,
            payload = json.encodeToJsonElement(payload),
            json = json
        )
    }

    private fun Exception.toResourceMessage(fallback: String): String {
        return when (this) {
            is NativeCliUnavailableException -> message ?: "MOCCA CLI bridge is not connected"
            is BridgeResponseException -> message ?: fallback
            else -> message ?: fallback
        }
    }
}

@Serializable private data class GitFilesPayload(val files: List<String>, val confirmation: BridgeConfirmation? = null)
@Serializable private data class GitCommitPayload(val message: String, val amend: Boolean = false)
@Serializable private data class GitPushPayload(val remote: String, val branch: String?, val force: Boolean, val setUpstream: Boolean, val confirmation: BridgeConfirmation? = null)
@Serializable private data class GitPullPayload(val remote: String, val branch: String?, val rebase: Boolean)
@Serializable private data class GitFetchPayload(val remote: String, val prune: Boolean, val all: Boolean)
@Serializable private data class GitCheckoutPayload(val ref: String, val create: Boolean, val force: Boolean, val confirmation: BridgeConfirmation? = null)
@Serializable private data class GitLogPayload(val branch: String?, val count: Int, val skip: Int)
@Serializable private data class GitDiffPayload(val path: String, val staged: Boolean)
@Serializable private data class GitStashCreatePayload(val message: String?)
@Serializable private data class GitStashApplyPayload(val index: Int, val pop: Boolean)
@Serializable private data class GitIndexPayload(val index: Int, val confirmation: BridgeConfirmation? = null)
@Serializable private data class GitBranchPayload(val branch: String)
@Serializable private data class GitRemotePayload(val name: String, val url: String)
@Serializable private data class GitRemoteRemovePayload(val name: String, val confirmation: BridgeConfirmation? = null)
@Serializable private data class GitTagPayload(val name: String, val message: String?)
@Serializable private data class GitTagDeletePayload(val name: String, val confirmation: BridgeConfirmation? = null)
