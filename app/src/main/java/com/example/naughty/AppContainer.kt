package com.example.naughty

import android.content.Context
import android.content.pm.PackageManager
import androidx.room.Room
import com.example.naughty.binding.BindingDetector
import com.example.naughty.data.file.MarkdownFileManager
import com.example.naughty.data.local.AppDatabase
import com.example.naughty.data.repository.BindingRepository
import com.example.naughty.data.repository.NoteRepository
import com.example.naughty.notification.ContextNotificationManager
import com.example.naughty.ui.binding.BindingViewModel
import com.example.naughty.ui.editor.NoteEditorViewModel
import com.example.naughty.ui.notelist.NoteListViewModel

class AppContainer(private val context: Context) {

    private val migration2To3 = object : androidx.room.migration.Migration(2, 3) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE app_binding ADD COLUMN expiresAt INTEGER DEFAULT NULL")
        }
    }

    private val migration3To4 = object : androidx.room.migration.Migration(3, 4) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE note_metadata ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val migration4To5 = object : androidx.room.migration.Migration(4, 5) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE note_metadata ADD COLUMN reminderTime INTEGER DEFAULT NULL")
        }
    }

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "naughty_db"
    )
        .addMigrations(migration2To3, migration3To4, migration4To5)
        .fallbackToDestructiveMigration()
        .build()

    private val markdownFileManager = MarkdownFileManager(context.applicationContext)

    val noteRepository = NoteRepository(
        dao = database.noteMetadataDao(),
        fileManager = markdownFileManager
    )

    val bindingRepository = BindingRepository(
        dao = database.appBindingDao()
    )

    val bindingDetector = BindingDetector(
        context = context.applicationContext,
        bindingRepository = bindingRepository
    )

    val contextNotificationManager = ContextNotificationManager(
        context = context.applicationContext,
        bindingDetector = bindingDetector,
        noteRepository = noteRepository
    )

    val reminderManager = com.example.naughty.notification.ReminderManager(
        context = context.applicationContext
    )

    private val packageManager: PackageManager = context.packageManager

    // ViewModel factories
    fun noteListViewModel() = NoteListViewModel(noteRepository, bindingRepository)
    fun noteEditorViewModel() = NoteEditorViewModel(noteRepository, bindingRepository, reminderManager)
    fun bindingViewModel() = BindingViewModel(bindingRepository, packageManager, context.applicationContext)
}
