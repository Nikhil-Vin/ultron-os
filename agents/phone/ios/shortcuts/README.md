# Apple Shortcuts for Ultron (iOS)

Apple Shortcuts export as binary `.shortcut` files signed per-device, so they can't be authored as
plain files here. Recreate these three in the Shortcuts app (each is a few actions), then "Add to
Siri". They call the Ultron backend over your LAN/VPN. Set the backend URL once in a Shortcut named
"Ultron Backend" returning `http://<laptop-ip>:8080`, or hardcode it.

---

## 1. siri_trigger.shortcut — "Hey Siri, Ultron"
- **Trigger:** "Ultron" (Add to Siri phrase)
- Actions:
  1. Dictate Text → `spoken`
  2. Get Contents of URL → POST `{backend}/api/voice/command`
     - Request Body (JSON): `{ "instruction": spoken }`
  3. Get Dictionary Value `spoken` from response
  4. Speak Text → the `spoken` reply
- Result: talk to Ultron hands-free from any iOS device.

## 2. focus_mode.shortcut — "Ultron Focus"
- Actions:
  1. Set Focus → Do Not Disturb → On
  2. Get Contents of URL → POST `{backend}/api/voice/mode` body `{ "mode": "DEEP_WORK" }`
  3. Set Brightness, Open "Things"/your work app
- Result: one tap/Siri phrase puts both the phone and Ultron into deep-work mode.

## 3. health_data.shortcut — "Ultron Health Sync"
- Actions:
  1. Get Health Sample → Sleep Analysis (last night), Steps (today), Resting Heart Rate
  2. Get Contents of URL → POST `{backend}/api/webhook`
     body `{ "source": "ios-health", "event": "health", "payload": "<sleep,steps,hr>" }`
- Result: Ultron sees your recovery metrics (used by the "don't trade on low sleep" rule).
- Automation: schedule daily at 8am (Shortcuts → Automation → Time of Day).

> No Shortcut here sends SMS, places calls, or spends money — those remain deliberate, gated,
> Twilio-extension-point actions per the moat.
