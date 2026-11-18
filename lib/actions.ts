"use server";

import bcrypt from "bcryptjs";
import { redirect } from "next/navigation";
import { revalidatePath } from "next/cache";
import { db } from "./db";
import { createSession, destroySession, getUser } from "./auth";
import { isYmd, minutesTehran, timeToMinutes, toEn, ymdTehran, ymdToDate } from "./date";
import { findCoachGym } from "./coach";
import { safeNext } from "./redirect";
import { verifyCheckinToken } from "./checkin";
import { ALL_DAYS, CODE_RE, cleanCard, isGymDay, isTime } from "./gym";

export type FormState = { error?: string; ok?: boolean };
export type Result = { error?: string };

const PHONE = /^09\d{9}$/;
const clean = (v: FormDataEntryValue | null) => String(v ?? "").trim();
const cleanPhone = (v: FormDataEntryValue | null) => toEn(clean(v)).replace(/[\s-]/g, "");
const DENIED = "دسترسی لازم را ندارید.";

/* ---------- auth ---------- */

export async function register(_: FormState, fd: FormData): Promise<FormState> {
  const firstName = clean(fd.get("firstName"));
  const lastName = clean(fd.get("lastName"));
  const phone = cleanPhone(fd.get("phone"));
  const password = String(fd.get("password") ?? "");
  const role = fd.get("role") === "COACH" ? "COACH" : "ATHLETE";

  if (!firstName || !lastName) return { error: "نام و نام خانوادگی را وارد کنید." };
  if (!PHONE.test(phone)) return { error: "شماره تلفن باید مثل ۰۹۱۲۱۲۳۴۵۶۷ باشد." };
  if (password.length < 6) return { error: "رمز عبور باید حداقل ۶ کاراکتر باشد." };
  if (await db.user.findUnique({ where: { phone } })) return { error: "این شماره قبلاً ثبت‌نام کرده است. وارد شوید." };

  const user = await db.user.create({
    data: { firstName, lastName, phone, role, passwordHash: await bcrypt.hash(password, 10) },
  });
  await createSession(user.id);
  redirect(role === "COACH" ? "/coach" : "/athlete");
}

export async function login(_: FormState, fd: FormData): Promise<FormState> {
  const phone = cleanPhone(fd.get("phone"));
  const password = String(fd.get("password") ?? "");
  const user = await db.user.findUnique({ where: { phone } });
  if (!user || !(await bcrypt.compare(password, user.passwordHash))) {
    return { error: "شماره تلفن یا رمز عبور درست نیست." };
  }
  await createSession(user.id);
  redirect(safeNext(String(fd.get("next") ?? "")) ?? (user.role === "COACH" ? "/coach" : "/athlete"));
}

export async function updateProfile(_: FormState, fd: FormData): Promise<FormState> {
  const user = await getUser();
  if (!user) return { error: DENIED };
  const firstName = clean(fd.get("firstName")).replace(/\s+/g, " ");
  const lastName = clean(fd.get("lastName")).replace(/\s+/g, " ");
  if (!firstName || !lastName) return { error: "نام و نام خانوادگی را وارد کنید." };
  if (firstName.length > 40 || lastName.length > 60) return { error: "نام یا نام خانوادگی بیش از حد طولانی است." };
  await db.user.update({ where: { id: user.id }, data: { firstName, lastName } });
  revalidatePath("/", "layout");
  return { ok: true };
}

export async function logout() {
  await destroySession();
  redirect("/");
}

/* ---------- helpers ---------- */

/** The gym of any coach (owner or assistant). */
async function coachGym() {
  const user = await getUser();
  if (!user || user.role !== "COACH") return null;
  return (await findCoachGym(user.id))?.gym ?? null;
}

/** The gym, only for its owner (settings, plans, coaches). */
async function ownerGym() {
  const user = await getUser();
  if (!user || user.role !== "COACH") return null;
  return db.gym.findUnique({ where: { coachId: user.id } });
}

const ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
function makeCode() {
  let s = "";
  for (const b of crypto.getRandomValues(new Uint8Array(6))) s += ALPHABET[b % ALPHABET.length];
  return s;
}

/* ---------- gym ---------- */

export async function createGym(_: FormState, fd: FormData): Promise<FormState> {
  const user = await getUser();
  if (!user || user.role !== "COACH") return { error: DENIED };
  const name = clean(fd.get("name"));
  if (name.length < 2) return { error: "نام باشگاه را وارد کنید." };
  if (await findCoachGym(user.id)) return { error: "شما قبلاً در یک باشگاه مربی هستید." };

  for (let i = 0; i < 5; i++) {
    try {
      await db.gym.create({ data: { name, code: makeCode(), coachId: user.id } });
      revalidatePath("/coach");
      return {};
    } catch {
      /* code collision, try another */
    }
  }
  return { error: "ساخت باشگاه انجام نشد. دوباره تلاش کنید." };
}

export type GymSettingsInput = {
  name: string;
  code: string;
  workDays: number[];
  openTime: string;
  closeTime: string;
  cardNumber: string;
};

export async function updateGymSettings(input: GymSettingsInput): Promise<Result> {
  const gym = await ownerGym();
  if (!gym) return { error: DENIED };

  const name = input.name.trim().replace(/\s+/g, " ");
  if (name.length < 2) return { error: "نام باشگاه را وارد کنید." };
  if (name.length > 60) return { error: "نام باشگاه بیش از حد طولانی است." };

  const code = toEn(input.code).trim().toUpperCase();
  if (!CODE_RE.test(code)) return { error: "کد دعوت باید ۴ تا ۶ حرف انگلیسی یا عدد باشد." };

  const workDays = [...new Set(input.workDays)].filter((d) => Number.isInteger(d) && ALL_DAYS.includes(d)).sort((a, b) => a - b);
  if (!workDays.length) return { error: "حداقل یک روز برای باشگاه انتخاب کنید." };

  if (!isTime(input.openTime) || !isTime(input.closeTime)) return { error: "ساعت شروع و پایان معتبر نیست." };
  if (input.closeTime <= input.openTime) return { error: "ساعت پایان باید بعد از ساعت شروع باشد." };

  const rawCard = input.cardNumber.trim();
  let cardNumber: string | null = null;
  if (rawCard) {
    cardNumber = cleanCard(rawCard);
    if (!cardNumber) return { error: "شماره کارت باید ۱۶ رقم باشد." };
  }

  if (code !== gym.code && (await db.gym.findUnique({ where: { code } }))) {
    return { error: "این کد دعوت قبلاً استفاده شده است. کد دیگری انتخاب کنید." };
  }

  try {
    await db.gym.update({ where: { id: gym.id }, data: { name, code, workDays, openTime: input.openTime, closeTime: input.closeTime, cardNumber } });
  } catch {
    return { error: "ذخیره تنظیمات انجام نشد. دوباره تلاش کنید." };
  }
  revalidatePath("/coach", "layout");
  revalidatePath("/athlete", "layout");
  return {};
}

/** Picks a fresh random invite code (does not save it; the coach saves with the form). */
export async function suggestGymCode(): Promise<{ code?: string; error?: string }> {
  const gym = await ownerGym();
  if (!gym) return { error: DENIED };
  for (let i = 0; i < 5; i++) {
    const code = makeCode();
    if (!(await db.gym.findUnique({ where: { code } }))) return { code };
  }
  return { error: "کد جدید ساخته نشد. دوباره تلاش کنید." };
}

export async function joinGym(_: FormState, fd: FormData): Promise<FormState> {
  const user = await getUser();
  if (!user || user.role !== "ATHLETE") return { error: DENIED };
  if (await db.membership.findUnique({ where: { athleteId: user.id } })) {
    return { error: "شما قبلاً عضو یک باشگاه هستید یا درخواست فعال دارید." };
  }
  const code = toEn(clean(fd.get("code"))).toUpperCase();
  const gym = code ? await db.gym.findUnique({ where: { code } }) : null;
  if (!gym) return { error: "باشگاهی با این کد پیدا نشد." };

  await db.membership.create({ data: { gymId: gym.id, athleteId: user.id } });
  revalidatePath("/athlete");
  return {};
}

export async function cancelRequest(): Promise<Result> {
  const user = await getUser();
  if (!user) return { error: DENIED };
  await db.membership.deleteMany({ where: { athleteId: user.id, status: "PENDING" } });
  revalidatePath("/athlete");
  return {};
}

export async function respondRequest(membershipId: string, approve: boolean): Promise<Result> {
  const gym = await coachGym();
  if (!gym) return { error: DENIED };
  const m = await db.membership.findFirst({ where: { id: membershipId, gymId: gym.id, status: "PENDING" } });
  if (!m) return { error: "این درخواست پیدا نشد." };
  if (approve) await db.membership.update({ where: { id: m.id }, data: { status: "APPROVED", approvedAt: new Date() } });
  else await db.membership.delete({ where: { id: m.id } });
  revalidatePath("/coach", "layout");
  return {};
}

export async function removeMember(membershipId: string): Promise<Result> {
  const gym = await coachGym();
  if (!gym) return { error: DENIED };
  const { count } = await db.membership.deleteMany({ where: { id: membershipId, gymId: gym.id } });
  if (!count) return { error: "عضو پیدا نشد." };
  revalidatePath("/coach", "layout");
  return {};
}

/* ---------- tuition ---------- */

export type PaymentInput = {
  id?: string;
  membershipId: string;
  amount: number;
  paidYmd: string;
  durationDays: number;
};

export async function savePayment(input: PaymentInput): Promise<Result> {
  const gym = await coachGym();
  if (!gym) return { error: DENIED };
  if (!Number.isInteger(input.amount) || input.amount <= 0 || input.amount > 2_000_000_000) {
    return { error: "مبلغ شهریه را درست وارد کنید." };
  }
  if (![30, 60, 90].includes(input.durationDays)) return { error: "مدت شهریه باید ۳۰، ۶۰ یا ۹۰ روز باشد." };
  if (!isYmd(input.paidYmd)) return { error: "تاریخ پرداخت معتبر نیست." };

  const member = await db.membership.findFirst({
    where: { id: input.membershipId, gymId: gym.id, status: "APPROVED" },
  });
  if (!member) return { error: "عضو انتخاب‌شده پیدا نشد." };

  const data = {
    membershipId: member.id,
    amount: input.amount,
    paidAt: ymdToDate(input.paidYmd),
    durationDays: input.durationDays,
  };
  if (input.id) {
    const own = await db.payment.findFirst({ where: { id: input.id, membership: { gymId: gym.id } } });
    if (!own) return { error: "پرداخت پیدا نشد." };
    await db.payment.update({ where: { id: own.id }, data });
  } else {
    await db.payment.create({ data });
  }
  revalidatePath("/coach", "layout");
  return {};
}

export async function deletePayment(id: string): Promise<Result> {
  const gym = await coachGym();
  if (!gym) return { error: DENIED };
  const { count } = await db.payment.deleteMany({ where: { id, membership: { gymId: gym.id } } });
  if (!count) return { error: "پرداخت پیدا نشد." };
  revalidatePath("/coach", "layout");
  return {};
}

/* ---------- attendance ---------- */

export async function saveAttendance(ymd: string, records: { membershipId: string; present: boolean }[]): Promise<Result> {
  const gym = await coachGym();
  if (!gym) return { error: DENIED };
  if (!isYmd(ymd)) return { error: "تاریخ معتبر نیست." };
  if (!isGymDay(gym.workDays, ymd)) return { error: "باشگاه در این روز فعال نیست و حضور و غیاب ثبت نمی‌شود." };

  const ids = new Set(
    (await db.membership.findMany({ where: { gymId: gym.id, status: "APPROVED" }, select: { id: true } })).map((m) => m.id),
  );
  const valid = records.filter((r) => ids.has(r.membershipId));
  await db.$transaction(
    valid.map((r) =>
      db.attendance.upsert({
        where: { membershipId_date: { membershipId: r.membershipId, date: ymd } },
        update: { present: r.present },
        create: { membershipId: r.membershipId, date: ymd, present: r.present },
      }),
    ),
  );
  revalidatePath("/coach", "layout");
  return {};
}

/* ---------- coaches (owner only) ---------- */

export async function addCoach(phoneInput: string): Promise<Result> {
  const gym = await ownerGym();
  if (!gym) return { error: DENIED };
  const phone = toEn(phoneInput).replace(/[\s-]/g, "");
  if (!PHONE.test(phone)) return { error: "شماره تلفن باید مثل ۰۹۱۲۱۲۳۴۵۶۷ باشد." };
  const target = await db.user.findUnique({ where: { phone } });
  if (!target || target.role !== "COACH") return { error: "مربی‌ای با این شماره پیدا نشد. اول باید با نقش «مربی» ثبت‌نام کند." };
  if (target.id === gym.coachId) return { error: "این شماره صاحب باشگاه است." };
  if (await findCoachGym(target.id)) return { error: "این مربی قبلاً در یک باشگاه عضو است." };
  await db.gymCoach.create({ data: { gymId: gym.id, userId: target.id } });
  revalidatePath("/coach", "layout");
  return {};
}

export async function removeCoach(userId: string): Promise<Result> {
  const gym = await ownerGym();
  if (!gym) return { error: DENIED };
  const { count } = await db.gymCoach.deleteMany({ where: { gymId: gym.id, userId } });
  if (!count) return { error: "مربی پیدا نشد." };
  revalidatePath("/coach", "layout");
  return {};
}

/* ---------- tuition plans (owner only) ---------- */

export type PlanInput = { id?: string; name: string; amount: number; durationDays: number };

export async function savePlan(input: PlanInput): Promise<Result> {
  const gym = await ownerGym();
  if (!gym) return { error: DENIED };
  const name = input.name.trim().replace(/\s+/g, " ");
  if (name.length < 2 || name.length > 40) return { error: "نام پلن باید ۲ تا ۴۰ حرف باشد." };
  if (!Number.isInteger(input.amount) || input.amount <= 0 || input.amount > 2_000_000_000) return { error: "مبلغ پلن را درست وارد کنید." };
  if (![30, 60, 90].includes(input.durationDays)) return { error: "مدت پلن باید ۳۰، ۶۰ یا ۹۰ روز باشد." };
  const data = { name, amount: input.amount, durationDays: input.durationDays };
  if (input.id) {
    const { count } = await db.plan.updateMany({ where: { id: input.id, gymId: gym.id }, data });
    if (!count) return { error: "پلن پیدا نشد." };
  } else {
    if ((await db.plan.count({ where: { gymId: gym.id } })) >= 12) return { error: "حداکثر ۱۲ پلن می‌توانید داشته باشید." };
    await db.plan.create({ data: { gymId: gym.id, ...data } });
  }
  revalidatePath("/coach", "layout");
  return {};
}

export async function deletePlan(id: string): Promise<Result> {
  const gym = await ownerGym();
  if (!gym) return { error: DENIED };
  const { count } = await db.plan.deleteMany({ where: { id, gymId: gym.id } });
  if (!count) return { error: "پلن پیدا نشد." };
  revalidatePath("/coach", "layout");
  return {};
}

/* ---------- member profile (any coach) ---------- */

export type MemberProfileInput = {
  membershipId: string;
  heightCm: string;
  weightKg: string;
  goal: string;
  program: string;
  notes: string;
  birthYmd: string; // "" or Gregorian YYYY-MM-DD
};

export async function updateMemberProfile(input: MemberProfileInput): Promise<Result> {
  const gym = await coachGym();
  if (!gym) return { error: DENIED };

  const num = (v: string) => Number(toEn(v).replace(/[^\d.]/g, ""));
  let heightCm: number | null = null;
  if (input.heightCm.trim()) {
    heightCm = Math.round(num(input.heightCm));
    if (!(heightCm >= 50 && heightCm <= 260)) return { error: "قد باید بین ۵۰ تا ۲۶۰ سانتی‌متر باشد." };
  }
  let weightKg: number | null = null;
  if (input.weightKg.trim()) {
    weightKg = Math.round(num(input.weightKg) * 10) / 10;
    if (!(weightKg >= 20 && weightKg <= 300)) return { error: "وزن باید بین ۲۰ تا ۳۰۰ کیلوگرم باشد." };
  }
  const goal = input.goal.trim();
  const program = input.program.trim();
  const notes = input.notes.trim();
  if (goal.length > 80) return { error: "هدف حداکثر ۸۰ حرف باشد." };
  if (program.length > 2000) return { error: "برنامه حداکثر ۲۰۰۰ حرف باشد." };
  if (notes.length > 1000) return { error: "یادداشت حداکثر ۱۰۰۰ حرف باشد." };

  let birthYmd: string | null = null;
  if (input.birthYmd) {
    if (!isYmd(input.birthYmd) || input.birthYmd > ymdTehran(new Date()) || input.birthYmd < "1900-01-01") return { error: "تاریخ تولد معتبر نیست." };
    birthYmd = input.birthYmd;
  }

  const { count } = await db.membership.updateMany({
    where: { id: input.membershipId, gymId: gym.id, status: "APPROVED" },
    data: { heightCm, weightKg, goal: goal || null, program: program || null, notes: notes || null, birthYmd },
  });
  if (!count) return { error: "عضو پیدا نشد." };
  revalidatePath("/coach", "layout");
  revalidatePath("/athlete", "layout");
  return {};
}

/* ---------- self check-in (athlete scans the gym's daily QR) ---------- */

export async function checkIn(gymId: string, token: string): Promise<{ error?: string; ok?: boolean }> {
  const user = await getUser();
  if (!user || user.role !== "ATHLETE") return { error: DENIED };
  const m = await db.membership.findUnique({ where: { athleteId: user.id }, include: { gym: true } });
  if (!m || m.status !== "APPROVED" || m.gymId !== gymId) return { error: "شما عضو تاییدشده‌ی این باشگاه نیستید." };

  const now = new Date();
  const today = ymdTehran(now);
  if (!verifyCheckinToken(gymId, today, token)) return { error: "این QR منقضی شده است. QR امروز را از مربی بگیرید." };
  if (!isGymDay(m.gym.workDays, today)) return { error: "باشگاه امروز فعال نیست." };
  const minutes = minutesTehran(now);
  if (minutes < timeToMinutes(m.gym.openTime) - 30 || minutes > timeToMinutes(m.gym.closeTime) + 30) {
    return { error: "الان خارج از ساعت کاری باشگاه است." };
  }

  await db.attendance.upsert({
    where: { membershipId_date: { membershipId: m.id, date: today } },
    update: { present: true },
    create: { membershipId: m.id, date: today, present: true },
  });
  revalidatePath("/coach", "layout");
  revalidatePath("/athlete", "layout");
  return { ok: true };
}
