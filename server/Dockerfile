FROM python:3.12-slim

# node = necessário pro provedor de PO Token (bgutil) gerar o BotGuard local
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg ca-certificates curl nodejs \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /srv
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY main.py .
EXPOSE 8080
# Cloud Run injeta $PORT (padrão 8080); respeita ambos
CMD ["sh", "-c", "uvicorn main:app --host 0.0.0.0 --port ${PORT:-8080}"]
