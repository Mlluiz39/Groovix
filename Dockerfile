# Dockerfile raiz — só para deploy pelo Console Web do Cloud Run
# (o console usa a raiz do repo como contexto de build).
# Para builds locais/CLI, continue usando server/Dockerfile a partir de server/.
FROM python:3.12-slim

RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg ca-certificates curl nodejs \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /srv
COPY server/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY server/main.py .
EXPOSE 8080
CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port ${PORT:-8080}"]
