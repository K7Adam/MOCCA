package com.mocca.app.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class OpenCodeApiModelsTest {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Test
    fun agentWithOmittedBooleanFieldsDefaultsToFalse() {
        val input = """[{"name":"build"}]"""
        val agents = json.decodeFromString(ListSerializer(Agent.serializer()), input)

        assertEquals("build", agents.single().name)
        assertFalse(agents.single().native, "native should default to false when omitted")
        assertFalse(agents.single().hidden, "hidden should default to false when omitted")
    }

    @Test
    fun commandWithOmittedBooleanFieldsDefaultsToFalse() {
        val input = """[{"name":"test"}]"""
        val commands = json.decodeFromString(ListSerializer(Command.serializer()), input)

        assertEquals("test", commands.single().name)
        assertFalse(commands.single().subtask, "subtask should default to false when omitted")
        assertFalse(commands.single().mcp, "mcp should default to false when omitted")
    }

    @Test
    fun agentWithExplicitFalseBooleanFields() {
        val input = """[{"name":"build","native":false,"hidden":false}]"""
        val agents = json.decodeFromString(ListSerializer(Agent.serializer()), input)

        assertFalse(agents.single().native)
        assertFalse(agents.single().hidden)
    }

    @Test
    fun commandWithExplicitFalseBooleanFields() {
        val input = """[{"name":"test","subtask":false,"mcp":false}]"""
        val commands = json.decodeFromString(ListSerializer(Command.serializer()), input)

        assertFalse(commands.single().subtask)
        assertFalse(commands.single().mcp)
    }
}
