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
        val input = """
            [
              {"name":"build","native":null,"hidden":null}
            ]
        """.trimIndent()
        println("DEBUG: Decoding Agent list from: $input")
        val agents = try {
            json.decodeFromString(ListSerializer(Agent.serializer()), input)
        } catch (e: Exception) {
            println("DEBUG: Agent decode failed: ${e::class.simpleName} - ${e.message}")
            throw e
        }
        println("DEBUG: Agent decoded successfully: $agents")

        assertFalse(agents.single().native)
        assertFalse(agents.single().hidden)
    }

    @Test
    fun commandBooleanFieldsTreatNullAsFalse() {
        val input = """
            [
              {"name":"test","subtask":null,"mcp":null}
            ]
        """.trimIndent()
        println("DEBUG: Decoding Command list from: $input")
        val commands = try {
            json.decodeFromString(ListSerializer(Command.serializer()), input)
        } catch (e: Exception) {
            println("DEBUG: Command decode failed: ${e::class.simpleName} - ${e.message}")
            throw e
        }
        println("DEBUG: Command decoded successfully: $commands")

        assertFalse(commands.single().subtask)
        assertFalse(commands.single().mcp)
    }
}
