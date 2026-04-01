package com.synapse.social.studioasinc.shared.core.util

import kotlin.test.Test
import kotlin.test.assertEquals

class SearchUtilTest {

    @Test
    fun testSanitizeSearchQuery_trimsWhitespace() {
        val input = "  search query  "
        val expected = "search query"
        assertEquals(expected, sanitizeSearchQuery(input))
    }

    @Test
    fun testSanitizeSearchQuery_limitsLengthTo100() {
        val input = "a".repeat(150)
        val expected = "a".repeat(100)
        assertEquals(expected, sanitizeSearchQuery(input))
    }

    @Test
    fun testSanitizeSearchQuery_escapesBackslash() {
        val input = "path\\to\\file"
        val expected = "path\\\\to\\\\file"
        assertEquals(expected, sanitizeSearchQuery(input))
    }

    @Test
    fun testSanitizeSearchQuery_escapesPercent() {
        val input = "100%"
        val expected = "100\\%"
        assertEquals(expected, sanitizeSearchQuery(input))
    }

    @Test
    fun testSanitizeSearchQuery_escapesUnderscore() {
        val input = "user_name"
        val expected = "user\\_name"
        assertEquals(expected, sanitizeSearchQuery(input))
    }

    @Test
    fun testSanitizeSearchQuery_complexQuery() {
        val input = "  %_\\  "
        val expected = "\\%\\_\\\\"
        assertEquals(expected, sanitizeSearchQuery(input))
    }
}
