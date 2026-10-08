package com.example.naughty.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class SpeechTranscriptionHelper(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _finalText = MutableStateFlow("")
    val finalText: StateFlow<String> = _finalText.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _errorCode = MutableStateFlow<Int?>(null)
    val errorCode: StateFlow<Int?> = _errorCode.asStateFlow()

    val isOnDeviceAvailable: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
        } else {
            SpeechRecognizer.isRecognitionAvailable(context)
        }

    fun startListening(
        onPartial: ((String) -> Unit)? = null,
        onFinal: ((String) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        destroy()

        _errorMessage.value = null
        _errorCode.value = null
        _partialText.value = ""
        _finalText.value = ""
        _soundLevel.value = 0f

        val recognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
        } else if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            val msg = "Speech recognition service is not available on this device"
            _errorMessage.value = msg
            onError?.invoke(msg)
            return
        }

        speechRecognizer = recognizer

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _isListening.value = true
            }

            override fun onBeginningOfSpeech() {
                _isListening.value = true
            }

            override fun onRmsChanged(rmsdB: Float) {
                // rmsdB typically ranges from -2 to 10
                _soundLevel.value = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                _isListening.value = false
            }

            override fun onError(error: Int) {
                _isListening.value = false
                _errorCode.value = error
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Offline model not found or network disabled"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy"
                    SpeechRecognizer.ERROR_SERVER -> "Recognition server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                    SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests, please wait"
                    SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Recognition service disconnected"
                    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Language not supported for offline recognition on this device"
                    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Offline speech model not installed on this device"
                    SpeechRecognizer.ERROR_CANNOT_CHECK_SUPPORT -> "Cannot verify language support on device"
                    SpeechRecognizer.ERROR_CANNOT_LISTEN_TO_DOWNLOAD_EVENTS -> "Cannot monitor model download"
                    else -> "Speech recognition error ($error)"
                }
                // Don't treat NO_MATCH or SPEECH_TIMEOUT as critical crash errors
                if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                    _errorMessage.value = errorMsg
                    onError?.invoke(errorMsg)
                }
            }

            override fun onResults(results: Bundle?) {
                _isListening.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                if (text.isNotBlank()) {
                    _finalText.value = text
                    _partialText.value = text
                    onFinal?.invoke(text)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val text = matches?.firstOrNull()?.trim() ?: ""
                if (text.isNotBlank()) {
                    _partialText.value = text
                    onPartial?.invoke(text)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }

        try {
            recognizer.startListening(intent)
        } catch (e: Exception) {
            val msg = "Could not start speech recognition: ${e.localizedMessage}"
            _errorMessage.value = msg
            _isListening.value = false
            onError?.invoke(msg)
        }
    }

    fun triggerModelDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                }
                val recognizer = speechRecognizer ?: if (SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    SpeechRecognizer.createOnDeviceSpeechRecognizer(context).also { speechRecognizer = it }
                } else null
                recognizer?.triggerModelDownload(intent)
            } catch (e: Exception) {
                _errorMessage.value = "Model download error: ${e.localizedMessage}"
            }
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {}
        _isListening.value = false
    }

    fun destroy() {
        _isListening.value = false
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }
}
