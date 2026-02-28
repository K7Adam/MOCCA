package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.Resource
import com.mocca.app.domain.model.SkillInfo
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Repository for loading OpenCode skills from GET /skill.
 *
 * Skills are agent skills registered with the OpenCode server (e.g. from .opencode/skills/).
 * No local cache — skills are transient and change with server state.
 */
class SkillsRepository(
    private val apiClient: MoccaApiClient
) {

    /** Load all available skills — emits Loading then Success/Error. */
    fun getSkills(): Flow<Resource<List<SkillInfo>>> = flow {
        emit(Resource.Loading())
        apiClient.listSkills().fold(
            onSuccess = { skills ->
                Napier.d("[SkillsRepository] Loaded ${skills.size} skills")
                emit(Resource.Success(skills))
            },
            onFailure = { error ->
                Napier.e("[SkillsRepository] Failed to load skills", error)
                emit(Resource.Error(error.message ?: "Failed to load skills"))
            }
        )
    }.flowOn(Dispatchers.IO)
}
