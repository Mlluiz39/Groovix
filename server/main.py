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

import json
import os
import re
import threading
import time
from functools import lru_cache
from urllib.request import Request, urlopen

from fastapi import Body, FastAPI, Query
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

_HERE = os.path.dirname(os.path.abspath(__file__))
CONFIG_FILE = os.path.join(_HERE, "config.json")
LOG_DIR = os.path.join(_HERE, "logs")
FAIL_LOG = os.path.join(LOG_DIR, "failures.jsonl")

# Flags de estratégia (idéia do Muka: mudar comportamento do app SEM
# recompilar). O app lê isto em GET /config e aplica na hora.
DEFAULT_CONFIG: dict = {
    "version": 1,
    # falhas seguidas do InnerTube no CELULAR antes de pular direto pro servidor
    "ytFailLimit": 3,
    # checa a stream (Range 1 byte) antes de entregar — evita URL morta na fila
    "validateStream": True,
    # app pode POST /api/fail quando uma faixa falha
    "telemetry": True,
    # ordens de player_client em cascata quando um cliente leva 403
    "playerClientOrders": [
        ["android", "web", "ios"],
        ["web", "android", "ios"],
        ["ios", "web", "android"],
        ["tv", "web", "android"],
        # clientes embedded furam parte dos bloqueios de IP/rede
        ["web_embedded", "tv_embedded", "mweb"],
        ["tv_embedded", "web_embedded", "android"],
    ],
}

_config_state: dict = {"mtime": None, "value": dict(DEFAULT_CONFIG)}


def load_config() -> dict:
    """Lê config.json (hot-reload por mtime). Inválido/ausente => default."""
    try:
        mtime = os.path.getmtime(CONFIG_FILE)
    except OSError:
        return dict(DEFAULT_CONFIG)
    if _config_state["mtime"] == mtime:
        return _config_state["value"]
    cfg = dict(DEFAULT_CONFIG)
    try:
        with open(CONFIG_FILE, encoding="utf-8") as f:
            data = json.load(f)
        if isinstance(data, dict):
            if isinstance(data.get("ytFailLimit"), int):
                cfg["ytFailLimit"] = min(max(data["ytFailLimit"], 1), 20)
            if isinstance(data.get("validateStream"), bool):
                cfg["validateStream"] = data["validateStream"]
            if isinstance(data.get("telemetry"), bool):
                cfg["telemetry"] = data["telemetry"]
            orders = data.get("playerClientOrders")
            if isinstance(orders, list):
                clean = [
                    c
                    for c in orders
                    if isinstance(c, list)
                    and c
                    and all(isinstance(x, str) and x for x in c)
                ]
                if clean:
                    cfg["playerClientOrders"] = clean
    except Exception:  # noqa: BLE001 - config quebrado nunca derruba o server
        cfg = dict(DEFAULT_CONFIG)
    _config_state["mtime"] = mtime
    _config_state["value"] = cfg
    return cfg


def _env(name: str) -> str:
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


@app.get("/config")
def config():
    """(A) Config remota — flags de estratégia que o app aplica sem recompilar.

    Edite server/config.json ao lado do main.py (releitura automática).
    """
    return load_config()


def _clip(value, limit: int = 300) -> str:
    return str(value if value is not None else "")[:limit]


@app.post("/api/fail")
def report_fail(report: dict = Body(...)):
    """(B) Telemetria — o app avisa quando uma faixa falhou e por quê.

    Grava em logs/failures.jsonl (uma linha JSON por falha) pra a gente ver
    quando o YouTube muda a estratégia.
    """
    if not load_config().get("telemetry"):
        return {"ok": True, "stored": False}
    entry = {
        "ts": int(time.time()),
        "track": _clip(report.get("track")),
        "artist": _clip(report.get("artist")),
        "source": _clip(report.get("source")),
        "stage": _clip(report.get("stage")),
        "reason": _clip(report.get("reason")),
        "app": _clip(report.get("app") or "android"),
    }
    try:
        os.makedirs(LOG_DIR, exist_ok=True)
        with open(FAIL_LOG, "a", encoding="utf-8") as f:
            f.write(json.dumps(entry, ensure_ascii=False) + "\n")
    except OSError as e:
        return JSONResponse(status_code=500, content={"error": str(e)[:200]})
    return {"ok": True, "stored": True}


@app.get("/api/fail/recent")
def fail_recent(n: int = Query(20, ge=1, le=200)):
    """Últimas falhas reportadas (mais recente primeiro)."""
    try:
        with open(FAIL_LOG, encoding="utf-8") as f:
            lines = f.readlines()[-n:]
    except OSError:
        lines = []
    out = []
    for line in reversed(lines):
        try:
            out.append(json.loads(line))
        except ValueError:
            continue
    return {"failures": out}


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


_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Safari/537.36"


def _validate_stream(url: str) -> bool:
    """(C) A stream ainda responde? (stilo Muka: checa generate_204/Range
    antes de enfileirar). Pede 1 byte; 200/206 = viva, 403/404 = morta."""
    try:
        req = Request(url, headers={"Range": "bytes=0-0", "User-Agent": _UA})
        with urlopen(req, timeout=8) as resp:  # noqa: S310 - URL vem do yt-dlp
            return resp.status in (200, 206)
    except Exception:  # noqa: BLE001 - qualquer erro = stream não tocaria
        return False


@lru_cache(maxsize=512)
def _audio_cached(url: str) -> dict:
    import yt_dlp

    cfg = load_config()
    last_err = "unknown"
    # (D) tenta ordens de player_client diferentes (o YouTube bloqueia
    # por cliente/IP; o que falha no android passa no web e vice-versa)
    client_orders = cfg["playerClientOrders"]
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
        # (C) stream extraída mas morta? próxima ordem de cliente em vez de
        # devolver URL que o player vai estourar na cara do usuário.
        if cfg["validateStream"] and not _validate_stream(stream):
            last_err = f"stream morta com o cliente {clients[0]} (status != 200/206)"
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
