# Ultron-OS — n8n Workflows

Importable [n8n](https://n8n.io) workflows that wire external triggers to Ultron's REST API. Import
via **n8n → Workflows → Import from File**. Set an n8n credential / variable `ULTRON_BACKEND` =
`http://<host>:8080`. None of these perform CRITICAL actions — they hit READ/LOW endpoints; anything
consequential still passes Ultron's approval gate.

| File | What it does |
|---|---|
| `morning_brief.json` | 07:00 daily → `POST /api/brief` → posts the brief to Slack/email node |
| `skill_intake_webhook.json` | Webhook → `POST /api/skills` so any tool (Zapier, RSS, etc.) can teach Ultron |

Run n8n itself with: `docker run -it --rm -p 5678:5678 n8nio/n8n` (or add it to compose).
