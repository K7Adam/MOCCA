package com.mocca.app.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class OpenCodeApiModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun agentBooleanFieldsTreatNullAsFalse() {
        val agents = json.decodeFromString(
            ListSerializer(Agent.serializer()),
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
        val commands = json.decodeFromString(
            ListSerializer(Command.serializer()),
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

