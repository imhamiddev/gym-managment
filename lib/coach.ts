import { redirect } from "next/navigation";
import { db } from "./db";
import { requireUser } from "./auth";

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
