/**
 * Device certificate (mTLS) auth (Section 11.7 / 11.8). The agent presents a client certificate to
 * the backend; the backend verifies it against its trust store. Certs live on disk (paths via env),
 * never in code. This module builds the TLS options used for the agent↔backend connection.
 */
import { readFileSync } from "node:fs";
import type { SecureContextOptions } from "node:tls";

export interface DeviceIdentity {
  deviceId: string;
  tls: SecureContextOptions;
}

/**
 * Load the device's mTLS material. Required env:
 *   ULTRON_DEVICE_ID, ULTRON_CLIENT_CERT, ULTRON_CLIENT_KEY, ULTRON_CA_CERT
 * Throws if anything is missing — the agent must not run unauthenticated.
 */
export function loadDeviceIdentity(): DeviceIdentity {
  const deviceId = process.env.ULTRON_DEVICE_ID;
  const certPath = process.env.ULTRON_CLIENT_CERT;
  const keyPath = process.env.ULTRON_CLIENT_KEY;
  const caPath = process.env.ULTRON_CA_CERT;

  if (!deviceId || !certPath || !keyPath || !caPath) {
    throw new Error(
      "mTLS not configured. Set ULTRON_DEVICE_ID, ULTRON_CLIENT_CERT, ULTRON_CLIENT_KEY, ULTRON_CA_CERT. " +
        "Generate device certs with your own CA — the agent refuses to run without them."
    );
  }

  return {
    deviceId,
    tls: {
      cert: readFileSync(certPath),
      key: readFileSync(keyPath),
      ca: readFileSync(caPath),
    },
  };
}
