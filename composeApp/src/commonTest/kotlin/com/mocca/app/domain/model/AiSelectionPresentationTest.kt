package com.mocca.app.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

class AiSelectionPresentationTest {
    @Test
    fun readySelectionUsesEffectiveModelAndAgentNames() {
        val state = AiConfigState(status = AiConfigStatus.READY)
        val effective = AiEffectiveSelection(
            providerId = "openai",
            providerName = "OpenAI",
            modelId = "gpt-5",
            modelName = "GPT-5",
            agentId = "build",
            agentName = "Build"
        )

        val presentation = deriveAiShellStatus(state, effective)

        assertEquals("GPT-5", presentation.modelName)
        assertEquals("Build", presentation.agentName)
    }

    @Test
    fun updateRequiredUsesCliUpdateLabel() {
        val presentation = deriveAiShellStatus(
            configState = AiConfigState(status = AiConfigStatus.UPDATE_REQUIRED),
            effectiveSelection = null
        )

        assertEquals("UPDATE CLI", presentation.modelName)
        assertEquals("--", presentation.agentName)
    }

    @Test
    fun errorUsesNoModelLabel() {
        val presentation = deriveAiShellStatus(
            configState = AiConfigState(status = AiConfigStatus.ERROR),
            effectiveSelection = null
        )

        assertEquals("NO MODEL", presentation.modelName)
        assertEquals("--", presentation.agentName)
    }
}
