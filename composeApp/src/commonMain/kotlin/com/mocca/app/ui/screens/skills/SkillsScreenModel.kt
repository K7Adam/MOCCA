package com.mocca.app.ui.screens.skills

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.SkillRepository
import com.mocca.app.domain.model.SkillInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SkillsScreenState(
    val skills: List<SkillInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class SkillsScreenModel(
    private val skillRepository: SkillRepository
) : ScreenModel {

    private val _state = MutableStateFlow(SkillsScreenState())
    val state: StateFlow<SkillsScreenState> = _state.asStateFlow()

    init {
        loadSkills()
    }

    fun loadSkills() {
        _state.update { it.copy(isLoading = true, error = null) }
        screenModelScope.launch {
            skillRepository.refreshSkills()
            _state.update {
                it.copy(
                    skills = skillRepository.skills.value,
                    isLoading = false
                )
            }
        }
    }

    fun search(query: String): List<SkillInfo> {
        return skillRepository.search(query)
    }
}
