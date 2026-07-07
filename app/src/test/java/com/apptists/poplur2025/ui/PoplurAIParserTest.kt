package com.apptists.poplur2025.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PoplurAIParserTest {

    @Test
    fun `wake phrase requires hey poplur`() {
        assertNotNull(PoplurAIParser.wakePhraseMatch(PoplurAIParser.normalize("Hey Poplur")))
        assertNotNull(PoplurAIParser.wakePhraseMatch(PoplurAIParser.normalize("Hay Poplur")))
    }

    @Test
    fun `wake phrase ignores poplur-like words without hey`() {
        assertNull(PoplurAIParser.wakePhraseMatch(PoplurAIParser.normalize("Poplur")))
        assertNull(PoplurAIParser.wakePhraseMatch(PoplurAIParser.normalize("popular")))
        assertNull(PoplurAIParser.wakePhraseMatch(PoplurAIParser.normalize("poplar")))
        assertNull(PoplurAIParser.wakePhraseMatch(PoplurAIParser.normalize("okay poplur")))
    }

    @Test
    fun `wake phrase range leaves following command text`() {
        val normalized = PoplurAIParser.normalize("Hey Poplur open camera")
        val wakeRange = PoplurAIParser.wakePhraseMatch(normalized)

        assertNotNull(wakeRange)
        assertEquals("open camera", normalized.substring(wakeRange!!.last + 1).trim())
    }
}
