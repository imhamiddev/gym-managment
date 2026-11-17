import webpush from "web-push";
import { db } from "./db";

export type PushPayload = { title: string; body: string; url?: string; tag?: string };

let configured = false;
export function pushReady() {
  return Boolean(process.env.VAPID_PUBLIC_KEY && process.env.VAPID_PRIVATE_KEY);
}
function configure() {
  if (configured) return;
  webpush.setVapidDetails(
    process.env.VAPID_SUBJECT || "mailto:admin@example.com",
    process.env.VAPID_PUBLIC_KEY!,
    process.env.VAPID_PRIVATE_KEY!,
  );
  configured = true;
}

/** Sends to every device of a user. Dead subscriptions (404/410) are removed. Returns how many were delivered. */
export async function sendPushToUser(userId: string, payload: PushPayload) {
  if (!pushReady()) return 0;
  configure();
  const subs = await db.pushSubscription.findMany({ where: { userId } });
  let sent = 0;
  await Promise.all(
    subs.map(async (s) => {
      try {
        await webpush.sendNotification(
          { endpoint: s.endpoint, keys: { p256dh: s.p256dh, auth: s.auth } },
          JSON.stringify(payload),
          { TTL: 60 * 60 * 12, urgency: "high" },
        );
        sent++;
      } catch (e) {
        const code = (e as { statusCode?: number }).statusCode;
        if (code === 404 || code === 410) await db.pushSubscription.deleteMany({ where: { id: s.id } });
        else console.error("push failed", code ?? e);
      }
    }),
  );
  return sent;
}
