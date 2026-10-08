package com.example.naughty

import android.content.Context
import android.content.pm.PackageManager
import androidx.room.Room
import com.example.naughty.binding.BindingDetector
import com.example.naughty.data.file.MarkdownFileManager
import com.example.naughty.data.local.AppDatabase
import com.example.naughty.data.repository.BindingRepository
import com.example.naughty.data.repository.NoteRepository
import com.example.naughty.data.repository.TimelineRepository
import com.example.naughty.notification.ContextNotificationManager
import com.example.naughty.ui.binding.BindingViewModel
import com.example.naughty.ui.editor.NoteEditorViewModel
import com.example.naughty.ui.notelist.NoteListViewModel
import com.example.naughty.ui.timeline.TimelineDetailViewModel
import com.example.naughty.ui.timeline.TimelineListViewModel

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

    private val migration5To6 = object : androidx.room.migration.Migration(5, 6) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE note_metadata ADD COLUMN isLocked INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val migration6To7 = object : androidx.room.migration.Migration(6, 7) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `timeline_tracker` (
                    `id` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `description` TEXT NOT NULL DEFAULT '',
                    `priority` TEXT NOT NULL,
                    `accent` TEXT NOT NULL DEFAULT 'green',
                    `createdAt` INTEGER NOT NULL,
                    `modifiedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `timeline_stage` (
                    `id` TEXT NOT NULL,
                    `trackerId` TEXT NOT NULL,
                    `label` TEXT NOT NULL,
                    `colorKey` TEXT NOT NULL,
                    `category` TEXT NOT NULL,
                    `position` INTEGER NOT NULL,
                    `isBuiltIn` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`trackerId`) REFERENCES `timeline_tracker`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_timeline_stage_trackerId` ON `timeline_stage` (`trackerId`)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `timeline_milestone` (
                    `id` TEXT NOT NULL,
                    `trackerId` TEXT NOT NULL,
                    `title` TEXT NOT NULL,
                    `note` TEXT NOT NULL DEFAULT '',
                    `stageId` TEXT NOT NULL,
                    `position` INTEGER NOT NULL,
                    `dueAt` INTEGER DEFAULT NULL,
                    `reminderEnabled` INTEGER NOT NULL DEFAULT 0,
                    `completedAt` INTEGER DEFAULT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `modifiedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`trackerId`) REFERENCES `timeline_tracker`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_timeline_milestone_trackerId` ON `timeline_milestone` (`trackerId`)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `timeline_update` (
                    `id` TEXT NOT NULL,
                    `trackerId` TEXT NOT NULL,
                    `milestoneId` TEXT DEFAULT NULL,
                    `type` TEXT NOT NULL,
                    `text` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`trackerId`) REFERENCES `timeline_tracker`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_timeline_update_trackerId` ON `timeline_update` (`trackerId`)")
        }
    }

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "naughty_db"
    )
        .addMigrations(migration2To3, migration3To4, migration4To5, migration5To6, migration6To7)
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

    val timelineRepository = TimelineRepository(
        dao = database.timelineDao()
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
    fun noteListViewModel() = NoteListViewModel(noteRepository, bindingRepository, timelineRepository)
    fun noteEditorViewModel() = NoteEditorViewModel(noteRepository, bindingRepository, reminderManager)
    fun bindingViewModel() = BindingViewModel(bindingRepository, packageManager, context.applicationContext)
    fun timelineListViewModel() = TimelineListViewModel(timelineRepository)
    fun timelineDetailViewModel(trackerId: String) = TimelineDetailViewModel(trackerId, timelineRepository, reminderManager)
}
