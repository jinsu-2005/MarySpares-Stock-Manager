package com.marytwowheelers.spares.util

import kotlin.math.min

/**
 * High-performance, memory-efficient Fuzzy Search Engine tailored for mobile inventory.
 * Supports:
 * 1. Exact & substring matching
 * 2. Prefix & word-boundary matching
 * 3. Typo tolerance via Damerau-Levenshtein distance (insertions, deletions, substitutions, transpositions)
 *    e.g. "brke" -> "Brake Shoe", "cluch" -> "Clutch Plate", "c7re" -> "CR7E Spark Plug"
 * 4. Token-level matching across multi-word part names and part numbers
 */
object FuzzySearchEngine {

    data class ScoredItem<T>(
        val item: T,
        val score: Int
    )

    /**
     * Searches a collection of items, scoring each against the user's query.
     * Returns matching items sorted by highest relevance score.
     */
    fun <T> search(
        items: List<T>,
        query: String,
        nameExtractor: (T) -> String,
        partNumberExtractor: (T) -> String,
        maxEditDistance: Int = 2
    ): List<T> {
        val cleanQuery = query.trim().lowercase()
        if (cleanQuery.isEmpty()) return items

        val queryTokens = cleanQuery.split(Regex("[\\s\\-_/]+")).filter { it.isNotEmpty() }

        return items.mapNotNull { item ->
            val name = nameExtractor(item).lowercase()
            val partNum = partNumberExtractor(item).lowercase()
            val fullText = "$name $partNum"

            val score = calculateMatchScore(cleanQuery, queryTokens, name, partNum, fullText, maxEditDistance)
            if (score > 0) ScoredItem(item, score) else null
        }
        .sortedByDescending { it.score }
        .map { it.item }
    }

    private fun calculateMatchScore(
        cleanQuery: String,
        queryTokens: List<String>,
        name: String,
        partNum: String,
        fullText: String,
        maxEditDistance: Int
    ): Int {
        // 1. Exact full-phrase match (highest priority)
        if (name == cleanQuery || partNum == cleanQuery) return 1000
        if (partNum.startsWith(cleanQuery)) return 900
        if (name.startsWith(cleanQuery)) return 800

        // 2. Substring match
        if (fullText.contains(cleanQuery)) return 700

        // 3. Token-by-token matching (handles acronyms, word permutations, and typos)
        val targetTokens = fullText.split(Regex("[\\s\\-_/]+")).filter { it.isNotEmpty() }
        var totalScore = 0
        var matchedTokens = 0

        for (qToken in queryTokens) {
            var bestTokenScore = 0

            for (tToken in targetTokens) {
                // Exact token match
                if (tToken == qToken) {
                    bestTokenScore = maxOf(bestTokenScore, 500)
                    continue
                }
                // Prefix token match (typing mid-word)
                if (tToken.startsWith(qToken)) {
                    bestTokenScore = maxOf(bestTokenScore, 400 + (qToken.length * 10))
                    continue
                }
                // Substring token match
                if (tToken.contains(qToken)) {
                    bestTokenScore = maxOf(bestTokenScore, 300)
                    continue
                }

                // Typo match via Damerau-Levenshtein distance
                val distance = damerauLevenshteinDistance(qToken, tToken)
                val allowedDistance = when {
                    qToken.length <= 3 -> 1 // e.g. "brk" vs "brk"
                    qToken.length <= 6 -> min(2, maxEditDistance) // e.g. "brke" vs "brake" (dist = 1), "cluch" vs "clutch" (dist = 1)
                    else -> maxEditDistance
                }

                if (distance <= allowedDistance) {
                    // Score penalty proportional to edit distance
                    val score = 250 - (distance * 60)
                    bestTokenScore = maxOf(bestTokenScore, score)
                }
            }

            if (bestTokenScore > 0) {
                totalScore += bestTokenScore
                matchedTokens++
            }
        }

        // Only return a valid score if all query tokens matched something
        return if (matchedTokens == queryTokens.size) totalScore else 0
    }

    /**
     * Damerau-Levenshtein distance supporting:
     * - Deletion (e.g. "brke" -> "brake")
     * - Insertion (e.g. "brakke" -> "brake")
     * - Substitution (e.g. "bruke" -> "brake")
     * - Transposition of adjacent characters (e.g. "baer" -> "bear")
     */
    fun damerauLevenshteinDistance(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length

        // Quick bounds check
        if (kotlin.math.abs(len1 - len2) > 3) return 999

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1

                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // Deletion
                    dp[i][j - 1] + 1,      // Insertion
                    dp[i - 1][j - 1] + cost // Substitution
                )

                // Transposition
                if (i > 1 && j > 1 && s1[i - 1] == s2[j - 2] && s1[i - 2] == s2[j - 1]) {
                    dp[i][j] = minOf(dp[i][j], dp[i - 2][j - 2] + 1)
                }
            }
        }

        return dp[len1][len2]
    }
}
