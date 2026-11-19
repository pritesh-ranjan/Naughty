package com.example.naughty.util

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.text.Normalizer
import java.util.Locale
import kotlin.math.sqrt

/**
 * Abstraction for sentence-level semantic embeddings and cosine similarity, allowing
 * [VideoNoteSummarizer] to use the on-device LiteRT MiniLM embedder in production and
 * deterministic test doubles in JVM unit tests.
 */
interface SemanticSimilarityProvider {
    fun embedBatch(texts: List<String>): List<FloatArray>?
    fun similarity(textA: String, textB: String): Float?
}

data class TokenizedInput(
    val inputIds: LongArray,
    val attentionMask: LongArray,
    val activeLength: Int
)

/**
 * Lightweight WordPiece tokenizer compatible with BERT / `all-MiniLM-L6-v2` uncased vocabularies.
 */
class WordPieceTokenizer(
    private val vocab: Map<String, Int>,
    val maxSeqLen: Int = MAX_SEQ_LEN
) {
    private val padId: Int = vocab[PAD_TOKEN] ?: 0
    private val unkId: Int = vocab[UNK_TOKEN] ?: 100
    private val clsId: Int = vocab[CLS_TOKEN] ?: 101
    private val sepId: Int = vocab[SEP_TOKEN] ?: 102

    fun isWholeWord(word: String): Boolean {
        val clean = word.lowercase(Locale.ROOT).trim()
        return clean.isNotEmpty() && vocab.containsKey(clean)
    }

    fun tokenize(text: String, seqLen: Int = maxSeqLen): TokenizedInput {
        val targetLen = seqLen.coerceAtLeast(4)
        val normalized = stripAccents(text.lowercase(Locale.ROOT)).trim()
        val basicTokens = splitIntoBasicTokens(normalized)

        val wordPieceIds = ArrayList<Int>(basicTokens.size + 4)
        wordPieceIds.add(clsId)

        val maxContentTokens = targetLen - 2
        for (token in basicTokens) {
            if (wordPieceIds.size - 1 >= maxContentTokens) break
            val subIds = tokenizeWord(token)
            for (id in subIds) {
                if (wordPieceIds.size - 1 >= maxContentTokens) break
                wordPieceIds.add(id)
            }
        }
        wordPieceIds.add(sepId)

        val activeLength = wordPieceIds.size.coerceAtMost(targetLen)
        val inputIds = LongArray(targetLen) { idx ->
            if (idx < activeLength) wordPieceIds[idx].toLong() else padId.toLong()
        }
        val attentionMask = LongArray(targetLen) { idx ->
            if (idx < activeLength) 1L else 0L
        }
        return TokenizedInput(
            inputIds = inputIds,
            attentionMask = attentionMask,
            activeLength = activeLength
        )
    }

    private fun tokenizeWord(token: String): List<Int> {
        vocab[token]?.let { return listOf(it) }
        if (token.length > MAX_WORD_CHARS) return listOf(unkId)

        val subTokens = ArrayList<Int>(4)
        var start = 0
        while (start < token.length) {
            var end = token.length
            var matchedId: Int? = null
            while (start < end) {
                val substr = if (start == 0) {
                    token.substring(start, end)
                } else {
                    "##" + token.substring(start, end)
                }
                val id = vocab[substr]
                if (id != null) {
                    matchedId = id
                    break
                }
                end--
            }
            if (matchedId == null) {
                return listOf(unkId)
            }
            subTokens.add(matchedId)
            start = end
        }
        return subTokens
    }

    private fun splitIntoBasicTokens(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val tokens = ArrayList<String>(16)
        val current = StringBuilder()

        fun flush() {
            if (current.isNotEmpty()) {
                tokens.add(current.toString())
                current.setLength(0)
            }
        }

        for (ch in text) {
            when {
                ch.isWhitespace() -> flush()
                isPunctuationChar(ch) -> {
                    flush()
                    tokens.add(ch.toString())
                }
                else -> current.append(ch)
            }
        }
        flush()
        return tokens
    }

    private fun isPunctuationChar(ch: Char): Boolean {
        val cp = ch.code
        if ((cp in 33..47) || (cp in 58..64) || (cp in 91..96) || (cp in 123..126)) {
            return true
        }
        val type = Character.getType(ch)
        return type == Character.CONNECTOR_PUNCTUATION.toInt() ||
            type == Character.DASH_PUNCTUATION.toInt() ||
            type == Character.START_PUNCTUATION.toInt() ||
            type == Character.END_PUNCTUATION.toInt() ||
            type == Character.INITIAL_QUOTE_PUNCTUATION.toInt() ||
            type == Character.FINAL_QUOTE_PUNCTUATION.toInt() ||
            type == Character.OTHER_PUNCTUATION.toInt() ||
            type == Character.MATH_SYMBOL.toInt() ||
            type == Character.CURRENCY_SYMBOL.toInt() ||
            type == Character.OTHER_SYMBOL.toInt()
    }

    private fun stripAccents(input: String): String {
        val normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
        val sb = StringBuilder(normalized.length)
        for (ch in normalized) {
            if (Character.getType(ch) != Character.NON_SPACING_MARK.toInt()) {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    companion object {
        const val MAX_SEQ_LEN = 128
        private const val MAX_WORD_CHARS = 100
        private const val PAD_TOKEN = "[PAD]"
        private const val UNK_TOKEN = "[UNK]"
        private const val CLS_TOKEN = "[CLS]"
        private const val SEP_TOKEN = "[SEP]"

        fun fromReader(reader: BufferedReader, maxSeqLen: Int = MAX_SEQ_LEN): WordPieceTokenizer {
            val map = HashMap<String, Int>(32768)
            var index = 0
            reader.forEachLine { line ->
                val token = line.trimEnd('\r', '\n')
                if (token.isNotEmpty() || index == 0) {
                    map[token] = index
                }
                index++
            }
            return WordPieceTokenizer(map, maxSeqLen)
        }
    }
}

/**
 * Offline sentence embedder powered by quantized `all-MiniLM-L6-v2` on Google AI Edge LiteRT.
 * Produces 384-dimensional L2-normalized vectors for fast dot-product cosine similarity,
 * cross-modal Audio-OCR gating, semantic clustering, and TextRank centroid scoring.
 */
class SemanticEmbedder private constructor(
    context: Context
) : SemanticSimilarityProvider {

    private val appContext = context.applicationContext
    private val initLock = Any()
    private val inferenceLock = Any()

    @Volatile
    private var initialized = false

    @Volatile
    private var initFailed = false

    private var interpreter: Interpreter? = null
    private var tokenizer: WordPieceTokenizer? = null
    private var modelSeqLen: Int = WordPieceTokenizer.MAX_SEQ_LEN

    private fun ensureInitialized(): Boolean {
        if (initialized) return interpreter != null && tokenizer != null
        if (initFailed) return false

        return synchronized(initLock) {
            if (initialized) return@synchronized (interpreter != null && tokenizer != null)
            if (initFailed) return@synchronized false

            try {
                val loadedTokenizer = cachedTokenizer ?: appContext.assets.open(VOCAB_ASSET_NAME).use { stream ->
                    BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                        WordPieceTokenizer.fromReader(reader, WordPieceTokenizer.MAX_SEQ_LEN)
                    }
                }.also { cachedTokenizer = it }

                val modelBuffer = getOrLoadModelBuffer(appContext)
                if (modelBuffer == null) {
                    initFailed = true
                    return@synchronized false
                }

                val options = Interpreter.Options().apply {
                    setNumThreads(2)
                }
                val interp = Interpreter(modelBuffer, options)
                val inputShape = interp.getInputTensor(0).shape()
                if (inputShape.size >= 2 && inputShape[1] > 0) {
                    modelSeqLen = inputShape[1]
                }

                tokenizer = loadedTokenizer
                interpreter = interp
                initialized = true
                true
            } catch (e: Throwable) {
                Log.w(TAG, "MiniLM LiteRT initialization failed, falling back to lexical scoring: ${e.message}")
                initFailed = true
                false
            }
        }
    }

    override fun embedBatch(texts: List<String>): List<FloatArray>? {
        if (texts.isEmpty()) return emptyList()
        if (!ensureInitialized()) return null

        val interp = interpreter ?: return null
        val tok = tokenizer ?: return null

        return synchronized(inferenceLock) {
            try {
                val results = ArrayList<FloatArray>(texts.size)
                val inputCount = interp.inputTensorCount
                val outputTensor = interp.getOutputTensor(0)
                val outputShape = outputTensor.shape()

                for (text in texts) {
                    if (text.isBlank()) {
                        results.add(FloatArray(EMBEDDING_DIM))
                        continue
                    }

                    val tokenized = tok.tokenize(text, modelSeqLen)
                    val inputs = Array<Any>(inputCount) { idx ->
                        val tensor = interp.getInputTensor(idx)
                        val name = tensor.name().lowercase(Locale.ROOT)
                        val dataType = tensor.dataType()

                        val sourceLongs = when {
                            name.contains("mask") || idx == 1 -> tokenized.attentionMask
                            name.contains("type") || name.contains("segment") || idx == 2 -> LongArray(modelSeqLen)
                            else -> tokenized.inputIds
                        }

                        if (dataType == DataType.INT32) {
                            arrayOf(IntArray(modelSeqLen) { pos -> sourceLongs[pos].toInt() })
                        } else {
                            arrayOf(sourceLongs)
                        }
                    }

                    val outputs = HashMap<Int, Any>(1)
                    if (outputShape.size == 2) {
                        val embDim = outputShape[1].coerceAtLeast(1)
                        val outBuf = Array(1) { FloatArray(embDim) }
                        outputs[0] = outBuf
                        interp.runForMultipleInputsOutputs(inputs, outputs)
                        results.add(l2Normalize(outBuf[0]))
                    } else if (outputShape.size == 3) {
                        val seqLenOut = outputShape[1].coerceAtLeast(1)
                        val embDim = outputShape[2].coerceAtLeast(1)
                        val outBuf = Array(1) { Array(seqLenOut) { FloatArray(embDim) } }
                        outputs[0] = outBuf
                        interp.runForMultipleInputsOutputs(inputs, outputs)
                        results.add(meanPoolAndNormalize(outBuf[0], tokenized.attentionMask))
                    } else {
                        return@synchronized null
                    }
                }
                results
            } catch (e: Throwable) {
                Log.w(TAG, "MiniLM inference failed, falling back to lexical scoring: ${e.message}")
                null
            }
        }
    }

    override fun similarity(textA: String, textB: String): Float? {
        val embeddings = embedBatch(listOf(textA, textB)) ?: return null
        if (embeddings.size < 2) return null
        return dotProduct(embeddings[0], embeddings[1])
    }

    companion object {
        private const val TAG = "SemanticEmbedder"
        private const val MODEL_ASSET_NAME = "all_minilm_l6_v2_int8.tflite"
        private const val VOCAB_ASSET_NAME = "vocab.txt"
        const val EMBEDDING_DIM = 384

        @Volatile
        private var instance: SemanticEmbedder? = null

        @Volatile
        private var cachedModelBuffer: MappedByteBuffer? = null

        @Volatile
        private var cachedTokenizer: WordPieceTokenizer? = null

        fun getInstance(context: Context): SemanticEmbedder {
            return instance ?: synchronized(this) {
                instance ?: SemanticEmbedder(context.applicationContext).also { instance = it }
            }
        }

        fun getOrLoadTokenizer(context: Context? = null): WordPieceTokenizer? {
            cachedTokenizer?.let { return it }
            return synchronized(this) {
                cachedTokenizer ?: try {
                    if (context != null) {
                        context.applicationContext.assets.open(VOCAB_ASSET_NAME).use { stream ->
                            BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { reader ->
                                WordPieceTokenizer.fromReader(reader, WordPieceTokenizer.MAX_SEQ_LEN)
                            }
                        }
                    } else {
                        // JVM unit-test fallback: look up bundled vocab.txt directly on disk
                        val candidates = listOf(
                            java.io.File("app/src/main/assets/$VOCAB_ASSET_NAME"),
                            java.io.File("src/main/assets/$VOCAB_ASSET_NAME")
                        )
                        val vocabFile = candidates.firstOrNull { it.isFile } ?: return@synchronized null
                        vocabFile.bufferedReader(Charsets.UTF_8).use { reader ->
                            WordPieceTokenizer.fromReader(reader, WordPieceTokenizer.MAX_SEQ_LEN)
                        }
                    }
                } catch (_: Throwable) {
                    null
                }?.also { cachedTokenizer = it }
            }
        }

        private fun getOrLoadModelBuffer(context: Context): MappedByteBuffer? {
            cachedModelBuffer?.let { return it }
            return synchronized(this) {
                cachedModelBuffer ?: try {
                    val afd: AssetFileDescriptor = context.assets.openFd(MODEL_ASSET_NAME)
                    afd.use { descriptor ->
                        FileInputStream(descriptor.fileDescriptor).use { input ->
                            input.channel.map(
                                FileChannel.MapMode.READ_ONLY,
                                descriptor.startOffset,
                                descriptor.declaredLength
                            )
                        }
                    }.also { cachedModelBuffer = it }
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to load $MODEL_ASSET_NAME from assets: ${e.message}")
                    null
                }
            }
        }

        fun dotProduct(a: FloatArray, b: FloatArray): Float {
            val len = minOf(a.size, b.size)
            var sum = 0f
            for (i in 0 until len) {
                sum += a[i] * b[i]
            }
            return sum
        }

        fun l2Normalize(vec: FloatArray): FloatArray {
            var sumSq = 0.0
            for (v in vec) {
                sumSq += (v * v).toDouble()
            }
            val norm = sqrt(sumSq).toFloat()
            if (norm <= 1e-12f) return FloatArray(vec.size)
            val inv = 1f / norm
            return FloatArray(vec.size) { i -> vec[i] * inv }
        }

        fun meanPoolAndNormalize(
            tokenEmbeddings: Array<FloatArray>,
            attentionMask: LongArray
        ): FloatArray {
            if (tokenEmbeddings.isEmpty()) return FloatArray(EMBEDDING_DIM)
            val dim = tokenEmbeddings[0].size
            val pooled = FloatArray(dim)
            var activeTokens = 0
            val seqCount = minOf(tokenEmbeddings.size, attentionMask.size)

            for (t in 0 until seqCount) {
                if (attentionMask[t] > 0L) {
                    val row = tokenEmbeddings[t]
                    for (d in 0 until dim) {
                        pooled[d] += row[d]
                    }
                    activeTokens++
                }
            }
            if (activeTokens == 0) return FloatArray(dim)
            val inv = 1f / activeTokens.toFloat()
            for (d in 0 until dim) {
                pooled[d] *= inv
            }
            return l2Normalize(pooled)
        }

        fun computeCentroid(
            embeddings: List<FloatArray>,
            weights: FloatArray? = null
        ): FloatArray {
            if (embeddings.isEmpty()) return FloatArray(EMBEDDING_DIM)
            val dim = embeddings[0].size
            val acc = FloatArray(dim)
            for (i in embeddings.indices) {
                val w = weights?.getOrNull(i)?.coerceAtLeast(0f) ?: 1f
                if (w <= 0f) continue
                val vec = embeddings[i]
                for (d in 0 until minOf(dim, vec.size)) {
                    acc[d] += vec[d] * w
                }
            }
            return l2Normalize(acc)
        }
    }
}
