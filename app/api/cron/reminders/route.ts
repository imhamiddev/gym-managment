import { NextResponse } from "next/server";
import { dayNum, formatDays, isBirthday, toFa, ymdAdd, ymdTehran } from "@/lib/date";
import { db } from "@/lib/db";
import { pushReady, sendPushToUser } from "@/lib/push";

export const dynamic = "force-dynamic";

const MESSAGES: Record<number, (gym: string) => { title: string; body: string }> = {
  2: (gym) => ({ title: `${toFa(2)} روز تا پایان شهریه`, body: `شهریه شما در «${gym}» تا ${formatDays(2)} دیگر تمام می‌شود.` }),
  1: (gym) => ({ title: "فردا آخرین روز شهریه است", body: `شهریه شما در «${gym}» فردا تمام می‌شود. برای تمدید اقدام کنید.` }),
  0: (gym) => ({ title: "امروز موعد پرداخت شهریه است", body: `شهریه شما در «${gym}» امروز به پایان می‌رسد. لطفاً شهریه را پرداخت کنید.` }),
};

/**
 * Runs once a day from Vercel Cron (see vercel.json). Sends a push to athletes whose tuition
 * ends in 2 days, 1 day, or today. A ReminderLog row makes each reminder go out only once.
 */
export async function GET(req: Request) {
  const secret = process.env.CRON_SECRET;
  if (!secret || req.headers.get("authorization") !== `Bearer ${secret}`) {
    return NextResponse.json({ error: "unauthorized" }, { status: 401 });
  }
  if (!pushReady()) return NextResponse.json({ error: "VAPID keys are not set" }, { status: 503 });

  const today = ymdTehran(new Date());
  const todayNum = dayNum(today);

  const members = await db.membership.findMany({
    where: { status: "APPROVED", athlete: { pushSubscriptions: { some: {} } }, payments: { some: {} } },
    include: { gym: true, payments: true },
  });

  let sent = 0;
  for (const m of members) {
    const endNum = Math.max(...m.payments.map((p) => dayNum(p.paidAt.toISOString().slice(0, 10)) + p.durationDays));
    const daysLeft = endNum - todayNum;
    const make = MESSAGES[daysLeft];
    if (!make) continue;

    const endYmd = ymdAdd("1970-01-01", endNum);
    try {
      await db.reminderLog.create({ data: { membershipId: m.id, endYmd, daysLeft } });
    } catch {
      continue; // unique violation: this reminder was already sent
    }
    const n = await sendPushToUser(m.athleteId, { ...make(m.gym.name), url: "/athlete", tag: `tuition-${daysLeft}` });
    if (n === 0) await db.reminderLog.deleteMany({ where: { membershipId: m.id, endYmd, daysLeft } }); // allow a retry
    sent += n;
  }

  // Birthday greetings (one per member per day; reuses ReminderLog with daysLeft = -100).
  const birthdayMembers = await db.membership.findMany({
    where: { status: "APPROVED", birthYmd: { not: null }, athlete: { pushSubscriptions: { some: {} } } },
    include: { gym: true, athlete: true },
  });
  let greeted = 0;
  for (const m of birthdayMembers) {
    if (!isBirthday(m.birthYmd, today)) continue;
    try {
      await db.reminderLog.create({ data: { membershipId: m.id, endYmd: today, daysLeft: -100 } });
    } catch {
      continue;
    }
    const n = await sendPushToUser(m.athleteId, {
      title: `تولدت مبارک ${m.athlete.firstName}!`,
      body: `«${m.gym.name}» تولدت را تبریک می‌گوید. روز خوبی داشته باشی!`,
      url: "/athlete",
      tag: "birthday",
    });
    if (n === 0) await db.reminderLog.deleteMany({ where: { membershipId: m.id, endYmd: today, daysLeft: -100 } });
    greeted += n;
  }
  return NextResponse.json({ ok: true, day: today, checked: members.length, sent, greeted });
}
