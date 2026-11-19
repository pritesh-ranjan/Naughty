package com.example.naughty.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoNoteSummarizerTest {

    @Test
    fun testTrackAndConsolidateOcr_deduplicatesAndTracksVisibilityDuration() {
        val frames = listOf(
            OcrFrameSample(0L, "Project Launch Plan\n0:01 / 0:30\n- Finalize API schema"),
            OcrFrameSample(500L, "Project Launch Plan\n- Finalize API schema and auth"),
            OcrFrameSample(2500L, "Project Launch Plan\n- Finalize API schema and auth\n- Run load tests at 5000 RPS")
        )

        val blocks = VideoNoteSummarizer.trackAndConsolidateOcr(frames)
        // Player timestamp "0:01 / 0:30" should be filtered out
        assertEquals(3, blocks.size)

        assertEquals("Project Launch Plan", blocks[0].text)
        assertEquals(0L, blocks[0].firstSeenMs)
        assertEquals(2500L, blocks[0].lastSeenMs)
        assertEquals(2500L, blocks[0].visibilityDurationMs)
        assertEquals(3, blocks[0].sampleCount)

        // Progressive line reveal should keep the fuller text
        assertEquals("Finalize API schema and auth", blocks[1].text)
        assertEquals(0L, blocks[1].firstSeenMs)
        assertEquals(2500L, blocks[1].lastSeenMs)

        assertEquals("Run load tests at 5000 RPS", blocks[2].text)
        assertEquals(2500L, blocks[2].firstSeenMs)
    }

    @Test
    fun testSegmentAndCleanTranscript_stripsFillersAndSplitsClauses() {
        val raw = "Hey guys, basically today we're going to install the SDK first open terminal and run gradle sync then make sure your java version is 17"
        val clauses = VideoNoteSummarizer.segmentAndCleanTranscript(raw)

        assertTrue(clauses.size >= 2)
        assertFalse(clauses.any { it.lowercase().startsWith("hey guys") })
        assertFalse(clauses.any { it.lowercase().startsWith("basically") })
        assertTrue(clauses.any { it.contains("install the SDK", ignoreCase = true) })
        assertTrue(clauses.any { it.contains("java version is 17", ignoreCase = true) })
    }

    @Test
    fun testSummarizeToCrispNote_combinesAudioAndOcrWithoutTimestamps() {
        val ocrFrames = listOf(
            OcrFrameSample(500L, "Quarterly Budget Review\nRevenue grew 24% YoY"),
            OcrFrameSample(3500L, "Quarterly Budget Review\nRevenue grew 24% YoY\nCut cloud spend by 15%"),
            OcrFrameSample(6000L, "Quarterly Budget Review\nRevenue grew 24% YoY\nCut cloud spend by 15%")
        )
        val transcript = "Um welcome back. Quarterly revenue grew 24 percent year over year. Also we need to hire two backend engineers in Q3."

        val summary = VideoNoteSummarizer.summarizeToCrispNote(
            subject = null,
            accompanyingText = null,
            ocrFrames = ocrFrames,
            transcript = transcript
        )

        assertTrue(summary.title.isNotBlank())
        assertTrue(summary.pointCount >= 3)
        val lines = summary.content.lines().filter { it.isNotBlank() }
        assertTrue(lines.all { it.startsWith("- ") })
        // Ensure no [mm:ss] timestamps appear in the crisp bullets
        assertFalse(summary.content.contains(Regex("""\[\d{1,2}:\d{2}]""")))
        assertTrue(summary.content.contains("Cut cloud spend by 15%"))
        assertTrue(summary.content.contains("hire two backend engineers", ignoreCase = true))
        // Cross-modal duplicate ("Revenue grew 24% YoY" vs "Quarterly revenue grew 24 percent year over year") should not appear twice
        val revMentions = lines.count { it.contains("24", ignoreCase = true) }
        assertEquals(1, revMentions)
    }

    @Test
    fun testSummarizeToCrispNote_fallbackWhenSilentAndNoText() {
        val summary = VideoNoteSummarizer.summarizeToCrispNote(
            subject = "Empty Clip",
            accompanyingText = null,
            ocrFrames = emptyList(),
            transcript = ""
        )

        assertEquals("Empty Clip", summary.title)
        assertEquals(0, summary.pointCount)
        assertTrue(summary.content.contains("No speech or on-screen text detected"))
    }

    @Test
    fun testSummarizeToCrispNote_cleansReelNoiseStuttersWatermarksAndTitleDuplication() {
        val ocrFrames = listOf(
            OcrFrameSample(200L, "POV: HowN Women want to\n8AMANJOTWALIA_\nNil Gilel • Z thOp. 9, No. 2\nLead"),
            OcrFrameSample(1200L, "POV: How Women want to\nBe treated\nTêlls\nLikef")
        )
        val transcript = "I want a guy who's controlling who who would lead the way and who tells me what to do yeah that's what I'm saying like now go make me a sandwich"

        val summary = VideoNoteSummarizer.summarizeToCrispNote(
            subject = null,
            accompanyingText = null,
            ocrFrames = ocrFrames,
            transcript = transcript
        )

        // Cleaner "How" should win over OCR anomaly "HowN" and merge with "Be treated"
        assertTrue(summary.title.contains("How Women want to be treated", ignoreCase = true))
        assertFalse(summary.title.contains("HowN"))

        val lines = summary.content.lines().filter { it.isNotBlank() }
        // First bullet should not duplicate the title
        assertFalse(lines.first().contains(summary.title, ignoreCase = true))

        // Handles, music tickers, single-word flashing captions, and stutters should be stripped
        assertFalse(summary.content.contains("8AMANJOTWALIA_"))
        assertFalse(summary.content.contains("Nil Gilel"))
        assertFalse(lines.any { it.trim() in setOf("- Lead", "- Têlls", "- Likef", "- Be treated") })
        assertFalse(summary.content.contains("who who", ignoreCase = true))

        // Long unpunctuated speech should be split into crisp separate points
        assertTrue(summary.pointCount >= 2)
        assertTrue(summary.content.contains("go make me a sandwich", ignoreCase = true))
    }

    @Test
    fun testSummarizeToCrispNote_dropsUnrelatedSongWhenOcrIsRich() {
        val ocrFrames = listOf(
            OcrFrameSample(
                500L,
                "3 Morning Habits\nDrink 500ml water on waking\nGet 10 minutes of natural sunlight\nDelay caffeine by 90 minutes"
            )
        )
        val unrelatedSongTranscript = "Baby oh baby you light up my dark nights yeah yeah we run away under the stars tonight"

        val summary = VideoNoteSummarizer.summarizeToCrispNote(
            subject = null,
            accompanyingText = null,
            ocrFrames = ocrFrames,
            transcript = unrelatedSongTranscript
        )

        assertEquals("3 Morning Habits", summary.title)
        assertTrue(summary.content.contains("Drink 500ml water on waking"))
        assertTrue(summary.content.contains("Delay caffeine by 90 minutes"))
        assertFalse(summary.content.contains("light up my dark nights", ignoreCase = true))
    }

    @Test
    fun testSummarizeToCrispNote_dropsAudioWhenClassifierReportsMusicOnly() {
        val ocrFrames = listOf(
            OcrFrameSample(500L, "Workout Checklist\n5 sets of pullups\n3 sets of dips")
        )
        val hallucinatedLyrics = "Dance all night feel the rhythm inside your soul"

        val summary = VideoNoteSummarizer.summarizeToCrispNote(
            subject = null,
            accompanyingText = null,
            ocrFrames = ocrFrames,
            transcript = hallucinatedLyrics,
            audioClassification = AudioClassificationResult(
                profile = AcousticProfile.MUSIC_ONLY,
                speechScore = 0.08f,
                musicScore = 0.84f,
                speechWindowRatio = 0.0f
            )
        )

        assertTrue(summary.content.contains("5 sets of pullups"))
        assertFalse(summary.content.contains("Dance all night", ignoreCase = true))
    }

    @Test
    fun testSummarizeToCrispNote_prefersAudioWhenOcrIsMinimal() {
        val minimalOcrFrames = listOf(
            OcrFrameSample(200L, "@techcreator\nQuick tip")
        )
        val spokenTranscript = "Always configure connection pooling before load testing. Second enable WAL mode in SQLite for concurrent writes."

        val summary = VideoNoteSummarizer.summarizeToCrispNote(
            subject = null,
            accompanyingText = null,
            ocrFrames = minimalOcrFrames,
            transcript = spokenTranscript,
            audioClassification = AudioClassificationResult(
                profile = AcousticProfile.CLEAN_SPEECH,
                speechScore = 0.85f,
                musicScore = 0.04f,
                speechWindowRatio = 1.0f
            )
        )

        assertTrue(
            summary.title.contains("connection pooling", ignoreCase = true) ||
                summary.content.contains("connection pooling", ignoreCase = true)
        )
        assertTrue(summary.content.contains("WAL mode", ignoreCase = true))
    }

    @Test
    fun testSummarizeToCrispNote_preservesTwoWordOcrAndStripsOpusMusicCredit() {
        val ocrFrames = listOf(
            OcrFrameSample(200L, "POV: How women want to\nNil GilelOp. 9, No.2\nDUNESD 3\nDUNES TADE"),
            OcrFrameSample(1200L, "POV: How women want to\nBe treated\nRespect boundaries\nDUNES TADE")
        )
        val transcript = "I want a guy who's controlling who would lead the way yeah that's what I'm saying"

        val summary = VideoNoteSummarizer.summarizeToCrispNote(
            subject = null,
            accompanyingText = null,
            ocrFrames = ocrFrames,
            transcript = transcript,
            audioClassification = AudioClassificationResult(
                profile = AcousticProfile.SPEECH_WITH_MUSIC,
                speechScore = 0.45f,
                musicScore = 0.48f,
                speechWindowRatio = 0.75f
            )
        )

        assertEquals("POV: How women want to be treated", summary.title)
        assertTrue(summary.content.contains("- Respect boundaries"))
        assertFalse(summary.content.contains("- Be treated"))
        assertFalse(summary.content.contains("Gilel"))
        assertFalse(summary.content.contains("Op. 9"))
        assertFalse(summary.content.contains("DUNES"))
        assertTrue(summary.content.contains("controlling", ignoreCase = true))
    }

    @Test
    fun testSegmentAndCleanTranscript_syntacticCompressionAndCtaPruning() {
        val raw = "You need to make sure you enable two factor auth you know what I mean. " +
            "What you need to do is rotate keys every 90 days that's what I'm saying. " +
            "Follow for more! Like and subscribe."

        val clauses = VideoNoteSummarizer.segmentAndCleanTranscript(raw)

        assertTrue(clauses.any { it == "Make sure to enable two factor auth" })
        assertTrue(clauses.any { it.contains("rotate keys every 90 days", ignoreCase = true) })
        assertFalse(clauses.any { it.contains("you know what i mean", ignoreCase = true) })
        assertFalse(clauses.any { it.contains("that's what i'm saying", ignoreCase = true) })
        assertFalse(clauses.any { it.contains("follow for more", ignoreCase = true) })
        assertFalse(clauses.any { it.contains("like and subscribe", ignoreCase = true) })
    }

    @Test
    fun testSummarizeToCrispNote_semanticClusteringPrefersNumericOcrOverParaphrase() {
        val ocrFrames = listOf(
            OcrFrameSample(500L, "Q4 Cloud Optimization\nCut cloud spend by 15%\nMigrate 4 services to ARM64"),
            OcrFrameSample(3500L, "Q4 Cloud Optimization\nCut cloud spend by 15%\nMigrate 4 services to ARM64")
        )
        val transcript = "Lowered AWS bills significantly this quarter. Also add automated Canary rollbacks before Friday."

        val provider = FakeSemanticProvider(
            similarityRules = listOf(
                Triple("Cut cloud spend by 15%", "Lowered AWS bills significantly this quarter", 0.84f),
                Triple("Q4 Cloud Optimization", "Lowered AWS bills significantly this quarter", 0.45f)
            ),
            defaultCrossScore = 0.12f
        )

        val summary = VideoNoteSummarizer.summarizeToCrispNote(
            subject = null,
            accompanyingText = null,
            ocrFrames = ocrFrames,
            transcript = transcript,
            similarityProvider = provider
        )

        val lines = summary.content.lines().filter { it.isNotBlank() }
        // Paraphrase pair should collapse into 1 bullet, preferring exact OCR metric "Cut cloud spend by 15%"
        assertTrue(summary.content.contains("- Cut cloud spend by 15%"))
        assertFalse(summary.content.contains("Lowered AWS bills", ignoreCase = true))
        // Unmatched complementary spoken point survives
        assertTrue(summary.content.contains("Canary rollbacks", ignoreCase = true))
        assertEquals(3, lines.size)
    }

    @Test
    fun testSummarizeToCrispNote_semanticClusterDedupThreshold072() {
        val ocrFrames = listOf(
            OcrFrameSample(500L, "Database Tuning Guide\nEnable SQLite WAL mode for faster writes\nUse connection pooling for bursts")
        )
        val transcript = "Turn on write ahead logging in SQLite to speed up concurrent writes. " +
            "Reuse database sockets via connection pooling. " +
            "Set a 5000ms busy timeout."

        val provider = FakeSemanticProvider(
            similarityRules = listOf(
                Triple(
                    "Enable SQLite WAL mode for faster writes",
                    "Turn on write ahead logging in SQLite to speed up concurrent writes",
                    0.78f
                ),
                Triple(
                    "Use connection pooling for bursts",
                    "Reuse database sockets via connection pooling",
                    0.74f
                ),
                Triple(
                    "Database Tuning Guide",
                    "Set a 5000ms busy timeout",
                    0.46f
                )
            ),
            defaultCrossScore = 0.15f
        )

        val summary = VideoNoteSummarizer.summarizeToCrispNote(
            subject = null,
            accompanyingText = null,
            ocrFrames = ocrFrames,
            transcript = transcript,
            similarityProvider = provider
        )

        // Two >=0.72 paraphrase pairs collapse, leaving 2 OCR points + 1 distinct audio point = 3 bullets
        val bullets = summary.content.lines().filter { it.startsWith("- ") }
        assertEquals(3, bullets.size)
        assertTrue(summary.content.contains("Enable SQLite WAL mode for faster writes"))
        assertTrue(summary.content.contains("Use connection pooling for bursts"))
        assertTrue(summary.content.contains("5000ms busy timeout"))
        assertFalse(summary.content.contains("Reuse database sockets", ignoreCase = true))
    }

    @Test
    fun testSummarizeToCrispNote_dropsUnrelatedAudioOnRichOcrViaSemantics() {
        val ocrFrames = listOf(
            OcrFrameSample(
                500L,
                "Docker Build Checklist\nUse multi-stage builds\nPin alpine base image versions\nRun containers as non-root user"
            )
        )
        val unrelatedTranscript = "Summer breeze makes me feel fine. Dancing underneath the neon skyline."

        val provider = FakeSemanticProvider(
            similarityRules = emptyList(),
            defaultCrossScore = 0.08f
        )

        val summary = VideoNoteSummarizer.summarizeToCrispNote(
            subject = null,
            accompanyingText = null,
            ocrFrames = ocrFrames,
            transcript = unrelatedTranscript,
            similarityProvider = provider
        )

        assertEquals("Docker Build Checklist", summary.title)
        assertTrue(summary.content.contains("Use multi-stage builds"))
        assertTrue(summary.content.contains("Pin alpine base image versions"))
        assertFalse(summary.content.contains("Summer breeze", ignoreCase = true))
        assertFalse(summary.content.contains("neon skyline", ignoreCase = true))
    }

    @Test
    fun testSummarizeToCrispNote_prefersAudioOnMinimalOcrWithSemantics() {
        val ocrFrames = listOf(
            OcrFrameSample(200L, "@devtips\nOriginal Audio\nQuick tip")
        )
        val transcript = "Profile memory allocations before optimizing CPU hotspots. Avoid allocating objects inside tight Compose recompositions."

        val provider = FakeSemanticProvider(
            similarityRules = emptyList(),
            defaultCrossScore = 0.10f
        )

        val summary = VideoNoteSummarizer.summarizeToCrispNote(
            subject = null,
            accompanyingText = null,
            ocrFrames = ocrFrames,
            transcript = transcript,
            audioClassification = AudioClassificationResult(
                profile = AcousticProfile.CLEAN_SPEECH,
                speechScore = 0.90f,
                musicScore = 0.02f,
                speechWindowRatio = 1.0f
            ),
            similarityProvider = provider
        )

        assertFalse(summary.content.contains("Original Audio"))
        assertTrue(
            summary.title.contains("memory allocations", ignoreCase = true) ||
                summary.content.contains("memory allocations", ignoreCase = true)
        )
        assertTrue(summary.content.contains("Compose recompositions", ignoreCase = true))
    }

    @Test
    fun testWordPieceTokenizer_andVectorMath() {
        val vocab = mapOf(
            "[PAD]" to 0,
            "[UNK]" to 100,
            "[CLS]" to 101,
            "[SEP]" to 102,
            "cut" to 3013,
            "cloud" to 6112,
            "spend" to 5247,
            "##s" to 2015,
            "aws" to 22091
        )
        val tokenizer = WordPieceTokenizer(vocab, maxSeqLen = 16)
        val tokenized = tokenizer.tokenize("Cut clouds spend")

        assertEquals(101L, tokenized.inputIds[0]) // [CLS]
        assertEquals(3013L, tokenized.inputIds[1]) // cut
        assertEquals(6112L, tokenized.inputIds[2]) // cloud
        assertEquals(2015L, tokenized.inputIds[3]) // ##s
        assertEquals(5247L, tokenized.inputIds[4]) // spend
        assertEquals(102L, tokenized.inputIds[5]) // [SEP]
        assertEquals(0L, tokenized.inputIds[6])   // [PAD]
        assertEquals(1L, tokenized.attentionMask[5])
        assertEquals(0L, tokenized.attentionMask[6])

        // Mean pooling + L2 normalization + centroid dot product
        val tokenHidden = arrayOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(3f, 0f, 0f),
            floatArrayOf(99f, 99f, 99f) // masked out
        )
        val mask = longArrayOf(1L, 1L, 0L)
        val pooled = SemanticEmbedder.meanPoolAndNormalize(tokenHidden, mask)
        assertEquals(1.0f, pooled[0], 1e-5f)
        assertEquals(0.0f, pooled[1], 1e-5f)

        val centroid = SemanticEmbedder.computeCentroid(
            listOf(floatArrayOf(1f, 0f, 0f), floatArrayOf(0f, 1f, 0f))
        )
        assertEquals(0.7071f, centroid[0], 1e-3f)
        assertEquals(0.7071f, centroid[1], 1e-3f)
    }

    @Test
    fun testSummarizeToCrispNote_textRankCentroidAndSemanticTitleDedup() {
        // 14 distinct points (> MAX_SUMMARY_POINTS = 12) where core topic vectors align strongly with centroid
        val coreTopicVec = floatArrayOf(1f, 0f, 0f, 0f)
        val offTopicVec = floatArrayOf(0f, 0f, 0f, 1f)

        val ocrFrames = listOf(
            OcrFrameSample(
                1000L,
                listOf(
                    "Android Performance Playbook",
                    "Baseline profiles speed up cold start by 30%",
                    "Defer non-critical SDK initialization",
                    "Use R8 full mode for smaller APKs",
                    "Random off-topic distraction line",
                    "Keep main thread work under 16ms",
                    "Prefer LazyColumn stable keys",
                    "Avoid allocations in draw phases",
                    "Use remember for expensive state",
                    "Enable baseline profile gradle plugin",
                    "Inspect Macrobenchmark startup traces",
                    "Compress WebP assets offline",
                    "Preload Room queries on IO dispatcher",
                    "Pin LiteRT buffers with zero copy"
                ).joinToString("\n")
            )
        )

        val distinctProvider = object : SemanticSimilarityProvider {
            override fun embedBatch(texts: List<String>): List<FloatArray> {
                return texts.mapIndexed { idx, t ->
                    val vec = FloatArray(16)
                    if (t.contains("off-topic")) {
                        vec[15] = 1.0f
                    } else {
                        // High alignment (~0.62) with shared topic axis 0, yet pairwise sim = 0.384 < 0.72
                        vec[0] = 0.62f
                        vec[1 + (idx % 14)] = 0.78f
                    }
                    SemanticEmbedder.l2Normalize(vec)
                }
            }

            override fun similarity(textA: String, textB: String): Float? = null
        }

        val summary = VideoNoteSummarizer.summarizeToCrispNote(
            subject = null,
            accompanyingText = null,
            ocrFrames = ocrFrames,
            transcript = null,
            similarityProvider = distinctProvider
        )

        assertEquals("Android Performance Playbook", summary.title)
        assertFalse(summary.content.contains("Random off-topic distraction line"))
        assertTrue(summary.pointCount <= 12)
        // Title should not be duplicated as the first bullet
        assertFalse(summary.content.lines().first().contains("Android Performance Playbook"))
    }

    @Test
    fun testSummarizeToCrispNote_splitsNumberedStepsAndSuppressesPackagingOcrGarbles() {
        val ocrFrames = listOf(
            OcrFrameSample(200L, "5 WAYS TO MAKE YOUR HOME SMELL GOOD\nOMES MELL GOfOD\nVINEGAR e\nANDARD OF P"),
            OcrFrameSample(1500L, "5 WAYS TO MAKE YOUR HOME SMELL GOOD\nFUELING FUTURE SCIENTISTS ONE EXPERIMENT AT A TIME\nK57S CHE EXPERINENT ATATIME")
        )
        val transcript = "Listen here five simple ways to keep your home smelling amazing 24 7 " +
            "starting up with an air purifier helps remove dust and pet dander " +
            "number two put dryer sheets inside of your pillows " +
            "number three make a simmer pot with citrus and cinnamon " +
            "number four put baking soda in a coffee filter at the bottom of your trash can " +
            "number five clean your garbage disposal regularly"

        val summary = VideoNoteSummarizer.summarizeToCrispNote(
            subject = null,
            accompanyingText = null,
            ocrFrames = ocrFrames,
            transcript = transcript
        )

        assertTrue(summary.title.contains("Home Smell Good", ignoreCase = true))
        assertFalse(summary.content.contains("GOfOD"))
        assertFalse(summary.content.contains("VINEGAR e"))
        assertFalse(summary.content.contains("ANDARD OF P"))
        assertFalse(summary.content.contains("FUELING FUTURE SCIENTISTS"))
        assertFalse(summary.content.contains("K57S"))
        assertTrue(summary.pointCount >= 5)
        assertTrue(summary.content.contains("Put dryer sheets inside of your pillows"))
        assertTrue(summary.content.contains("Make a simmer pot with citrus and cinnamon"))
        assertTrue(summary.content.contains("Clean your garbage disposal regularly"))
    }

    @Test
    fun testSummarizeToCrispNote_geometricCaptionMergeOverlayScoringGibberishAndCompression() {
        val frameW = 720
        val frameH = 1280
        val ocrFrames = listOf(
            OcrFrameSample(
                timestampMs = 500L,
                text = "5 WAYS TO MAKE YOUR\nHOME SMELL GOOD\nHO EeoD",
                lines = listOf(
                    OcrLine("5 WAYS TO MAKE YOUR", left = 110, top = 220, right = 610, bottom = 285, confidence = 0.94f, angle = 0.1f, blockIndex = 0),
                    OcrLine("HOME SMELL GOOD", left = 145, top = 295, right = 575, bottom = 360, confidence = 0.93f, angle = 0.2f, blockIndex = 0),
                    OcrLine("HO EeoD", left = 160, top = 380, right = 420, bottom = 420, confidence = 0.42f, angle = 2.0f, blockIndex = 1)
                ),
                frameWidth = frameW,
                frameHeight = frameH
            ),
            OcrFrameSample(
                timestampMs = 15000L,
                text = "DRYER SHEETS IN PILLOWS\nHeles Rsoe Pes Hai",
                lines = listOf(
                    OcrLine("DRYER SHEETS IN PILLOWS", left = 120, top = 600, right = 600, bottom = 655, confidence = 0.92f, angle = 0f, blockIndex = 0),
                    OcrLine("Heles Rsoe Pes Hai", left = 210, top = 740, right = 480, bottom = 770, confidence = 0.78f, angle = 14f, blockIndex = 1)
                ),
                frameWidth = frameW,
                frameHeight = frameH
            ),
            OcrFrameSample(
                timestampMs = 24000L,
                text = "America's #1 Trusted Baking Soda Brend\nFUELING FUTURE SCIENTISTS ONE EXPERIMENT AT A TIME\nSted Bali",
                lines = listOf(
                    OcrLine("America's #1 Trusted Baking Soda Brend", left = 240, top = 780, right = 590, bottom = 830, confidence = 0.72f, angle = 18f, blockIndex = 0),
                    OcrLine("FUELING FUTURE SCIENTISTS ONE EXPERIMENT AT A TIME", left = 250, top = 845, right = 610, bottom = 875, confidence = 0.70f, angle = 16f, blockIndex = 1),
                    OcrLine("Sted Bali", left = 300, top = 910, right = 420, bottom = 940, confidence = 0.65f, angle = 10f, blockIndex = 2)
                ),
                frameWidth = frameW,
                frameHeight = frameH
            )
        )

        val transcript = "Listen here five simple ways to keep your home smelling amazing 24 7 " +
            "starting up with an air purifier this is one of the perfect things to help remove dust pet dander and other things floating around the air that can contribute to unwanted smells " +
            "number two put dryer sheets inside of your pillows tucking a dryer sheet inside a decorated pillow cover can give off a subtle fresh scent " +
            "number three make a simmer pot simmering things like citrus cinnamon or herbs naturally fills your home with a fragrance without needing to spray anything " +
            "number four put baking soda in a coffee filter at the bottom of your trash can baking soda helps neutralize odors instead of just covering them up " +
            "number five clean your garbage disposal food build up inside your disposal can be a hidden source of bad smells regularly cleaning it helps remove that buildup"

        val provider = FakeSemanticProvider(
            similarityRules = listOf(
                Triple("5 WAYS TO MAKE YOUR HOME SMELL GOOD", "Five simple ways to keep your home smelling amazing", 0.76f),
                Triple("DRYER SHEETS IN PILLOWS", "Put dryer sheets inside of your pillows", 0.80f),
                Triple("America's #1 Trusted Baking Soda Brend", "Put baking soda in a coffee filter", 0.75f)
            ),
            defaultCrossScore = 0.15f
        )

        val summary = VideoNoteSummarizer.summarizeToCrispNote(
            subject = null,
            accompanyingText = null,
            ocrFrames = ocrFrames,
            transcript = transcript,
            similarityProvider = provider
        )

        assertEquals("5 Ways to Make Your Home Smell Good", summary.title)
        assertFalse(summary.content.contains("5 WAYS TO MAKE YOUR"))
        assertFalse(summary.content.contains("Five simple ways"))
        assertFalse(summary.content.contains("HO EeoD"))
        assertFalse(summary.content.contains("Heles"))
        assertFalse(summary.content.contains("Sted Bali"))
        assertFalse(summary.content.contains("America's #1"))
        assertFalse(summary.content.contains("FUELING FUTURE SCIENTISTS"))
        assertFalse(summary.content.contains("Instead of just covering"))

        val bullets = summary.content.lines().filter { it.startsWith("- ") }
        assertEquals(5, bullets.size)
        assertTrue(bullets[0].contains("air purifier", ignoreCase = true))
        assertTrue(bullets[1].contains("dryer sheets inside of your pillows", ignoreCase = true))
        assertTrue(bullets[2].contains("simmer pot", ignoreCase = true))
        assertTrue(bullets[3].contains("baking soda in a coffee filter", ignoreCase = true))
        assertTrue(bullets[4].contains("Clean your garbage disposal", ignoreCase = true))
    }

    private class FakeSemanticProvider(
        private val similarityRules: List<Triple<String, String, Float>>,
        private val defaultCrossScore: Float = 0.1f
    ) : SemanticSimilarityProvider {
        override fun embedBatch(texts: List<String>): List<FloatArray>? = null

        override fun similarity(textA: String, textB: String): Float {
            if (textA == textB) return 1.0f
            for ((a, b, score) in similarityRules) {
                val matchForward = textA.contains(a, ignoreCase = true) && textB.contains(b, ignoreCase = true)
                val matchReverse = textA.contains(b, ignoreCase = true) && textB.contains(a, ignoreCase = true)
                if (matchForward || matchReverse) return score
            }
            return defaultCrossScore
        }
    }
}
