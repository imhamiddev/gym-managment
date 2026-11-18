import { toEn, toFa, weekdayOf } from "./date";

/** Weekday names, JS numbering (0 = Sunday). */
export const WEEKDAY_NAMES: Record<number, string> = {
  6: "شنبه",
  0: "یک‌شنبه",
  1: "دوشنبه",
  2: "سه‌شنبه",
  3: "چهارشنبه",
  4: "پنج‌شنبه",
  5: "جمعه",
};

/** Iranian week order: Saturday first. */
export const WEEK_ORDER = [6, 0, 1, 2, 3, 4, 5] as const;

/** "Even days" = شنبه، دوشنبه، چهارشنبه. "Odd days" = یک‌شنبه، سه‌شنبه، پنج‌شنبه. */
export const EVEN_DAYS = [6, 1, 3];
export const ODD_DAYS = [0, 2, 4];
export const ALL_DAYS = [0, 1, 2, 3, 4, 5, 6];

export const DAY_PRESETS = [
  { key: "even", label: "روزهای زوج", days: EVEN_DAYS },
  { key: "odd", label: "روزهای فرد", days: ODD_DAYS },
  { key: "all", label: "همه روزها", days: ALL_DAYS },
] as const;

const sameDays = (a: number[], b: number[]) => a.length === b.length && a.every((d) => b.includes(d));

/** Human readable schedule, e.g. "روزهای زوج (شنبه، دوشنبه، چهارشنبه)". */
export function describeDays(days: number[]) {
  const list = WEEK_ORDER.filter((d) => days.includes(d)).map((d) => WEEKDAY_NAMES[d]).join("، ");
  const preset = DAY_PRESETS.find((p) => sameDays(p.days, days));
  if (preset?.key === "all") return "همه روزهای هفته";
  return preset ? `${preset.label} (${list})` : list || "—";
}

/** Is the gym open on this Tehran date? */
export const isGymDay = (workDays: number[], ymd: string) => workDays.includes(weekdayOf(ymd));

/** Nearest gym day before/after `ymd` (step -1 or 1). Falls back to the same day if none. */
export function nextGymDay(workDays: number[], ymd: string, step: 1 | -1, addDays: (ymd: string, n: number) => string) {
  if (!workDays.length) return ymd;
  let cur = ymd;
  for (let i = 0; i < 7; i++) {
    cur = addDays(cur, step);
    if (isGymDay(workDays, cur)) return cur;
  }
  return ymd;
}

export const isTime = (s: string) => /^([01]\d|2[0-3]):[0-5]\d$/.test(s);

/** Any digits (Persian too) -> clean 16-digit string, or null if it isn't one. */
export function cleanCard(input: string) {
  const digits = toEn(input).replace(/[\s\-_.]/g, "");
  return /^\d{16}$/.test(digits) ? digits : null;
}

/** "6037991234567890" -> "۶۰۳۷ ۹۹۱۲ ۳۴۵۶ ۷۸۹۰" */
export const formatCard = (card: string) => toFa(card.replace(/(\d{4})(?=\d)/g, "$1 "));

export const formatHours = (open: string, close: string) => `${toFa(open)} تا ${toFa(close)}`;

export const CODE_RE = /^[A-Z0-9]{4,6}$/;
