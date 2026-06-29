package com.mocca.app.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class OpenCodeApiModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun agentBooleanFieldsTreatNullAsFalse() {
        val agents = json.decodeFromString<List<Agent>>(
            """
            [
              {"name":"build","native":null,"hidden":null}
            ]
            """.trimIndent()
        )

        assertFalse(agents.single().native)
        assertFalse(agents.single().hidden)
    }

    @Test
    fun commandBooleanFieldsTreatNullAsFalse() {
        val commands = json.decodeFromString<List<Command>>(
            """
            [
              {"name":"test","subtask":null,"mcp":null}
            ]
            """.trimIndent()
        )

        assertFalse(commands.single().subtask)
        assertFalse(commands.single().mcp)
    }
}
