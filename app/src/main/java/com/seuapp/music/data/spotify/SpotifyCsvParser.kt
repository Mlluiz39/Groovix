package com.seuapp.music.data.spotify

import java.text.Normalizer

/**
 * Uma música lida do CSV exportado do Spotify (Exportify, Stats.fm, etc).
 * [query] é o que a gente manda pro Groovix buscar (SoundCloud + Audius + YouTube).
 */
data class SpotifySong(
    val title: String,
    val artist: String
) {
    val query: String get() = if (artist.isBlank()) title else "$title $artist"
}

/**
 * Parser flexível de CSV de backup de playlist do Spotify.
 *
 * Aceita:
 * - Delimitador `,`, `;` ou tab (detectado sozinho — cobre Excel em pt-BR)
 * - Aspas duplas com vírgula dentro e "" escapado
 * - Cabeçalhos em inglês ("Track Name", "Artist Name(s)") e em português
 *   ("Nome da faixa", "Artista") — detecção por normalização de acentos/caixa
 * - CSV sem cabeçalho (assume 1ª coluna = faixa, 2ª = artista)
 */
object SpotifyCsvParser {

    private const val TITLE_EXACT = 100
    private const val TITLE_STRONG = 90
    private const val TITLE_OK = 60
    private const val TITLE_WEAK = 40

    fun parse(csv: String): List<SpotifySong> {
        val text = csv.removePrefix("\uFEFF")
        if (text.isBlank()) return emptyList()

        val delimiter = detectDelimiter(text)
        val rows = parseRows(text, delimiter)
        if (rows.isEmpty()) return emptyList()

        val header = rows.first()
        val (titleCol, artistCol) = detectColumns(header)

        val dataRows = if (titleCol >= 0) rows.drop(1) else rows
        val tCol = if (titleCol >= 0) titleCol else 0
        val aCol = if (titleCol >= 0) artistCol else 1

        val seen = LinkedHashSet<String>()
        val songs = mutableListOf<SpotifySong>()
        for (row in dataRows) {
            val title = row.getOrNull(tCol)?.trim().orEmpty()
            val artist = if (aCol >= 0) row.getOrNull(aCol)?.trim().orEmpty() else ""
            if (!isValidTitle(title)) continue
            val key = "${norm(title)}|${norm(artist)}"
            if (seen.add(key)) songs.add(SpotifySong(title, artist))
        }
        return songs
    }

    // ---- detecção de cabeçalho/colunas ----

    /** Retorna (coluna da faixa, coluna do artista). (-1, -1) se não achar cabeçalho. */
    private fun detectColumns(header: List<String>): Pair<Int, Int> {
        var titleCol = -1
        var titleScore = 0
        var artistCol = -1
        var artistScore = 0

        header.forEachIndexed { i, cell ->
            val n = norm(cell)
            if (n.isEmpty()) return@forEachIndexed
            val t = titleScore(n)
            if (t > titleScore) { titleScore = t; titleCol = i }
            val a = artistScore(n)
            if (a > artistScore) { artistScore = a; artistCol = i }
        }

        // Precisa achar faixa e artista com folga mínima pra não confundir com dados
        if (titleScore < TITLE_OK) return Pair(-1, -1)
        if (artistCol == titleCol) artistCol = -1
        return Pair(titleCol, artistCol)
    }

    private fun titleScore(n: String): Int {
        // Colunas que nunca são a faixa
        val blocked = listOf("uri", "url", "link", "album", "duration", "dura", "explicit",
            "added", "data", "date", "playlist", "popularity", "rank", "ranking", "image",
            "cover", "preview", "tipo", "type", "id", "isrc")
        if (blocked.any { n == it || n.contains(it) }) return 0
        return when {
            n in setOf("track name", "track title", "song name", "song title", "nome da faixa",
                "nome da musica", "titulo da faixa", "nome da música") -> TITLE_STRONG
            n in setOf("title", "título", "titulo", "track", "faixa", "música", "musica", "song") -> TITLE_EXACT
            n.contains("track name") || n.contains("song title") -> TITLE_STRONG
            n.contains("faixa") || n.contains("música") || n.contains("musica") -> TITLE_OK
            n == "name" || n == "nome" -> TITLE_OK
            n.contains("track") || n.contains("title") -> TITLE_OK
            else -> 0
        }
    }

    private fun artistScore(n: String): Int {
        if (n.contains("uri") || n.contains("url")) return 0
        return when {
            n in setOf("artist", "artists", "artista", "artistas") -> TITLE_EXACT
            n.contains("artist") || n.contains("artista") -> TITLE_STRONG
            n == "band" || n == "banda" -> TITLE_OK
            else -> 0
        }
    }

    /** Rejeita lixo: URLs, URIs do Spotify e cabeçalhos que sobraram. */
    private fun isValidTitle(title: String): Boolean {
        if (title.isBlank()) return false
        val n = norm(title)
        if (n.startsWith("http") || n.startsWith("www.")) return false
        if (title.startsWith("spotify:")) return false
        return titleScore(n) < TITLE_STRONG // cabeçalho perdido não vira "música"
    }

    // ---- parsing ----

    private fun detectDelimiter(text: String): Char {
        val firstLine = text.lineSequence().firstOrNull { it.isNotBlank() } ?: return ','
        return listOf(',', ';', '\t')
            .maxByOrNull { countOutsideQuotes(firstLine, it) } ?: ','
    }

    private fun countOutsideQuotes(line: String, delimiter: Char): Int {
        var count = 0
        var inQuotes = false
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == delimiter && !inQuotes -> count++
            }
        }
        return count
    }

    private fun parseRows(text: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes && c == '"' -> {
                    if (i + 1 < text.length && text[i + 1] == '"') { cell.append('"'); i++ }
                    else inQuotes = false
                }
                !inQuotes && c == '"' -> inQuotes = true
                !inQuotes && c == delimiter -> { row.add(cell.toString()); cell.setLength(0) }
                !inQuotes && (c == '\n' || c == '\r') -> {
                    if (c == '\r' && i + 1 < text.length && text[i + 1] == '\n') i++
                    row.add(cell.toString()); cell.setLength(0)
                    if (row.any { it.isNotBlank() }) rows.add(row.toList())
                    row.clear()
                }
                else -> cell.append(c)
            }
            i++
        }
        row.add(cell.toString())
        if (row.any { it.isNotBlank() }) rows.add(row.toList())
        return rows
    }

    private fun norm(s: String): String =
        Normalizer.normalize(s.trim().lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("\\s+"), " ")
}
