package com.example.naughty.data.mention

enum class MentionType {
    CONTACT,
    APP,
    CALENDAR
}

data class MentionItem(
    val id: String, // Contact ID, Package Name, or Calendar Event ID
    val type: MentionType,
    val title: String,
    val subtitle: String? = null,
    val iconUri: String? = null,
    val extras: Map<String, String> = emptyMap()
)

data class ParsedMention(
    val rawTag: String,
    val title: String,
    val type: MentionType,
    val id: String,
    val startIndex: Int,
    val endIndex: Int
)

data class MentionTriggerState(
    val query: String,
    val startIndex: Int,
    val endIndex: Int
)
