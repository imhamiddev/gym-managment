"use client";

import { Input } from "@/components/ui/input";
import { Icon } from "@/components/ui/icon";
import { FieldHint } from "@/components/ui/field";
import { jalaliToYmd } from "@/lib/date";

export function JalaliField({
  label = "تاریخ",
  value,
  onChange,
  hideHint = false,
}: {
  label?: string;
  value: string;
  onChange: (v: string) => void;
  hideHint?: boolean;
}) {
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
        error={invalid ? "تاریخ معتبر نیست" : undefined}
      />
      {hideHint ? null : <FieldHint>تاریخ شمسی به شکل سال/ماه/روز</FieldHint>}
    </div>
  );
}
