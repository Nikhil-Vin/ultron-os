# Ultron voice agent. Python 3.12 for broad ML wheel availability.
FROM python:3.12-slim

# System libs: audio I/O (portaudio), soundfile, and ffmpeg for whisper/yt-dlp.
RUN apt-get update && apt-get install -y --no-install-recommends \
    ffmpeg libportaudio2 libsndfile1 \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# Models (Piper voices, openWakeWord, whisper) are downloaded into ./models at first run or
# mounted as a volume — they are intentionally not baked into the image.
ENV ULTRON_BACKEND_URL=http://backend:8080

CMD ["python", "agent.py"]
