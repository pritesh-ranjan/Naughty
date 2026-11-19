package com.example.naughty.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ln

data class OcrLine(
    val text: String,
    val left: Int = 0,
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
    val confidence: Float = 1f,
    val angle: Float = 0f,
    val blockIndex: Int = -1
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
    val centerX: Float get() = (left + right) * 0.5f
    val centerY: Float get() = (top + bottom) * 0.5f
}

data class OcrFrameSample(
    val timestampMs: Long,
    val text: String,
    val lines: List<OcrLine> = emptyList(),
    val frameWidth: Int = 0,
    val frameHeight: Int = 0
)

data class TrackedOcrBlock(
    val text: String,
    val firstSeenMs: Long,
    val lastSeenMs: Long,
    val sampleCount: Int,
    val meanConfidence: Float = 1f,
    val maxAbsAngle: Float = 0f,
    val meanHeightRatio: Float = 0.04f,
    val centerJitter: Float = 0f,
    val hasGeometry: Boolean = false
) {
    val visibilityDurationMs: Long
        get() = (lastSeenMs - firstSeenMs).coerceAtLeast(250L)
}

data class SummarizedVideoNote(
    val title: String,
    val content: String,
    val pointCount: Int,
    val cardType: String = "modular"
)

object VideoNoteSummarizer {

    private const val MAX_SUMMARY_POINTS = 12
    internal const val CROSS_MODAL_SEMANTIC_THRESHOLD = 0.38f
    internal const val SEMANTIC_CLUSTER_THRESHOLD = 0.72f
    private const val CLUSTER_SEED_GUARD_THRESHOLD = 0.58f
    private const val TITLE_SEMANTIC_DEDUP_THRESHOLD = 0.65f
    private const val OVERLAY_LIKELIHOOD_THRESHOLD = 0.45f
    private const val MIN_OCR_LINE_CONFIDENCE = 0.55f
    private const val MAX_OVERLAY_ANGLE_DEGREES = 7f
    private const val MIN_PLAUSIBLE_WORD_RATIO = 0.60f
    private const val MAX_SPOKEN_STEP_WORDS = 22

    private val STOP_WORDS = setOf(
        "a", "an", "and", "are", "as", "at", "be", "been", "being", "but", "by",
        "can", "could", "did", "do", "does", "doing", "done", "for", "from", "get",
        "got", "had", "has", "have", "having", "he", "her", "here", "hers", "him",
        "his", "how", "i", "if", "in", "into", "is", "it", "its", "just", "let",
        "like", "may", "me", "might", "more", "most", "much", "must", "my", "no",
        "not", "now", "of", "off", "on", "or", "other", "our", "out", "over", "own",
        "put", "really", "said", "same", "see", "she", "should", "so", "some",
        "such", "take", "than", "that", "the", "their", "them", "then", "there",
        "these", "they", "this", "those", "through", "to", "too", "under", "up",
        "us", "use", "very", "was", "we", "were", "what", "when", "where", "which",
        "while", "who", "whom", "why", "will", "with", "would", "you", "your"
    )

    private val MERGE_CONNECTOR_ENDINGS = setOf(
        "to", "your", "of", "for", "the", "a", "an", "in", "on", "with",
        "and", "or", "how", "want", "make", "keep", "be", "from", "into", "about"
    )

    private val TITLE_SMALL_WORDS = setOf(
        "a", "an", "and", "as", "at", "but", "by", "for", "from", "in",
        "into", "nor", "of", "on", "or", "over", "per", "the", "to", "via", "with"
    )

    private val PRESERVED_ACRONYMS = setOf(
        "POV", "DIY", "FAQ", "AI", "ML", "OCR", "API", "SDK", "UI", "UX",
        "URL", "HTTP", "HTTPS", "TLS", "SQL", "PDF", "JSON", "CSV", "TSV",
        "CPU", "GPU", "RAM", "GB", "MB", "IO", "JVM", "CRT"
    )

    private val VALID_TWO_LETTER_WORDS = setOf(
        "am", "an", "as", "at", "be", "by", "do", "go", "he", "hi", "id", "if",
        "in", "is", "it", "me", "my", "no", "of", "oh", "ok", "on", "or", "ox",
        "pm", "so", "to", "up", "us", "vs", "we", "tv", "ai", "io", "pc", "ad",
        "lb", "oz", "hr", "wk", "yr"
    )

    private val NUMBER_WORD_MAP = mapOf(
        "one" to "1", "two" to "2", "three" to "3", "four" to "4", "five" to "5",
        "six" to "6", "seven" to "7", "eight" to "8", "nine" to "9", "ten" to "10"
    )

    private val FILLER_PREFIX_REGEX = Regex(
        pattern = """^(?:um+|uh+|ah+|er+|hm+|so|well|okay|ok|alright|right|like|basically|actually|literally|honestly|anyway|you know|i mean|hey guys|hi everyone|hello everyone|welcome back|in this video|today we(?:'re| are) going to|we(?:'re| are) going to|let's see|let's get started|as you can see|listen(?:\s+(?:here|up))?)\b[,:\-\s]*""",
        option = RegexOption.IGNORE_CASE
    )

    private val TRAILING_PHATIC_REGEX = Regex(
        pattern = """(?:[,\s]*(?:yeah\s+)?(?:that's what i'm saying|if you know what i mean|you know what i mean|you know what i'm saying|if that makes sense|or whatever|you know|i mean|to be honest|at the end of the day|like that|right\??)(?:\s+(?:like|though|man|bro))?)+[.!?\s]*$""",
        option = RegexOption.IGNORE_CASE
    )

    private val CTA_NOISE_REGEX = Regex(
        pattern = """^(?:(?:please\s+|don't forget to\s+|be sure to\s+)?(?:like(?:\s+and\s+(?:follow|subscribe|share))?|follow(?:\s+me)?\s+for\s+more|subscribe(?:\s+for\s+more)?|hit\s+the\s+bell|comment\s+below|drop\s+a\s+comment|check\s+(?:the\s+)?link\s+in\s+bio|link\s+in\s+bio|save\s+this(?:\s+(?:post|video|reel))?\s+for\s+later|share\s+this(?:\s+with\s+a\s+friend)?))[.!?\s]*$""",
        option = RegexOption.IGNORE_CASE
    )

    private val REACTION_NOISE_REGEX = Regex(
        pattern = """^(?:[a-z]{2,4}[,!\s]+what\s+the\b|what\s+the\s+(?:f\*+|fuck|heck|hell))\b.*$""",
        option = RegexOption.IGNORE_CASE
    )

    private val BOILERPLATE_MAKE_SURE_REGEX = Regex(
        pattern = """^(?:you\s+(?:need|have|want)\s+to\s+)?make\s+sure\s+(?:that\s+)?you\s+""",
        option = RegexOption.IGNORE_CASE
    )

    private val BOILERPLATE_WHAT_TO_DO_REGEX = Regex(
        pattern = """^what\s+you\s+(?:need|want|have)\s+to\s+do\s+is\s+(?:you\s+(?:need|want|have)\s+to\s+)?""",
        option = RegexOption.IGNORE_CASE
    )

    private val BOILERPLATE_FIRST_THING_REGEX = Regex(
        pattern = """^the\s+first\s+thing\s+you\s+(?:need|want|have)\s+to\s+do\s+is\s+""",
        option = RegexOption.IGNORE_CASE
    )

    private val BOILERPLATE_SHOW_HOW_REGEX = Regex(
        pattern = """^i(?:'m|\s+am)\s+going\s+to\s+show\s+you\s+how\s+to\s+""",
        option = RegexOption.IGNORE_CASE
    )

    private val BOILERPLATE_REASON_REGEX = Regex(
        pattern = """^the\s+reason\s+why\s+is\s+(?:because\s+)?""",
        option = RegexOption.IGNORE_CASE
    )

    private val BOILERPLATE_STARTING_WITH_REGEX = Regex(
        pattern = """^(?:starting|beginning|kicking)\s+(?:up|off|out)?\s*with\s+""",
        option = RegexOption.IGNORE_CASE
    )

    private val BOILERPLATE_PERFECT_THING_REGEX = Regex(
        pattern = """[,\s]*\b(?:this|it)\s+is\s+(?:one\s+of\s+)?the\s+(?:best|perfect|easiest|greatest|great|main|key|top)\s+(?:things?|ways?)\s+to\s+""",
        option = RegexOption.IGNORE_CASE
    )

    private val IN_ORDER_TO_REGEX = Regex("""\bin\s+order\s+to\b""", RegexOption.IGNORE_CASE)

    // Enumerators and sequence markers only (never split mid-sentence subordinators like "instead" or "because")
    private val DISCOURSE_MARKER_REGEX = Regex(
        pattern = """\b(?:firstly|first|secondly|second|thirdly|third|fourth|fifth|next|then|additionally|moreover|finally|lastly|(?:starting|beginning|kicking)\s+(?:up|off|out)?\s+with|(?:number|step)\s+(?:one|two|three|four|five|six|seven|eight|nine|ten|\d+))\b""",
        option = RegexOption.IGNORE_CASE
    )

    private val NUMBERED_STEP_PREFIX_REGEX = Regex(
        pattern = """^(?:number|step)\s+(?:one|two|three|four|five|six|seven|eight|nine|ten|\d+)\b[,:\-\s]*""",
        option = RegexOption.IGNORE_CASE
    )

    private val CONVERSATIONAL_BREAK_REGEX = Regex(
        pattern = """\b(yeah|like now|that's what|what the|wait|so now|and then|and also)\b""",
        option = RegexOption.IGNORE_CASE
    )

    private val LONG_CLAUSE_ELABORATION_REGEX = Regex(
        pattern = """\b(?:that\s+can|which\s+can|which\s+helps?|leaving\s+your|whenever\s+you|without\s+needing\s+to|instead\s+of)\b""",
        option = RegexOption.IGNORE_CASE
    )

    private val INTRO_HOOK_REGEX = Regex(
        pattern = """^(?:here\s+are\s+)?(?:\d+|two|three|four|five|six|seven|eight|nine|ten)\s+(?:simple\s+|easy\s+|quick\s+|best\s+)?(?:ways|tips|steps|habits|things|reasons)\s+(?:to|for)\b.*$""",
        option = RegexOption.IGNORE_CASE
    )

    private val QUANTITATIVE_METRIC_REGEX = Regex(
        pattern = """(?:\b\d+(?:\.\d+)?\s*(?:%|ms|s|sec|mins?|minutes?|hours?|days?|ml|l|g|kg|rps|x|k|m|gb|mb|sets?|reps?)\b|\$\s*\d+|\b(?:q[1-4]|yoy)\b)""",
        option = RegexOption.IGNORE_CASE
    )

    private val PACKAGING_SLOGAN_REGEX = Regex(
        pattern = """(?:#\s*1\s+trusted\b|\btrusted\b.*\bbr[ae]nd\b|\bstandard\s+of\s+purity\b|\bnet\s+wt\.?\b|\b\d+\s*(?:oz|lb)\b|\(\s*\d+\s*g\s*\)|\b\d+\+\s*uses\b|\ball\s+rights\s+reserved\b)""",
        option = RegexOption.IGNORE_CASE
    )

    private val ENUM_STEP_SPAN_REGEX = Regex(
        pattern = """\b(?:number|step)\s+(?:two|three|four|five|six|seven|eight|nine|ten|[2-9]|10)\b""",
        option = RegexOption.IGNORE_CASE
    )

    // General syntactic repair for false ASR sentence breaks before noun "can"
    // e.g. "...bottom of your <noun>. Can <non-pronoun>..." -> "...bottom of your <noun> can. <non-pronoun>..."
    private val ASR_FALSE_MODAL_PERIOD_REGEX = Regex(
        pattern = """\b((?:your|the|a|an|my|our|their)\s+[a-z]{3,})\.\s+Can\s+(?!(?:you|we|i|it|they|he|she|this|that|anyone|someone)\b)""",
        option = RegexOption.IGNORE_CASE
    )

    private val EXPLANATORY_VERB_SET = setOf(
        "helps", "help", "keeps", "keep", "can", "will", "may", "might", "prevents",
        "removes", "eliminates", "neutralizes", "absorbs", "gives", "fills", "makes",
        "is", "are", "was", "were"
    )

    private val DANGLING_SPATIAL_PREPS = setOf(
        "inside", "outside", "within", "underneath", "beneath", "around"
    )

    private val POSSESSIVE_OR_DET = setOf("your", "the", "a", "an", "my", "our", "their")

    private val PARTICIPLE_PREDECESSOR_BLOCKLIST = setOf(
        "by", "for", "of", "from", "in", "on", "to", "without", "before", "after",
        "is", "are", "was", "were", "be", "been", "keep", "keeps", "start", "starts",
        "stop", "stops", "try", "avoid", "while"
    )

    private val PARTICIPLE_FOLLOWER_ALLOWLIST = setOf(
        "a", "an", "the", "your", "my", "our", "their", "its", "this", "that",
        "things", "items", "something"
    )

    private val STUTTER_REGEX = Regex("""\b([a-zA-Z']+)\s+\1\b""", RegexOption.IGNORE_CASE)
    private val TIMECODE_NOISE_REGEX = Regex("""^\d{1,2}:\d{2}(?::\d{2})?(?:\s*/\s*\d{1,2}:\d{2}(?::\d{2})?)?$""")
    private val PERCENT_OR_NUMBER_ONLY_REGEX = Regex("""^\d{1,3}%?$""")
    private val NON_ALNUM_ONLY_REGEX = Regex("""^[^a-zA-Z0-9]+$""")
    private val HANDLE_OR_WATERMARK_REGEX = Regex("""^(?:@[\w.]+|[0-9]*[A-Z0-9_]{4,}_[A-Z0-9_]*|[0-9]+[A-Z]{3,}[A-Z0-9_]*)$""")
    private val MIXED_CASE_ANOMALY_REGEX = Regex("""[a-z][A-Z]|[A-Z]{2,}[a-z]+[A-Z]|[A-Za-z]+\d+[A-Za-z]+""")
    private val GARBLED_OCR_CHAR_REGEX = Regex("""[A-Z]{2,}[a-z]+[A-Z]|[a-z]+[A-Z]{2,}[a-z]|[A-Za-z]\d+[A-Za-z]""")
    private val ACRONYM_TOKEN_REGEX = Regex("""^[A-Z]{2,6}\d{0,2}$""")
    private val AUDIO_TICKER_REGEX = Regex("""^(?:.{2,35}\s*[•·♪♫]\s*.{2,40}|[♪♫].{2,50})$""")
    private val MUSIC_CREDIT_REGEX = Regex(
        pattern = """(?:\b[O0]p(?:us)?\.?\s*\d+(?:\s*,?\s*No\.?(?:\s*[\d!Il]+)?)?|[O0]p(?:us)?\.?\s*\d+\s*,\s*No\.?(?:\s*[\d!Il]+)?|\bOriginal\s+(?:Audio|Sound|Track|Music)\b|\bOfficial\s+(?:Audio|Soundtrack|Music\s+Video)\b|^(?:feat\.|ft\.|prod\.\s*by|music:|audio:|sound:|track:)\s*.+$|^.{2,35}\s*[•·♪♫\-]\s*(?:original\s+audio|official\s+audio|soundtrack|feat\..+|ft\..+)$)""",
        option = RegexOption.IGNORE_CASE
    )
    private val LYRIC_VOCALIZATION_REGEX = Regex(
        pattern = """\b(?:la\s+la+|na\s+na+|ooh+|oh\s+oh+|yeah\s+yeah+|baby\s+baby+|da\s+da+|mmm+|hey\s+hey+)\b""",
        option = RegexOption.IGNORE_CASE
    )

    internal enum class OcrRichness {
        MINIMAL,
        MODERATE,
        RICH
    }

    internal enum class AudioModalityDecision {
        PREFER_AUDIO,
        MERGE_BOTH,
        FILTER_AUDIO_TO_MATCHING_CLAUSES,
        OCR_ONLY_DROP_UNRELATED_AUDIO
    }

    private enum class CandidateSource {
        OCR,
        AUDIO,
        MERGED
    }

    private data class CandidatePoint(
        val text: String,
        val source: CandidateSource,
        val orderTimestampMs: Long,
        val visibilityDurationMs: Long,
        val sampleCount: Int,
        val isOverlay: Boolean = true,
        val isIntroHook: Boolean = false
    )

    private data class TrackedOcrAccumulator(
        var text: String,
        var firstSeenMs: Long,
        var lastSeenMs: Long,
        var sampleCount: Int,
        val confidences: MutableList<Float> = mutableListOf(),
        val absAngles: MutableList<Float> = mutableListOf(),
        val heightRatios: MutableList<Float> = mutableListOf(),
        val normCenterXs: MutableList<Float> = mutableListOf(),
        val normCenterYs: MutableList<Float> = mutableListOf(),
        var hasGeometry: Boolean = false
    ) {
        fun toBlock(): TrackedOcrBlock {
            val meanConf = if (confidences.isEmpty()) 1f else confidences.average().toFloat()
            val maxAng = absAngles.maxOrNull() ?: 0f
            val meanH = if (heightRatios.isEmpty()) 0.04f else heightRatios.average().toFloat()
            val jitterX = if (normCenterXs.size < 2) 0f else (normCenterXs.maxOrNull()!! - normCenterXs.minOrNull()!!)
            val jitterY = if (normCenterYs.size < 2) 0f else (normCenterYs.maxOrNull()!! - normCenterYs.minOrNull()!!)
            return TrackedOcrBlock(
                text = text,
                firstSeenMs = firstSeenMs,
                lastSeenMs = lastSeenMs,
                sampleCount = sampleCount,
                meanConfidence = meanConf,
                maxAbsAngle = maxAng,
                meanHeightRatio = meanH,
                centerJitter = maxOf(jitterX, jitterY),
                hasGeometry = hasGeometry
            )
        }
    }

    /**
     * Consolidates raw per-frame OCR snapshots at the line/caption level, merging wrapped
     * multi-line overlays and progressive reveals while tracking visibility duration and geometry.
     */
    fun trackAndConsolidateOcr(
        frames: List<OcrFrameSample>,
        tokenizer: WordPieceTokenizer? = SemanticEmbedder.getOrLoadTokenizer(null)
    ): List<TrackedOcrBlock> {
        if (frames.isEmpty()) return emptyList()

        val accumulators = mutableListOf<TrackedOcrAccumulator>()
        val sortedFrames = frames.sortedBy { it.timestampMs }

        for (frame in sortedFrames) {
            val mergedLines = consolidateFrameLines(frame)
            for (line in mergedLines) {
                val matchIndex = findMatchingBlockIndex(accumulators.map { it.text }, line.text)
                val fw = frame.frameWidth.toFloat()
                val fh = frame.frameHeight.toFloat()
                val hasLineGeom = frame.lines.isNotEmpty() && fw > 0f && fh > 0f && line.width > 0 && line.height > 0

                if (matchIndex >= 0) {
                    val existing = accumulators[matchIndex]
                    existing.text = choosePreferredOcrText(existing.text, line.text, tokenizer)
                    existing.firstSeenMs = minOf(existing.firstSeenMs, frame.timestampMs)
                    existing.lastSeenMs = maxOf(existing.lastSeenMs, frame.timestampMs)
                    existing.sampleCount += 1
                    if (hasLineGeom) {
                        existing.hasGeometry = true
                        existing.confidences.add(line.confidence)
                        existing.absAngles.add(abs(line.angle))
                        existing.heightRatios.add(line.height.toFloat() / fh)
                        existing.normCenterXs.add(line.centerX / fw)
                        existing.normCenterYs.add(line.centerY / fh)
                    }
                } else {
                    val acc = TrackedOcrAccumulator(
                        text = line.text,
                        firstSeenMs = frame.timestampMs,
                        lastSeenMs = frame.timestampMs,
                        sampleCount = 1,
                        hasGeometry = hasLineGeom
                    )
                    if (hasLineGeom) {
                        acc.confidences.add(line.confidence)
                        acc.absAngles.add(abs(line.angle))
                        acc.heightRatios.add(line.height.toFloat() / fh)
                        acc.normCenterXs.add(line.centerX / fw)
                        acc.normCenterYs.add(line.centerY / fh)
                    }
                    accumulators.add(acc)
                }
            }
        }

        return accumulators.map { it.toBlock() }
    }

    private fun consolidateFrameLines(frame: OcrFrameSample): List<OcrLine> {
        if (frame.lines.isNotEmpty()) {
            val cleaned = frame.lines
                .map { it.copy(text = cleanOcrLine(it.text)) }
                .filter {
                    it.confidence >= MIN_OCR_LINE_CONFIDENCE &&
                        abs(it.angle) <= MAX_OVERLAY_ANGLE_DEGREES &&
                        isMeaningfulOcrLine(it.text)
                }
                .sortedWith(compareBy<OcrLine> { it.top }.thenBy { it.left })

            if (cleaned.isEmpty()) return emptyList()
            val fw = frame.frameWidth.coerceAtLeast(480).toFloat()
            val merged = mutableListOf<OcrLine>()
            var current = cleaned.first()

            for (i in 1 until cleaned.size) {
                val next = cleaned[i]
                if (canMergeGeometricOcrLines(current, next, fw)) {
                    current = current.copy(
                        text = joinMergedOcrTexts(current.text, next.text),
                        left = minOf(current.left, next.left),
                        top = minOf(current.top, next.top),
                        right = maxOf(current.right, next.right),
                        bottom = maxOf(current.bottom, next.bottom),
                        confidence = minOf(current.confidence, next.confidence),
                        angle = if (abs(next.angle) > abs(current.angle)) next.angle else current.angle
                    )
                } else {
                    merged.add(current)
                    current = next
                }
            }
            merged.add(current)
            return merged.distinctBy { normalizeForComparison(it.text) }
        }

        val textLines = frame.text.lineSequence()
            .map { cleanOcrLine(it) }
            .filter { isMeaningfulOcrLine(it) }
            .toList()

        if (textLines.isEmpty()) return emptyList()
        val mergedTexts = mutableListOf<String>()
        var currentText = textLines.first()
        for (i in 1 until textLines.size) {
            val nextText = textLines[i]
            if (canMergeTextOnlyOcrLines(currentText, nextText)) {
                currentText = joinMergedOcrTexts(currentText, nextText)
            } else {
                mergedTexts.add(currentText)
                currentText = nextText
            }
        }
        mergedTexts.add(currentText)
        return mergedTexts
            .distinctBy { normalizeForComparison(it) }
            .map { OcrLine(text = it) }
    }

    private fun canMergeGeometricOcrLines(a: OcrLine, b: OcrLine, frameWidth: Float): Boolean {
        if (!canMergeTextOnlyOcrLines(a.text, b.text)) return false
        val sameBlock = a.blockIndex >= 0 && a.blockIndex == b.blockIndex
        val maxH = maxOf(a.height, b.height).coerceAtLeast(1)
        val minH = minOf(a.height, b.height)
        val heightRatio = minH.toFloat() / maxH.toFloat()
        if (heightRatio < 0.45f) return false

        val verticalGap = b.top - a.bottom
        if (!sameBlock && (verticalGap < -maxH * 0.4f || verticalGap > maxH * 1.25f)) return false

        val bothAllCaps = isAllCapsBannerFragment(a.text) && isAllCapsBannerFragment(b.text)
        val endsWithConnector = tokenizeWords(a.text).lastOrNull() in MERGE_CONNECTOR_ENDINGS
        if (bothAllCaps && !endsWithConnector && (verticalGap > maxH * 0.55f || abs(a.angle - b.angle) >= 1.5f)) {
            return false
        }

        val centerAligned = abs(a.centerX - b.centerX) <= frameWidth * 0.15f
        val leftAligned = abs(a.left - b.left) <= frameWidth * 0.08f
        if (!centerAligned && !leftAligned) return false

        return abs(a.angle) < 6f && abs(b.angle) < 6f
    }

    private fun canMergeTextOnlyOcrLines(lineA: String, lineB: String): Boolean {
        val trimA = lineA.trim()
        val trimB = lineB.trim()
        if (trimA.isBlank() || trimB.isBlank()) return false
        if (trimB.startsWith("-") || trimB.startsWith("*") || trimB.startsWith("•")) return false
        if (trimB.matches(Regex("""^\d+[.)]\s+.*"""))) return false
        if (trimA.contains('%') || trimA.contains('$') || trimB.contains('%') || trimB.contains('$')) return false

        val wordsA = tokenizeWords(trimA)
        val wordsB = tokenizeWords(trimB)
        if (wordsA.isEmpty() || wordsB.isEmpty()) return false
        if (wordsA.size + wordsB.size > 12) return false

        val endsWithConnector = !trimA.endsWith('.') &&
            !trimA.endsWith('!') &&
            !trimA.endsWith('?') &&
            wordsA.last() in MERGE_CONNECTOR_ENDINGS &&
            wordsB.size <= 6

        val bothAllCapsBanners = isAllCapsBannerFragment(trimA) && isAllCapsBannerFragment(trimB)
        return endsWithConnector || bothAllCapsBanners
    }

    private fun isAllCapsBannerFragment(text: String): Boolean {
        val hasLower = text.any { it.isLowerCase() }
        val upperCount = text.count { it.isUpperCase() }
        val words = tokenizeWords(text)
        return !hasLower && upperCount >= 3 && words.size in 1..5
    }

    private fun joinMergedOcrTexts(a: String, b: String): String {
        val trimA = a.trim()
        val trimB = b.trim()
        val aHasLower = trimA.any { it.isLowerCase() }
        val aEndsPunct = trimA.endsWith('.') || trimA.endsWith('!') || trimA.endsWith('?')
        val bSecondIsLower = trimB.length >= 2 && trimB[0].isUpperCase() && trimB[1].isLowerCase()
        val adjustedB = if (aHasLower && !aEndsPunct && bSecondIsLower && !trimB.startsWith("I ")) {
            trimB.replaceFirstChar { it.lowercase(Locale.ROOT) }
        } else {
            trimB
        }
        return "$trimA $adjustedB"
    }

    /**
     * Segments continuous or punctuated speech transcripts into crisp standalone clauses,
     * stripping conversational filler phrases, trailing phatic noise, CTA outros, and
     * tightening verbose boilerplate prior to semantic embedding.
     */
    fun segmentAndCleanTranscript(rawTranscript: String?): List<String> {
        val normalizedSpace = rawTranscript?.replace(Regex("\\s+"), " ")?.trim().orEmpty()
        if (normalizedSpace.isBlank()) return emptyList()
        val cleanedRaw = ASR_FALSE_MODAL_PERIOD_REGEX.replace(normalizedSpace) { match ->
            "${match.groupValues[1]} can. "
        }

        val enumMatches = ENUM_STEP_SPAN_REGEX.findAll(cleanedRaw).toList()
        if (enumMatches.size >= 2) {
            return segmentEnumeratedTranscript(cleanedRaw, enumMatches)
        }

        // First split on strong punctuation
        val primaryClauses = cleanedRaw.split(Regex("""(?<=[.!?;?])\s+|\n+"""))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val expandedClauses = mutableListOf<String>()
        for (clause in primaryClauses) {
            val destuttered = STUTTER_REGEX.replace(clause, "$1")
            val wordCount = destuttered.split(" ").count { it.isNotBlank() }
            if (wordCount > 10) {
                val splitByMarkers = splitByDiscourseMarkers(destuttered)
                for (sub in splitByMarkers) {
                    val subWords = sub.split(" ").count { it.isNotBlank() }
                    if (subWords > 14) {
                        expandedClauses.addAll(splitByConversationalBreaks(sub))
                    } else {
                        expandedClauses.add(sub)
                    }
                }
            } else {
                expandedClauses.add(destuttered)
            }
        }

        val result = mutableListOf<String>()
        val seenNorms = mutableSetOf<String>()
        for (rawClause in expandedClauses) {
            val polished = stripFillersAndFormat(rawClause) ?: continue
            val norm = normalizeForComparison(polished)
            if (norm.isNotBlank() && seenNorms.add(norm)) {
                result.add(polished)
            }
        }
        return result
    }

    private fun segmentEnumeratedTranscript(
        cleanedRaw: String,
        enumMatches: List<MatchResult>
    ): List<String> {
        val result = mutableListOf<String>()
        val seenNorms = mutableSetOf<String>()

        fun addClause(raw: String) {
            val destuttered = STUTTER_REGEX.replace(raw, "$1")
            val polished = stripFillersAndFormat(destuttered) ?: return
            val norm = normalizeForComparison(polished)
            if (norm.isNotBlank() && seenNorms.add(norm)) {
                result.add(polished)
            }
        }

        // Handle pre-enumerator prefix (intro hook + Step 1)
        val firstEnumStart = enumMatches.first().range.first
        val prefix = cleanedRaw.substring(0, firstEnumStart).trim()
        if (prefix.isNotBlank()) {
            val prefixParts = prefix.split(Regex("""(?<=[.!?;])\s+|\n+"""))
                .flatMap { splitByDiscourseMarkers(it) }
                .map { it.trim() }
                .filter { it.isNotBlank() }
            for (part in prefixParts) {
                addClause(part)
            }
        }

        // For each enumerated step span, keep only its primary instructional sentence
        for (i in enumMatches.indices) {
            val start = enumMatches[i].range.first
            val end = if (i + 1 < enumMatches.size) enumMatches[i + 1].range.first else cleanedRaw.length
            val span = cleanedRaw.substring(start, end).trim()
            if (span.isBlank()) continue
            val firstSentence = span.split(Regex("""(?<=[.!?;?])\s+|\n+"""))
                .firstOrNull { it.isNotBlank() }
                ?: span
            addClause(firstSentence)
        }
        return result
    }

    /**
     * Async entry point executed on [Dispatchers.Default] that lazily initializes the
     * quantized `all-MiniLM-L6-v2` LiteRT [SemanticEmbedder] off the main thread.
     */
    suspend fun summarizeToCrispNoteAsync(
        context: Context,
        subject: String?,
        accompanyingText: String?,
        ocrFrames: List<OcrFrameSample>,
        transcript: String?,
        audioClassification: AudioClassificationResult? = null
    ): SummarizedVideoNote = withContext(Dispatchers.Default) {
        val embedder = try {
            SemanticEmbedder.getInstance(context)
        } catch (_: Throwable) {
            null
        }
        val tokenizer = try {
            SemanticEmbedder.getOrLoadTokenizer(context)
        } catch (_: Throwable) {
            null
        }
        summarizeToCrispNote(
            subject = subject,
            accompanyingText = accompanyingText,
            ocrFrames = ocrFrames,
            transcript = transcript,
            audioClassification = audioClassification,
            similarityProvider = embedder,
            tokenizer = tokenizer
        )
    }

    /**
     * Synthesizes visibility-weighted OCR blocks and segmented speech transcript clauses
     * into a pure, timestamp-free, crisp point-wise markdown note.
     */
    fun summarizeToCrispNote(
        subject: String?,
        accompanyingText: String?,
        ocrFrames: List<OcrFrameSample>,
        transcript: String?,
        audioClassification: AudioClassificationResult? = null,
        similarityProvider: SemanticSimilarityProvider? = null,
        tokenizer: WordPieceTokenizer? = SemanticEmbedder.getOrLoadTokenizer(null)
    ): SummarizedVideoNote {
        val hasMusicTicker = ocrFrames.any { frame ->
            frame.text.lineSequence().any { rawLine ->
                val cleaned = cleanOcrLine(rawLine)
                AUDIO_TICKER_REGEX.matches(cleaned) || MUSIC_CREDIT_REGEX.containsMatchIn(cleaned)
            }
        }
        val ocrBlocks = trackAndConsolidateOcr(ocrFrames, tokenizer)
        val audioClauses = segmentAndCleanTranscript(transcript)

        val allTextsForEmbedding = (ocrBlocks.map { it.text } + audioClauses).distinct()
        val embeddingMap = buildEmbeddingMap(allTextsForEmbedding, similarityProvider)

        val ocrRichness = classifyOcrRichness(ocrBlocks)
        val modalityDecision = evaluateAudioRelevance(
            ocrBlocks = ocrBlocks,
            ocrRichness = ocrRichness,
            rawTranscript = transcript,
            audioClauses = audioClauses,
            hasMusicTicker = hasMusicTicker,
            audioClassification = audioClassification,
            embeddingMap = embeddingMap,
            similarityProvider = similarityProvider
        )

        val effectiveAudioClauses = when (modalityDecision) {
            AudioModalityDecision.OCR_ONLY_DROP_UNRELATED_AUDIO -> emptyList()
            AudioModalityDecision.FILTER_AUDIO_TO_MATCHING_CLAUSES -> {
                val ocrStems = extractStemSet(ocrBlocks.map { it.text })
                audioClauses.filter { clause ->
                    isAudioClauseRelevantToOcr(
                        clause = clause,
                        ocrBlocks = ocrBlocks,
                        ocrStems = ocrStems,
                        embeddingMap = embeddingMap,
                        similarityProvider = similarityProvider
                    )
                }
            }
            AudioModalityDecision.PREFER_AUDIO,
            AudioModalityDecision.MERGE_BOTH -> audioClauses
        }

        val rawCandidates = buildCandidatePool(
            ocrBlocks = ocrBlocks,
            audioClauses = effectiveAudioClauses,
            tokenizer = tokenizer,
            embeddingMap = embeddingMap,
            similarityProvider = similarityProvider
        )
        val rawEmbeddedVectors = rawCandidates.mapNotNull { embeddingMap[it.text] }
        val videoCentroid = if (rawEmbeddedVectors.isNotEmpty()) {
            SemanticEmbedder.computeCentroid(rawEmbeddedVectors)
        } else null
        val clusteredCandidates = clusterCandidates(
            candidates = rawCandidates,
            embeddingMap = embeddingMap,
            similarityProvider = similarityProvider,
            videoCentroid = videoCentroid
        )

        val cleanSubject = subject?.trim().orEmpty()
        val cleanAccompanying = accompanyingText?.trim().orEmpty()

        val rawDerivedTitle = when {
            cleanSubject.isNotBlank() -> cleanSubject
            cleanAccompanying.isNotBlank() -> ShareIntentHandler.cleanTitleFromText(cleanAccompanying)
            clusteredCandidates.isNotEmpty() -> deriveConciseTitle(
                points = clusteredCandidates,
                preferAudio = (modalityDecision == AudioModalityDecision.PREFER_AUDIO),
                embeddingMap = embeddingMap
            )
            else -> "Video Summary"
        }
        val derivedTitle = if (cleanSubject.isBlank() && cleanAccompanying.isBlank()) {
            formatTitleForDisplay(rawDerivedTitle)
        } else {
            rawDerivedTitle
        }

        val bulletCandidates = if (clusteredCandidates.size > 1) {
            clusteredCandidates.filter { candidate ->
                !isPointEquivalentToTitle(
                    pointText = candidate.text,
                    titleText = derivedTitle,
                    isIntroHook = candidate.isIntroHook,
                    embeddingMap = embeddingMap,
                    similarityProvider = similarityProvider
                )
            }
        } else {
            clusteredCandidates
        }

        val finalPoints = selectCrispPoints(
            candidates = bulletCandidates,
            preferAudio = (modalityDecision == AudioModalityDecision.PREFER_AUDIO),
            audioClauses = effectiveAudioClauses,
            embeddingMap = embeddingMap
        )

        val bulletLines = finalPoints.map { "- ${formatBulletPoint(it.text)}" }
        val summaryBody = if (bulletLines.isNotEmpty()) {
            bulletLines.joinToString("\n")
        } else {
            "- No speech or on-screen text detected in this video."
        }

        val fullContent = if (cleanAccompanying.isNotBlank()) {
            "$cleanAccompanying\n\n$summaryBody"
        } else {
            summaryBody
        }

        return SummarizedVideoNote(
            title = derivedTitle,
            content = fullContent,
            pointCount = finalPoints.size,
            cardType = "modular"
        )
    }

    internal fun computeOverlayLikelihood(block: TrackedOcrBlock): Float {
        if (!block.hasGeometry) return 1.0f
        val stability = (1f - (block.centerJitter / 0.15f)).coerceIn(0f, 1f)
        val levelness = (1f - (block.maxAbsAngle / 12f)).coerceIn(0f, 1f)
        val confScore = block.meanConfidence.coerceIn(0f, 1f)
        val sizeScore = (block.meanHeightRatio / 0.06f).coerceIn(0f, 1f)
        return 0.35f * stability + 0.25f * levelness + 0.25f * confScore + 0.15f * sizeScore
    }

    private fun buildEmbeddingMap(
        texts: List<String>,
        similarityProvider: SemanticSimilarityProvider?
    ): Map<String, FloatArray> {
        if (texts.isEmpty() || similarityProvider == null) return emptyMap()
        val vectors = try {
            similarityProvider.embedBatch(texts)
        } catch (_: Throwable) {
            null
        } ?: return emptyMap()

        val map = HashMap<String, FloatArray>(texts.size)
        for (i in 0 until minOf(texts.size, vectors.size)) {
            map[texts[i]] = vectors[i]
        }
        return map
    }

    private fun pairwiseSemanticSimilarity(
        textA: String,
        textB: String,
        embeddingMap: Map<String, FloatArray>,
        similarityProvider: SemanticSimilarityProvider?
    ): Float? {
        if (textA == textB) return 1f
        val vecA = embeddingMap[textA]
        val vecB = embeddingMap[textB]
        if (vecA != null && vecB != null) {
            return SemanticEmbedder.dotProduct(vecA, vecB)
        }
        return try {
            similarityProvider?.similarity(textA, textB)
        } catch (_: Throwable) {
            null
        }
    }

    private fun isAudioClauseRelevantToOcr(
        clause: String,
        ocrBlocks: List<TrackedOcrBlock>,
        ocrStems: Set<String>,
        embeddingMap: Map<String, FloatArray>,
        similarityProvider: SemanticSimilarityProvider?
    ): Boolean {
        if (countSharedStems(clause, ocrStems) > 0) return true
        if (ocrBlocks.any { areCrossModalDuplicates(it.text, clause) }) return true

        var maxSemantic = Float.NEGATIVE_INFINITY
        for (block in ocrBlocks) {
            val sim = pairwiseSemanticSimilarity(block.text, clause, embeddingMap, similarityProvider)
            if (sim != null && sim > maxSemantic) {
                maxSemantic = sim
            }
        }
        return maxSemantic >= CROSS_MODAL_SEMANTIC_THRESHOLD
    }

    private fun buildCandidatePool(
        ocrBlocks: List<TrackedOcrBlock>,
        audioClauses: List<String>,
        tokenizer: WordPieceTokenizer?,
        embeddingMap: Map<String, FloatArray>,
        similarityProvider: SemanticSimilarityProvider?
    ): List<CandidatePoint> {
        val candidates = mutableListOf<CandidatePoint>()

        val audioTokens = audioClauses.flatMap { tokenizeWords(it) }.filter { it !in STOP_WORDS }.toSet()
        val audioStems = extractStemSet(audioClauses)
        val tokenizedOcr = ocrBlocks.map { tokenizeWords(it.text) }

        val hasRicherContent = audioClauses.isNotEmpty() || tokenizedOcr.any { it.size >= 2 }
        for ((idx, block) in ocrBlocks.withIndex()) {
            val words = tokenizedOcr[idx]
            val contentWords = words.filter { it !in STOP_WORDS }
            val isSingleWhitespaceToken = block.text.trim().none { it.isWhitespace() }

            if (hasRicherContent && (words.size <= 1 || isSingleWhitespaceToken)) {
                continue
            }

            if (hasRicherContent && block.hasGeometry && block.meanConfidence < MIN_OCR_LINE_CONFIDENCE) {
                continue
            }

            if (hasRicherContent && block.hasGeometry && block.meanHeightRatio in 0.001f..0.019f) {
                continue
            }

            if (hasRicherContent && PACKAGING_SLOGAN_REGEX.containsMatchIn(block.text)) {
                continue
            }

            // Restrict cross-block subsumption to >=3-word OCR blocks so valid 2-word OCR subtitles are preserved
            val otherLongOcrTokens = tokenizedOcr
                .filterIndexed { otherIdx, otherWords -> otherIdx != idx && otherWords.size >= 3 }
                .flatten()
                .toSet()
            if (words.size <= 2 &&
                block.visibilityDurationMs < 1500L &&
                contentWords.isNotEmpty() &&
                (contentWords.all { it in audioTokens } || contentWords.all { it in otherLongOcrTokens })
            ) {
                continue
            }

            if (hasRicherContent &&
                block.visibilityDurationMs < 2500L &&
                (anomalyScore(block.text) >= 2 ||
                    (hasMidWordOcrGarble(block.text, tokenizer, audioTokens) && block.visibilityDurationMs < 1500L))
            ) {
                continue
            }

            if (hasRicherContent &&
                block.visibilityDurationMs < 1500L &&
                hasDanglingSingleLetterArtifact(block.text)
            ) {
                continue
            }

            // Plausibility gate against bundled 30k BERT vocab + audio stems for single-frame or short ML Kit fragments
            if (hasRicherContent && tokenizer != null && block.hasGeometry && (block.sampleCount == 1 || words.size <= 3)) {
                val plausibility = computePlausibleWordRatio(block.text, tokenizer, audioTokens, audioStems)
                if (plausibility < MIN_PLAUSIBLE_WORD_RATIO) {
                    continue
                }
            }

            // Drop all-caps background labels that share no meaningful words with spoken audio, or fleeting <=2 word caps
            if (hasRicherContent && isAllCapsBackgroundArtifact(block.text)) {
                val meaningfulCapsWords = contentWords.filter { it !in NUMBER_WORD_MAP }
                val sharesAudioContent = audioTokens.isNotEmpty() && meaningfulCapsWords.any { it in audioTokens }
                val shortFlash = words.size <= 2 && block.visibilityDurationMs < 2500L
                val unrelatedToSpeech = audioTokens.isNotEmpty() && !sharesAudioContent
                if (shortFlash || unrelatedToSpeech) {
                    continue
                }
            }

            // Overlay vs. scene-text gate when ML Kit geometry is available
            val overlayScore = computeOverlayLikelihood(block)
            val isOverlay = !block.hasGeometry || overlayScore >= OVERLAY_LIKELIHOOD_THRESHOLD
            if (hasRicherContent && !isOverlay) {
                val confirmedByAudio = countSharedStems(block.text, audioStems) > 0 ||
                    audioClauses.any { clause ->
                        val sim = pairwiseSemanticSimilarity(block.text, clause, embeddingMap, similarityProvider)
                        sim != null && sim >= 0.50f
                    }
                if (!confirmedByAudio) {
                    continue
                }
            }

            candidates.add(
                CandidatePoint(
                    text = block.text,
                    source = CandidateSource.OCR,
                    orderTimestampMs = block.firstSeenMs,
                    visibilityDurationMs = block.visibilityDurationMs,
                    sampleCount = block.sampleCount,
                    isOverlay = isOverlay,
                    isIntroHook = INTRO_HOOK_REGEX.matches(block.text.trim())
                )
            )
        }

        val estimatedSpeechStepMs = if (audioClauses.isNotEmpty()) {
            val maxOcrTs = ocrBlocks.maxOfOrNull { it.lastSeenMs }?.coerceAtLeast(10_000L) ?: 30_000L
            maxOcrTs / audioClauses.size.coerceAtLeast(1)
        } else 1000L

        for ((index, clause) in audioClauses.withIndex()) {
            candidates.add(
                CandidatePoint(
                    text = clause,
                    source = CandidateSource.AUDIO,
                    orderTimestampMs = index * estimatedSpeechStepMs,
                    visibilityDurationMs = 1000L,
                    sampleCount = 1,
                    isOverlay = true,
                    isIntroHook = INTRO_HOOK_REGEX.matches(clause.trim())
                )
            )
        }

        return candidates
    }

    /**
     * Connected-components / agglutinative semantic clustering over surviving OCR and audio
     * candidates (`cosine >= 0.72` or lexical equivalence), selecting one crisp representative
     * per cluster while preferring concrete metrics over vague paraphrases and actionable
     * spoken steps over terse background/chapter labels.
     */
    private fun clusterCandidates(
        candidates: List<CandidatePoint>,
        embeddingMap: Map<String, FloatArray>,
        similarityProvider: SemanticSimilarityProvider?,
        videoCentroid: FloatArray? = null
    ): List<CandidatePoint> {
        if (candidates.size <= 1) return candidates

        val n = candidates.size
        val visited = BooleanArray(n)
        val clustered = ArrayList<CandidatePoint>(n)

        for (seedIdx in 0 until n) {
            if (visited[seedIdx]) continue
            visited[seedIdx] = true

            val component = ArrayList<CandidatePoint>(4)
            val queue = ArrayDeque<Int>()
            queue.addLast(seedIdx)

            while (queue.isNotEmpty()) {
                val curr = queue.removeFirst()
                component.add(candidates[curr])

                for (other in 0 until n) {
                    if (visited[other]) continue
                    val simCurr = pairwiseSemanticSimilarity(
                        candidates[curr].text,
                        candidates[other].text,
                        embeddingMap,
                        similarityProvider
                    )
                    val simSeed = if (curr == seedIdx) {
                        simCurr
                    } else {
                        pairwiseSemanticSimilarity(
                            candidates[seedIdx].text,
                            candidates[other].text,
                            embeddingMap,
                            similarityProvider
                        )
                    }

                    val semanticMatch = (simCurr != null && simCurr >= SEMANTIC_CLUSTER_THRESHOLD) &&
                        (simSeed == null || simSeed >= CLUSTER_SEED_GUARD_THRESHOLD)
                    val lexicalMatch = areCrossModalDuplicates(candidates[curr].text, candidates[other].text) &&
                        (simCurr == null || simCurr >= 0.35f)

                    if (semanticMatch || lexicalMatch) {
                        visited[other] = true
                        queue.addLast(other)
                    }
                }
            }

            clustered.add(selectClusterRepresentative(component, embeddingMap, videoCentroid))
        }

        return clustered
    }

    private fun selectClusterRepresentative(
        cluster: List<CandidatePoint>,
        embeddingMap: Map<String, FloatArray> = emptyMap(),
        videoCentroid: FloatArray? = null
    ): CandidatePoint {
        if (cluster.size == 1) return cluster.first()

        val hasOcr = cluster.any { it.source == CandidateSource.OCR || it.source == CandidateSource.MERGED }
        val hasAudio = cluster.any { it.source == CandidateSource.AUDIO || it.source == CandidateSource.MERGED }
        val mergedSource = when {
            hasOcr && hasAudio -> CandidateSource.MERGED
            else -> cluster.first().source
        }

        val best = cluster.maxWithOrNull(
            compareBy<CandidatePoint> { scoreRepresentativeQuality(it, embeddingMap, videoCentroid) }
                .thenBy { it.text.length }
        ) ?: cluster.first()

        val ocrMembers = cluster.filter { it.source != CandidateSource.AUDIO }
        val resolvedOrderTs = if (best.source == CandidateSource.AUDIO) {
            best.orderTimestampMs
        } else if (ocrMembers.isNotEmpty()) {
            ocrMembers.minOf { it.orderTimestampMs }
        } else {
            cluster.minOf { it.orderTimestampMs }
        }
        val resolvedVisibilityMs = if (ocrMembers.isNotEmpty()) {
            ocrMembers.maxOf { it.visibilityDurationMs }
        } else {
            cluster.maxOf { it.visibilityDurationMs }
        }

        return best.copy(
            source = mergedSource,
            orderTimestampMs = resolvedOrderTs,
            visibilityDurationMs = resolvedVisibilityMs,
            sampleCount = cluster.sumOf { it.sampleCount },
            isOverlay = cluster.any { it.isOverlay },
            isIntroHook = cluster.any { it.isIntroHook }
        )
    }

    private fun scoreRepresentativeQuality(
        point: CandidatePoint,
        embeddingMap: Map<String, FloatArray> = emptyMap(),
        videoCentroid: FloatArray? = null
    ): Int {
        var score = 0
        val hasRealMetric = QUANTITATIVE_METRIC_REGEX.containsMatchIn(point.text)
        val anomalies = anomalyScore(point.text)
        val words = tokenizeWords(point.text)

        if (hasRealMetric) score += 60
        if (PACKAGING_SLOGAN_REGEX.containsMatchIn(point.text)) score -= 100

        val isTerseAllCapsLabel = isAllCapsBackgroundArtifact(point.text) && words.size <= 5
        if (point.isIntroHook && point.source != CandidateSource.AUDIO && point.isOverlay) {
            score += 85
        } else if (point.source != CandidateSource.AUDIO && anomalies == 0 && point.isOverlay && !isTerseAllCapsLabel) {
            score += 80
        }

        // Complete spoken clauses (5-24 words) beat terse ALL-CAPS topic banners or non-overlay scene text,
        // while clean mixed-case OCR bullets (+80) and metric-bearing OCR (+60) remain preferred.
        if (!point.isIntroHook && point.source == CandidateSource.AUDIO && words.size in 5..24) {
            score += 65
        }
        if (point.source != CandidateSource.AUDIO && !hasRealMetric && isTerseAllCapsLabel) {
            score -= 20
        }

        val vec = embeddingMap[point.text]
        if (vec != null && videoCentroid != null) {
            score += (SemanticEmbedder.dotProduct(vec, videoCentroid).coerceAtLeast(0f) * 8f).toInt()
        }

        if (point.text.length in 10..110) score += 25
        if (point.text.length > 135) score -= 15
        score += (point.visibilityDurationMs / 350L).toInt().coerceIn(0, 15)
        score -= anomalies * 30
        return score
    }

    private fun selectCrispPoints(
        candidates: List<CandidatePoint>,
        preferAudio: Boolean = false,
        audioClauses: List<String> = emptyList(),
        embeddingMap: Map<String, FloatArray> = emptyMap()
    ): List<CandidatePoint> {
        if (candidates.isEmpty()) return emptyList()

        // Compute video centroid across available embeddings (TextRank centrality)
        val embeddedVectors = ArrayList<FloatArray>(candidates.size)
        val embeddedWeights = ArrayList<Float>(candidates.size)
        for (c in candidates) {
            val vec = embeddingMap[c.text] ?: continue
            embeddedVectors.add(vec)
            embeddedWeights.add(1.0f + minOf(c.visibilityDurationMs / 3000f, 1.0f))
        }
        val centroid = if (embeddedVectors.isNotEmpty()) {
            SemanticEmbedder.computeCentroid(embeddedVectors, embeddedWeights.toFloatArray())
        } else null

        val audioStems = extractStemSet(audioClauses)
        val hasRicherMultiSampleOrAudio = candidates.any {
            it.source != CandidateSource.OCR || it.sampleCount >= 2 || it.visibilityDurationMs > 300L
        }

        // Quality floor: drop low-centrality single-frame OCR-only outliers when richer points exist
        val qualityFiltered = if (candidates.size > 1 && hasRicherMultiSampleOrAudio) {
            candidates.filter { point ->
                val isSingleFrameOcrOnly = point.source == CandidateSource.OCR &&
                    point.sampleCount == 1 &&
                    point.visibilityDurationMs <= 300L
                if (!isSingleFrameOcrOnly) {
                    true
                } else {
                    val vec = embeddingMap[point.text]
                    val centroidSim = if (vec != null && centroid != null) {
                        SemanticEmbedder.dotProduct(vec, centroid)
                    } else null
                    val sharesAudio = countSharedStems(point.text, audioStems) > 0
                    val hasMetric = QUANTITATIVE_METRIC_REGEX.containsMatchIn(point.text)
                    val isLowCentralityOutlier = !sharesAudio && !hasMetric && (centroidSim != null && centroidSim < 0.25f)
                    !isLowCentralityOutlier
                }
            }.ifEmpty { candidates }
        } else {
            candidates
        }

        if (qualityFiltered.size <= MAX_SUMMARY_POINTS) {
            return qualityFiltered.sortedBy { it.orderTimestampMs }
        }

        // Compute document frequency of content tokens for TF-IDF scoring
        val docFreq = mutableMapOf<String, Int>()
        val tokenLists = qualityFiltered.map { point ->
            val tokens = tokenizeWords(point.text).filter { it !in STOP_WORDS && it.length > 2 }
            tokens.distinct().forEach { token ->
                docFreq[token] = (docFreq[token] ?: 0) + 1
            }
            tokens
        }

        val totalDocs = qualityFiltered.size.toDouble()
        val scored = qualityFiltered.mapIndexed { idx, point ->
            val tokens = tokenLists[idx]
            val tfidf = if (tokens.isEmpty()) {
                0.0
            } else {
                tokens.sumOf { token ->
                    val df = docFreq[token] ?: 1
                    ln(1.0 + totalDocs / df)
                } / tokens.size.toDouble()
            }

            // Semantic TextRank centrality (cosine similarity to L2 video centroid)
            val vec = embeddingMap[point.text]
            val centroidCentrality = if (vec != null && centroid != null) {
                SemanticEmbedder.dotProduct(vec, centroid).coerceAtLeast(0f).toDouble() * 1.4
            } else {
                0.0
            }

            // Information density bonuses
            val hasNumbersOrMetrics = point.text.any { it.isDigit() }
            val densityBonus = (if (hasNumbersOrMetrics) 0.45 else 0.0) +
                (tokens.size.coerceIn(2, 14) / 20.0)

            // Visibility duration & cross-modal confirmation bonus
            val visibilityBonus = when {
                point.source == CandidateSource.MERGED -> 1.1
                preferAudio && point.source == CandidateSource.AUDIO -> 0.95
                point.visibilityDurationMs >= 3000L -> 0.85
                point.visibilityDurationMs >= 1500L -> 0.55
                point.sampleCount >= 2 -> 0.35
                else -> 0.0
            }

            val totalScore = centroidCentrality + tfidf + densityBonus + visibilityBonus
            idx to totalScore
        }

        val topIndices = scored
            .sortedByDescending { it.second }
            .take(MAX_SUMMARY_POINTS)
            .map { it.first }
            .toSet()

        return qualityFiltered
            .filterIndexed { idx, _ -> idx in topIndices }
            .sortedBy { it.orderTimestampMs }
    }

    private fun deriveConciseTitle(
        points: List<CandidatePoint>,
        preferAudio: Boolean = false,
        embeddingMap: Map<String, FloatArray> = emptyMap()
    ): String {
        val embeddedVectors = points.mapNotNull { embeddingMap[it.text] }
        val centroid = if (embeddedVectors.isNotEmpty()) {
            SemanticEmbedder.computeCentroid(embeddedVectors)
        } else null

        val preferred = points
            .sortedByDescending { point ->
                val sourcePriority = if (preferAudio) {
                    if (point.source == CandidateSource.AUDIO || point.source == CandidateSource.MERGED) 1 else 0
                } else {
                    if ((point.source == CandidateSource.OCR || point.source == CandidateSource.MERGED) && point.isOverlay) 1 else 0
                }
                val vec = embeddingMap[point.text]
                val centroidScore = if (vec != null && centroid != null) {
                    SemanticEmbedder.dotProduct(vec, centroid).coerceAtLeast(0f) * 8.0f
                } else {
                    0f
                }
                val visibilityScore = (point.visibilityDurationMs / 1000f).coerceAtMost(5.0f)
                val introBonus = if (point.isIntroHook) 3.0f else 0f
                val sloganPenalty = if (PACKAGING_SLOGAN_REGEX.containsMatchIn(point.text)) 20.0f else 0f
                val garblePenalty = (anomalyScore(point.text) * 4.0f) +
                    (if (hasMidWordOcrGarble(point.text, null, emptySet())) 4.0f else 0f)
                sourcePriority * 10.0f + visibilityScore + centroidScore + introBonus - garblePenalty - sloganPenalty
            }
            .firstOrNull { it.text.length in 6..80 }
            ?: points.first()

        val cleaned = preferred.text
            .replace(Regex("[.!?]+$"), "")
            .trim()
        return if (cleaned.length > 60) {
            cleaned.take(57).trimEnd() + "..."
        } else {
            cleaned.ifBlank { "Video Summary" }
        }
    }

    private fun formatTitleForDisplay(rawTitle: String): String {
        val trimmed = rawTitle.trim()
        if (trimmed.isEmpty()) return "Video Summary"
        val isAllCaps = !trimmed.any { it.isLowerCase() } && trimmed.count { it.isUpperCase() } >= 5
        if (!isAllCaps) return trimmed

        val words = trimmed.split(Regex("\\s+"))
        return words.mapIndexed { idx, word ->
            val upper = word.uppercase(Locale.ROOT)
            val cleanAlpha = upper.trimEnd(':', ',', '.', '!', '?')
            when {
                cleanAlpha in PRESERVED_ACRONYMS -> word
                idx > 0 && idx < words.lastIndex && cleanAlpha.lowercase(Locale.ROOT) in TITLE_SMALL_WORDS -> {
                    word.lowercase(Locale.ROOT)
                }
                else -> word.lowercase(Locale.ROOT).replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.ROOT) else ch.toString()
                }
            }
        }.joinToString(" ")
    }

    private fun isPointEquivalentToTitle(
        pointText: String,
        titleText: String,
        isIntroHook: Boolean,
        embeddingMap: Map<String, FloatArray>,
        similarityProvider: SemanticSimilarityProvider?
    ): Boolean {
        if (normalizeForComparison(pointText) == normalizeForComparison(titleText)) return true
        val pointStems = extractStemSet(listOf(pointText))
        val titleStems = extractStemSet(listOf(titleText))
        val sharedStemRatio = if (pointStems.isNotEmpty() && titleStems.isNotEmpty()) {
            val shared = pointStems.count { it in titleStems }
            shared.toDouble() / minOf(pointStems.size, titleStems.size).toDouble()
        } else 0.0

        if (isIntroHook && sharedStemRatio >= 0.50) return true
        if (sharedStemRatio >= 0.75 && pointStems.size <= titleStems.size + 3) return true

        val sim = pairwiseSemanticSimilarity(pointText, titleText, embeddingMap, similarityProvider)
        if (sim != null && sim >= 0.80f) return true
        return sim != null && sim >= TITLE_SEMANTIC_DEDUP_THRESHOLD && sharedStemRatio >= 0.35
    }

    private fun findMatchingBlockIndex(existingTexts: List<String>, candidateLine: String): Int {
        val normNew = normalizeForComparison(candidateLine)
        if (normNew.isBlank()) return -1

        for (i in existingTexts.indices) {
            val existingText = existingTexts[i]
            val normOld = normalizeForComparison(existingText)
            if (normOld == normNew) return i

            // Progressive typing / line reveal check
            if (normOld.length >= 4 && normNew.startsWith(normOld)) return i
            if (normNew.length >= 4 && normOld.startsWith(normNew)) return i
            if (normOld.length >= 8 && normNew.contains(normOld)) return i
            if (normNew.length >= 8 && normOld.contains(normNew)) return i

            // Fuzzy token & bigram similarity check for minor OCR jitter
            val jaccard = tokenJaccard(existingText, candidateLine)
            if (jaccard >= 0.70) return i
            val dice = bigramDice(normOld, normNew)
            if (dice >= 0.68) return i
        }
        return -1
    }

    private fun choosePreferredOcrText(
        existing: String,
        incoming: String,
        tokenizer: WordPieceTokenizer? = null
    ): String {
        val existingAnomalies = anomalyScore(existing)
        val incomingAnomalies = anomalyScore(incoming)
        if (incomingAnomalies != existingAnomalies) {
            return if (incomingAnomalies < existingAnomalies) incoming else existing
        }
        if (tokenizer != null) {
            val existingPlaus = computePlausibleWordRatio(existing, tokenizer, emptySet(), emptySet())
            val incomingPlaus = computePlausibleWordRatio(incoming, tokenizer, emptySet(), emptySet())
            if (kotlin.math.abs(incomingPlaus - existingPlaus) >= 0.15f) {
                return if (incomingPlaus > existingPlaus) incoming else existing
            }
        }
        val normOld = normalizeForComparison(existing)
        val normNew = normalizeForComparison(incoming)
        if (normNew.length > normOld.length + 2 && (normNew.startsWith(normOld) || normNew.contains(normOld))) {
            return incoming
        }
        if (normOld.length > normNew.length + 2 && (normOld.startsWith(normNew) || normOld.contains(normNew))) {
            return existing
        }
        return if (incoming.length > existing.length) incoming else existing
    }

    private fun anomalyScore(text: String): Int {
        var score = 0
        for (token in text.split(Regex("\\s+"))) {
            if (MIXED_CASE_ANOMALY_REGEX.containsMatchIn(token)) score++
            if (token.any { it.code > 127 && it.isLetter() }) score++
            if (token.contains('_')) score++
        }
        return score
    }

    private fun isAllCapsBackgroundArtifact(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length !in 4..75) return false
        val hasLower = trimmed.any { it.isLowerCase() }
        val upperCount = trimmed.count { it.isUpperCase() }
        return !hasLower && upperCount >= 4
    }

    private fun hasMidWordOcrGarble(
        text: String,
        tokenizer: WordPieceTokenizer?,
        audioTokens: Set<String>
    ): Boolean {
        return text.split(Regex("\\s+")).any { rawToken ->
            val clean = rawToken.trim('.', ',', ':', ';', '!', '?')
            if (clean.isEmpty()) return@any false
            val matchesPattern = GARBLED_OCR_CHAR_REGEX.containsMatchIn(clean)
            if (!matchesPattern) return@any false
            val lower = clean.lowercase(Locale.ROOT)
            val isKnownTech = ACRONYM_TOKEN_REGEX.matches(clean) ||
                clean.uppercase(Locale.ROOT) in PRESERVED_ACRONYMS ||
                lower in audioTokens ||
                (tokenizer?.isWholeWord(lower) == true)
            !isKnownTech
        }
    }

    private fun computePlausibleWordRatio(
        text: String,
        tokenizer: WordPieceTokenizer,
        audioTokens: Set<String>,
        audioStems: Set<String>
    ): Float {
        val tokens = tokenizeWords(text)
        if (tokens.isEmpty()) return 0f
        var plausibleCount = 0
        for (word in tokens) {
            if (isPlausibleOcrWord(word, tokenizer, audioTokens, audioStems)) {
                plausibleCount++
            }
        }
        return plausibleCount.toFloat() / tokens.size.toFloat()
    }

    private fun isPlausibleOcrWord(
        word: String,
        tokenizer: WordPieceTokenizer,
        audioTokens: Set<String>,
        audioStems: Set<String>
    ): Boolean {
        if (word.any { it.isDigit() }) return true
        if (word.uppercase(Locale.ROOT) in PRESERVED_ACRONYMS) return true
        if (word in audioTokens || stemToken(word) in audioStems) return true
        if (word.length <= 2) return word in VALID_TWO_LETTER_WORDS
        if (tokenizer.isWholeWord(word)) return true

        val stem = stemToken(word)
        if (tokenizer.isWholeWord(stem)) return true
        if (word.length > 3) {
            val suffixes = arrayOf("s", "es", "ed", "ing", "er", "ers", "ly", "ies")
            for (sfx in suffixes) {
                if (word.endsWith(sfx) && word.length - sfx.length >= 3) {
                    val base = word.removeSuffix(sfx)
                    if (tokenizer.isWholeWord(base) || (sfx == "ies" && tokenizer.isWholeWord("${base}y"))) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun hasDanglingSingleLetterArtifact(text: String): Boolean {
        val rawTokens = text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        if (rawTokens.size < 2) return false
        val last = rawTokens.last().trim('.', ',', ':', ';', '!', '?')
        return last.length == 1 && last[0].isLetter() && last.lowercase(Locale.ROOT) !in setOf("a", "i")
    }

    private fun areCrossModalDuplicates(a: String, b: String): Boolean {
        val normA = normalizeForComparison(a)
        val normB = normalizeForComparison(b)
        if (normA.isBlank() || normB.isBlank()) return false
        if (normA == normB) return true
        if (normA.length >= 10 && normB.contains(normA)) return true
        if (normB.length >= 10 && normA.contains(normB)) return true
        if (tokenJaccard(a, b) >= 0.55 || bigramDice(normA, normB) >= 0.65) return true

        val contentA = tokenizeWords(a).filter { it !in STOP_WORDS }.toSet()
        val contentB = tokenizeWords(b).filter { it !in STOP_WORDS }.toSet()
        val minSize = minOf(contentA.size, contentB.size)
        if (minSize >= 3) {
            val shared = contentA.count { it in contentB }
            if (shared.toDouble() / minSize.toDouble() >= 0.66) return true
        }
        return false
    }

    private fun splitByDiscourseMarkers(clause: String): List<String> {
        val matches = DISCOURSE_MARKER_REGEX.findAll(clause).toList()
        if (matches.isEmpty()) return listOf(clause)

        val splits = mutableListOf<String>()
        var lastStart = 0
        for (match in matches) {
            val idx = match.range.first
            if (idx > lastStart + 12) {
                splits.add(clause.substring(lastStart, idx).trim().trimEnd(',', ';'))
                lastStart = idx
            }
        }
        if (lastStart < clause.length) {
            splits.add(clause.substring(lastStart).trim())
        }
        return splits.filter { it.isNotBlank() }
    }

    private fun stripFillersAndFormat(rawClause: String): String? {
        var text = rawClause.trim()
            .trimStart('-', '*', '•', ',', ';', ':')
            .trim()

        if (CTA_NOISE_REGEX.matches(text)) return null
        if (REACTION_NOISE_REGEX.matches(text)) return null

        // Strip iterative leading conversational fillers
        var changed = true
        var iterations = 0
        while (changed && iterations < 4) {
            val next = FILLER_PREFIX_REGEX.replace(text, "").trim()
            changed = (next != text)
            text = next
            iterations++
        }

        // Tighten verbose spoken boilerplate and numbered step prefixes into crisp directives
        text = NUMBERED_STEP_PREFIX_REGEX.replace(text, "")
        text = BOILERPLATE_MAKE_SURE_REGEX.replace(text, "Make sure to ")
        text = BOILERPLATE_WHAT_TO_DO_REGEX.replace(text, "")
        text = BOILERPLATE_FIRST_THING_REGEX.replace(text, "First, ")
        text = BOILERPLATE_SHOW_HOW_REGEX.replace(text, "How to ")
        text = BOILERPLATE_REASON_REGEX.replace(text, "Because ")
        text = BOILERPLATE_STARTING_WITH_REGEX.replace(text, "Use ")
        text = BOILERPLATE_PERFECT_THING_REGEX.replace(text, " to ")
        text = text.replace(Regex("\\s+"), " ").trim()
        text = IN_ORDER_TO_REGEX.replace(text, "to")

        // Prune trailing phatic conversational noise and trailing punctuation
        text = TRAILING_PHATIC_REGEX.replace(text, "").trim()
        text = text.trimEnd('.', ',', ';', ':', '!', '?').trim()

        // Conservative step-length cap: trim trailing elaboration beyond ~22 words at a clause boundary
        text = compressLongClauseIfNeeded(text)

        if (CTA_NOISE_REGEX.matches(text)) return null
        if (REACTION_NOISE_REGEX.matches(text)) return null
        if (text.length < 5) return null
        val words = tokenizeWords(text)
        if (words.size < 2) return null

        return text.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
        }
    }

    private fun compressLongClauseIfNeeded(text: String): String {
        val trimmedDangling = trimDanglingSpatialPrepositionFragment(text)
        val workingWords = trimmedDangling.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (workingWords.size <= MAX_SPOKEN_STEP_WORDS) {
            // Even under 22 words, trim secondary comma elaboration when clause is >= 14 words
            if (workingWords.size >= 14) {
                val earlyComma = trimmedDangling.indices.firstOrNull { idx ->
                    if (trimmedDangling[idx] != ',' && trimmedDangling[idx] != ';') return@firstOrNull false
                    val prefixWords = trimmedDangling.substring(0, idx).trim().split(Regex("\\s+")).count { it.isNotBlank() }
                    prefixWords in 6..12 && !trimmedDangling.substring(idx + 1).trim().startsWith("and ", ignoreCase = true)
                }
                if (earlyComma != null) {
                    val prefix = trimmedDangling.substring(0, earlyComma).trim()
                    if (!prefix.contains(",")) return prefix
                }
            }
            return trimmedDangling
        }

        // First check universal grammatical run-on boundaries (repeated-stem participial or explanatory subjects)
        val syntacticCutoff = findSyntacticRunOnCutoff(trimmedDangling)
        if (syntacticCutoff != null) {
            return syntacticCutoff
        }

        // Next prefer cutting cleanly at a general subordinating elaboration marker
        val match = LONG_CLAUSE_ELABORATION_REGEX.findAll(trimmedDangling)
            .firstOrNull { m ->
                val prefixWords = trimmedDangling.substring(0, m.range.first).trim().split(Regex("\\s+")).count { it.isNotBlank() }
                prefixWords in 7..MAX_SPOKEN_STEP_WORDS
            }
        if (match != null) {
            val candidate = trimmedDangling.substring(0, match.range.first).trim().trimEnd(',', ';', ':')
            if (candidate.split(Regex("\\s+")).size >= 6) {
                return candidate
            }
        }

        // Fallback: cut at a comma boundary between word 6 and MAX_SPOKEN_STEP_WORDS
        val commaIndex = trimmedDangling.indices.firstOrNull { idx ->
            if (trimmedDangling[idx] != ',' && trimmedDangling[idx] != ';') return@firstOrNull false
            val prefixWords = trimmedDangling.substring(0, idx).trim().split(Regex("\\s+")).count { it.isNotBlank() }
            prefixWords in 6..MAX_SPOKEN_STEP_WORDS
        }
        if (commaIndex != null) {
            return trimmedDangling.substring(0, commaIndex).trim()
        }

        return trimmedDangling
    }

    /**
     * Trims ASR-truncated trailing noun-phrase fragments ending in a stranded spatial preposition
     * (e.g. "<Verb> your <noun1> <noun2> <bare NP> inside" where ASR broke before ". Your <noun2> can...").
     */
    private fun trimDanglingSpatialPrepositionFragment(text: String): String {
        val rawWords = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (rawWords.size !in 7..12) return text
        val cleanWords = rawWords.map { it.lowercase(Locale.ROOT).trim('.', ',', ';', ':', '!', '?') }
        if (cleanWords[0] in STOP_WORDS || cleanWords[1] !in POSSESSIVE_OR_DET) return text
        if (cleanWords.last() !in DANGLING_SPATIAL_PREPS) return text
        val fourth = cleanWords[4]
        val isModifierOrConnector = fourth in STOP_WORDS ||
            fourth in DANGLING_SPATIAL_PREPS ||
            fourth.endsWith("ly")
        if (isModifierOrConnector) return text
        return rawWords.take(4).joinToString(" ")
    }

    /**
     * Detects unpunctuated second sentences in long (> 22 word) spoken steps using morphological
     * repeated-stem and participial/explanatory-verb boundaries without domain-specific phrases.
     */
    private fun findSyntacticRunOnCutoff(text: String): String? {
        val rawWords = text.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (rawWords.size <= MAX_SPOKEN_STEP_WORDS) return null
        val normWords = rawWords.map { it.lowercase(Locale.ROOT).trim('.', ',', ';', ':', '!', '?') }

        val priorStems = HashSet<String>()
        for (i in normWords.indices) {
            val w = normWords[i]
            val stem = stemToken(w)
            if (i >= 4 && w !in STOP_WORDS && w.length >= 4) {
                val repeatedStem = stem in priorStems || w in priorStems

                // Pattern 1: Participial elaboration boundary:
                // (a) repeated-stem participle ("<verb> a <stem> <noun> <stem>ing ...")
                // (b) post-clause bare participle after word 6 ("... <noun>, <verb>ing a/the/your ...")
                if (w.endsWith("ing") && w.length >= 6) {
                    val prev = normWords[i - 1]
                    val next = normWords.getOrNull(i + 1)
                    val isBareParticipialSplit = i >= 6 &&
                        prev !in PARTICIPLE_PREDECESSOR_BLOCKLIST &&
                        prev !in POSSESSIVE_OR_DET &&
                        next in PARTICIPLE_FOLLOWER_ALLOWLIST
                    if (repeatedStem || isBareParticipialSplit) {
                        return rawWords.take(i).joinToString(" ").trimEnd(',', ';', ':')
                    }
                }

                // Pattern 2: Repeated-stem noun subject followed closely by explanatory verb
                // ("...<noun> <subject> <explVerb>..." or "...<det> <stem> <NP> <prep> <det> <stem> <explVerb>...")
                if (repeatedStem) {
                    val hasFollowingVerb = (i + 1..minOf(i + 2, normWords.lastIndex)).any { normWords[it] in EXPLANATORY_VERB_SET }
                    if (hasFollowingVerb) {
                        var cutIdx = i
                        if (cutIdx >= 5 && normWords[cutIdx - 1] in POSSESSIVE_OR_DET) {
                            cutIdx -= 1
                        }
                        if (cutIdx >= 5 && normWords[cutIdx - 1] in DANGLING_SPATIAL_PREPS) {
                            var scanBack = cutIdx - 2
                            while (scanBack >= 4 && normWords[scanBack] !in STOP_WORDS && !normWords[scanBack].endsWith("ly")) {
                                scanBack--
                            }
                            if (scanBack == 3 && normWords[1] in POSSESSIVE_OR_DET) {
                                cutIdx = 4
                            }
                        }
                        // Never leave a stranded article/preposition from an intact PP (e.g. "...inside a decorated | pillow")
                        val danglingCutEnd = normWords.getOrNull(cutIdx - 1)
                        val beforeCut = normWords.getOrNull(cutIdx - 2)
                        val isMidPhraseCut = (danglingCutEnd in setOf("a", "an", "the")) ||
                            (beforeCut in setOf("a", "an", "the") && cutIdx >= 6 && normWords[cutIdx - 3] in DANGLING_SPATIAL_PREPS)
                        if (!isMidPhraseCut && cutIdx in 5..MAX_SPOKEN_STEP_WORDS) {
                            return rawWords.take(cutIdx).joinToString(" ").trimEnd(',', ';', ':')
                        }
                    }
                }
            }
            if (w !in STOP_WORDS && w.length >= 4) {
                priorStems.add(stem)
                priorStems.add(w)
            }
        }
        return null
    }

    private fun formatBulletPoint(raw: String): String {
        val cleaned = raw
            .replace(Regex("""^[-*•]\s+"""), "")
            .replace(Regex("\\s+"), " ")
            .trim()
        val cased = if (!cleaned.any { it.isLowerCase() } && cleaned.count { it.isUpperCase() } >= 5) {
            cleaned.split(Regex("\\s+")).mapIndexed { idx, w ->
                val alpha = w.uppercase(Locale.ROOT).trimEnd(':', ',', '.', '!', '?')
                when {
                    alpha in PRESERVED_ACRONYMS -> w
                    idx == 0 -> w.lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) }
                    else -> w.lowercase(Locale.ROOT)
                }
            }.joinToString(" ")
        } else {
            cleaned
        }
        return cased.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase(Locale.getDefault()) else ch.toString()
        }
    }

    private fun cleanOcrLine(rawLine: String): String {
        return rawLine
            .replace(Regex("""^[-*•·]\s*"""), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun splitByConversationalBreaks(clause: String): List<String> {
        val matches = CONVERSATIONAL_BREAK_REGEX.findAll(clause).toList()
        if (matches.isEmpty()) return listOf(clause)

        val splits = mutableListOf<String>()
        var lastStart = 0
        for (match in matches) {
            val idx = match.range.first
            if (idx > lastStart + 18) {
                splits.add(clause.substring(lastStart, idx).trim().trimEnd(',', ';'))
                lastStart = idx
            }
        }
        if (lastStart < clause.length) {
            splits.add(clause.substring(lastStart).trim())
        }
        return splits.filter { it.isNotBlank() }
    }

    private fun isMeaningfulOcrLine(line: String): Boolean {
        if (line.length < 2) return false
        if (NON_ALNUM_ONLY_REGEX.matches(line)) return false
        if (TIMECODE_NOISE_REGEX.matches(line)) return false
        if (PERCENT_OR_NUMBER_ONLY_REGEX.matches(line)) return false
        if (HANDLE_OR_WATERMARK_REGEX.matches(line)) return false
        if (AUDIO_TICKER_REGEX.matches(line)) return false
        if (MUSIC_CREDIT_REGEX.containsMatchIn(line)) return false
        if (PACKAGING_SLOGAN_REGEX.containsMatchIn(line)) return false
        if (line.contains('_') && !line.contains(' ')) return false
        return line.any { it.isLetterOrDigit() }
    }

    internal fun classifyOcrRichness(ocrBlocks: List<TrackedOcrBlock>): OcrRichness {
        if (ocrBlocks.isEmpty()) return OcrRichness.MINIMAL
        val meaningfulBlocks = ocrBlocks.filter { block ->
            val words = tokenizeWords(block.text)
            words.size >= 2 || block.visibilityDurationMs >= 1500L
        }
        val contentStems = extractStemSet(meaningfulBlocks.map { it.text })
        val multiWordCount = meaningfulBlocks.count { tokenizeWords(it.text).size >= 2 }

        return when {
            contentStems.size < 4 || (meaningfulBlocks.size <= 1 && contentStems.size < 6) -> OcrRichness.MINIMAL
            multiWordCount >= 3 || contentStems.size >= 12 -> OcrRichness.RICH
            else -> OcrRichness.MODERATE
        }
    }

    internal fun evaluateAudioRelevance(
        ocrBlocks: List<TrackedOcrBlock>,
        ocrRichness: OcrRichness,
        rawTranscript: String?,
        audioClauses: List<String>,
        hasMusicTicker: Boolean,
        audioClassification: AudioClassificationResult?,
        embeddingMap: Map<String, FloatArray> = emptyMap(),
        similarityProvider: SemanticSimilarityProvider? = null
    ): AudioModalityDecision {
        if (audioClauses.isEmpty()) {
            return AudioModalityDecision.OCR_ONLY_DROP_UNRELATED_AUDIO
        }
        if (audioClassification != null && !audioClassification.shouldTranscribeSpeech) {
            return AudioModalityDecision.OCR_ONLY_DROP_UNRELATED_AUDIO
        }

        val lyricScore = computeLyricNoiseScore(
            rawTranscript = rawTranscript,
            audioClauses = audioClauses,
            hasMusicTicker = hasMusicTicker,
            audioClassification = audioClassification
        )

        if (ocrRichness == OcrRichness.MINIMAL) {
            val isPureLyricNoiseOverCard = ocrBlocks.isNotEmpty() &&
                audioClassification?.profile == AcousticProfile.SPEECH_WITH_MUSIC &&
                lyricScore >= 4
            return if (isPureLyricNoiseOverCard) {
                AudioModalityDecision.OCR_ONLY_DROP_UNRELATED_AUDIO
            } else {
                AudioModalityDecision.PREFER_AUDIO
            }
        }

        val ocrStems = extractStemSet(ocrBlocks.map { it.text })
        val matchedClauses = audioClauses.count { clause ->
            isAudioClauseRelevantToOcr(
                clause = clause,
                ocrBlocks = ocrBlocks,
                ocrStems = ocrStems,
                embeddingMap = embeddingMap,
                similarityProvider = similarityProvider
            )
        }

        val ocrVecs = ocrBlocks.mapNotNull { embeddingMap[it.text] }
        val audioVecs = audioClauses.mapNotNull { embeddingMap[it] }
        val centroidSimilarity = if (ocrVecs.isNotEmpty() && audioVecs.isNotEmpty()) {
            SemanticEmbedder.dotProduct(
                SemanticEmbedder.computeCentroid(ocrVecs),
                SemanticEmbedder.computeCentroid(audioVecs)
            )
        } else null

        if (matchedClauses > 0) {
            val partialRatio = matchedClauses.toFloat() / audioClauses.size.toFloat()
            val shouldFilterPartialAudio = ocrRichness == OcrRichness.RICH &&
                matchedClauses < audioClauses.size &&
                audioClauses.size >= 3 &&
                (
                    lyricScore >= 2 ||
                        (matchedClauses == 1 && audioClauses.size >= 4 && lyricScore >= 1) ||
                        (centroidSimilarity != null && centroidSimilarity < 0.30f && partialRatio <= 0.5f)
                    )
            return if (shouldFilterPartialAudio) {
                AudioModalityDecision.FILTER_AUDIO_TO_MATCHING_CLAUSES
            } else {
                AudioModalityDecision.MERGE_BOTH
            }
        }

        // Zero contextual overlap between OCR and audio
        return when (ocrRichness) {
            OcrRichness.RICH -> AudioModalityDecision.OCR_ONLY_DROP_UNRELATED_AUDIO
            OcrRichness.MODERATE -> {
                if (lyricScore >= 3 ||
                    (centroidSimilarity != null && centroidSimilarity < 0.25f &&
                        audioClassification?.profile == AcousticProfile.SPEECH_WITH_MUSIC)
                ) {
                    AudioModalityDecision.OCR_ONLY_DROP_UNRELATED_AUDIO
                } else {
                    AudioModalityDecision.MERGE_BOTH
                }
            }
            OcrRichness.MINIMAL -> AudioModalityDecision.PREFER_AUDIO
        }
    }

    internal fun computeLyricNoiseScore(
        rawTranscript: String?,
        audioClauses: List<String>,
        hasMusicTicker: Boolean,
        audioClassification: AudioClassificationResult?
    ): Int {
        var score = 0
        if (hasMusicTicker) score += 1
        if (audioClassification?.profile == AcousticProfile.SPEECH_WITH_MUSIC) score += 1

        val joined = rawTranscript?.takeIf { it.isNotBlank() } ?: audioClauses.joinToString(" ")
        val tokens = tokenizeWords(joined).filter { it !in STOP_WORDS && it.length > 2 }
        if (tokens.size >= 10) {
            val diversity = tokens.distinct().size.toDouble() / tokens.size.toDouble()
            if (diversity < 0.50) score += 2
        }

        if (LYRIC_VOCALIZATION_REGEX.containsMatchIn(joined)) {
            score += 2
        }

        if (audioClauses.size >= 3) {
            val avgWords = audioClauses.sumOf { tokenizeWords(it).size }.toDouble() / audioClauses.size.toDouble()
            if (avgWords <= 3.5) score += 1
        }
        return score
    }

    private fun extractStemSet(texts: List<String>): Set<String> {
        val stems = HashSet<String>()
        for (t in texts) {
            for (word in tokenizeWords(t)) {
                val mappedDigit = NUMBER_WORD_MAP[word]
                if (mappedDigit != null) {
                    stems.add(mappedDigit)
                } else if (word.any { it.isDigit() }) {
                    stems.add(word)
                } else if (word !in STOP_WORDS && word.length > 2) {
                    stems.add(stemToken(word))
                }
            }
        }
        return stems
    }

    private fun countSharedStems(text: String, targetStems: Set<String>): Int {
        if (targetStems.isEmpty()) return 0
        var count = 0
        val seen = HashSet<String>()
        for (word in tokenizeWords(text)) {
            val key = when {
                NUMBER_WORD_MAP.containsKey(word) -> NUMBER_WORD_MAP[word]!!
                word.any { it.isDigit() } -> word
                word !in STOP_WORDS && word.length > 2 -> stemToken(word)
                else -> continue
            }
            if (key in targetStems && seen.add(key)) {
                count++
            }
        }
        return count
    }

    internal fun stemToken(word: String): String {
        val w = word.lowercase(Locale.getDefault())
        if (w.length <= 4) return w
        val suffixes = arrayOf("ations", "ation", "ments", "ment", "ing", "ies", "ied", "ers", "er", "ed", "es", "ly", "s")
        for (suffix in suffixes) {
            if (w.endsWith(suffix) && w.length - suffix.length >= 3) {
                val base = w.removeSuffix(suffix)
                return if (suffix == "ies" || suffix == "ied") "${base}y" else base
            }
        }
        return w
    }

    private fun normalizeForComparison(text: String): String {
        return text.lowercase(Locale.getDefault())
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun tokenizeWords(text: String): List<String> {
        return normalizeForComparison(text)
            .split(" ")
            .filter { it.isNotBlank() }
    }

    internal fun tokenJaccard(a: String, b: String): Double {
        val setA = tokenizeWords(a).toSet()
        val setB = tokenizeWords(b).toSet()
        if (setA.isEmpty() || setB.isEmpty()) return 0.0
        val intersection = setA.count { it in setB }
        val union = setA.size + setB.size - intersection
        return if (union == 0) 0.0 else intersection.toDouble() / union.toDouble()
    }

    internal fun bigramDice(normA: String, normB: String): Double {
        if (normA.length < 2 || normB.length < 2) {
            return if (normA == normB) 1.0 else 0.0
        }
        val bigramsA = mutableMapOf<String, Int>()
        for (i in 0 until normA.length - 1) {
            val bg = normA.substring(i, i + 2)
            bigramsA[bg] = (bigramsA[bg] ?: 0) + 1
        }
        var overlap = 0
        val totalB = normB.length - 1
        for (i in 0 until totalB) {
            val bg = normB.substring(i, i + 2)
            val count = bigramsA[bg] ?: 0
            if (count > 0) {
                overlap++
                bigramsA[bg] = count - 1
            }
        }
        val totalA = normA.length - 1
        return (2.0 * overlap) / (totalA + totalB).toDouble()
    }
}
