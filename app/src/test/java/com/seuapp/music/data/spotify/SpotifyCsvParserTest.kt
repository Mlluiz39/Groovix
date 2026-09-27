package com.seuapp.music.data.spotify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyCsvParserTest {

    @Test
    fun `csv exportify com aspas e coluna URI`() {
        val csv = """
            Artist Name(s),Track Name,Album Name(s),Spotify URI,Preview URL,Explicit
            "Artist 1, Featuring X",My Song,My Album,spotify:track:abc,https://example.com,FALSE
            Solo Artist,Another Song,Other Album,spotify:track:def,https://example.com,FALSE
        """.trimIndent()

        val songs = SpotifyCsvParser.parse(csv)

        assertEquals(2, songs.size)
        assertEquals("My Song", songs[0].title)
        assertEquals("Artist 1, Featuring X", songs[0].artist)
        assertEquals("My Song Artist 1, Featuring X", songs[0].query)
        assertEquals("Another Song", songs[1].title)
        assertEquals("Solo Artist", songs[1].artist)
    }

    @Test
    fun `csv em portugues com ponto e virgula`() {
        val csv = """
            Nome da faixa;Artista;Álbum
            Música Boa;Banda Tal;Disco Um
            Noite Inteira;Cantora X;Disco Dois
        """.trimIndent()

        val songs = SpotifyCsvParser.parse(csv)

        assertEquals(2, songs.size)
        assertEquals("Música Boa", songs[0].title)
        assertEquals("Banda Tal", songs[0].artist)
        assertEquals("Noite Inteira", songs[1].title)
    }

    @Test
    fun `csv sem cabecalho assume faixa e artista`() {
        val csv = """
            Song A,Artist A
            Song B,Artist B
        """.trimIndent()

        val songs = SpotifyCsvParser.parse(csv)

        assertEquals(2, songs.size)
        assertEquals("Song A", songs[0].title)
        assertEquals("Artist A", songs[0].artist)
    }

    @Test
    fun `remove duplicatas e ignora linhas invalidas`() {
        val csv = """
            Track Name,Artist Name
            Duplicate Song,Some Artist
            Duplicate Song,Some Artist
            https://open.spotify.com/track/123,Someone
            ,Sem Titulo
            spotify:track:xyz,Someone
            Unique Song,Other Artist
        """.trimIndent()

        val songs = SpotifyCsvParser.parse(csv)

        assertEquals(2, songs.size)
        assertEquals("Duplicate Song", songs[0].title)
        assertEquals("Unique Song", songs[1].title)
    }

    @Test
    fun `cabecalho em ingles generico Title`() {
        val csv = """
            Title,Artist
            Hello,Adelle
        """.trimIndent()

        val songs = SpotifyCsvParser.parse(csv)

        assertEquals(1, songs.size)
        assertEquals("Hello", songs[0].title)
        assertEquals("Adelle", songs[0].artist)
    }

    @Test
    fun `csv vazio retorna lista vazia`() {
        assertTrue(SpotifyCsvParser.parse("").isEmpty())
        assertTrue(SpotifyCsvParser.parse("   \n  ").isEmpty())
    }

    @Test
    fun `tab delimited e BOM sao aceitos`() {
        val csv = "\uFEFFTrack Name\tArtist Name\nTab Song\tTab Artist\n"
        val songs = SpotifyCsvParser.parse(csv)

        assertEquals(1, songs.size)
        assertEquals("Tab Song", songs[0].title)
        assertEquals("Tab Artist", songs[0].artist)
    }
}
