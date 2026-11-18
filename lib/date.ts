import jalaali from "jalaali-js";

const DAY = 86_400_000;
const FA = "۰۱۲۳۴۵۶۷۸۹";

export const toFa = (v: string | number) => String(v).replace(/\d/g, (d) => FA[+d]);
export const toEn = (s: string) =>
  s.replace(/[۰-۹]/g, (d) => String(FA.indexOf(d))).replace(/[٠-٩]/g, (d) => String(d.charCodeAt(0) - 0x0660));

export const formatToman = (n: number) => `${toFa(n.toLocaleString("en-US").replace(/,/g, "٬"))} تومان`;
export const formatDays = (n: number) => `${toFa(n)} روز`;

const pad = (n: number) => String(n).padStart(2, "0");

/** Today's date in Tehran as YYYY-MM-DD */
export function ymdTehran(d: Date) {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Tehran",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(d);
}

export const dayNum = (ymd: string) => Math.floor(Date.parse(`${ymd}T00:00:00Z`) / DAY);
export const ymdAdd = (ymd: string, days: number) => new Date((dayNum(ymd) + days) * DAY).toISOString().slice(0, 10);
export const ymdToDate = (ymd: string) => new Date(`${ymd}T12:00:00Z`);
export const isYmd = (s: string) => /^\d{4}-\d{2}-\d{2}$/.test(s) && !Number.isNaN(Date.parse(`${s}T00:00:00Z`));

/** Gregorian YYYY-MM-DD -> "1405/08/26" (Latin digits, used inside inputs) */
export function ymdToJalali(ymd: string) {
  const [gy, gm, gd] = ymd.split("-").map(Number);
  const j = jalaali.toJalaali(gy, gm, gd);
  return `${j.jy}/${pad(j.jm)}/${pad(j.jd)}`;
}

/** Gregorian YYYY-MM-DD -> "۱۴۰۵/۰۸/۲۶" for display */
export const formatJalali = (ymd: string) => toFa(ymdToJalali(ymd));

/** "1405/8/26" or Persian digits -> Gregorian YYYY-MM-DD, or null if invalid */
export function jalaliToYmd(input: string): string | null {
  const m = toEn(input.trim()).match(/^(\d{4})[/\-.](\d{1,2})[/\-.](\d{1,2})$/);
  if (!m) return null;
  const [jy, jm, jd] = [+m[1], +m[2], +m[3]];
  if (!jalaali.isValidJalaaliDate(jy, jm, jd)) return null;
  const g = jalaali.toGregorian(jy, jm, jd);
  return `${g.gy}-${pad(g.gm)}-${pad(g.gd)}`;
}

/** Days left from today until the latest payment runs out. null = no payment yet. */
export function remainingDays(payments: { paidYmd: string; durationDays: number }[], todayYmd: string) {
  if (!payments.length) return null;
  const end = Math.max(...payments.map((p) => dayNum(p.paidYmd) + p.durationDays));
  return Math.max(0, end - dayNum(todayYmd));
}

/** Weekday of a Gregorian YYYY-MM-DD: 0 = Sunday ... 6 = Saturday. */
export const weekdayOf = (ymd: string) => (((dayNum(ymd) + 4) % 7) + 7) % 7;

/** Current time in Tehran as minutes since midnight. */
export function minutesTehran(d: Date) {
  const [h, m] = new Intl.DateTimeFormat("en-GB", { timeZone: "Asia/Tehran", hour: "2-digit", minute: "2-digit", hour12: false })
    .format(d)
    .split(":")
    .map(Number);
  return (h % 24) * 60 + m;
}

/** "HH:MM" -> minutes since midnight. */
export const timeToMinutes = (t: string) => {
  const [h, m] = t.split(":").map(Number);
  return h * 60 + m;
};

/** True when the Jalali month/day of `birthYmd` matches `todayYmd`. */
export function isBirthday(birthYmd: string | null, todayYmd: string) {
  if (!birthYmd) return false;
  const [by, bm, bd] = birthYmd.split("-").map(Number);
  const [ty, tm, td] = todayYmd.split("-").map(Number);
  const b = jalaali.toJalaali(by, bm, bd);
  const t = jalaali.toJalaali(ty, tm, td);
  return b.jm === t.jm && b.jd === t.jd;
}

/** Full years between a Gregorian birth date and today. */
export function ageOn(birthYmd: string, todayYmd: string) {
  const b = jalaali.toJalaali(...(birthYmd.split("-").map(Number) as [number, number, number]));
  const t = jalaali.toJalaali(...(todayYmd.split("-").map(Number) as [number, number, number]));
  return t.jy - b.jy - (t.jm < b.jm || (t.jm === b.jm && t.jd < b.jd) ? 1 : 0);
}
