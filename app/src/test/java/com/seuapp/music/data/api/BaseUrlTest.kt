package com.seuapp.music.data.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BaseUrlTest {

    @Test
    fun `adiciona barra no final`() {
        assertEquals(
            "https://groovix-api-xxxxx-sa.a.run.app/",
            RetrofitClient.normalizeBaseUrl("https://groovix-api-xxxxx-sa.a.run.app")
        )
    }

    @Test
    fun `aceita sem esquema e adiciona https`() {
        assertEquals(
            "https://api-music.mlluizdevtech.qzz.io/",
            RetrofitClient.normalizeBaseUrl("api-music.mlluizdevtech.qzz.io")
        )
    }

    @Test
    fun `preserva http e porta`() {
        assertEquals(
            "http://192.168.1.5:8099/",
            RetrofitClient.normalizeBaseUrl("http://192.168.1.5:8099")
        )
    }

    @Test
    fun `aceita localhost com porta`() {
        assertEquals(
            "http://localhost:8099/",
            RetrofitClient.normalizeBaseUrl("http://localhost:8099/")
        )
    }

    @Test
    fun `normaliza espacos em branco`() {
        assertEquals(
            "https://meuservidor.com/",
            RetrofitClient.normalizeBaseUrl("   https://meuservidor.com  ")
        )
    }

    @Test
    fun `url padrao e idempotente`() {
        assertEquals(
            RetrofitClient.DEFAULT_BASE_URL,
            RetrofitClient.normalizeBaseUrl(RetrofitClient.DEFAULT_BASE_URL)
        )
    }

    @Test
    fun `rejeita urls invalidas`() {
        assertNull(RetrofitClient.normalizeBaseUrl(""))
        assertNull(RetrofitClient.normalizeBaseUrl("   "))
        assertNull(RetrofitClient.normalizeBaseUrl("sem-ponto"))
        assertNull(RetrofitClient.normalizeBaseUrl("ftp://x.com"))
        assertNull(RetrofitClient.normalizeBaseUrl("https://"))
    }
}
