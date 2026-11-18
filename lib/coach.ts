import { redirect } from "next/navigation";
import { db } from "./db";
import { requireUser } from "./auth";
import { newCheckinToken } from "./checkin";

/** The gym a coach works in: owned (owner) or joined as an assistant. */
export async function findCoachGym(userId: string) {
  const owned = await db.gym.findUnique({ where: { coachId: userId } });
  if (owned) return { gym: owned, isOwner: true };
  const link = await db.gymCoach.findUnique({ where: { userId }, include: { gym: true } });
  return link ? { gym: link.gym, isOwner: false } : null;
}

/** For coach sub-pages: the coach must already work in a gym. */
export async function requireGym() {
  const user = await requireUser("COACH");
  const found = await findCoachGym(user.id);
  if (!found) redirect("/coach");
  return { ...found.gym, isOwner: found.isOwner };
}

/** Pages only the gym owner may open (settings). */
export async function requireOwnerGym() {
  const gym = await requireGym();
  if (!gym.isOwner) redirect("/coach");
  return gym;
}

/** The gym's check-in token, created on first use. */
export async function ensureCheckinToken(gym: { id: string; checkinToken: string | null }) {
  if (gym.checkinToken) return gym.checkinToken;
  await db.gym.updateMany({ where: { id: gym.id, checkinToken: null }, data: { checkinToken: newCheckinToken() } });
  const fresh = await db.gym.findUnique({ where: { id: gym.id }, select: { checkinToken: true } });
  return fresh!.checkinToken!;
}
