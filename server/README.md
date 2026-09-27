# Groovix music-api (substituto do Node, mesmo contrato)

Backend no estilo do Muka (`api.ddsiiwid.com`): o celular pede,
o **servidor** resolve o YouTube com yt-dlp atualizado e devolve
a URL direta do `googlevideo.com`. O app Android já fala com ele
sem nenhuma mudança (`GET /api/search`, `GET /api/audio`).

## Rodar local

```bash
cd server
python3 -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8080
# teste:
curl "http://localhost:8080/api/search?q=brasil" | head -c 400
curl "http://localhost:8080/api/audio?url=https://www.youtube.com/watch?v=dQw4w9WgXcQ" | head -c 400
```

## Config remota + telemetria (estilo Muka)

Endpoints novos que deixam o app mudar de estratégia **sem recompilar**:

```bash
# (A) flags de estratégia — app busca no start e em "Salvar e testar"
curl http://localhost:8080/config
# {"version":1,"ytFailLimit":3,"validateStream":true,"telemetry":true,
#  "playerClientOrders":[["android","web","ios"], ...]}

# (B) telemetria — app relata quando uma faixa falhou
curl -X POST http://localhost:8080/api/fail -H 'Content-Type: application/json' \
  -d '{"track":"Shape of You","artist":"Ed Sheeran","source":"youtube",
       "stage":"resolve","reason":"nenhuma fonte resolveu a stream"}'
# leitura (mais recente primeiro):
curl "http://localhost:8080/api/fail/recent?n=20"
```

No `/api/audio` acontecem mais duas coisas:

- **(C) validação de stream:** antes de devolver, o servidor pede 1 byte
  (`Range: bytes=0-0`); status 200/206 = viva, 403/404 = morta e ele tenta o
  próximo cliente (nunca enfileira URL que o player vai estourar).
- **(D) rotação de cliente:** as ordens de `player_client` vêm da config
  (`playerClientOrders`) e são tentadas em cascata.

Pra mudar sem reiniciar, crie `server/config.json` ao lado do `main.py`
(leitura automática por mtime; arquivo ausente/quebrado = defaults):

```json
{
  "ytFailLimit": 7,
  "validateStream": true,
  "telemetry": true,
  "playerClientOrders": [["tv", "web", "android"], ["android", "web", "ios"]]
}
```

Relatos ficam em `server/logs/failures.jsonl` (uma linha JSON por ocorrência).

## Deploy na VPS + tunnel qzz.io (feito em 27/09/2026)

A VPS `integrator` (`184.107.88.151`, alias SSH `integrator`) hospeda o tunnel
`api-music` **e agora também o FastAPI** — o domínio
`https://api-music.mlluizdevtech.qzz.io` já responde com `/health` `{"ok":true,...}`
e todos os endpoints novos.

```bash
# subir/atualizar o código (de qualquer máquina com SSH configurado)
rsync -av --exclude '.venv' --exclude '__pycache__' --exclude 'logs' \
  --exclude 'config.json' server/ integrator:/opt/groovix-api/

# na VPS
ssh integrator
cd /opt/groovix-api
python3 -m venv .venv && .venv/bin/pip install -r requirements.txt
systemctl restart groovix-api        # systemd: porta 8099, só localhost
```

- **Origem do tunnel:** `/root/.cloudflared/api-music.yml` →
  `service: http://localhost:8099` (service `cloudflared@api-music`)
- **Rollback:** o Node antigo continua rodando em `localhost:8081`
  (`/root/projects/music-api-node/src/server.js`) — basta voltar o ingress
  (cópia em `api-music.yml.bak-8081`) e `systemctl restart cloudflared@api-music`
- **Config remota:** crie `/opt/groovix-api/config.json` (hot-reload, sem reiniciar)
- **Telemetria:** `/opt/groovix-api/logs/failures.jsonl`
- A VPS tem Node v22 → plugin PO Token (`bgutil-ytdlp-pot-provider`) funciona

## Docker

```bash
cd server
docker build -t groovix-api .
docker run --rm -p 8080:8080 groovix-api
```

## Publicar (igual ao Muka)

O app aponta para `https://api-music.mlluizdevtech.qzz.io`
(hoje erro 1033 = tunnel fora do ar).

### Checklist pra religar o tunnel (erro Cloudflare 1033)

1033 = o Cloudflare não alcança a origem (tunnel desconectado ou
máquina apagada). Na máquina que rodava o backend:

```bash
# 1) sobe a API
cd server
docker build -t groovix-api . && docker run -d -p 8080:8080 groovix-api
# (sem docker: .venv/bin/uvicorn main:app --host 0.0.0.0 --port 8080)

# 2) religa o tunnel (o DNS do qzz.io aponta pra ele)
cloudflared tunnel run NOME_DO_TUNNEL
# se era tunnel rápido (trycloudflare), o de antes morreu:
# cloudflared tunnel --url http://localhost:8080
#   -> gera URL nova, atualize no app (Configurações -> Servidor)
```

Verificação, de qualquer lugar:

```bash
curl https://api-music.mlluizdevtech.qzz.io/health   # deve responder {"ok":true,...}
```

> **O app agora tem Configurações → Servidor (Backend):** dá pra trocar
> a URL do backend por lá, com botão "Salvar e testar" (chama `/health`)
> — sem reinstalar o APK.

Teste rápido desta máquina (sem Docker):

```bash
cd server
python3 -m venv --without-pip .venv && curl -sSL https://bootstrap.pypa.io/get-pip.py | .venv/bin/python
.venv/bin/pip install -r requirements.txt
.venv/bin/uvicorn main:app --host 0.0.0.0 --port 8099
# app: http://<IP_DA_MAQUINA>:8099 em Configurações -> Servidor
```

### Opção A — nuvem grátis, PC desligado (recomendado)

O `requirements.txt` já inclui o provedor de **PO Token**
(`bgutil-ytdlp-pot-provider` + node no Dockerfile). Ele gera o
token BotGuard no próprio servidor, então IP de datacenter
passa na maioria dos casos — sem PC ligado.

**Google Cloud Run (R$0 dentro do free tier, 2M req/mês):**

```bash
cd server
gcloud auth login
gcloud config set project SEU_PROJECT_ID
gcloud run deploy groovix-api \
  --source . \
  --region southamerica-east1 \
  --allow-unauthenticated \
  --memory 1Gi \
  --timeout 60
# URL sai no final: https://groovix-api-xxxxx-sa.a.run.app
```

Troque o `BASE_URL` no app por essa URL (terminando com `/`),
gere o APK e pronto. Se der `502 no stream`, adicione proxy:

```bash
gcloud run services update groovix-api \
  --region southamerica-east1 \
  --update-env-vars YOUTUBE_PROXY="http://user:pass@host:port"
```

**Render (alternativa, `render.yaml` já incluído):** conecte o
repo e faça deploy automático. Mesma lógica do proxy acima.

### Opção B — VPS barato + proxy residencial (mais estável)

YouTube bloqueia IP de datacenter com mais força. O padrão que
funciona 24/7 sem PC ligado:

1. VPS de ~$5 (Hetzner, Oracle Free, etc.) com este server via Docker
2. Proxy **residencial** barato (~$5/mês, ex. WebShare/NSocks) e:
   ```bash
   docker run -e YOUTUBE_PROXY="http://user:pass@host:port" -p 8080:8080 groovix-api
   ```
   Só o tráfego do YouTube passa pelo proxy; o resto é direto.

### Opção C — casa, mas sem o PC principal

Troque o PC por algo de baixo consumo ligado 24/7:
Raspberry Pi (~5W) ou mini-PC rodando o mesmo Docker + tunnel:

```bash
cloudflared tunnel --url http://localhost:8080
```

Se mudar de domínio, troque `BASE_URL` em
`app/src/main/java/com/seuapp/music/data/api/RetrofitClient.kt`
(deve terminar com `/`).

## Notas

- yt-dlp quebra quando o YouTube muda o player: `pip install -U yt-dlp`
  e restart. O Muka resolve isso com dex/JS hotfix (`api.ddsiiwid.com`);
  aqui basta atualizar o pacote.
- Se der `502 no stream`, o IP do servidor foi bloqueado: rode o
  server na sua casa (IP residencial) em vez de VPS/datacenter.
- **Cold start (Render free / Cloud Run):** a 1ª busca do dia pode
  demorar até 1 min e o app mostra "servidor acordando — busque de
  novo". O app tenta 2x sozinho (timeout 60s) e nunca crasha.
  Pra evitar acordar: crie um monitor grátis no UptimeRobot
  (intervalo 5 min) apontando pra `https://SEU-SERVER/health` —
  mantém o container aquecido de graça.
