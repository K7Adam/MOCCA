package com.mocca.app.data.repository

import com.mocca.app.bridge.client.MoccaBridgeClient
import com.mocca.app.bridge.client.decodePayloadOrThrow
import com.mocca.app.bridge.client.toBridgePayload
import com.mocca.app.bridge.connection.BridgeConnectionManager
import com.mocca.app.bridge.connection.BridgeConnectionStatus
import com.mocca.app.domain.model.AiBridgeMessageModel
import com.mocca.app.domain.model.AiBridgeMessageRequest
import com.mocca.app.domain.model.AiEffectiveSelection
import com.mocca.app.domain.model.AttachedFile
import com.mocca.app.domain.model.ChatPart
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

class AiChatGateway(
    private val sessionRepository: SessionRepository,
    private val bridgeConnectionManager: BridgeConnectionManager,
    private val json: Json = MoccaBridgeClient.DefaultBridgeJson
) {
    suspend fun sendMessage(
        sessionId: String,
        text: String,
        selection: AiEffectiveSelection,
        attachments: List<AttachedFile> = emptyList()
    ): Result<Unit> {
        val parts = buildList {
            add(ChatPart.Text(text = text))
            attachments.forEach { file -> add(file.toChatPart()) }
        }

        val bridgeStatus = bridgeConnectionManager.status.value
        val bridgeClient = bridgeConnectionManager.client.value
        if (bridgeStatus is BridgeConnectionStatus.Connected &&
            bridgeStatus.capabilities.ai.messages &&
            bridgeClient != null
        ) {
            return runCatching {
                val request = AiBridgeMessageRequest(
                    sessionId = sessionId,
                    text = text,
                    parts = parts,
                    model = AiBridgeMessageModel(
                        providerId = selection.providerId,
                        modelId = selection.modelId
                    ),
                    variant = selection.variantId,
                    agent = selection.agentId,
                    legacyMode = selection.modeId
                )

                Napier.i(
                    "[AiChatGateway] Sending prompt through bridge: " +
                        "session=$sessionId provider=${selection.providerId} model=${selection.modelId} " +
                        "agent=${selection.agentId ?: selection.modeId.orEmpty()}"
                )
                bridgeClient.request(
                    ns = "ai",
                    action = "messages.send",
                    payload = json.toBridgePayload(request)
                ).decodePayloadOrThrow<JsonElement>(json)
                Napier.i("[AiChatGateway] Bridge accepted prompt for session=$sessionId")
            }
        }

        Napier.i(
            "[AiChatGateway] Sending prompt through direct OpenCode fallback: " +
                "session=$sessionId provider=${selection.providerId} model=${selection.modelId}"
        )
        return sessionRepository.sendMessageAsync(
            sessionId = sessionId,
            text = text,
            mode = selection.modeId ?: selection.agentId,
            variant = selection.variantId,
            attachments = attachments,
            modelId = selection.modelId,
            providerId = selection.providerId
        )
    }
}
