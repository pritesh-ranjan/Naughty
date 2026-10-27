package com.example.naughty

import android.content.Context
import android.content.pm.PackageManager
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    private val migration7To8 = object : androidx.room.migration.Migration(7, 8) {
        override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `graph_node` (
                    `id` TEXT NOT NULL,
                    `noteId` TEXT NOT NULL,
                    `nodeType` TEXT NOT NULL,
                    `label` TEXT NOT NULL,
                    `subtitle` TEXT NOT NULL DEFAULT '',
                    `rawIdentifier` TEXT DEFAULT NULL,
                    `attributesJson` TEXT NOT NULL DEFAULT '{}',
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`noteId`) REFERENCES `note_metadata`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_graph_node_noteId` ON `graph_node` (`noteId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_graph_node_nodeType` ON `graph_node` (`nodeType`)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `graph_edge` (
                    `id` TEXT NOT NULL,
                    `sourceNodeId` TEXT NOT NULL,
                    `targetNodeId` TEXT NOT NULL,
                    `relationType` TEXT NOT NULL,
                    `confidenceScore` REAL NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`sourceNodeId`) REFERENCES `graph_node`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`targetNodeId`) REFERENCES `graph_node`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_graph_edge_sourceNodeId` ON `graph_edge` (`sourceNodeId`)")
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_graph_edge_targetNodeId` ON `graph_edge` (`targetNodeId`)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `graph_evidence` (
                    `id` TEXT NOT NULL,
                    `edgeId` TEXT NOT NULL,
                    `sourceType` TEXT NOT NULL,
                    `snippet` TEXT NOT NULL,
                    `confidence` REAL NOT NULL,
                    `timestamp` INTEGER NOT NULL,
                    `metadataJson` TEXT NOT NULL DEFAULT '{}',
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`edgeId`) REFERENCES `graph_edge`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_graph_evidence_edgeId` ON `graph_evidence` (`edgeId`)")

            db.execSQL("""
                CREATE TABLE IF NOT EXISTS `recent_notification_log` (
                    `id` TEXT NOT NULL,
                    `packageName` TEXT NOT NULL,
                    `title` TEXT NOT NULL DEFAULT '',
                    `text` TEXT NOT NULL DEFAULT '',
                    `postTime` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX IF NOT EXISTS `index_recent_notification_log_postTime` ON `recent_notification_log` (`postTime`)")
        }
    }

    private val migration8To9 = object : Migration(8, 9) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Merge all archived notes into deleted notes (soft deleted)
            db.execSQL("UPDATE note_metadata SET isDeleted = 1, isArchived = 0 WHERE isArchived = 1")
            // Clean up any app bindings associated with soft-deleted notes
            db.execSQL("DELETE FROM app_binding WHERE noteId IN (SELECT id FROM note_metadata WHERE isDeleted = 1)")
        }
    }

    private val migration9To10 = object : Migration(9, 10) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("DROP TABLE IF EXISTS `graph_evidence`")
            db.execSQL("DROP TABLE IF EXISTS `graph_edge`")
            db.execSQL("DROP TABLE IF EXISTS `graph_node`")
            db.execSQL("DROP TABLE IF EXISTS `recent_notification_log`")
        }
    }

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "naughty_db"
    )
        .addMigrations(migration2To3, migration3To4, migration4To5, migration5To6, migration6To7, migration7To8, migration8To9, migration9To10)
        .fallbackToDestructiveMigration()
        .build()

    private val markdownFileManager = MarkdownFileManager(context.applicationContext)

    val noteRepository = NoteRepository(
        dao = database.noteMetadataDao(),
        fileManager = markdownFileManager,
        bindingDao = database.appBindingDao()
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
    fun settingsViewModel() = com.example.naughty.ui.settings.SettingsViewModel(noteRepository, bindingRepository)
    fun bindingViewModel() = BindingViewModel(bindingRepository, packageManager, context.applicationContext)
    fun timelineListViewModel() = TimelineListViewModel(timelineRepository)
    fun timelineDetailViewModel(trackerId: String) = TimelineDetailViewModel(trackerId, timelineRepository, reminderManager)
}

