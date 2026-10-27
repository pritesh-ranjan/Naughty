package com.example.naughty.ui.editor

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.widget.Toast
import com.example.naughty.data.mention.MentionType
import com.example.naughty.data.mention.ParsedMention

object MentionActionHandler {

    fun openMention(context: Context, mention: ParsedMention) {
        when (mention.type) {
            MentionType.CONTACT -> openContact(context, mention.id, mention.title)
            MentionType.APP -> openApp(context, mention.id, mention.title)
            MentionType.CALENDAR -> openCalendarEvent(context, mention.id, mention.title)
        }
    }

    fun openContact(context: Context, contactId: String, title: String = "") {
        try {
            val numericId = contactId.toLongOrNull()
            val uri = if (numericId != null) {
                ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, numericId)
            } else {
                Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contactId)
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val fallbackIntent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("content://contacts/people/$contactId")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    if (title.isNotBlank()) "Unable to open contact '$title'" else "Unable to open contact",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun openApp(context: Context, packageName: String, title: String = "") {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
            } else {
                Toast.makeText(
                    context,
                    if (title.isNotBlank()) "App '$title' is not installed" else "App is not installed",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to open app: ${e.localizedMessage ?: "Unknown error"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun openCalendarEvent(context: Context, eventId: String, title: String = "") {
        try {
            val numericId = eventId.toLongOrNull()
            val uri = if (numericId != null) {
                ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, numericId)
            } else {
                Uri.withAppendedPath(CalendarContract.Events.CONTENT_URI, eventId)
            }
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                // Fallback: open calendar at current time
                val builder = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
                ContentUris.appendId(builder, System.currentTimeMillis())
                val intent = Intent(Intent.ACTION_VIEW, builder.build()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(
                    context,
                    if (title.isNotBlank()) "Unable to open calendar event '$title'" else "Unable to open calendar event",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
