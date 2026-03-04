package com.mocca.app.ui.screens.settings

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.mocca.app.data.repository.ConfigRepository
import com.mocca.app.domain.model.AppConfigUpdate
import com.mocca.app.domain.model.GlobalAppConfig
import com.mocca.app.domain.model.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════
// UI STATE
// ═══════════════════════════════════════════════════════════════════════

data class FeatureFlagsUiState(
    val isLoading: Boolean = true,
    val config: GlobalAppConfig? = null,
    val isSaving: Boolean = false,
    val error: String? = null,
    val saveError: String? = null,
    val successMessage: String? = null
)

// ═══════════════════════════════════════════════════════════════════════
// SCREEN MODEL
// ═══════════════════════════════════════════════════════════════════════

class FeatureFlagsScreenModel(
    private val configRepository: ConfigRepository
) : ScreenModel {

    private val _uiState = MutableStateFlow(FeatureFlagsUiState())
    val uiState: StateFlow<FeatureFlagsUiState> = _uiState.asStateFlow()

    init {
        loadGlobalConfig()
    }

    fun loadGlobalConfig() {
        screenModelScope.launch {
            configRepository.getGlobalConfig().collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(
                        isLoading = true,
                        error = null
                    )
                    is Resource.Success -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        config = resource.data,
                        error = null
                    )
                    is Resource.Error -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = resource.message
                    )
                }
            }
        }
    }

    fun setAutoshare(enabled: Boolean) {
        val current = _uiState.value.config ?: return
        _uiState.value = _uiState.value.copy(
            config = current.copy(autoshare = enabled)
        )
        saveGlobalConfig(AppConfigUpdate(autoshare = enabled))
    }

    fun setAutoupdate(enabled: Boolean) {
        val current = _uiState.value.config ?: return
        _uiState.value = _uiState.value.copy(
            config = current.copy(autoupdate = enabled)
        )
        saveGlobalConfig(AppConfigUpdate(autoupdate = enabled))
    }

    fun setTelemetry(enabled: Boolean) {
        val current = _uiState.value.config ?: return
        _uiState.value = _uiState.value.copy(
            config = current.copy(telemetry = enabled)
        )
        saveGlobalConfig(AppConfigUpdate(telemetry = enabled))
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(saveError = null, successMessage = null)
    }

    private fun saveGlobalConfig(update: AppConfigUpdate) {
        screenModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, saveError = null)
            val result = configRepository.updateGlobalConfig(update)
            when (result) {
                is Resource.Success -> _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    config = result.data,
                    successMessage = "Saved"
                )
                is Resource.Error -> _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    saveError = result.message
                )
                is Resource.Loading -> Unit
            }
        }
    }
}
