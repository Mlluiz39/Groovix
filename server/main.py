"""
Groovix music-api — backend compatível com o app Android.

Equivalente funcional ao backend do Muka (api.ddsiiwid.com) no que
o app precisa: resolver busca + stream do YouTube no SERVIDOR
(onde dá pra gerenciar PO Token / IP / yt-dlp atualizado),
em vez de tentar no celular onde o youtubei bloqueia.

Contrato (idêntico ao Node music-api original):
  GET /api/search?q=...  -> {"results": [{id,title,channel,duration,thumbnail,url}]}
  GET /api/audio?url=... -> {"title",channel,thumbnail,streamUrl}
  GET /health             -> {"ok": true}
"""
from __future__ import annotations

import re
import threading
import time
from functools import lru_cache

from fastapi import FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

app = FastAPI(title="groovix-music-api")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

_YTDLP_LOCK = threading.Lock()


def _env(name: str) -> str:
    import os

    return (os.environ.get(name) or "").strip()


def _base_extractor_args():
    # PO Token via plugin bgutil (env PO_TOKEN_* se usar servidor externo),
    # + proxy residencial opcional SÓ pro YouTube (YOUTUBE_PROXY).
    args: dict = {
        "player_client": ["android", "web", "ios"],
    }
    return {"youtube": args}


def _ydl_search_opts():
    # ytsearch no servidor (IP residencial/VPS funciona melhor que datacenter
    # gigante; se este host for bloqueado, rode em casa via tunnel).
    opts: dict = {
        "quiet": True,
        "no_warnings": True,
        "noplaylist": True,
        "extract_flat": "in_playlist",
        "skip_download": True,
        # clientes na ordem que mais funciona em 2025/2026
        "extractor_args": _base_extractor_args(),
        "socket_timeout": 20,
    }
    proxy = _env("YOUTUBE_PROXY")
    if proxy:
        opts["proxy"] = proxy
    return opts


def _ydl_audio_opts():
    opts: dict = {
        "quiet": True,
        "no_warnings": True,
        "noplaylist": True,
        "skip_download": True,
        "format": "bestaudio[ext=m4a]/bestaudio/best[ext=m4a]/best",
        "extractor_args": _base_extractor_args(),
        "socket_timeout": 25,
    }
    proxy = _env("YOUTUBE_PROXY")
    if proxy:
        opts["proxy"] = proxy
    return opts


def _to_ms(duration) -> int | None:
    try:
        if duration is None:
            return None
        return int(float(duration) * 1000)
    except Exception:
        return None


@app.get("/health")
def health():
    return {"ok": True, "time": int(time.time())}


@app.get("/api/search")
def search(q: str = Query(..., min_length=1)):
    import yt_dlp

    with _YTDLP_LOCK:
        with yt_dlp.YoutubeDL(_ydl_search_opts()) as ydl:
            info = ydl.extract_info(f"ytsearch15:{q}", download=False)
    results = []
    for e in (info.get("entries") or [])[:15]:
        if not e:
            continue
        vid = e.get("id")
        if not vid:
            continue
        title = e.get("title") or vid
        channel = e.get("channel") or e.get("uploader") or ""
        thumb = e.get("thumbnail") or f"https://i.ytimg.com/vi/{vid}/hqdefault.jpg"
        results.append(
            {
                "id": vid,
                "title": title,
                "channel": channel,
                "duration": _to_ms(e.get("duration")),
                "thumbnail": thumb,
                "url": f"https://www.youtube.com/watch?v={vid}",
            }
        )
    return {"results": results}


@lru_cache(maxsize=512)
def _audio_cached(url: str) -> dict:
    import yt_dlp

    last_err = "unknown"
    # tenta ordens de player_client diferentes (o YouTube bloqueia
    # por cliente/IP; o que falha no android passa no web e vice-versa)
    client_orders = [
        ["android", "web", "ios"],
        ["web", "android", "ios"],
        ["ios", "web", "android"],
        ["tv", "web", "android"],
        # clientes embedded furam parte dos bloqueios de IP/rede
        ["web_embedded", "tv_embedded", "mweb"],
        ["tv_embedded", "web_embedded", "android"],
    ]
    for clients in client_orders:
        opts = _ydl_audio_opts()
        opts["extractor_args"] = {"youtube": {"player_client": clients}}
        try:
            with _YTDLP_LOCK:
                with yt_dlp.YoutubeDL(opts) as ydl:
                    info = ydl.extract_info(url, download=False)
        except Exception as e:  # noqa: BLE001 - vira 502 com motivo, nunca 500
            last_err = f"{type(e).__name__}: {str(e)[:300]}"
            continue
        if info is None:
            last_err = "empty info"
            continue
        stream = info.get("url")
        if not stream:
            # pega melhor formato com url
            for f in info.get("formats") or []:
                if f.get("url") and (f.get("acodec") not in (None, "none")):
                    stream = f["url"]
                    break
            if not stream:
                for f in info.get("formats") or []:
                    if f.get("url"):
                        stream = f["url"]
                        break
        if not stream:
            last_err = "no url in formats"
            continue
        vid = info.get("id") or ""
        return {
            "title": info.get("title"),
            "channel": info.get("channel") or info.get("uploader"),
            "thumbnail": info.get("thumbnail")
            or (f"https://i.ytimg.com/vi/{vid}/hqdefault.jpg" if vid else None),
            "streamUrl": stream,
        }
    return {"_error": last_err}


@app.get("/api/audio")
def audio(url: str = Query(..., min_length=1)):
    # aceita id puro também
    if re.fullmatch(r"[A-Za-z0-9_-]{11}", url):
        url = f"https://www.youtube.com/watch?v={url}"
    try:
        data = _audio_cached(url)
    except Exception as e:  # noqa: BLE001 - nunca 500 sem motivo
        return JSONResponse(
            status_code=502,
            content={"error": f"extract crash: {type(e).__name__}: {str(e)[:300]}"},
        )
    if not data.get("streamUrl"):
        _audio_cached.cache_clear()
        return JSONResponse(
            status_code=502,
            content={
                "error": "no stream (youtube bloqueou este IP; rode o server em casa)",
                "detail": data.get("_error", "")[:300],
            },
        )
    return data
