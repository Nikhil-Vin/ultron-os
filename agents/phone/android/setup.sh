#!/data/data/com.termux/files/usr/bin/bash
# Ultron Android agent — one-shot Termux setup.
# Usage:  ULTRON_WS=ws://<laptop-ip>:8080/ws/device/android bash setup.sh
set -e

echo "[ultron] installing packages…"
pkg update -y
pkg install -y python termux-api android-tools
pip install --upgrade pip
pip install websocket-client

echo "[ultron] install the 'Termux:API' app (F-Droid) for battery/torch/notifications."

: "${ULTRON_WS:=ws://192.168.1.20:8080/ws/device/android}"
grep -q ULTRON_WS "$HOME/.bashrc" 2>/dev/null || echo "export ULTRON_WS=$ULTRON_WS" >> "$HOME/.bashrc"

echo "[ultron] starting agent (logs → ~/ultron_agent.log)…"
nohup python "$(dirname "$0")/ultron_phone_agent.py" > "$HOME/ultron_agent.log" 2>&1 &
echo "[ultron] done.  tail -f ~/ultron_agent.log   ·   add to Termux:Boot for persistence."
