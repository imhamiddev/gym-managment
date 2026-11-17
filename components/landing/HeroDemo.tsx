"use client";

import { motion, useReducedMotion } from "motion/react";
import { useState } from "react";
import { Avatar } from "@/components/ui/avatar";
import { Badge, type Tone } from "@/components/ui/badge";
import { FaNumber } from "@/components/ui/fa-number";
import { Icon } from "@/components/ui/icon";
import { cn } from "@/lib/utils";

const SAMPLE: { first: string; last: string; days: string; tone: Tone }[] = [
  { first: "سارا", last: "احمدی", days: "۲۳ روز", tone: "success" },
  { first: "امیر", last: "کریمی", days: "۶ روز", tone: "warning" },
  { first: "نگار", last: "رستمی", days: "۴۱ روز", tone: "success" },
  { first: "رضا", last: "مرادی", days: "منقضی شده", tone: "danger" },
];

/** Tappable sample of the attendance screen, used on the landing page and beside the auth forms. */
export function HeroDemo() {
  const reduce = useReducedMotion();
  const [on, setOn] = useState<boolean[]>([true, false, true, false]);
  const count = on.filter(Boolean).length;
  return (
    <div className="overflow-hidden rounded-[28px] border border-border bg-background shadow-card">
      <div className="flex items-center justify-between border-b border-border px-5 py-4">
        <span className="flex items-center gap-2 font-semibold">
          <span className="grid size-8 place-items-center rounded-xl bg-attendance/12 text-attendance">
            <Icon name="attendance" size={18} set="bulk" />
          </span>
          حضور و غیاب امروز
        </span>
        <span className="text-sm text-muted-foreground"><FaNumber value={count} /> از ۴</span>
      </div>
      <ul className="divide-y divide-border">
        {SAMPLE.map((s, i) => (
          <li key={s.last}>
            <button
              type="button"
              aria-pressed={on[i]}
              onClick={() => setOn((p) => p.map((v, j) => (j === i ? !v : v)))}
              className="flex w-full items-center gap-3 px-5 py-3 text-start transition-colors hover:bg-card active:bg-card"
            >
              <Avatar name={s.first + s.last} firstName={s.first} lastName={s.last} size="sm" />
              <span className="flex-1 font-medium">{s.first} {s.last}</span>
              <Badge tone={s.tone} dot className="hidden sm:inline-flex">{s.days}</Badge>
              <motion.span
                animate={{ scale: on[i] || reduce ? 1 : 0.92 }}
                className={cn("grid size-8 place-items-center rounded-full transition-colors", on[i] ? "bg-success text-white dark:text-[#06140c]" : "bg-card text-muted-foreground")}
              >
                <Icon name={on[i] ? "check" : "close"} size={16} />
              </motion.span>
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
