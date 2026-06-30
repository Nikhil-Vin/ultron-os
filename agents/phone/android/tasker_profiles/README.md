# Tasker Profiles for Ultron (Android)

Tasker stores profiles as XML you import via **Tasker → ⋮ → Data → Import**. Because the binary/XML
exports are environment-specific, this folder ships **profile descriptors** you recreate in Tasker
in ~2 minutes each (or hand to Tasker's "Import Description"). All three relay through the Ultron
backend — nothing contact-touching (SMS/call) is included; that stays a Twilio extension point.

Set one global variable first: `%ULTRON_BACKEND` = `http://<laptop-ip>:8080`.

---

## 1. notification_relay.json — Notification Relay
Forwards every status-bar notification to Ultron's webhook so it can be context-filtered.
```json
{
  "profile": "Ultron Notification Relay",
  "event": "Notification (any app)",
  "task": [
    { "action": "HTTP Request", "method": "POST", "url": "%ULTRON_BACKEND/api/webhook",
      "headers": "Content-Type:application/json",
      "body": "{\"source\":\"android\",\"event\":\"notification\",\"payload\":\"%evtprm\"}" }
  ]
}
```

## 2. location_context.json — Location Context
On entering/leaving a known location, push context up (used by proactive nudges).
```json
{
  "profile": "Ultron Location Context",
  "state": "Location (lat,long, radius)",
  "task": [
    { "action": "HTTP Request", "method": "POST", "url": "%ULTRON_BACKEND/api/webhook",
      "headers": "Content-Type:application/json",
      "body": "{\"source\":\"android\",\"event\":\"location\",\"payload\":\"%LOC\"}" }
  ]
}
```

## 3. app_launch.json — App Launch on Command
Lets Ultron open an app on the phone. Triggered by a Tasker "Command" (intent) the backend sends
via FCM/AutoApps. Mutating → only fires when the command carries an approval flag.
```json
{
  "profile": "Ultron App Launch",
  "event": "Command [ ultron_launch ]",
  "task": [
    { "action": "If", "condition": "%apar1 ~ approved" },
    { "action": "Launch App", "app": "%apar2" },
    { "action": "End If" }
  ]
}
```
