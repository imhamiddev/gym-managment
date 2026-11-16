"use client";
import { Input } from "@/components/arc/input/input";
import { jalaliToYmd } from "@/lib/date";

export function JalaliField({
  label = "تاریخ",
  value,
  onChange,
}: {
  label?: string;
  value: string;
  onChange: (v: string) => void;
}) {
  const invalid = value.trim() !== "" && !jalaliToYmd(value);
  return (
    <Input
      label={label}
      dir="ltr"
      inputMode="numeric"
      placeholder="1405/08/26"
      value={value}
      onChange={(e) => onChange(e.target.value)}
      description="تاریخ شمسی به شکل سال/ماه/روز"
      error={invalid ? "تاریخ معتبر نیست" : undefined}
    />
  );
}
