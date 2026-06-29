package com.mocca.app.domain.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.fail
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class OpenCodeApiModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun agentBooleanFieldsTreatNullAsFalse() {
        val input = """
            [
              {"name":"build","native":null,"hidden":null}
            ]
        """.trimIndent()
        val agents = try {
            json.decodeFromString(ListSerializer(Agent.serializer()), input)
        } catch (e: Exception) {
            fail("Agent decode failed: ${e::class.qualifiedName} - ${e.message}", e)
        }

        assertFalse(agents.single().native, "native should be false when null")
        assertFalse(agents.single().hidden, "hidden should be false when null")
    }

    @Test
    fun commandBooleanFieldsTreatNullAsFalse() {
        val input = """
            [
              {"name":"test","subtask":null,"mcp":null}
            ]
        """.trimIndent()
        val commands = try {
            json.decodeFromString(ListSerializer(Command.serializer()), input)
        } catch (e: Exception) {
            fail("Command decode failed: ${e::class.qualifiedName} - ${e.message}", e)
        }

        assertFalse(commands.single().subtask, "subtask should be false when null")
        assertFalse(commands.single().mcp, "mcp should be false when null")
    }
}
