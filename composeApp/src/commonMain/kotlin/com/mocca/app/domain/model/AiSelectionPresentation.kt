package com.mocca.app.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class AiShellStatus(
    val modelName: String = "--",
    val agentName: String = "--"
)

fun deriveAiShellStatus(
    configState: AiConfigState,
    effectiveSelection: AiEffectiveSelection?
): AiShellStatus {
    if (effectiveSelection != null) {
        return AiShellStatus(
            modelName = effectiveSelection.displayModel,
            agentName = effectiveSelection.displayAgentOrMode
        )
    }

    return when (configState.status) {
        AiConfigStatus.UPDATE_REQUIRED -> AiShellStatus(modelName = "UPDATE CLI")
        AiConfigStatus.ERROR -> AiShellStatus(modelName = "NO MODEL")
        else -> AiShellStatus()
    }
}
