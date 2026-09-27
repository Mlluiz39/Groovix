package com.seuapp.music.data.api

import com.google.gson.Gson
import com.seuapp.music.data.model.HealthResponse
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Os dois backends que o app aceita: FastAPI novo e Node antigo. */
class HealthResponseTest {
    private val gson = Gson()

    @Test
    fun `fastapi novo devolve ok=true e conta como saudavel`() {
        val h = gson.fromJson("""{"ok":true,"time":1790548767}""", HealthResponse::class.java)
        assertTrue(h.healthy)
    }

    @Test
    fun `node antigo devolve status=ok e tambem conta como saudavel`() {
        val h = gson.fromJson("""{"status":"ok"}""", HealthResponse::class.java)
        assertTrue(h.healthy)
    }

    @Test
    fun `resposta sem ok nenhum nao e saudavel`() {
        assertFalse(gson.fromJson("""{}""", HealthResponse::class.java).healthy)
        assertFalse(gson.fromJson("""{"ok":false}""", HealthResponse::class.java).healthy)
        assertFalse(gson.fromJson("""{"status":"degraded"}""", HealthResponse::class.java).healthy)
    }
}
