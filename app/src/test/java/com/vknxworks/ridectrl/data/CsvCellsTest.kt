package com.vknxworks.ridectrl.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvCellsTest {
    @Test
    fun ordinaryTextIsUnchanged() {
        listOf("", "Shell Roeselare", "oil change", "10W-40", "a=b", "mail me @ home", "5", "3.2", " -5", "x+y").forEach {
            assertEquals(it, CsvCells.neutralise(it))
        }
    }

    @Test
    fun formulaStartsGetAnApostrophe() {
        assertEquals("'=1+1", CsvCells.neutralise("=1+1"))
        assertEquals("'=HYPERLINK(\"http://x\")", CsvCells.neutralise("=HYPERLINK(\"http://x\")"))
        assertEquals("'@SUM(A1)", CsvCells.neutralise("@SUM(A1)"))
        assertEquals("'\t=1", CsvCells.neutralise("\t=1"))
        assertEquals("'\r=1", CsvCells.neutralise("\r=1"))
    }

    @Test
    fun signedPlainNumbersStayNumbers() {
        listOf("-5", "+5", "-3.2", "+0.5", "-0,5", "-1.0E-4", "+2e3", "-12345678").forEach {
            assertEquals(it, CsvCells.neutralise(it))
        }
    }

    @Test
    fun signedTextThatIsNotANumberGetsAnApostrophe() {
        assertEquals("'-1+1", CsvCells.neutralise("-1+1"))
        assertEquals("'+1-1", CsvCells.neutralise("+1-1"))
        assertEquals("'-A1", CsvCells.neutralise("-A1"))
        assertEquals("'+cmd|' /C calc'!A0", CsvCells.neutralise("+cmd|' /C calc'!A0"))
        assertEquals("'- replaced the filter", CsvCells.neutralise("- replaced the filter"))
        assertEquals("'-", CsvCells.neutralise("-"))
        assertEquals("'-5.", CsvCells.neutralise("-5."))
        assertEquals("'-5 km", CsvCells.neutralise("-5 km"))
        assertEquals("'-1.2.3", CsvCells.neutralise("-1.2.3"))
    }

    @Test
    fun quotingIsAsBefore() {
        assertEquals("plain", CsvCells.quote("plain"))
        assertEquals("\"a,b\"", CsvCells.quote("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvCells.quote("say \"hi\""))
        assertEquals("\"two\nlines\"", CsvCells.quote("two\nlines"))
        assertEquals("\"'=1,2\"", CsvCells.quote(CsvCells.neutralise("=1,2")))
        assertEquals("\"'\r=1\"", CsvCells.quote(CsvCells.neutralise("\r=1")))
    }
}
