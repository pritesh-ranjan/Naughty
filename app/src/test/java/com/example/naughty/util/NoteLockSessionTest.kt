package com.example.naughty.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NoteLockSessionTest {

    @Before
    fun setUp() {
        NoteLockSession.clear()
    }

    @Test
    fun testInitialStateIsLocked() {
        assertFalse(NoteLockSession.isUnlocked("note-1"))
        assertFalse(NoteLockSession.isUnlocked("note-2"))
    }

    @Test
    fun testUnlockAndLock() {
        NoteLockSession.unlock("note-1")
        assertTrue(NoteLockSession.isUnlocked("note-1"))
        assertFalse(NoteLockSession.isUnlocked("note-2"))

        NoteLockSession.lock("note-1")
        assertFalse(NoteLockSession.isUnlocked("note-1"))
    }

    @Test
    fun testMultipleNotes() {
        NoteLockSession.unlock("note-1")
        NoteLockSession.unlock("note-2")
        assertTrue(NoteLockSession.isUnlocked("note-1"))
        assertTrue(NoteLockSession.isUnlocked("note-2"))
        assertFalse(NoteLockSession.isUnlocked("note-3"))

        NoteLockSession.lock("note-1")
        assertFalse(NoteLockSession.isUnlocked("note-1"))
        assertTrue(NoteLockSession.isUnlocked("note-2"))
    }

    @Test
    fun testClearRemovesAll() {
        NoteLockSession.unlock("note-1")
        NoteLockSession.unlock("note-2")
        NoteLockSession.unlock("note-3")

        NoteLockSession.clear()
        assertFalse(NoteLockSession.isUnlocked("note-1"))
        assertFalse(NoteLockSession.isUnlocked("note-2"))
        assertFalse(NoteLockSession.isUnlocked("note-3"))
    }
}
