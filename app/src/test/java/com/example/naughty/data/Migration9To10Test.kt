package com.example.naughty.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class Migration9To10Test {

    @Test
    fun testMigration9To10ExecutesExpectedDropSql() {
        val executedSql = mutableListOf<String>()
        val mockDb = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL") {
                executedSql.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        val migration9To10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `graph_evidence`")
                db.execSQL("DROP TABLE IF EXISTS `graph_edge`")
                db.execSQL("DROP TABLE IF EXISTS `graph_node`")
                db.execSQL("DROP TABLE IF EXISTS `recent_notification_log`")
            }
        }

        assertEquals(9, migration9To10.startVersion)
        assertEquals(10, migration9To10.endVersion)

        migration9To10.migrate(mockDb)

        assertEquals(4, executedSql.size)
        assertTrue("Expected graph_evidence drop", executedSql.any { it.contains("DROP TABLE IF EXISTS `graph_evidence`") })
        assertTrue("Expected graph_edge drop", executedSql.any { it.contains("DROP TABLE IF EXISTS `graph_edge`") })
        assertTrue("Expected graph_node drop", executedSql.any { it.contains("DROP TABLE IF EXISTS `graph_node`") })
        assertTrue("Expected recent_notification_log drop", executedSql.any { it.contains("DROP TABLE IF EXISTS `recent_notification_log`") })
    }
}
