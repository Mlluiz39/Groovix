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

## Docker

```bash
cd server
docker build -t groovix-api .
docker run --rm -p 8080:8080 groovix-api
```

## Publicar (igual ao Muka)

O app aponta para `https://api-music.mlluizdevtech.qzz.io`
(hoje erro 1033 = tunnel fora do ar).

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
