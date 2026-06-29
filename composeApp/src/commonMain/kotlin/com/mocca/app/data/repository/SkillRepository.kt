package com.mocca.app.data.repository

import com.mocca.app.api.MoccaApiClient
import com.mocca.app.domain.model.SkillInfo
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Repository for agent skills.
 *
 * Fetches available skills from the V2 API: GET /skill
 * Skills are read-only (discovered by the server from .opencode/skills/, .claude/skills/, etc.)
 */
class SkillRepository(
    private val apiClient: MoccaApiClient
) {
    private val _skills = MutableStateFlow<List<SkillInfo>>(emptyList())
    val skills: StateFlow<List<SkillInfo>> = _skills.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Refresh skills from the server.
     */
    suspend fun refreshSkills() = withContext(Dispatchers.IO) {
        _isLoading.value = true
        try {
            val result = apiClient.getSkills()
            result.fold(
                onSuccess = { skillList ->
                    _skills.value = skillList.sortedBy { it.name }
                    Napier.i("[SkillRepository] Loaded ${skillList.size} skills")
                },
                onFailure = { error ->
                    Napier.e("[SkillRepository] Failed to load skills", error)
                }
            )
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Search skills by name or description.
     */
    fun search(query: String): List<SkillInfo> {
        if (query.isBlank()) return _skills.value
        val lowerQuery = query.lowercase()
        return _skills.value.filter {
            it.name.lowercase().contains(lowerQuery) ||
            it.description?.lowercase()?.contains(lowerQuery) == true
        }
    }

    /**
     * Get a skill by name.
     */
    fun getByName(name: String): SkillInfo? {
        return _skills.value.find { it.name == name }
    }
}
