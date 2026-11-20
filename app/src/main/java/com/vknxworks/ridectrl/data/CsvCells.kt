package com.vknxworks.ridectrl.data

/**
 * Text cells for the CSV export. A spreadsheet treats a cell that starts with `=`, `+`, `-` or `@` as a
 * formula, so text you typed yourself (a note, a station, a part name) could be run instead of shown.
 * Such a cell gets a leading apostrophe, which a spreadsheet shows as plain text.
 */
object CsvCells {
    /** A plain number, with an optional sign: `-5`, `+3.2`, `-0,5`, `1.0E-4`. Never a formula. */
    private val plainNumber = Regex("[+-]?[0-9]+([.,][0-9]+)?([eE][+-]?[0-9]+)?")

    /**
     * [text] made safe to open in a spreadsheet: unchanged, or with `'` in front when it would be read as a
     * formula. That is every text starting with `=`, `@`, a tab or a carriage return, and text starting with
     * `+` or `-` unless the whole cell is a plain number (so `-5` and `+3.2` stay numbers).
     */
    fun neutralise(text: String): String {
        if (text.isEmpty()) return text
        val risky = when (text[0]) {
            '=', '@', '\t', '\r' -> true
            '+', '-' -> !plainNumber.matches(text)
            else -> false
        }
        return if (risky) "'$text" else text
    }

    /** One CSV cell: [text] in quotes (with `"` doubled) when it holds a comma, a quote or a line break. */
    fun quote(text: String): String =
        if (text.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + text.replace("\"", "\"\"") + "\"" else text
}
