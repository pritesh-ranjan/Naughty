package com.example.naughty.util

import java.io.File
import java.util.Locale
import java.util.Properties

data class LoadedVideoFixture(
    val frames: List<OcrFrameSample>,
    val transcript: String,
    val classification: AudioClassificationResult?,
    val similarityProvider: SemanticSimilarityProvider?
)

object VideoFixtureLoader {

    fun load(fixtureName: String): LoadedVideoFixture {
        val dir = resolveFixtureDir(fixtureName)
        val framesFile = File(dir, "frames.tsv")
        val transcriptFile = File(dir, "transcript.txt")
        val metaFile = File(dir, "meta.properties")
        val embeddingsFile = File(dir, "embeddings.tsv")

        val frames = parseFramesTsv(framesFile)
        val transcript = if (transcriptFile.isFile) transcriptFile.readText(Charsets.UTF_8) else ""
        val classification = parseMeta(metaFile)
        val provider = if (embeddingsFile.isFile) parseEmbeddingsTsv(embeddingsFile) else null

        return LoadedVideoFixture(
            frames = frames,
            transcript = transcript,
            classification = classification,
            similarityProvider = provider
        )
    }

    private fun resolveFixtureDir(fixtureName: String): File {
        val candidates = listOf(
            File("src/test/resources/video_fixtures/$fixtureName"),
            File("app/src/test/resources/video_fixtures/$fixtureName")
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("Fixture directory not found for '$fixtureName' (checked ${candidates.map { it.absolutePath }})")
    }

    private fun parseFramesTsv(file: File): List<OcrFrameSample> {
        val grouped = LinkedHashMap<Long, MutableList<Pair<Pair<Int, Int>, OcrLine>>>()
        file.useLines(Charsets.UTF_8) { lines ->
            lines.drop(1).forEach { raw ->
                if (raw.isBlank()) return@forEach
                val parts = raw.split('\t')
                if (parts.size < 11) return@forEach
                val ts = parts[0].toLongOrNull() ?: return@forEach
                val fw = parts[1].toIntOrNull() ?: 0
                val fh = parts[2].toIntOrNull() ?: 0
                val blockIdx = parts[3].toIntOrNull() ?: -1
                val left = parts[4].toIntOrNull() ?: 0
                val top = parts[5].toIntOrNull() ?: 0
                val right = parts[6].toIntOrNull() ?: 0
                val bottom = parts[7].toIntOrNull() ?: 0
                val conf = parts[8].toFloatOrNull() ?: 1f
                val angle = parts[9].toFloatOrNull() ?: 0f
                val text = parts.drop(10).joinToString("\t")
                grouped.getOrPut(ts) { mutableListOf() }.add(
                    (fw to fh) to OcrLine(
                        text = text,
                        left = left,
                        top = top,
                        right = right,
                        bottom = bottom,
                        confidence = conf,
                        angle = angle,
                        blockIndex = blockIdx
                    )
                )
            }
        }
        return grouped.entries.map { (ts, items) ->
            val (fw, fh) = items.first().first
            val ocrLines = items.map { it.second }
            OcrFrameSample(
                timestampMs = ts,
                text = ocrLines.joinToString("\n") { it.text },
                lines = ocrLines,
                frameWidth = fw,
                frameHeight = fh
            )
        }
    }

    private fun parseMeta(file: File): AudioClassificationResult? {
        if (!file.isFile) return null
        val props = Properties().apply { file.inputStream().use { load(it) } }
        val profileName = props.getProperty("profile") ?: return null
        val profile = runCatching { AcousticProfile.valueOf(profileName) }.getOrNull() ?: return null
        return AudioClassificationResult(
            profile = profile,
            speechScore = props.getProperty("speechScore", "0")?.toFloatOrNull() ?: 0f,
            musicScore = props.getProperty("musicScore", "0")?.toFloatOrNull() ?: 0f,
            speechWindowRatio = props.getProperty("speechWindowRatio", "0")?.toFloatOrNull() ?: 0f
        )
    }

    private fun parseEmbeddingsTsv(file: File): SemanticSimilarityProvider {
        val exactMap = HashMap<String, FloatArray>()
        val normMap = HashMap<String, FloatArray>()
        file.useLines(Charsets.UTF_8) { lines ->
            lines.forEach { raw ->
                val tabIdx = raw.indexOf('\t')
                if (tabIdx <= 0) return@forEach
                val text = raw.substring(0, tabIdx)
                val vec = raw.substring(tabIdx + 1)
                    .split(',')
                    .mapNotNull { it.toFloatOrNull() }
                    .toFloatArray()
                if (vec.isNotEmpty()) {
                    exactMap[text] = vec
                    normMap[normalizeKey(text)] = vec
                }
            }
        }

        fun lookup(text: String): FloatArray {
            exactMap[text]?.let { return it }
            normMap[normalizeKey(text)]?.let { return it }
            // Fallback: prefix match for truncated clauses
            val norm = normalizeKey(text)
            val prefixHit = normMap.entries.firstOrNull { (k, _) ->
                (norm.length >= 12 && k.startsWith(norm)) || (k.length >= 12 && norm.startsWith(k))
            }?.value
            return prefixHit ?: FloatArray(SemanticEmbedder.EMBEDDING_DIM)
        }

        return object : SemanticSimilarityProvider {
            override fun embedBatch(texts: List<String>): List<FloatArray> = texts.map(::lookup)
            override fun similarity(textA: String, textB: String): Float {
                val va = lookup(textA)
                val vb = lookup(textB)
                val normA = va.any { it != 0f }
                val normB = vb.any { it != 0f }
                return if (normA && normB) SemanticEmbedder.dotProduct(va, vb) else 0f
            }
        }
    }

    private fun normalizeKey(text: String): String {
        return text.lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
