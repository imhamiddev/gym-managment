"use client";

import { useState } from "react";
import { CalendarReveal, JalaliCalendar } from "@/components/JalaliDatePicker";
import { FieldHint } from "@/components/ui/field";
import { Icon } from "@/components/ui/icon";
import { Input } from "@/components/ui/input";
import { jalaliToYmd } from "@/lib/date";

/**
 * Jalali date field: type "1405/08/27" or tap the calendar button to pick from a Jalali calendar.
 * `value` / `onChange` use Latin digits in "yyyy/mm/dd".
 */
export function JalaliField({
  label = "تاریخ",
  value,
  onChange,
  hideHint = false,
  markDay,
}: {
  label?: string;
  value: string;
  onChange: (v: string) => void;
  hideHint?: boolean;
  /** Draws a dot under these days in the calendar (e.g. the gym's open days). */
  markDay?: (ymd: string) => boolean;
}) {
  const [open, setOpen] = useState(false);
  const invalid = value.trim() !== "" && !jalaliToYmd(value);
  return (
    <div className="flex flex-col gap-1.5">
      <Input
        label={label}
        dir="ltr"
        inputMode="numeric"
        placeholder="1405/08/26"
        value={value}
        onChange={onChange}
        leftIcon={<Icon name="calendar" size={18} />}
        rightIcon={
          <button
            type="button"
            aria-label={open ? "بستن تقویم" : "باز کردن تقویم"}
            aria-expanded={open}
            onClick={() => setOpen((o) => !o)}
            className="text-muted-foreground transition-colors hover:text-foreground"
          >
            <Icon name={open ? "chevronUp" : "chevronDown"} size={18} />
          </button>
        }
        error={invalid ? "تاریخ معتبر نیست" : undefined}
      />
      <CalendarReveal open={open}>
        <JalaliCalendar
          value={value}
          markDay={markDay}
          onSelect={(v) => {
            onChange(v);
            setOpen(false);
          }}
        />
      </CalendarReveal>
      {hideHint ? null : <FieldHint>تاریخ شمسی به شکل سال/ماه/روز، یا از تقویم انتخاب کنید</FieldHint>}
    </div>
  );
}
