import { createHmac, timingSafeEqual } from "node:crypto";

function key() {
  const s = process.env.AUTH_SECRET;
  if (!s && process.env.NODE_ENV === "production") throw new Error("AUTH_SECRET is not set");
  return s ?? "dev-only-secret-change-me-in-env-file";
}

/** Daily check-in token for a gym: the QR on the wall changes every Tehran day. */
export const checkinToken = (gymId: string, ymd: string) =>
  createHmac("sha256", key()).update(`checkin:${gymId}:${ymd}`).digest("base64url").slice(0, 22);

export function verifyCheckinToken(gymId: string, ymd: string, token: string) {
  const a = Buffer.from(checkinToken(gymId, ymd));
  const b = Buffer.from(token);
  return a.length === b.length && timingSafeEqual(a, b);
}
