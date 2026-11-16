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
