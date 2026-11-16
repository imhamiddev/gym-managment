import { redirect } from "next/navigation";
import { db } from "./db";
import { requireUser } from "./auth";

/** For coach sub-pages: the coach must already have a gym. */
export async function requireGym() {
  const user = await requireUser("COACH");
  const gym = await db.gym.findUnique({ where: { coachId: user.id } });
  if (!gym) redirect("/coach");
  return gym;
}
