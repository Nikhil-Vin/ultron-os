import { AccessToken } from "livekit-server-sdk";
import { NextRequest, NextResponse } from "next/server";

// Mints a short-lived LiveKit join token for the HUD viewer. The API secret never leaves the server.
export async function GET(req: NextRequest) {
  const room = req.nextUrl.searchParams.get("room") || process.env.NEXT_PUBLIC_ULTRON_ROOM || "ultron";
  const identity = "hud-" + Math.random().toString(36).slice(2, 10);

  const apiKey = process.env.LIVEKIT_API_KEY;
  const apiSecret = process.env.LIVEKIT_API_SECRET;
  if (!apiKey || !apiSecret) {
    return NextResponse.json({ error: "LiveKit credentials not configured" }, { status: 500 });
  }

  const at = new AccessToken(apiKey, apiSecret, { identity, name: "HUD" });
  at.addGrant({ roomJoin: true, room, canSubscribe: true, canPublish: true, canPublishData: true });

  return NextResponse.json({ token: await at.toJwt(), room });
}
