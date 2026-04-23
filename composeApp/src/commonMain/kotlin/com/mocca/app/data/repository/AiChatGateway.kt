package com.mocca.app.data.repository

import com.mocca.app.bridge.connection.BridgeConnectionManager
import com.mocca.app.bridge.connection.BridgeConnectionStatus
import com.mocca.app.bridge.opencode.OpenCodeBridgeRepository
import com.mocca.app.domain.model.AiBridgeMessageModel
import com.mocca.app.domain.model.AiBridgeMessageRequest
import com.mocca.app.domain.model.AiEffectiveSelection
import com.mocca.app.domain.model.AttachedFile
import com.mocca.app.domain.model.ChatPart

class AiChatGateway(
    private val sessionRepository: SessionRepository,
    private val bridgeConnectionManager: BridgeConnectionManager
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
                OpenCodeBridgeRepository(bridgeClient).sendMessage(
                    AiBridgeMessageRequest(
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
                )
            }
        }

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
