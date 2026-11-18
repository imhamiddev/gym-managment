"use client";

import jalaali from "jalaali-js";
import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import { useMemo, useState } from "react";
import { Icon } from "@/components/ui/icon";
import { jalaliToYmd, toFa, weekdayOf, ymdTehran } from "@/lib/date";
import { cn } from "@/lib/utils";

const MONTHS = ["فروردین", "اردیبهشت", "خرداد", "تیر", "مرداد", "شهریور", "مهر", "آبان", "آذر", "دی", "بهمن", "اسفند"];
// Saturday first, like the Iranian calendar.
const WEEK_HEAD = ["ش", "ی", "د", "س", "چ", "پ", "ج"];
const pad = (n: number) => String(n).padStart(2, "0");

type View = { jy: number; jm: number };

/** Gregorian ymd -> view (jalali year + month). */
function viewOf(ymd: string): View {
  const [gy, gm, gd] = ymd.split("-").map(Number);
  const j = jalaali.toJalaali(gy, gm, gd);
  return { jy: j.jy, jm: j.jm };
}

const ymdOf = (jy: number, jm: number, jd: number) => {
  const g = jalaali.toGregorian(jy, jm, jd);
  return `${g.gy}-${pad(g.gm)}-${pad(g.gd)}`;
};

/**
 * Jalali month calendar. `value` is "1405/08/27" (Latin digits) or "", and `onSelect` receives the
 * same format. `markDay` draws a small dot under days (e.g. the gym's open days).
 */
export function JalaliCalendar({
  value,
  onSelect,
  markDay,
}: {
  value: string;
  onSelect: (v: string) => void;
  markDay?: (ymd: string) => boolean;
}) {
  const today = ymdTehran(new Date());
  const selected = jalaliToYmd(value);
  const [view, setView] = useState<View>(() => viewOf(selected ?? today));
  const [mode, setMode] = useState<"days" | "months">("days");

  const shift = (delta: number) =>
    setView(({ jy, jm }) => {
      const t = jy * 12 + (jm - 1) + delta;
      return { jy: Math.floor(t / 12), jm: (t % 12) + 1 };
    });

  const cells = useMemo(() => {
    const len = jalaali.jalaaliMonthLength(view.jy, view.jm);
    const first = ymdOf(view.jy, view.jm, 1);
    const lead = (weekdayOf(first) + 1) % 7; // Saturday = 0
    return [...Array<null>(lead).fill(null), ...Array.from({ length: len }, (_, i) => i + 1)];
  }, [view]);

  const goToday = () => {
    setView(viewOf(today));
    setMode("days");
    const [gy, gm, gd] = today.split("-").map(Number);
    const j = jalaali.toJalaali(gy, gm, gd);
    onSelect(`${j.jy}/${pad(j.jm)}/${pad(j.jd)}`);
  };

  const navBtn = "grid size-10 place-items-center rounded-full text-muted-foreground transition-[background-color,color,transform] hover:bg-background hover:text-foreground active:scale-90";

  return (
    <div className="rounded-3xl border border-border bg-card p-3" dir="rtl">
      <div className="mb-2 flex items-center justify-between gap-1">
        <button type="button" aria-label={mode === "days" ? "ماه قبل" : "سال قبل"} className={navBtn} onClick={() => (mode === "days" ? shift(-1) : setView((v) => ({ ...v, jy: v.jy - 1 })))}>
          <Icon name="chevronRight" size={18} />
        </button>
        <button
          type="button"
          onClick={() => setMode(mode === "days" ? "months" : "days")}
          aria-label="انتخاب ماه و سال"
          className="inline-flex h-10 items-center gap-1 rounded-full px-3 text-sm font-semibold transition-colors hover:bg-background"
        >
          {mode === "days" ? `${MONTHS[view.jm - 1]} ${toFa(view.jy)}` : toFa(view.jy)}
          <Icon name={mode === "days" ? "chevronDown" : "chevronUp"} size={14} className="text-muted-foreground" />
        </button>
        <button type="button" aria-label={mode === "days" ? "ماه بعد" : "سال بعد"} className={navBtn} onClick={() => (mode === "days" ? shift(1) : setView((v) => ({ ...v, jy: v.jy + 1 })))}>
          <Icon name="chevronLeft" size={18} />
        </button>
      </div>

      {mode === "months" ? (
        <div className="grid grid-cols-3 gap-1.5">
          {MONTHS.map((m, i) => (
            <button
              key={m}
              type="button"
              onClick={() => {
                setView((v) => ({ ...v, jm: i + 1 }));
                setMode("days");
              }}
              className={cn(
                "h-11 rounded-2xl text-sm font-medium transition-[background-color,transform] active:scale-95",
                view.jm === i + 1 ? "bg-primary text-primary-foreground" : "hover:bg-background",
              )}
            >
              {m}
            </button>
          ))}
        </div>
      ) : (
        <>
          <div className="mb-1 grid grid-cols-7 text-center text-xs text-muted-foreground">
            {WEEK_HEAD.map((d) => (
              <span key={d} className="py-1.5">{d}</span>
            ))}
          </div>
          <div className="grid grid-cols-7 gap-y-1">
            {cells.map((d, i) => {
              if (d === null) return <span key={`e${i}`} />;
              const ymd = ymdOf(view.jy, view.jm, d);
              const isSel = ymd === selected;
              const isToday = ymd === today;
              const marked = markDay?.(ymd);
              return (
                <button
                  key={d}
                  type="button"
                  aria-label={`${toFa(d)} ${MONTHS[view.jm - 1]} ${toFa(view.jy)}`}
                  aria-pressed={isSel}
                  onClick={() => onSelect(`${view.jy}/${pad(view.jm)}/${pad(d)}`)}
                  className={cn(
                    "relative mx-auto grid size-10 place-items-center rounded-full text-sm transition-[background-color,transform] active:scale-90",
                    isSel ? "bg-primary font-semibold text-primary-foreground" : "hover:bg-background",
                    isToday && !isSel && "ring-1 ring-primary/60 font-semibold text-primary",
                    markDay && !marked && !isSel && "text-muted-foreground/60",
                  )}
                >
                  {toFa(d)}
                  {marked && !isSel ? <span aria-hidden className="absolute bottom-1 size-1 rounded-full bg-attendance" /> : null}
                </button>
              );
            })}
          </div>
        </>
      )}

      <div className="mt-2 flex justify-center">
        <button type="button" onClick={goToday} className="h-9 rounded-full px-4 text-sm font-medium text-primary transition-colors hover:bg-background">
          امروز
        </button>
      </div>
    </div>
  );
}

/** Wrapper that animates the calendar open/closed under a field. */
export function CalendarReveal({ open, children }: { open: boolean; children: React.ReactNode }) {
  const reduce = useReducedMotion();
  return (
    <AnimatePresence initial={false}>
      {open ? (
        <motion.div
          initial={reduce ? { opacity: 0 } : { opacity: 0, height: 0 }}
          animate={reduce ? { opacity: 1 } : { opacity: 1, height: "auto" }}
          exit={reduce ? { opacity: 0 } : { opacity: 0, height: 0 }}
          transition={{ duration: 0.2 }}
          className="overflow-hidden"
        >
          <div className="pt-1">{children}</div>
        </motion.div>
      ) : null}
    </AnimatePresence>
  );
}
