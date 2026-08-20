package com.udc.collection.util

object SearchUtils {

    /**
     * Matches a test/package name against a user query.
     *
     * Supports:
     *   - Substring match:   "blood"  → "Blood Sugar", "Blood Group"
     *   - Abbreviation:      "CBC"    → "Complete Blood Count"
     *                        "LFT"    → "Liver Function Test"
     *                        "KFT"    → "Kidney Function Test"
     *   - Word-prefix:       "comp"   → "Complete Blood Count"
     *   - Bracket content:   "ALT"    → "SGPT (ALT)"
     */
    fun matches(text: String, query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true

        // 1. Direct substring (fastest path)
        if (text.contains(q, ignoreCase = true)) return true

        val words = text.split(" ", "(", ")", "-", "/", ",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // 2. Abbreviation of significant words (words that start with a letter)
        val abbreviation = words
            .filter { it.first().isLetter() }
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
        if (abbreviation.equals(q, ignoreCase = true)) return true
        if (abbreviation.startsWith(q, ignoreCase = true) && q.length >= 2) return true

        // 3. Any word starts with query
        if (words.any { it.startsWith(q, ignoreCase = true) }) return true

        // 4. Match against words inside parentheses (e.g. "ALT" in "SGPT (ALT)")
        val parentheticalContent = Regex("\\(([^)]+)\\)").findAll(text)
            .map { it.groupValues[1].trim() }
            .toList()
        if (parentheticalContent.any { it.equals(q, ignoreCase = true) || it.contains(q, ignoreCase = true) }) return true

        return false
    }

    /** Filter a list of named items in-memory. O(n) per query — fast for ≤5000 items. */
    fun <T> filter(items: List<T>, query: String, nameSelector: (T) -> String): List<T> {
        if (query.isBlank()) return items
        return items.filter { matches(nameSelector(it), query) }
    }
}
