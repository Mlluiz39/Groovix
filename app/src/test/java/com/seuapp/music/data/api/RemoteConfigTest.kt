package com.seuapp.music.data.api

import com.google.gson.Gson
import com.seuapp.music.data.model.FailReport
import com.seuapp.music.data.model.RemoteConfigResponse
import com.seuapp.music.data.repository.PlayerRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Flags remotas do backend (GET /config) e telemetria (POST /api/fail).
 * Regra: backend antigo sem /config ou campo ausente => default do app vale.
 */
class RemoteConfigTest {
    private val gson = Gson()

    @Test
    fun `config completo parseia camelCase igual o servidor responde`() {
        val cfg = gson.fromJson(
            """
            {"version":2,"ytFailLimit":5,"validateStream":true,"telemetry":false}
            """.trimIndent(),
            RemoteConfigResponse::class.java
        )
        assertEquals(2, cfg.version)
        assertEquals(5, cfg.ytFailLimit)
        assertEquals(true, cfg.validateStream)
        assertEquals(false, cfg.telemetry)
    }

    @Test
    fun `campos ausentes ficam null pra app manter os defaults`() {
        val cfg = gson.fromJson("{}", RemoteConfigResponse::class.java)
        assertNull(cfg.version)
        assertNull(cfg.ytFailLimit)
        assertNull(cfg.validateStream)
        assertNull(cfg.telemetry)
    }

    @Test
    fun `campo desconhecido do servidor e ignorado`() {
        val cfg = gson.fromJson(
            """{"version":1,"novaFlagDoFuturo":"x","ytFailLimit":2}""",
            RemoteConfigResponse::class.java
        )
        assertEquals(2, cfg.ytFailLimit)
    }

    @Test
    fun `limite do youtube e limitado a faixa segura`() {
        assertEquals(3, PlayerRepository.clampYtFailLimit(3))
        assertEquals(1, PlayerRepository.clampYtFailLimit(0))
        assertEquals(1, PlayerRepository.clampYtFailLimit(-7))
        assertEquals(20, PlayerRepository.clampYtFailLimit(999))
    }

    @Test
    fun `relato de falha serializa os campos que o servidor grava no log`() {
        val json = gson.toJson(
            FailReport(
                track = "Shape of You",
                artist = "Ed Sheeran",
                source = "youtube",
                stage = "resolve",
                reason = "nenhuma fonte resolveu a stream"
            )
        )
        assertTrue(json.contains("\"track\":\"Shape of You\""))
        assertTrue(json.contains("\"source\":\"youtube\""))
        assertTrue(json.contains("\"stage\":\"resolve\""))
        assertTrue(json.contains("\"reason\":\"nenhuma fonte resolveu a stream\""))
        assertFalse(json.contains("null"))
    }

    @Test
    fun `app por padrao assume telemetria e validacao ligadas`() {
        // null = servidor não disse nada => app segue com ligado (comportamento
        // esperado por quem lê `!= false` no MusicViewModel)
        val cfg = gson.fromJson("{}", RemoteConfigResponse::class.java)
        assertTrue(cfg.telemetry != false)
        assertTrue(cfg.validateStream != false)
    }
}
