package com.mocca.app.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.content.ContextCompat
import com.mocca.app.domain.model.VoiceInputState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AndroidVoiceInputProvider(
    private val context: Context
) : VoiceInputProvider {
    private val providerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val internalState = MutableStateFlow<VoiceInputState>(VoiceInputState.Idle)

    override val state: StateFlow<VoiceInputState> = internalState.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            internalState.value = VoiceInputState.Listening
        }

        override fun onBeginningOfSpeech() = Unit

        override fun onRmsChanged(rmsdB: Float) = Unit

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            if (internalState.value is VoiceInputState.Listening ||
                internalState.value is VoiceInputState.PartialResult
            ) {
                internalState.value = VoiceInputState.Idle
            }
        }

        override fun onError(error: Int) {
            val message = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording failed"
                SpeechRecognizer.ERROR_CLIENT -> "Voice input cancelled"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                SpeechRecognizer.ERROR_NETWORK,
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Voice input network error"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Voice recognition server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Voice input timed out"
                else -> "Voice input failed"
            }
            internalState.value = VoiceInputState.Error(message)
            Napier.w("[VoiceInput] Recognition error: $error ($message)")
        }

        override fun onResults(results: Bundle?) {
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()

            internalState.value = if (text.isNotEmpty()) {
                VoiceInputState.FinalResult(text)
            } else {
                VoiceInputState.Error("No speech detected")
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()

            if (text.isNotEmpty()) {
                internalState.value = VoiceInputState.PartialResult(text)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    override fun startListening() {
        providerScope.launch {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                internalState.value = VoiceInputState.NotAvailable
                return@launch
            }

            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                internalState.value = VoiceInputState.NeedsPermission
                return@launch
            }

            val recognizer = ensureSpeechRecognizer()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }

            internalState.value = VoiceInputState.Listening
            recognizer.cancel()
            recognizer.startListening(intent)
        }
    }

    override fun stopListening() {
        providerScope.launch {
            speechRecognizer?.stopListening()
            internalState.value = VoiceInputState.Idle
        }
    }

    override fun release() {
        providerScope.launch {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = null
            internalState.value = VoiceInputState.Idle
        }
        providerScope.cancel()
    }

    private fun ensureSpeechRecognizer(): SpeechRecognizer {
        val existing = speechRecognizer
        if (existing != null) return existing

        return SpeechRecognizer.createSpeechRecognizer(context).also {
            it.setRecognitionListener(listener)
            speechRecognizer = it
        }
    }
}
