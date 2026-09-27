package com.seuapp.music.data.match

import com.seuapp.music.data.model.Track

/**
 * Escolhe a melhor faixa entre resultados de fontes diferentes
 * (SoundCloud, Audius e YouTube), priorizando versões que realmente tocam.
 *
 * Contexto: o YouTube bloqueia o IP do celular na resolução do stream
 * (InnerTube -> UNPLAYABLE), então resultado do YouTube pode não tocar.
 * SoundCloud e Audius entregam o stream direto, sem passar pelo YouTube.
 */
object TrackMatcher {

    /** Similaridade mínima pra considerar que é a mesma música. */
    const val GOOD_MATCH = 60

    /** Quanto a versão que toca pode ser "pior" que a melhor e ainda assim vencer. */
    const val TOLERANCE = 60

    /** true = YouTube (pode estar bloqueado por IP); false = fonte que toca direto. */
    fun isYoutube(track: Track): Boolean =
        track.url.contains("youtube.com") || track.url.contains("youtu.be")

    /** 0..140: 100 = mesmo título, 60 = título contém, +40 = artista bate. */
    fun similarity(result: Track, title: String, artist: String): Int {
        val rt = key(result.title)
        val rc = key(result.channel)
        val t = key(title)
        val a = key(artist)
        var score = 0
        if (t.isNotEmpty()) {
            when {
                rt == t -> score += 100
                rt.isNotEmpty() && (rt.contains(t) || t.contains(rt)) -> score += 60
            }
        }
        if (a.isNotEmpty() && (rc.contains(a) || rt.contains(a))) score += 40
        return score
    }

    /**
     * Melhor resultado pra salvar/importar:
     * - entre resultados bons (>= [GOOD_MATCH]), prefere fonte que toca direto;
     * - YouTube só vence se a alternativa for muito pior ([TOLERANCE]) ou inexistente.
     */
    fun pickBest(results: List<Track>, title: String, artist: String): Track? {
        if (results.isEmpty()) return null
        val scored = results.map { it to similarity(it, title, artist) }
        val bestScore = scored.maxOf { it.second }
        val overall = scored.maxByOrNull { it.second }?.first ?: return null
        // Nada com cara da música certa: fica com o melhor mesmo
        if (bestScore < GOOD_MATCH) return overall

        val direct = scored.filter { !isYoutube(it.first) }.maxByOrNull { it.second }
        if (direct == null || direct.second < GOOD_MATCH) return overall
        return if (direct.second >= bestScore - TOLERANCE) direct.first else overall
    }

    /**
     * Alternativa quando a faixa atual não conseguiu stream (ex.: YouTube
     * bloqueou o IP). Só retorna versão que toca:
     * 1) SoundCloud/Audius com boa similaridade;
     * 2) outro resultado YouTube apenas se o original não era YouTube
     *    (a falha pode ser específica daquele vídeo, não de IP).
     */
    fun pickAlternative(
        results: List<Track>,
        original: Track,
        title: String,
        artist: String
    ): Track? {
        val good = results
            .filter { it.id != original.id }
            .map { it to similarity(it, title, artist) }
            .filter { it.second >= GOOD_MATCH }

        good.filter { !isYoutube(it.first) }.maxByOrNull { it.second }?.let { return it.first }
        if (!isYoutube(original)) good.maxByOrNull { it.second }?.let { return it.first }
        return null
    }

    private fun key(s: String): String =
        java.text.Normalizer.normalize(s.lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[^a-z0-9 ]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
