package com.cosmos.launcher

import org.junit.Assert.assertEquals
import org.junit.Test

class ClusterClassifierTest {

    @Test
    fun `known packages resolve through the explicit map`() {
        assertEquals("work", ClusterClassifier.classify("com.google.android.gm"))
        assertEquals("social", ClusterClassifier.classify("com.whatsapp"))
        assertEquals("creative", ClusterClassifier.classify("com.spotify.music"))
        assertEquals("utility", ClusterClassifier.classify("com.android.settings"))
        assertEquals("finance", ClusterClassifier.classify("com.revolut.revolutmoney"))
    }

    @Test
    fun `unknown packages fall back to keyword heuristics`() {
        assertEquals("work", ClusterClassifier.classify("com.acme.email.client"))
        assertEquals("work", ClusterClassifier.classify("com.foo.calendarpro"))
        assertEquals("social", ClusterClassifier.classify("com.acme.messenger"))
        assertEquals("creative", ClusterClassifier.classify("com.acme.musicplayer"))
        assertEquals("creative", ClusterClassifier.classify("com.acme.videoeditor"))
        assertEquals("finance", ClusterClassifier.classify("com.mybank.mobile"))
        assertEquals("finance", ClusterClassifier.classify("com.acme.cryptowallet"))
    }

    @Test
    fun `packages with no matching keyword default to utility`() {
        assertEquals("utility", ClusterClassifier.classify("com.example.unknownapp"))
        assertEquals("utility", ClusterClassifier.classify(""))
    }

    @Test
    fun `work keywords win when multiple categories could match`() {
        // "mail" is checked before "music" in the heuristic chain
        assertEquals("work", ClusterClassifier.guessCluster("com.mailmusic.app"))
    }
}
