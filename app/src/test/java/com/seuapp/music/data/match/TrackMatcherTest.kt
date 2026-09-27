package com.seuapp.music.data.match

import com.seuapp.music.data.model.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackMatcherTest {

    private fun yt(title: String, channel: String, id: String = "vid${title.hashCode()}") =
        Track(id = id, title = title, channel = channel, url = "https://www.youtube.com/watch?v=$id")

    private fun sc(title: String, channel: String, id: String = "1${title.hashCode()}") =
        Track(id = "soundcloud:$id", title = title, channel = channel, url = "https://soundcloud.com/a/b")

    private fun audius(title: String, channel: String, id: String = "2${title.hashCode()}") =
        Track(id = "audius:$id", title = title, channel = channel, url = "https://audius.co/x")

    @Test
    fun `similaridade - titulo exato com artista vale 140`() {
        val r = TrackMatcher.similarity(yt("Shape of You", "Ed Sheeran"), "Shape of You", "Ed Sheeran")
        assertEquals(140, r)
    }

    @Test
    fun `similaridade - titulo que contem vale 60 e artista soma 40`() {
        val r = TrackMatcher.similarity(
            sc("Ed Sheeran - Shape of You (Official)", "djbedroom"), "Shape of You", "Ed Sheeran"
        )
        assertEquals(100, r)
    }

    @Test
    fun `similaridade - sem match vale zero`() {
        val r = TrackMatcher.similarity(yt("Outra Música", "Banda"), "Shape of You", "Ed Sheeran")
        assertEquals(0, r)
    }

    @Test
    fun `pickBest - prefere fonte que toca quando o match e bom`() {
        // YouTube tem match perfeito (140), SoundCloud tem match bom (100)
        val results = listOf(
            yt("Shape of You", "Ed Sheeran"),
            sc("Ed Sheeran - Shape of You", "coversBR")
        )
        val pick = TrackMatcher.pickBest(results, "Shape of You", "Ed Sheeran")

        assertTrue(!TrackMatcher.isYoutube(pick!!))
    }

    @Test
    fun `pickBest - mantem YouTube quando so ele tem match bom`() {
        val results = listOf(
            yt("Shape of You", "Ed Sheeran"),
            sc("DJ Mix 2004", "qualquercoisa") // similaridade 0
        )
        val pick = TrackMatcher.pickBest(results, "Shape of You", "Ed Sheeran")

        assertTrue(TrackMatcher.isYoutube(pick!!))
    }

    @Test
    fun `pickBest - prefere Audius tambem`() {
        val results = listOf(
            yt("Levitating", "Dua Lipa"),
            audius("Dua Lipa - Levitating", "remixzone")
        )
        val pick = TrackMatcher.pickBest(results, "Levitating", "Dua Lipa")

        assertTrue(pick!!.id.startsWith("audius:"))
    }

    @Test
    fun `pickBest - vazio retorna null`() {
        assertNull(TrackMatcher.pickBest(emptyList(), "Song", "Artist"))
    }

    @Test
    fun `pickBest - nada parecido fica com o melhor mesmo`() {
        val results = listOf(yt("Qualquer coisa", "X"))
        val pick = TrackMatcher.pickBest(results, "Song", "Artist")

        assertEquals(results[0].id, pick!!.id)
    }

    @Test
    fun `alternative - retorna versao que toca quando original e YouTube bloqueado`() {
        val original = yt("Shape of You", "Ed Sheeran")
        val results = listOf(
            yt("Shape of You", "Ed Sheeran", id = "outrovideo"),
            sc("Ed Sheeran - Shape of You", "coversBR")
        )
        val alt = TrackMatcher.pickAlternative(results, original, "Shape of You", "Ed Sheeran")

        assertTrue(alt != null && !TrackMatcher.isYoutube(alt))
    }

    @Test
    fun `alternative - descarta a propria faixa`() {
        val original = sc("Ed Sheeran - Shape of You", "cbr")
        val results = listOf(original)
        assertNull(TrackMatcher.pickAlternative(results, original, "Shape of You", "Ed Sheeran"))
    }

    @Test
    fun `alternative - so YouTube na lista e original YouTube bloqueado retorna null`() {
        val original = yt("Shape of You", "Ed Sheeran")
        val results = listOf(yt("Shape of You", "Ed Sheeran", id = "outro"))
        assertNull(TrackMatcher.pickAlternative(results, original, "Shape of You", "Ed Sheeran"))
    }

    @Test
    fun `alternative - original nao-YouTube pode cair pra outro YouTube`() {
        val original = sc("Ed Sheeran - Shape of You", "cbr")
        val results = listOf(yt("Shape of You", "Ed Sheeran"))
        val alt = TrackMatcher.pickAlternative(results, original, "Shape of You", "Ed Sheeran")

        assertEquals(results[0].id, alt!!.id)
    }

    @Test
    fun `alternative - exige match bom (nao troca por musica errada)`() {
        val original = yt("Shape of You", "Ed Sheeran")
        val results = listOf(sc("DJ Mix 2004", "outro"))
        assertNull(TrackMatcher.pickAlternative(results, original, "Shape of You", "Ed Sheeran"))
    }

    @Test
    fun `isYoutube detecta youtube e youtu be`() {
        assertTrue(TrackMatcher.isYoutube(yt("a", "b")))
        assertTrue(TrackMatcher.isYoutube(
            Track(id = "x", title = "t", channel = "c", url = "https://youtu.be/abcdefghijk")
        ))
        assertFalse(TrackMatcher.isYoutube(sc("t", "c")))
        assertFalse(TrackMatcher.isYoutube(audius("t", "c")))
    }
}
