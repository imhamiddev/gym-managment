import { randomBytes, timingSafeEqual } from "node:crypto";

/** A fresh secret for the gym's printed check-in QR. */
export const newCheckinToken = () => randomBytes(18).toString("base64url");

export function tokensMatch(expected: string | null | undefined, given: string) {
  if (!expected) return false;
  const a = Buffer.from(expected);
  const b = Buffer.from(given);
  return a.length === b.length && timingSafeEqual(a, b);
}
