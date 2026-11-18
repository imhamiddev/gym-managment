"use client";

import { AnimatePresence, motion, useReducedMotion } from "motion/react";
import { useRouter } from "next/navigation";
import { useState, useTransition } from "react";
import { FormError } from "@/components/FormError";
import { JalaliField } from "@/components/JalaliField";
import { Avatar } from "@/components/ui/avatar";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { EmptyState } from "@/components/ui/empty-state";
import { FaNumber } from "@/components/ui/fa-number";
import { Icon } from "@/components/ui/icon";
import { useToast } from "@/components/ui/toast";
import { saveAttendance } from "@/lib/actions";
import { jalaliToYmd, toFa, ymdAdd, ymdToJalali, ymdTehran } from "@/lib/date";
import { describeDays, isGymDay, nextGymDay } from "@/lib/gym";
import { cn } from "@/lib/utils";

type Mark = "present" | "absent";

/** Two-state control: a pill glides between حاضر and غایب, and changes color with the state. */
function PresenceToggle({ id, name, value, onChange, disabled }: { id: string; name: string; value: Mark; onChange: (v: Mark) => void; disabled?: boolean }) {
  const reduce = useReducedMotion();
  const options: { v: Mark; label: string; icon: "check" | "close"; pill: string; text: string }[] = [
    { v: "present", label: "حاضر", icon: "check", pill: "bg-success", text: "text-white dark:text-[#06140c]" },
    { v: "absent", label: "غایب", icon: "close", pill: "bg-danger/15", text: "text-danger" },
  ];
  return (
    <div role="radiogroup" aria-label={`حضور ${name}`} aria-disabled={disabled || undefined} className={cn("relative flex shrink-0 rounded-full bg-card p-1", disabled && "pointer-events-none opacity-50")}>
      {options.map((o) => {
        const active = value === o.v;
        return (
          <button
            key={o.v}
            type="button"
            role="radio"
            aria-checked={active}
            disabled={disabled}
            onClick={() => onChange(o.v)}
            className={cn(
              "relative flex h-9 min-w-[4.5rem] items-center justify-center gap-1 rounded-full px-3 text-[13px] font-medium transition-colors active:scale-95",
              active ? o.text : "text-muted-foreground hover:text-foreground",
            )}
          >
            {active ? (
              <motion.span
                layoutId={`pill-${id}`}
                aria-hidden
                transition={reduce ? { duration: 0 } : { type: "spring", stiffness: 520, damping: 34, mass: 0.6 }}
                className={cn("absolute inset-0 rounded-full", o.pill)}
              />
            ) : null}
            <span className="relative inline-flex items-center gap-1">
              {active ? <Icon name={o.icon} size={14} /> : null}
              {o.label}
            </span>
          </button>
        );
      })}
    </div>
  );
}

export function AttendanceBoard({
  ymd,
  workDays,
  members,
  initial,
}: {
  ymd: string;
  workDays: number[];
  members: { id: string; name: string; firstName: string; lastName: string }[];
  initial: Record<string, boolean>;
}) {
  const router = useRouter();
  const toast = useToast();
  const [dateText, setDateText] = useState(ymdToJalali(ymd));
  const [marks, setMarks] = useState<Record<string, Mark>>(() =>
    Object.fromEntries(members.map((m) => [m.id, initial[m.id] ? "present" : "absent"])),
  );
  const [dirty, setDirty] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [pending, start] = useTransition();

  const go = (next: string) => router.push(`/coach/attendance?d=${next}`);
  const change = (text: string) => {
    setDateText(text);
    const next = jalaliToYmd(text);
    if (next && next !== ymd) go(next);
  };
  const setMark = (id: string, v: Mark) => {
    if (!open) return;
    setMarks({ ...marks, [id]: v });
    setDirty(true);
  };
  const presentCount = Object.values(marks).filter((v) => v === "present").length;
  const total = members.length;
  const allPresent = total > 0 && presentCount === total;
  const today = ymdTehran(new Date());
  const open = isGymDay(workDays, ymd);
  const prevDay = nextGymDay(workDays, ymd, -1, ymdAdd);
  const nextDay = nextGymDay(workDays, ymd, 1, ymdAdd);
  // Closest open day that is not in the future, used by the "go to last session" shortcut.
  const lastOpen = isGymDay(workDays, today) ? today : nextGymDay(workDays, today, -1, ymdAdd);

  const save = () =>
    start(async () => {
      if (!open) return;
      setError(null);
      const r = await saveAttendance(ymd, members.map((m) => ({ membershipId: m.id, present: marks[m.id] === "present" })));
      if (r.error) setError(r.error);
      else {
        setDirty(false);
        toast.success("حضور و غیاب ذخیره شد", `${toFa(presentCount)} حاضر از ${toFa(total)} نفر`);
      }
    });

  return (
    <>
      <Card className="gap-0 p-3 shadow-card sm:p-4">
        <div className="flex items-end gap-2">
          <Button variant="secondary" size="icon" aria-label="جلسه قبل" onClick={() => go(prevDay)} className="mb-px size-11">
            <Icon name="chevronRight" size={20} />
          </Button>
          <div className="min-w-0 flex-1">
            <JalaliField label="تاریخ جلسه" value={dateText} onChange={change} hideHint markDay={(d) => isGymDay(workDays, d)} />
          </div>
          <Button variant="secondary" size="icon" aria-label="جلسه بعد" onClick={() => go(nextDay)} className="mb-px size-11">
            <Icon name="chevronLeft" size={20} />
          </Button>
        </div>
        {ymd !== lastOpen ? (
          <div className="mt-3">
            <Button variant="soft" size="sm" onClick={() => go(lastOpen)} className="bg-attendance/10 text-attendance hover:bg-attendance/15">
              <Icon name="calendar" size={16} />
              {lastOpen === today ? "برو به امروز" : "برو به آخرین جلسه"}
            </Button>
          </div>
        ) : null}
      </Card>

      {!open ? (
        <div role="status" className="flex items-start gap-3 rounded-3xl border border-warning/30 bg-warning/10 p-4">
          <span className="grid size-10 shrink-0 place-items-center rounded-2xl bg-warning/15 text-warning">
            <Icon name="lock" size={20} set="bulk" />
          </span>
          <p className="text-sm leading-7">
            باشگاه در این روز فعال نیست و حضور و غیاب قفل است.
            <span className="block text-muted-foreground">روزهای فعال: {describeDays(workDays)}</span>
          </p>
        </div>
      ) : null}

      <Card className="gap-0 overflow-hidden py-0 shadow-card">
        {total === 0 ? (
          <EmptyState icon="attendance" tone="attendance" title="عضوی برای ثبت وجود ندارد" description="برای ثبت حضور و غیاب، ابتدا درخواست یک عضو را تایید کنید." />
        ) : (
          <ul className="divide-y divide-border">
            {members.map((m) => (
              <li key={m.id} className="flex items-center gap-3 px-4 py-3 sm:px-5">
                <Avatar name={m.name} firstName={m.firstName} lastName={m.lastName} size="sm" />
                <span className="min-w-0 flex-1 truncate font-medium">{m.name}</span>
                <PresenceToggle id={m.id} name={m.name} value={marks[m.id]} onChange={(v) => setMark(m.id, v)} disabled={!open} />
              </li>
            ))}
          </ul>
        )}
      </Card>

      <FormError message={error} />

      {total > 0 && open ? (
        <div className="glass sticky bottom-[max(0.75rem,env(safe-area-inset-bottom))] z-20 rounded-3xl border border-border-strong p-3 shadow-card sm:p-4">
          <div className="mb-3 flex items-center justify-between gap-3 text-sm">
            <span className="font-medium">
              <FaNumber value={presentCount} /> حاضر از {toFa(total)} نفر
            </span>
            <AnimatePresence initial={false}>
              {dirty ? (
                <motion.span
                  initial={{ opacity: 0, scale: 0.9 }}
                  animate={{ opacity: 1, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.9 }}
                  className="inline-flex items-center gap-1.5 text-xs text-warning"
                >
                  <span className="size-1.5 rounded-full bg-warning" />
                  ذخیره نشده
                </motion.span>
              ) : null}
            </AnimatePresence>
          </div>
          <div className="mb-3 h-1.5 overflow-hidden rounded-full bg-card">
            <motion.div
              className="h-full origin-right rounded-full bg-success"
              initial={false}
              animate={{ scaleX: total ? presentCount / total : 0 }}
              transition={{ type: "spring", stiffness: 260, damping: 30 }}
            />
          </div>
          <div className="flex gap-2">
            <Button
              variant="secondary"
              className="flex-1 sm:flex-none"
              disabled={allPresent}
              onClick={() => {
                setMarks(Object.fromEntries(members.map((m) => [m.id, "present" as Mark])));
                setDirty(true);
              }}
            >
              همه حاضر
            </Button>
            <Button loading={pending} onClick={save} disabled={!dirty && !pending} className="flex-[2] sm:flex-none">
              <Icon name="check" size={18} />
              ذخیره حضور و غیاب
            </Button>
          </div>
        </div>
      ) : null}
    </>
  );
}
