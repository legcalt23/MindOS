package com.mindos.android.voice

import com.mindos.speech.AudioEncoding
import com.mindos.speech.AudioInput
import com.mindos.speech.SpeechBrokerResult
import com.mindos.speech.SpeechBrokerTranscript
import com.mindos.speech.SpeechBrokerTransport
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URI
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class SpeechBrokerTokens(
    val firebaseIdToken: String,
    val appCheckToken: String,
) {
    init {
        require(firebaseIdToken.isNotBlank()) { "firebaseIdToken must not be blank." }
        require(appCheckToken.isNotBlank()) { "appCheckToken must not be blank." }
    }
}

fun interface SpeechBrokerTokenProvider {
    suspend fun getTokens(): SpeechBrokerTokens
}

data class AndroidSpeechBrokerConfig(
    val endpointUrl: String,
    val connectTimeoutMs: Int = 5_000,
    val readTimeoutMs: Int = 15_000,
    val maxResponseBytes: Int = 16 * 1024,
) {
    init {
        val uri = URI(endpointUrl)
        require(uri.scheme == "https") { "Speech broker endpoint must use HTTPS." }
        require(uri.host != null && uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "Speech broker endpoint must be an absolute HTTPS URL without credentials, query, or fragment."
        }
        require(connectTimeoutMs in 1..30_000) { "connectTimeoutMs must be between 1 and 30000." }
        require(readTimeoutMs in 1..30_000) { "readTimeoutMs must be between 1 and 30000." }
        require(maxResponseBytes in 1..(64 * 1024)) { "maxResponseBytes must be between 1 and 65536." }
    }
}

data class SpeechBrokerHttpRequest(
    val url: String,
    val headers: Map<String, String>,
    val body: ByteArray,
    val connectTimeoutMs: Int,
    val readTimeoutMs: Int,
    val maxResponseBytes: Int,
)

data class SpeechBrokerHttpResponse(
    val statusCode: Int,
    val body: String,
)

fun interface SpeechBrokerHttpEngine {
    suspend fun execute(request: SpeechBrokerHttpRequest): SpeechBrokerHttpResponse
}

class AndroidSpeechBrokerTransport(
    private val tokenProvider: SpeechBrokerTokenProvider,
    private val config: AndroidSpeechBrokerConfig,
    private val httpEngine: SpeechBrokerHttpEngine = UrlConnectionSpeechBrokerHttpEngine(),
    private val json: Json = Json { ignoreUnknownKeys = true },
) : SpeechBrokerTransport {

    override suspend fun transcribe(input: AudioInput): SpeechBrokerResult {
        val audioBytes = input.payload.copyBytes()
        return try {
            val tokens = tokenProvider.getTokens()
            val response = httpEngine.execute(
                SpeechBrokerHttpRequest(
                    url = config.endpointUrl,
                    headers = requestHeaders(input, tokens),
                    body = audioBytes,
                    connectTimeoutMs = config.connectTimeoutMs,
                    readTimeoutMs = config.readTimeoutMs,
                    maxResponseBytes = config.maxResponseBytes,
                ),
            )
            response.toBrokerResult()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: SocketTimeoutException) {
            SpeechBrokerResult.Timeout
        } catch (_: Exception) {
            SpeechBrokerResult.Unavailable
        } finally {
            audioBytes.fill(0)
        }
    }

    private fun requestHeaders(input: AudioInput, tokens: SpeechBrokerTokens): Map<String, String> = buildMap {
        put("Authorization", "Bearer ${tokens.firebaseIdToken}")
        put("X-Firebase-AppCheck", tokens.appCheckToken)
        put("Content-Type", "application/octet-stream")
        put("Accept", "application/json")
        put("X-MindOS-Request-Id", input.requestId)
        input.sessionId?.let { put("X-MindOS-Session-Id", it) }
        put("X-MindOS-Language-Tag", input.languageTag)
        put("X-MindOS-Audio-Encoding", input.format.encoding.toWireValue())
        put("X-MindOS-Sample-Rate-Hz", input.format.sampleRateHz.toString())
        put("X-MindOS-Channel-Count", input.format.channelCount.toString())
        put("X-MindOS-Duration-Ms", input.durationMs.toString())
    }

    private fun SpeechBrokerHttpResponse.toBrokerResult(): SpeechBrokerResult = when (statusCode) {
        200 -> {
            val body = json.decodeFromString<BrokerResponseBody>(body)
            SpeechBrokerResult.Success(
                SpeechBrokerTranscript(
                    requestId = body.requestId,
                    sessionId = body.sessionId,
                    text = body.transcript,
                    confidence = body.confidence,
                    languageTag = body.languageTag,
                    durationMs = body.durationMs,
                    backendId = body.backendId,
                    isFinal = body.isFinal,
                ),
            )
        }
        408, 504 -> SpeechBrokerResult.Timeout
        400, 401, 403, 413, 415, 429 -> SpeechBrokerResult.Rejected
        else -> SpeechBrokerResult.Unavailable
    }

    private fun AudioEncoding.toWireValue(): String = when (this) {
        AudioEncoding.PCM_SIGNED_16_LE -> "pcm_s16le"
    }
}

class UrlConnectionSpeechBrokerHttpEngine : SpeechBrokerHttpEngine {
    override suspend fun execute(request: SpeechBrokerHttpRequest): SpeechBrokerHttpResponse =
        withContext(Dispatchers.IO) {
            val connection = URL(request.url).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = request.connectTimeoutMs
                connection.readTimeout = request.readTimeoutMs
                connection.doOutput = true
                connection.instanceFollowRedirects = false
                request.headers.forEach(connection::setRequestProperty)
                connection.setFixedLengthStreamingMode(request.body.size)
                connection.outputStream.use { it.write(request.body) }

                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                SpeechBrokerHttpResponse(
                    statusCode = status,
                    body = stream?.use { it.readBoundedUtf8(request.maxResponseBytes) }.orEmpty(),
                )
            } finally {
                connection.disconnect()
            }
        }
}

@Serializable
private data class BrokerResponseBody(
    val requestId: String,
    val sessionId: String? = null,
    val transcript: String,
    val confidence: Float? = null,
    val languageTag: String,
    val durationMs: Long,
    val backendId: String,
    @SerialName("final")
    val isFinal: Boolean,
)

private fun java.io.InputStream.readBoundedUtf8(maxBytes: Int): String {
    val output = ByteArrayOutputStream(minOf(maxBytes, 4 * 1024))
    val buffer = ByteArray(4 * 1024)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) throw IOException("Speech broker response exceeded the configured limit.")
        output.write(buffer, 0, read)
    }
    return output.toString(Charsets.UTF_8.name())
}
