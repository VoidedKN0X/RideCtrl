package com.vknxworks.ridectrl.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutoBackupNamesTest {
    @Test
    fun finishedBackupsCount() {
        assertTrue(AutoBackup.isBackupName("RideCtrl-auto-2026-11-21-0300.zip"))
        assertTrue(AutoBackup.isBackupName("RideCtrl-auto-2026-11-21-0300.ridectrl"))
    }

    @Test
    fun aCutOffBackupIsNeverCountedButIsCleanedUp() {
        val part = "RideCtrl-auto-2026-11-21-0300.zip" + AutoBackup.PART
        assertFalse(AutoBackup.isBackupName(part))
        assertTrue(AutoBackup.isStalePart(part))
        assertTrue(AutoBackup.isStalePart("RideCtrl-auto-2026-11-21-0300.ridectrl.part"))
    }

    @Test
    fun otherFilesAreLeftAlone() {
        assertFalse(AutoBackup.isStalePart("holiday.zip.part"))
        assertFalse(AutoBackup.isStalePart("RideCtrl-auto-2026-11-21-0300.zip"))
        assertFalse(AutoBackup.isBackupName("RideCtrl-backup-2026-11-21.zip"))
        assertFalse(AutoBackup.isStalePart("RideCtrl-auto-latest.zip.part"))
    }
}
