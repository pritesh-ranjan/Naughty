package com.example.naughty.data.mention

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.provider.ContactsContract
import com.example.naughty.ui.components.hasCalendarPermission
import com.example.naughty.ui.components.hasContactsPermission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object MentionRepository {

    suspend fun searchMentions(
        context: Context,
        query: String,
        filter: MentionType? = null
    ): List<MentionItem> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        when (filter) {
            MentionType.CONTACT -> searchContacts(context, trimmedQuery)
            MentionType.APP -> searchApps(context, trimmedQuery)
            MentionType.CALENDAR -> searchCalendarEvents(context, trimmedQuery)
            null -> {
                val contacts = searchContacts(context, trimmedQuery).take(10)
                val apps = searchApps(context, trimmedQuery).take(10)
                val events = searchCalendarEvents(context, trimmedQuery).take(10)
                contacts + apps + events
            }
        }
    }

    suspend fun searchContacts(context: Context, query: String): List<MentionItem> = withContext(Dispatchers.IO) {
        if (!hasContactsPermission(context)) return@withContext emptyList()
        val results = mutableListOf<MentionItem>()
        val seenIds = mutableSetOf<String>()

        try {
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
            )

            val (selection, selectionArgs) = if (query.isBlank()) {
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} IS NOT NULL" to null
            } else {
                "(${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?)" to
                    arrayOf("%$query%", "%$query%")
            }

            val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} ASC LIMIT 40"

            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val photoIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

                while (cursor.moveToNext()) {
                    val id = if (idIdx >= 0) cursor.getString(idIdx) ?: "" else ""
                    val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
                    val number = if (numberIdx >= 0) cursor.getString(numberIdx) ?: "" else ""
                    val photo = if (photoIdx >= 0) cursor.getString(photoIdx) else null

                    if (id.isNotBlank() && name.isNotBlank() && seenIds.add(id)) {
                        results.add(
                            MentionItem(
                                id = id,
                                type = MentionType.CONTACT,
                                title = name,
                                subtitle = number.ifBlank { "Contact" },
                                iconUri = photo
                            )
                        )
                    }
                    if (results.size >= 25) break
                }
            }

            // Fallback for contacts without phone numbers if query returned few results
            if (results.size < 10) {
                val contactsProjection = arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                    ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
                )
                val (cSelection, cSelectionArgs) = if (query.isBlank()) {
                    "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} IS NOT NULL" to null
                } else {
                    "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} LIKE ?" to arrayOf("%$query%")
                }
                context.contentResolver.query(
                    ContactsContract.Contacts.CONTENT_URI,
                    contactsProjection,
                    cSelection,
                    cSelectionArgs,
                    "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC LIMIT 20"
                )?.use { cursor ->
                    val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                    val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                    val photoIdx = cursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI)

                    while (cursor.moveToNext() && results.size < 25) {
                        val id = if (idIdx >= 0) cursor.getString(idIdx) ?: "" else ""
                        val name = if (nameIdx >= 0) cursor.getString(nameIdx) ?: "" else ""
                        val photo = if (photoIdx >= 0) cursor.getString(photoIdx) else null

                        if (id.isNotBlank() && name.isNotBlank() && seenIds.add(id)) {
                            results.add(
                                MentionItem(
                                    id = id,
                                    type = MentionType.CONTACT,
                                    title = name,
                                    subtitle = "Contact",
                                    iconUri = photo
                                )
                            )
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        results
    }

    suspend fun searchApps(context: Context, query: String): List<MentionItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<MentionItem>()
        val seenPackages = mutableSetOf<String>()
        try {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(intent, 0)
            val currentPackage = context.packageName

            for (info in resolveInfos) {
                val pkg = info.activityInfo.packageName
                if (pkg == currentPackage) continue
                if (!seenPackages.add(pkg)) continue

                val label = try {
                    info.loadLabel(pm).toString()
                } catch (_: Exception) {
                    pkg
                }

                if (query.isBlank() ||
                    label.contains(query, ignoreCase = true) ||
                    pkg.contains(query, ignoreCase = true)
                ) {
                    results.add(
                        MentionItem(
                            id = pkg,
                            type = MentionType.APP,
                            title = label,
                            subtitle = pkg
                        )
                    )
                }
            }
        } catch (_: Exception) {}

        results.sortedBy { it.title.lowercase(Locale.ROOT) }.take(30)
    }

    suspend fun searchCalendarEvents(context: Context, query: String): List<MentionItem> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission(context)) return@withContext emptyList()
        val results = mutableListOf<MentionItem>()

        try {
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.ALL_DAY,
                CalendarContract.Events.EVENT_LOCATION
            )

            val now = System.currentTimeMillis()
            val lookbackStart = now - (14L * 24 * 60 * 60 * 1000) // 14 days ago

            val (selection, selectionArgs) = if (query.isBlank()) {
                "${CalendarContract.Events.DELETED} = 0 AND (${CalendarContract.Events.DTSTART} >= ? OR ${CalendarContract.Events.DTEND} >= ?)" to
                    arrayOf(lookbackStart.toString(), lookbackStart.toString())
            } else {
                "${CalendarContract.Events.DELETED} = 0 AND (${CalendarContract.Events.DTSTART} >= ? OR ${CalendarContract.Events.DTEND} >= ?) AND ${CalendarContract.Events.TITLE} LIKE ?" to
                    arrayOf(lookbackStart.toString(), lookbackStart.toString(), "%$query%")
            }

            val sortOrder = "${CalendarContract.Events.DTSTART} ASC LIMIT 40"

            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndex(CalendarContract.Events._ID)
                val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                val allDayIdx = cursor.getColumnIndex(CalendarContract.Events.ALL_DAY)
                val locationIdx = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION)

                while (cursor.moveToNext() && results.size < 25) {
                    val id = if (idIdx >= 0) cursor.getString(idIdx) ?: "" else ""
                    val title = if (titleIdx >= 0) cursor.getString(titleIdx) ?: "" else ""
                    val startMillis = if (startIdx >= 0) cursor.getLong(startIdx) else 0L
                    val isAllDay = if (allDayIdx >= 0) cursor.getInt(allDayIdx) == 1 else false
                    val location = if (locationIdx >= 0) cursor.getString(locationIdx) else null

                    if (id.isNotBlank() && title.isNotBlank()) {
                        val formattedTime = formatEventDate(startMillis, isAllDay)
                        val subtitle = if (!location.isNullOrBlank()) {
                            "$formattedTime • $location"
                        } else {
                            formattedTime
                        }

                        results.add(
                            MentionItem(
                                id = id,
                                type = MentionType.CALENDAR,
                                title = title,
                                subtitle = subtitle
                            )
                        )
                    }
                }
            }
        } catch (_: Exception) {}

        results
    }

    private fun formatEventDate(startMillis: Long, isAllDay: Boolean): String {
        if (startMillis <= 0L) return "Calendar Event"
        val target = Calendar.getInstance().apply { timeInMillis = startMillis }
        val now = Calendar.getInstance()

        val isSameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
        val timeFormat = if (isAllDay) "All Day" else SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(startMillis))

        val targetDay = Calendar.getInstance().apply {
            timeInMillis = startMillis
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val nowDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val dayDiff = Math.round((targetDay.timeInMillis - nowDay.timeInMillis) / (24.0 * 60 * 60 * 1000))

        return when (dayDiff) {
            0L -> if (isAllDay) "Today (All day)" else "Today at $timeFormat"
            1L -> if (isAllDay) "Tomorrow (All day)" else "Tomorrow at $timeFormat"
            -1L -> if (isAllDay) "Yesterday (All day)" else "Yesterday at $timeFormat"
            else -> {
                val pattern = if (isSameYear) "EEE, MMM d" else "MMM d, yyyy"
                val dateStr = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(startMillis))
                if (isAllDay) dateStr else "$dateStr, $timeFormat"
            }
        }
    }
}
