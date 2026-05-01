package com.mocca.app.domain.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerEventsDecodeTest {
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        encodeDefaults = true
    }

    @Test
    fun messageUpdatedAcceptsObjectSummaryFromOpenCode() {
        val event = json.decodeFromString<ServerEvent.MessageUpdated>(
            """
            {
              "type": "message.updated",
              "properties": {
                "info": {
                  "id": "msg_1",
                  "role": "assistant",
                  "sessionID": "ses_1",
                  "time": { "created": 1 },
                  "summary": {
                    "input": 1,
                    "output": 2
                  }
                }
              }
            }
            """.trimIndent()
        )

        assertEquals("msg_1", event.properties.info.id)
        assertEquals("1", event.properties.info.summary?.jsonObject?.get("input").toString())
    }

    @Test
    fun questionAskedAcceptsOpenAgentQuestionToolShape() {
        val event = json.decodeFromString<ServerEvent.QuestionAsked>(
            """
            {
              "type": "question.asked",
              "properties": {
                "id": "que_1",
                "sessionID": "ses_1",
                "questions": [
                  {
                    "header": "Test intent",
                    "question": "What would you like to test?",
                    "options": [
                      {
                        "label": "System responsiveness",
                        "description": "Check if I'm online and working"
                      },
                      {
                        "label": "Project test suite",
                        "description": "Run tests for a specific project"
                      },
                      {
                        "label": "Code feature test",
                        "description": "Test a specific code functionality"
                      }
                    ]
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val question = event.properties.questions.single()
        assertEquals("que_1", event.properties.id)
        assertEquals("ses_1", event.properties.sessionID)
        assertEquals("Test intent", question.header)
        assertEquals("What would you like to test?", question.question)
        assertEquals("System responsiveness", question.options.first().label)
        assertEquals("Check if I'm online and working", question.options.first().description)
    }
}
