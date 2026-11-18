"use client";

import { Icon } from "@/components/ui/icon";
import { formatJalali, ymdAdd } from "@/lib/date";
import { cn } from "@/lib/utils";

export const DURATIONS = [30, 60, 90] as const;

/**
 * Compact 30 / 60 / 90 day choice. Three equal cards; the selected one is tinted and shows the date
 * the tuition would run until (when a valid start date is known).
 */
export function DurationPicker({
  value,
  onChange,
  startYmd,
}: {
  value: string;
  onChange: (v: string) => void;
  /** Gregorian start date, used to preview each option's end date. */
  startYmd: string | null;
}) {
  return (
    <div role="radiogroup" aria-label="مدت شهریه" className="grid grid-cols-3 gap-2">
      {DURATIONS.map((d) => {
        const active = value === String(d);
        return (
          <button
            key={d}
            type="button"
            role="radio"
            aria-checked={active}
            onClick={() => onChange(String(d))}
            className={cn(
              "relative flex h-[68px] flex-col items-center justify-center gap-0.5 rounded-2xl border px-2 transition-[background-color,border-color,transform] active:scale-[0.97]",
              active ? "border-tuition bg-tuition/10" : "border-border bg-card hover:border-border-strong",
            )}
          >
            {active ? <Icon name="check" size={14} className="absolute end-2 top-2 text-tuition" /> : null}
            <span className={cn("text-base font-bold leading-6", active && "text-tuition")}>{new Intl.NumberFormat("fa-IR").format(d)} روز</span>
            <span className="text-[11px] leading-5 text-muted-foreground">{startYmd ? `تا ${formatJalali(ymdAdd(startYmd, d))}` : "—"}</span>
          </button>
        );
      })}
    </div>
  );
}
