# Ultron-OS ai-layer — FastAPI deep-learning services.
# Uses a Python with broad ML wheel availability (3.12) so optional upgrades install cleanly.
FROM python:3.12-slim

WORKDIR /app

# Core deps only by default (keeps the image small + offline-capable). Add optional ML by
# uncommenting entries in requirements.txt before building.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

EXPOSE 8000

# Non-root for safety.
RUN useradd -m ultron && chown -R ultron:ultron /app
USER ultron

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
