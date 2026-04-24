package com.mocca.app.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AiRecentModelsTest {
    @Test
    fun recentModelsStayScopedToTheCurrentProject() {
        val snapshot = AiRuntimeConfigSnapshot(
            projectDir = "C:\\Users\\ruzaq\\AndroidStudioProjects\\MOCCA",
            source = "mocca-cli"
        )
        val stored = listOf(
            recent(snapshot.projectKey, "anthropic", "claude-sonnet-4-5", 200),
            recent("legacy-http:default", "openai", "gpt-4o", 300),
            recent(snapshot.projectKey, "anthropic", "claude-sonnet-4-5", 100),
            recent(snapshot.projectKey, "openai", "gpt-5", 150)
        )

        val result = selectAiRecentModelsForSnapshot(snapshot, stored)

        assertTrue(result.all { it.projectKey == snapshot.projectKey })
        assertEquals(
            listOf("anthropic/claude-sonnet-4-5", "openai/gpt-5"),
            result.map { "${it.providerId}/${it.modelId}" }
        )
    }

    @Test
    fun recentModelsAreBoundedForPickerDensity() {
        val snapshot = AiRuntimeConfigSnapshot(projectDir = "repo", source = "mocca-cli")
        val stored = (1..12).map { index ->
            recent(snapshot.projectKey, "provider", "model-$index", index.toLong())
        }

        val result = selectAiRecentModelsForSnapshot(snapshot, stored)

        assertEquals(8, result.size)
        assertEquals("model-12", result.first().modelId)
        assertEquals("model-5", result.last().modelId)
    }

    private fun recent(
        projectKey: String,
        providerId: String,
        modelId: String,
        lastUsedAt: Long
    ): AiRecentModel = AiRecentModel(
        projectKey = projectKey,
        providerId = providerId,
        modelId = modelId,
        displayName = modelId,
        providerName = providerId,
        lastUsedAt = lastUsedAt
    )
}
